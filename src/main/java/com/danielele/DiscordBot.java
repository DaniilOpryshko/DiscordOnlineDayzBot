package com.danielele;

import com.danielele.config.ConfigService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public class DiscordBot
{
    private final JDA jda;
    private final ConfigService.BotInstance botInstanceConfig;


    public DiscordBot(JDA jda, ConfigService.BotInstance botInstance)
    {
        this.jda = jda;
        this.botInstanceConfig = botInstance;
    }

    public JDA getJda()
    {
        return jda;
    }

    public ConfigService.BotInstance getBotInstanceConfig()
    {
        return botInstanceConfig;
    }

    public void updatePresence(ServerOnlineFun serverOnlineFun)
    {
        String presence = formatPresenceMessage(serverOnlineFun);

        if (jda.getStatus() == JDA.Status.CONNECTED)
        {
            jda.getPresence().setActivity(Activity.of(getActivityType(botInstanceConfig.status.activityType), presence));
        }
    }

    private Activity.ActivityType getActivityType(String type)
    {
        return switch (type)
        {
            case "PLAYING" -> Activity.ActivityType.PLAYING;
            case "LISTENING" -> Activity.ActivityType.LISTENING;
            case "WATCHING" -> Activity.ActivityType.WATCHING;
            case "COMPETING" -> Activity.ActivityType.COMPETING;
            case "CUSTOM_STATUS" -> Activity.ActivityType.CUSTOM_STATUS;
            default -> Activity.ActivityType.PLAYING;
        };
    }

    private String formatPresenceMessage(ServerOnlineFun serverOnlineFun)
    {
        String timeEmoji = getTimeEmoji(serverOnlineFun.getServerTime());

        String message = botInstanceConfig.status.message;

        return message
                .replace("${emoji.player}", botInstanceConfig.emojis.player)
                .replace("${online}", String.valueOf(serverOnlineFun.getCurrentPlayers()))
                .replace("${max}", String.valueOf(serverOnlineFun.getMaxPlayers()))
                .replace("${emoji.daytime}", timeEmoji)
                .replace("${time}", serverOnlineFun.getServerTime())
                .replace("${status.queueBlock}", getQueueBlock(serverOnlineFun));
    }

    private String getQueueBlock(ServerOnlineFun serverOnlineFun)
    {
        boolean shouldShow = serverOnlineFun.isQueueActive()
                || botInstanceConfig.status.showQueueIfNotActive;

        if (!shouldShow)
        {
            return "";
        }

        return botInstanceConfig.status.queueBlock
                .replace("${emoji.queue}", botInstanceConfig.emojis.queue)
                .replace("${queue}", String.valueOf(serverOnlineFun.getQueueSize()));
    }

    private String getTimeEmoji(String serverTime)
    {
        try
        {
            int hour = Integer.parseInt(serverTime.split(":")[0]);
            if (hour >= 6 && hour < 20)
            {
                return botInstanceConfig.emojis.day;
            }
            else
            {
                return botInstanceConfig.emojis.night;
            }
        }
        catch (Exception e)
        {
            return botInstanceConfig.emojis.day;
        }
    }
}
