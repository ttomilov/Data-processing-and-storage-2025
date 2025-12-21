package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class KeyGenerationClient {
    public void requestKeys(String name, String host, int port, int delay, boolean exitBeforeRead) throws Exception {
        try (Socket socket = new Socket(host, port);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream()) {

            out.write(name.getBytes());
            out.write(0);
            out.flush();

            System.out.println("Sent request for " + name);

            if (exitBeforeRead) {
                System.out.println("Exiting before reading response (simulating crash)");
                return;
            }

            if (delay > 0) {
                System.out.println("Delaying for " + delay + " seconds...");
                Thread.sleep(delay * 1000L);
            }

            DataInputStream dis = new DataInputStream(in);

            int privateKeyLength = dis.readInt();
            byte[] privateKeyBytes = new byte[privateKeyLength];
            dis.readFully(privateKeyBytes);

            int certLength = dis.readInt();
            byte[] certBytes = new byte[certLength];
            dis.readFully(certBytes);

            saveKeyAndCertificate(name, privateKeyBytes, certBytes);
            System.out.println("Successfully saved keys for: " + name);
        }
    }

    private void saveKeyAndCertificate(String name, byte[] privateKeyBytes, byte[] certBytes) throws Exception {
        Files.write(Paths.get(name + ".key"), privateKeyBytes);
        Files.write(Paths.get(name + ".crt"), certBytes);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: <name> <host> <port> [--delay seconds] [--exit]");
            return;
        }

        String name = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        int delay = 0;
        boolean exit = false;

        for (int i = 3; i < args.length; i++) {
            if ("--delay".equals(args[i]) && i + 1 < args.length) {
                delay = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--exit".equals(args[i])) {
                exit = true;
            }
        }

        new KeyGenerationClient().requestKeys(name, host, port, delay, exit);
    }
}
