package org.spider;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

public class Spider {

    private final String Url;
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final Phaser phaser = new Phaser(1);
    private final Object logLock = new Object();

    private void log(String msg) {
        synchronized (logLock) {
            System.out.println(msg);
        }
    }

    public Spider(String host, int port) {
        this.Url = "http://" + host + ":" + port;
    }

    public void start() {
        try (ExecutorService executor = newVirtualThreadPerTaskExecutor()) {
            crawl(executor, "");
            phaser.arriveAndAwaitAdvance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(messages);
        System.out.println("\n--- Sorted messages ---");
        messages.forEach(System.out::println);
    }

    private void crawl(ExecutorService executor, String path) {
        if (!visited.add(path)) {
            log("Already visited " + path);
        }

        phaser.register();
        executor.submit(() -> {
            try {
                String url = Url.endsWith("/")
                        ? Url + (path.startsWith("/") ? path.substring(1) : path)
                        : Url + (path.startsWith("/") ? path : "/" + path);

                log("Fetching: " + url);

                ServerResponse response = new ServerResponse(url);
                response.getResponse();

                messages.add(response.getMessage());

                if (response.getSuccessors() != null) {
                    for (String next : response.getSuccessors()) {
                        if (next != null && !next.isBlank()) {
                            crawl(executor, next.startsWith("/") ? next : "/" + next);
                        }
                    }
                }
            } catch (Exception e) {
                log("Error on path " + path + ": " + e);
                e.printStackTrace();
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }
}
