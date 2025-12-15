package com.danielele.config;

import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ConfigValidator
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);

    public void validateAndFix(ConfigService.AppConfig config)
    {
        if (config.instances == null || config.instances.isEmpty())
        {
            logger.warn("No bot instances configured, adding default instance");
            config.instances = java.util.List.of(createDefaultInstance());
        }
        else
        {
            for (int i = 0; i < config.instances.size(); i++)
            {
                logger.debug("Validating instance {}", i);
                validateAndFixInstance(config.instances.get(i), i);
            }
        }
    }

    private void validateAndFixInstance(ConfigService.BotInstance instance, int index)
    {
        ConfigService.BotInstance defaults = createDefaultInstance();

        if (instance.server == null)
        {
            logger.warn("Instance[{}]: Server section missing, using defaults", index);
            instance.server = defaults.server;
        }
        else
        {
            if (instance.server.ip == null || instance.server.ip.isBlank())
            {
                logger.warn("Instance[{}]: Invalid server IP, using default", index);
                instance.server.ip = defaults.server.ip;
            }
            if (instance.server.port <= 0 || instance.server.port > 65535)
            {
                logger.warn("Instance[{}]: Invalid server port '{}', using default", index, instance.server.port);
                instance.server.port = defaults.server.port;
            }
        }

        // Emojis validation
        if (instance.emojis == null)
        {
            logger.warn("Instance[{}]: Emojis section missing, using defaults", index);
            instance.emojis = defaults.emojis;
        }
        else
        {
            if (instance.emojis.player == null || instance.emojis.player.isEmpty())
                instance.emojis.player = defaults.emojis.player;
            if (instance.emojis.day == null || instance.emojis.day.isEmpty())
                instance.emojis.day = defaults.emojis.day;
            if (instance.emojis.night == null || instance.emojis.night.isEmpty())
                instance.emojis.night = defaults.emojis.night;
            if (instance.emojis.queue == null || instance.emojis.queue.isEmpty())
                instance.emojis.queue = defaults.emojis.queue;
        }

        // Updater validation
        if (instance.updater == null)
        {
            logger.warn("Instance[{}]: Updater section missing, using defaults", index);
            instance.updater = defaults.updater;
        }
        else if (instance.updater.intervalSeconds <= 0)
        {
            logger.warn("Instance[{}]: Invalid updater interval '{}', using default",
                    index, instance.updater.intervalSeconds);
            instance.updater.intervalSeconds = defaults.updater.intervalSeconds;
        }

        if (instance.discord == null)
        {
            logger.warn("Instance[{}]: Discord section missing, using defaults", index);
            instance.discord = defaults.discord;
        }
        else if (instance.discord.token == null || instance.discord.token.isBlank() || instance.discord.token.equals("YOUR_BOT_TOKEN_HERE"))
        {
            instance.discord.token = defaults.discord.token;
        }

        // Status validation
        if (instance.status == null)
        {
            logger.warn("Instance[{}]: Status section missing, using defaults", index);
            instance.status = defaults.status;
        }
        else
        {
            if (instance.status.message == null || instance.status.message.isBlank())
            {
                logger.warn("Instance[{}]: Invalid status message, using default", index);
                instance.status.message = defaults.status.message;
            }

            if (instance.status.queueBlock == null)
            {
                logger.warn("Instance[{}]: Invalid queue block, using default", index);
                instance.status.queueBlock = defaults.status.queueBlock;
            }

            if (instance.status.serverOfflineMessage == null || instance.status.serverOfflineMessage.isBlank())
            {
                logger.warn("Instance[{}]: Invalid serverOfflineMessage, using default", index);
                instance.status.serverOfflineMessage = defaults.status.serverOfflineMessage;
            }

            if (instance.status.activityType == null)
            {
                logger.warn("Instance[{}]: No activity type, using default (PLAYING)", index);
                instance.status.activityType = "PLAYING";
            }
            else
            {
                boolean validActivityType = Arrays.stream(Activity.ActivityType.values())
                        .map(Activity.ActivityType::name)
                        .anyMatch(name -> name.equals(instance.status.activityType));

                if (!validActivityType)
                {
                    logger.warn("Instance[{}]: Invalid activity type, using default (PLAYING)", index);
                    instance.status.activityType = "PLAYING";
                }
            }
        }
    }

    private ConfigService.BotInstance createDefaultInstance()
    {
        ConfigService.BotInstance instance = new ConfigService.BotInstance();

        instance.server = new ConfigService.ServerConfig();
        instance.server.ip = "127.0.0.1";
        instance.server.port = 2302;

        instance.emojis = new ConfigService.EmojisConfig();
        instance.emojis.player = "👥";
        instance.emojis.day = "☀️";
        instance.emojis.night = "🌙";
        instance.emojis.queue = "👥";

        instance.updater = new ConfigService.UpdaterConfig();
        instance.updater.intervalSeconds = 10;

        instance.status = new ConfigService.StatusConfig();
        instance.status.message = "${emoji.player} ${online} / ${max} ${emoji.daytime} ${time} ${status.queueBlock} ";
        instance.status.serverOfflineMessage = "Server offline";
        instance.status.queueBlock = "${emoji.queue} ${queue}";
        instance.status.showQueueIfNotActive = true;
        instance.status.activityType = "PLAYING";

        instance.discord = new ConfigService.DiscordConfig();
        instance.discord.token = "YOUR_BOT_TOKEN_HERE";

        return instance;
    }
}