package observer.pelijot.craftypoweraction.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import observer.pelijot.craftypoweraction.Configuration;
import observer.pelijot.craftypoweraction.PowerActionAPI;

public class CraftyAPI implements PowerActionAPI {
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Logger logger;
    private final Configuration configuration;

    public CraftyAPI(Logger logger, Configuration configuration) {
        this.logger = logger;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> stop(String server) {
        Optional<String> serverIdentifier = configuration.getPterodactylServerIdentifier(server);
        if (serverIdentifier.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("No unique identifier for server " + server));
        }

        String identifier = serverIdentifier.get();
        logger.info("Stopping server {}", server);
        return makeRequest(identifier, "stop");
    }

    @Override
    public CompletableFuture<Void> start(String server) {
        Optional<String> serverIdentifier = configuration.getPterodactylServerIdentifier(server);
        if (serverIdentifier.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("No unique identifier for server " + server));
        }

        String identifier = serverIdentifier.get();
        logger.info("Starting server {}", server);
        return makeRequest(identifier, "start");
    }

    @Override
    public CompletableFuture<Boolean> isPlayerWhitelisted(String server, String playerName) {
        Optional<String> serverIdentifier = configuration.getPterodactylServerIdentifier(server);
        if (serverIdentifier.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String identifier = serverIdentifier.get();
        String baseUrl = configuration.getPterodactylClientApiBaseURL().orElse("");
        if (baseUrl.isEmpty() || configuration.getPterodactylApiKey().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        HttpRequest request;
        try {
            String body = "{\"path\":\"whitelist.json\"}";
            URI uri = new URI(baseUrl + "/servers/" + identifier + "/files");
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + configuration.getPterodactylApiKey().get())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } catch (URISyntaxException e) {
            return CompletableFuture.failedFuture(e);
        }

        return sendRequest(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        return false;
                    }
                    try {
                        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                        String content = root
                        .getAsJsonObject("data")
                        .get("content")
                        .getAsString();

                        WhitelistEntry[] entries = GSON.fromJson(content, WhitelistEntry[].class);
                        if (entries != null) {
                            for (WhitelistEntry entry : entries) {
                                if (playerName.equalsIgnoreCase(entry.name())) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse whitelist for server {}", server, e);
                    }
                    return false;
                });
    }

    public CompletableFuture<Boolean> exists(String server) {
        Optional<String> serverIdentifier = configuration.getPterodactylServerIdentifier(server);
        if (serverIdentifier.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        String identifier = serverIdentifier.get();

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(configuration.getPterodactylClientApiBaseURL().get() + "/servers/" + identifier))
                    .header("Authorization", "Bearer " + configuration.getPterodactylApiKey().get())
                    .GET()
                    .build();
        } catch (URISyntaxException e) {
            return CompletableFuture.failedFuture(e);
        }

        return sendRequest(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode == 200) {
                        String contentType = response.headers().firstValue("Content-Type").orElse("");
                        return contentType.contains("application/json");
                    } else {
                        return false;
                    }
                });
    }

    private CompletableFuture<Void> makeRequest(String identifier, String action) {
        assert action.equals("start") || action.equals("stop");
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(configuration.getPterodactylClientApiBaseURL().get() + "/servers/" + identifier + "/action/" + action + "_server"))
                    .header("Authorization", "Bearer " + configuration.getPterodactylApiKey().get())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
        } catch (URISyntaxException e) {
            return CompletableFuture.failedFuture(e);
        }

        return sendRequest(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode < 200 || statusCode >= 300) {
                        throw new RuntimeException("Unexpected response code: " + statusCode);
                    }
                });
    }

    private <T> CompletableFuture<HttpResponse<T>> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return httpClient.send(request, handler);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
