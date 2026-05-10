package observer.pelijot.craftypoweraction;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import observer.pelijot.craftypoweraction.configuration.APIType;
import observer.pelijot.craftypoweraction.configuration.PingMethod;
import observer.pelijot.craftypoweraction.configuration.ShutdownBehaviour;

public interface Configuration {
    Map<String, Object> getRawConfig();

    APIType getAPIType();

    ShutdownBehaviour getShutdownBehaviour();

    Optional<String> getPterodactylApiKey();

    Optional<String> getPterodactylClientApiBaseURL();

    Optional<String> getPterodactylServerIdentifier(String serverName);

    Optional<PowerCommands> getPowerCommands(String serverName);

    Optional<String> getWaitingServerName();

    boolean shouldStartWaitingServer();

    PingMethod getPingMethod();

    Duration getMaximumPingDuration();

    Duration getShutdownAfterDuration();

    boolean getRedirectToWaitingServerOnKick();

    Set<String> getAllServers();

    boolean shouldCheckWhitelist(String serverName);

    record PowerCommands(Optional<String> workingDirectory, String start, String stop) {
    }
}
