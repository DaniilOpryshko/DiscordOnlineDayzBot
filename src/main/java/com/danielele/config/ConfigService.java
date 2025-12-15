package com.danielele.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Startup
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
@ApplicationScoped
public class ConfigService
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_FILE = "OnlineBot_Config.json";
    private static final int CONFIG_VERSION = 4;

    private AppConfig config;
    private final ConfigLoader loader;
    private final ConfigValidator validator;
    private final ConfigMigrator migrator;

    public ConfigService()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);

        this.loader = new ConfigLoader(mapper, CONFIG_FILE);
        this.validator = new ConfigValidator();
        this.migrator = new ConfigMigrator(CONFIG_VERSION);
    }

    @PostConstruct
    public void init()
    {
        logger.info("Initializing configuration...");
        loadConfig();
        logger.info("Configuration loaded successfully");
        printConfig();
    }

    private void loadConfig()
    {
        File configFile = new File(CONFIG_FILE);

        try
        {
            if (!configFile.exists())
            {
                config = migrator.createDefaultConfig();
                loader.saveConfig(config);
                logger.info("Created default {} file", CONFIG_FILE);
                return;
            }

            logger.info("Loading config from file: {}", CONFIG_FILE);

            com.danielele.config.legacy.AppConfig legacyConfig = loadLegacyConfig(configFile);

            AppConfig loadedConfig;

            if (legacyConfig != null)
            {
                loadedConfig = migrator.migrateFromLegacy(legacyConfig);
            }
            else
            {
                loadedConfig = loader.loadFromFile(configFile);

                if (loadedConfig.version == null || loadedConfig.version < CONFIG_VERSION)
                {
                    logger.warn("Config version mismatch: {} < {}", loadedConfig.version, CONFIG_VERSION);
                    loadedConfig = migrator.migrate(loadedConfig);
                    loader.backupAndSave(configFile, loadedConfig);
                }
            }

            validator.validateAndFix(loadedConfig);
            config = loadedConfig;

            logger.info("Config loaded and validated successfully");
        }
        catch (Exception e)
        {
            logger.error("Failed to load config: {}", e.getMessage());
            loader.backupCorruptedConfig(configFile, e);
            config = migrator.createDefaultConfig();
            try
            {
                loader.saveConfig(config);
            }
            catch (IOException ex)
            {
                logger.error("Failed to save default config: {}", ex.getMessage());
            }
        }
    }

    private com.danielele.config.legacy.AppConfig loadLegacyConfig(File configFile)
    {
        try
        {
            return loader.loadLegacyFromFile(configFile);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private void printConfig()
    {
        logger.info("Config version: {}", config.version);
        logger.info("Bot instances configured: {}", config.instances != null ? config.instances.size() : 0);

        if (config.instances != null)
        {
            for (int i = 0; i < config.instances.size(); i++)
            {
                BotInstance inst = config.instances.get(i);
                logger.info("  Instance[{}]: {}:{} (update: {}s)",
                        i, inst.server.ip, inst.server.port, inst.updater.intervalSeconds);
            }
        }
    }


    public List<BotInstance> getInstances()
    {
        return config.instances;
    }

    @RegisterForReflection
    public static class AppConfig
    {
        public Integer version;
        public List<BotInstance> instances = new ArrayList<>();
    }

    @RegisterForReflection
    public static class BotInstance
    {
        public DiscordConfig discord;
        public ServerConfig server;
        public EmojisConfig emojis;
        public UpdaterConfig updater;
        public StatusConfig status;
    }

    @RegisterForReflection
    public static class ServerConfig
    {
        public String ip;
        public int port;
    }

    @RegisterForReflection
    public static class EmojisConfig
    {
        public String player;
        public String day;
        public String night;
        public String queue;
    }

    @RegisterForReflection
    public static class StatusConfig
    {
        public String message;
        public String queueBlock;
        public boolean showQueueIfNotActive;
        public String activityType;
        public String serverOfflineMessage;
    }

    @RegisterForReflection
    public static class UpdaterConfig
    {
        public int intervalSeconds;
    }

    @RegisterForReflection
    public static class DiscordConfig
    {
        public String token;
    }
}