package org.spider;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ServerResponse {
    private static final Gson gson = new Gson();

    private final String url;
    private String message;
    private List<String> successors;
    private final HttpClient client;

    public ServerResponse(String url) {
        this(url, HttpClient.newHttpClient());
    }

    ServerResponse(String url, HttpClient client) {
        this.url = url;
        this.client = client;
    }

    public void getResponse() {
        try {
            URI uri = URI.create(url);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ResponseData data = gson.fromJson(response.body(), ResponseData.class);
            this.message = data.message;
            this.successors = data.successors;

            System.out.println("Got response from " + url);

        } catch (Exception e) {
            System.err.println("Failed to fetch " + url + ": " + e.getMessage());
        }
    }

    public String getMessage() {
        return message;
    }

    public List<String> getSuccessors() {
        return successors;
    }

    private static class ResponseData {
        String message;
        List<String> successors;
    }
}