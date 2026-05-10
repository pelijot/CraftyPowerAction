package observer.pelijot.craftypoweraction.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import observer.pelijot.craftypoweraction.Configuration;
import observer.pelijot.craftypoweraction.OnlineChecker;
import observer.pelijot.craftypoweraction.PowerActionAPI;
import observer.pelijot.craftypoweraction.api.CraftyAPI;
import observer.pelijot.craftypoweraction.api.ShellCommandAPI;
import observer.pelijot.craftypoweraction.online.CraftyOnlineChecker;
import observer.pelijot.craftypoweraction.online.PingOnlineChecker;

public class ConfigurationLoader {
    private static ConfigurationLoader instance;
    private final Logger logger;
    private final Path dataDirectory;
    private Configuration configuration;

    public ConfigurationLoader(Logger logger, Path dataDirectory) {
        assert instance == null;
        instance = this;

        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public Configuration getConfiguration() {
        if (configuration == null) {
            this.loadConfiguration();
        }
        return configuration;
    }

    public boolean reload() {
        return loadConfiguration();
    }

    public PowerActionAPI getAPI() throws IllegalArgumentException {
        if (getConfiguration().getAPIType() == APIType.CRAFTY) {
            return new CraftyAPI(logger, getConfiguration());
        }
        if (getConfiguration().getAPIType() == APIType.SHELL) {
            return new ShellCommandAPI(logger, getConfiguration());
        }
        throw new IllegalArgumentException("Unsupported API type: " + getConfiguration().getAPIType());
    }

    public OnlineChecker getOnlineChecker(RegisteredServer server) {
        Configuration configuration = getConfiguration();

        if (configuration.getPingMethod() == PingMethod.CRAFTY) {
            return new CraftyOnlineChecker(server, configuration);
        } else {
            return new PingOnlineChecker(server, configuration);
        }
    }

    /**
     * Loads the configuration and stores it. If loading fails, keeps the previous configuration.
     *
     * @return true if success
     */
    private boolean loadConfiguration() {
        // Create the dataDirectory if it does not exist
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Error creating data directory", e);
                return false;
            }
        }

        // Load the config.yml file from the dataDirectory
        File configurationFile = dataDirectory.resolve("config.yml").toFile();
        if (!configurationFile.exists()) {
            logger.info("Configuration file not found, creating it: {}", configurationFile.getAbsolutePath());
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configurationFile.toPath());
                } else {
                    throw new FileNotFoundException("config.yml not found in resources");
                }
            } catch (IOException e) {
                logger.error("Error creating default configuration", e);
                return false;
            }
        }

        try {
            this.configuration = new YamlConfiguration(configurationFile, logger);
            return true;
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            return false;
        }
    }
}
