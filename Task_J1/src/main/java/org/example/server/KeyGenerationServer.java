package org.example.server;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.*;

public class KeyGenerationServer {
    private final int port;
    private final int generationThreads;
    private final PrivateKey caPrivateKey;
    private final X500Name issuerName;

    private final ExecutorService generationExecutor;
    private final BlockingQueue<GenerationTask> generationQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ResponseTask> responseQueue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, KeyPairAndCertificate> keyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ClientRequest>> pendingRequests = new ConcurrentHashMap<>();

    public KeyGenerationServer(int port, int generationThreads, PrivateKey caPrivateKey, X500Name issuerName) {
        this.port = port;
        this.generationThreads = generationThreads;
        this.caPrivateKey = caPrivateKey;
        this.issuerName = issuerName;
        this.generationExecutor = Executors.newFixedThreadPool(generationThreads);
    }

    public void start() throws IOException {
        startGenerationWorkers();
        startResponseWorker();
        startServerSocket();
    }

    private void startGenerationWorkers() {
        for (int i = 0; i < generationThreads; i++) {
            generationExecutor.submit(new GenerationWorker());
        }
    }

    private void startResponseWorker() {
        Thread responseThread = new Thread(new ResponseWorker());
        responseThread.setDaemon(true);
        responseThread.start();
    }

    private void startServerSocket() throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port " + port);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isAcceptable()) {
                    acceptClient(serverChannel, selector);
                } else if (key.isReadable()) {
                    readClientRequest(key);
                }
            }
        }
    }

    private void acceptClient(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new ClientSession());
        System.out.println("Client connected: " + clientChannel.getRemoteAddress());
    }

    private void readClientRequest(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = (ClientSession) key.attachment();
        ByteBuffer buffer = session.getBuffer();

        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            channel.close();
            return;
        }

        if (findZeroByte(buffer)) {
            buffer.flip();
            String clientName = readStringUntilZero(buffer);
            buffer.compact();

            System.out.println("Received request for name: " + clientName);
            handleKeyRequest(clientName, channel, session);
        }
    }

    private boolean findZeroByte(ByteBuffer buffer) {
        for (int i = 0; i < buffer.position(); i++) {
            if (buffer.get(i) == 0) {
                return true;
            }
        }
        return false;
    }

    private String readStringUntilZero(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == 0) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    private void handleKeyRequest(String clientName, SocketChannel channel, ClientSession session) {
        KeyPairAndCertificate cached = keyCache.get(clientName);
        if (cached != null) {
            responseQueue.offer(new ResponseTask(channel, session, cached));
            return;
        }

        ClientRequest request = new ClientRequest(channel, session);
        pendingRequests.compute(clientName, (name, requests) -> {
            if (requests == null) {
                requests = new CopyOnWriteArrayList<>();
                generationQueue.offer(new GenerationTask(clientName));
            }
            requests.add(request);
            return requests;
        });
    }

    private class GenerationWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    GenerationTask task = generationQueue.take();
                    processGenerationTask(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void processGenerationTask(GenerationTask task) {
            try {
                System.out.println("Generating keys for: " + task.clientName());

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(8192);
                KeyPair keyPair = keyGen.generateKeyPair();

                X509Certificate certificate = generateCertificate(task.clientName(), keyPair);

                KeyPairAndCertificate result = new KeyPairAndCertificate(keyPair, certificate);

                keyCache.put(task.clientName(), result);

                sendResultsToPendingClients(task.clientName(), result);

            } catch (Exception e) {
                e.printStackTrace();
                pendingRequests.remove(task.clientName());
            }
        }

        private X509Certificate generateCertificate(String clientName, KeyPair keyPair) throws Exception {
            X500Name subjectName = new X500Name("CN=" + clientName);

            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + 365L * 24 * 60 * 60 * 1000);

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuerName,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    startDate,
                    endDate,
                    subjectName,
                    keyPair.getPublic()
            );

            return new JcaX509CertificateConverter()
                    .getCertificate(certBuilder.build(new JcaContentSignerBuilder("SHA256WithRSA").build(caPrivateKey)));
        }

        private void sendResultsToPendingClients(String clientName, KeyPairAndCertificate result) {
            CopyOnWriteArrayList<ClientRequest> requests = pendingRequests.remove(clientName);
            if (requests != null) {
                for (ClientRequest request : requests) {
                    responseQueue.offer(new ResponseTask(request.channel(), request.session(), result));
                }
            }
        }
    }

    private class ResponseWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ResponseTask task = responseQueue.take();
                    sendResponse(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendResponse(ResponseTask task) throws IOException {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                byte[] privateKeyBytes = task.result.keyPair().getPrivate().getEncoded();
                dos.writeInt(privateKeyBytes.length);
                dos.write(privateKeyBytes);

                byte[] certBytes = task.result.certificate().getEncoded();
                dos.writeInt(certBytes.length);
                dos.write(certBytes);

                dos.flush();
                byte[] responseData = baos.toByteArray();

                ByteBuffer buffer = ByteBuffer.wrap(responseData);
                while (buffer.hasRemaining()) {
                    task.channel().write(buffer);
                }

                System.out.println("Sent keys to client");
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            } finally {
                task.channel().close();
            }
        }
    }

    private record GenerationTask(String clientName) {}
    private record ResponseTask(SocketChannel channel, ClientSession session, KeyPairAndCertificate result) {}
    private record ClientRequest(SocketChannel channel, ClientSession session) {}

    private static class ClientSession {
        private final ByteBuffer buffer = ByteBuffer.allocate(1024);

        public ByteBuffer getBuffer() {
            return buffer;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java KeyGenerationServer <port> <generationThreads> <caKeyFile> <issuerName>");
            System.out.println("Example: java KeyGenerationServer 8080 4 cakey.ser \"CN=MyCA,O=MyOrg,C=US\"");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int generationThreads = Integer.parseInt(args[1]);
        String caKeyFile = args[2];
        String issuerName = args[3];

        PrivateKey caPrivateKey = loadPrivateKey(caKeyFile);
        X500Name issuer = new X500Name(issuerName);

        KeyGenerationServer server = new KeyGenerationServer(port, generationThreads, caPrivateKey, issuer);
        server.start();
    }

    private static PrivateKey loadPrivateKey(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (PrivateKey) ois.readObject();
        }
    }
}

record KeyPairAndCertificate(KeyPair keyPair, X509Certificate certificate) {}