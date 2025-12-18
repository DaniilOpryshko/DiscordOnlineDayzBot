package com.danielele.config;

import com.danielele.config.legacy.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ConfigMigrator
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigMigrator.class);
    private final int targetVersion;

    public ConfigMigrator(int targetVersion)
    {
        this.targetVersion = targetVersion;
    }

    public ConfigService.AppConfig migrate(ConfigService.AppConfig oldConfig)
    {
        logger.info("Starting migration from version {} to {}", oldConfig.version, targetVersion);

        ConfigService.AppConfig newConfig = new ConfigService.AppConfig();
        newConfig.version = targetVersion;

        if (oldConfig.instances != null && !oldConfig.instances.isEmpty())
        {
            logger.info("Found existing instances array, migrating {} instances", oldConfig.instances.size());
            newConfig.instances = oldConfig.instances;
        }
        else
        {
            logger.info("Converting old single-server config to instances array");
            ConfigService.BotInstance instance = createDefaultInstance();
            newConfig.instances = new ArrayList<>();
            newConfig.instances.add(instance);
        }

        logger.info("Migration completed successfully");
        return newConfig;
    }

    public ConfigService.AppConfig migrateFromLegacy(AppConfig legacyConfig)
    {
        logger.info("Starting migration from version {} to {}", legacyConfig.version, targetVersion);

        ConfigService.AppConfig newConfig = new ConfigService.AppConfig();
        newConfig.version = targetVersion;

        ConfigService.BotInstance botInstance = convertLegacyToInstance(legacyConfig);

        newConfig.instances = new ArrayList<>();
        newConfig.instances.add(botInstance);

        logger.info("Migration completed successfully");
        return newConfig;
    }

    private ConfigService.BotInstance convertLegacyToInstance(AppConfig legacy)
    {
        ConfigService.BotInstance instance = new ConfigService.BotInstance();

        instance.server = new ConfigService.ServerConfig();
        instance.server.ip = legacy.server.ip;
        instance.server.port = legacy.server.port;

        instance.emojis = new ConfigService.EmojisConfig();
        instance.emojis.day  = legacy.emojis.day;
        instance.emojis.night = legacy.emojis.night;
        instance.emojis.player = legacy.emojis.player;
        instance.emojis.queue = legacy.emojis.queue;

        instance.updater = new ConfigService.UpdaterConfig();
        instance.updater.intervalSeconds = legacy.updater.intervalSeconds;

        instance.status = new ConfigService.StatusConfig();
        instance.status.message = legacy.status.message;
        instance.status.queueBlock = legacy.status.queueBlock;
        instance.status.showQueueIfNotActive = legacy.status.showQueueIfNotActive;
        instance.status.activityType = legacy.status.activityType;
        instance.status.serverOfflineMessage = legacy.status.serverOfflineMessage;

        instance.discord = new ConfigService.DiscordConfig();
        instance.discord.token = legacy.discord.token;

        return instance;
    }

    /**
     * Создаёт конфигурацию по умолчанию с одним инстансом
     */
    public ConfigService.AppConfig createDefaultConfig()
    {
        ConfigService.AppConfig config = new ConfigService.AppConfig();
        config.version = targetVersion;

        config.instances = new ArrayList<>();
        config.instances.add(createDefaultInstance());

        return config;
    }

    private ConfigService.BotInstance createDefaultInstance()
    {
        ConfigService.BotInstance instance = new ConfigService.BotInstance();

        instance.server = new ConfigService.ServerConfig();
        instance.server.ip = "127.0.0.1";
        instance.server.port = 2302;
        instance.server.steamQueryPort = 27015;
        instance.server.onlineProvider = "CFTOOLS";

        instance.emojis = createDefaultEmojis();
        instance.updater = new ConfigService.UpdaterConfig();
        instance.updater.intervalSeconds = 10;
        instance.status = createDefaultStatus();

        instance.discord = new ConfigService.DiscordConfig();
        instance.discord.token = "YOUR_BOT_TOKEN_HERE";

        return instance;
    }

    private ConfigService.EmojisConfig createDefaultEmojis()
    {
        ConfigService.EmojisConfig emojis = new ConfigService.EmojisConfig();
        emojis.player = "👥";
        emojis.day = "☀️";
        emojis.night = "🌙";
        emojis.queue = "👥";
        return emojis;
    }

    private ConfigService.StatusConfig createDefaultStatus()
    {
        ConfigService.StatusConfig status = new ConfigService.StatusConfig();
        status.message = "${emoji.player} ${online} / ${max} ${emoji.daytime} ${time} ${status.queueBlock} ";
        status.serverOfflineMessage = "Server offline";
        status.queueBlock = "${emoji.queue} ${queue}";
        status.showQueueIfNotActive = true;
        status.activityType = "PLAYING";
        return status;
    }
}