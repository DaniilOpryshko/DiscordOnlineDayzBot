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
    private static final int CONFIG_VERSION = 5;
    private static final String ENV_SERVER_IP_SUFFIX = "SERVER_IP";
    private static final String ENV_DISCORD_TOKEN_SUFFIX = "DISCORD_TOKEN";
    private static final String ENV_INSTANCE_PREFIX = "INSTANCE_%d_";
    private static final String ENV_GLOBAL_SERVER_IP = "SERVER_IP";
    private static final String ENV_GLOBAL_DISCORD_TOKEN = "DISCORD_TOKEN";

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
                validator.validateAndFix(config);
                applyEnvOverrides(config);
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
                    validator.validateAndFix(loadedConfig);
                    loader.backupAndSave(configFile, loadedConfig);
                }
            }

            validator.validateAndFix(loadedConfig);

            if (legacyConfig != null)
            {
                loader.saveConfig(loadedConfig);
            }

            applyEnvOverrides(loadedConfig);
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
            validator.validateAndFix(config);
            applyEnvOverrides(config);
        }
    }

    private void applyEnvOverrides(AppConfig loadedConfig)
    {
        if (loadedConfig.instances == null || loadedConfig.instances.isEmpty())
        {
            return;
        }

        boolean anyEnvOverride = false;

        for (int i = 0; i < loadedConfig.instances.size(); i++)
        {
            BotInstance instance = loadedConfig.instances.get(i);
            EnvPair envPair = resolveEnvPair(i);

            if (envPair.hasFullPair())
            {
                instance.server.ip = envPair.serverIp;
                instance.discord.token = envPair.discordToken;
                anyEnvOverride = true;

                logger.info("Instance[{}]: using environment variables '{}' and '{}'.",
                        i, envPair.serverIpEnvName, envPair.discordTokenEnvName);
            }
            else if (envPair.hasPartialPair())
            {
                logger.warn("Instance[{}]: partial environment override detected. Set both '{}' and '{}'{} to use ENV mode, or remove both to use JSON values.",
                        i, envPair.serverIpEnvName, envPair.discordTokenEnvName, optionalGlobalHint(i));
            }
            else
            {
                logger.info("Instance[{}]: ENV naming for override is '{}' + '{}'{}.",
                        i, envPair.serverIpEnvName, envPair.discordTokenEnvName, optionalGlobalHint(i));
            }
        }

        if (!anyEnvOverride)
        {
            if (isCloudRunAlikeEnvironment())
            {
                logger.warn("Cloud Run detected and no complete ENV override pairs were found. Define both 'INSTANCE_N_SERVER_IP' and 'INSTANCE_N_DISCORD_TOKEN' (or 'SERVER_IP' + 'DISCORD_TOKEN' for instance 0). Otherwise service will use JSON values baked into the image.");
            }
            logger.info("No complete environment override pairs found. Using JSON configuration for all instances.");
        }
    }

    private boolean isCloudRunAlikeEnvironment()
    {
        return hasValue(System.getenv("K_SERVICE"));
    }

    private String optionalGlobalHint(int instanceIndex)
    {
        if (instanceIndex == 0)
        {
            return " (for instance 0 you can also use '" + ENV_GLOBAL_SERVER_IP + "' and '" + ENV_GLOBAL_DISCORD_TOKEN + "')";
        }
        return "";
    }

    private EnvPair resolveEnvPair(int instanceIndex)
    {
        String instanceServerIpEnv = envKey(instanceIndex, ENV_SERVER_IP_SUFFIX);
        String instanceTokenEnv = envKey(instanceIndex, ENV_DISCORD_TOKEN_SUFFIX);

        String instanceServerIp = readEnv(instanceServerIpEnv);
        String instanceToken = readEnv(instanceTokenEnv);

        if (hasValue(instanceServerIp) || hasValue(instanceToken))
        {
            return new EnvPair(instanceServerIpEnv, instanceTokenEnv, instanceServerIp, instanceToken);
        }

        if (instanceIndex == 0)
        {
            String globalServerIp = readEnv(ENV_GLOBAL_SERVER_IP);
            String globalToken = readEnv(ENV_GLOBAL_DISCORD_TOKEN);
            if (hasValue(globalServerIp) || hasValue(globalToken))
            {
                return new EnvPair(ENV_GLOBAL_SERVER_IP, ENV_GLOBAL_DISCORD_TOKEN, globalServerIp, globalToken);
            }
        }

        return new EnvPair(instanceServerIpEnv, instanceTokenEnv, null, null);
    }

    private String readEnv(String envName)
    {
        String value = System.getenv(envName);
        if (value == null || value.isBlank())
        {
            return null;
        }
        return value;
    }

    private boolean hasValue(String value)
    {
        return value != null && !value.isBlank();
    }

    private String envKey(int instanceIndex, String suffix)
    {
        return String.format(ENV_INSTANCE_PREFIX, instanceIndex) + suffix;
    }

    private static final class EnvPair
    {
        private final String serverIpEnvName;
        private final String discordTokenEnvName;
        private final String serverIp;
        private final String discordToken;

        private EnvPair(String serverIpEnvName, String discordTokenEnvName, String serverIp, String discordToken)
        {
            this.serverIpEnvName = serverIpEnvName;
            this.discordTokenEnvName = discordTokenEnvName;
            this.serverIp = serverIp;
            this.discordToken = discordToken;
        }

        private boolean hasFullPair()
        {
            return serverIp != null && discordToken != null;
        }

        private boolean hasPartialPair()
        {
            return (serverIp != null && discordToken == null)
                    || (serverIp == null && discordToken != null);
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
        public int steamQueryPort;
        public String onlineProvider;
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
