package observer.pelijot.craftypoweraction.online;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import observer.pelijot.craftypoweraction.Configuration;
import observer.pelijot.craftypoweraction.OnlineChecker;

public class CraftyOnlineChecker implements OnlineChecker {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final RegisteredServer server;
    private final Configuration configuration;

    public CraftyOnlineChecker(RegisteredServer server, Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
    }

    @Override
    public CompletableFuture<Void> waitForRunning() {
        return CompletableFuture.supplyAsync(() -> {
            Instant start = Instant.now();

            while (Instant.now().isBefore(start.plus(configuration.getMaximumPingDuration()))) {
                if (checkRunning()) {
                    return null;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }

            throw new CompletionException(new TimeoutException("Max ping duration exceeded"));
        });
    }

    @Override
    public boolean isRunningNow() {
        return checkRunning();
    }

    private boolean checkRunning() {
        String serverName = server.getServerInfo().getName();
        String serverIdentifier = configuration.getPterodactylServerIdentifier(serverName).orElse(null);
        String baseUrl = configuration.getPterodactylClientApiBaseURL().orElse(null);
        String apiKey = configuration.getPterodactylApiKey().orElse(null);
        if (serverIdentifier == null || baseUrl == null || apiKey == null) {
            return false;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(baseUrl + "/servers/" + serverIdentifier + "/stats"))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
        } catch (URISyntaxException e) {
            return false;
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                return false;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement dataElement = root.get("data");
            if (dataElement == null || !dataElement.isJsonObject()) {
                return false;
            }
            JsonElement pingElement = dataElement.getAsJsonObject().get("int_ping_results");
            if (pingElement == null || !pingElement.isJsonPrimitive()) {
                return false;
            }
            return pingElement.getAsBoolean();
        } catch (IOException | InterruptedException | RuntimeException e) {
            return false;
        }
    }
}
