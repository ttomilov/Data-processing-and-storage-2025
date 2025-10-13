package org.spider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiderTest {

    private static HttpServer server;
    private static int port;

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/", exchange -> {
            String response = "{\"message\":\"root\",\"successors\":[\"/a\"]}";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.createContext("/a", exchange -> {
            String response = "{\"message\":\"a-message\",\"successors\":[\"/b\"]}";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.createContext("/b", exchange -> {
            String response = "{\"message\":\"b-message\",\"successors\":[]}";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        server.start();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testSpiderCrawling() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            Spider spider = new Spider("localhost", port);
            spider.start();

            String output = outContent.toString();
            String expected = """
                    --- Sorted messages ---
                    a-message
                    b-message
                    root""";

            assertTrue(output.contains(expected), "The output should contain the sorted messages.");

        } finally {
            System.setOut(originalOut);
        }
    }
}