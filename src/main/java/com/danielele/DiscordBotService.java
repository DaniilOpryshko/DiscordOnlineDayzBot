package com.danielele;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
public class DiscordBotService
{
    private static final Logger logger = LoggerFactory.getLogger(DiscordBotService.class);

    @Inject
    ConfigService configService;

    private JDA jda;
    private volatile boolean ready = false;

    @PostConstruct
    public void init()
    {
        try
        {
            String token = configService.getDiscord().token;

            if (token == null || token.equals("YOUR_BOT_TOKEN_HERE"))
            {
                logger.error("========================================");
                logger.error("INVALID TOKEN!");
                logger.error("Please set discord.token in config.json");
                logger.error("Application will shut down in 3 seconds...");
                logger.error("========================================");

                Executors.newSingleThreadExecutor().submit(() ->
                {
                    try
                    {
                        Thread.sleep(3000);
                        logger.info("Shutting down application...");
                        Quarkus.asyncExit(1);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                });

                return;
            }

            logger.info("Connecting to Discord...");

            jda = JDABuilder.create(token, EnumSet.noneOf(GatewayIntent.class))
                    .disableCache(CacheFlag.ACTIVITY,
                            CacheFlag.VOICE_STATE,
                            CacheFlag.EMOJI,
                            CacheFlag.STICKER,
                            CacheFlag.CLIENT_STATUS,
                            CacheFlag.ONLINE_STATUS,
                            CacheFlag.SCHEDULED_EVENTS
                    )
                    .enableIntents(GatewayIntent.GUILD_PRESENCES)
                    .setActivity(Activity.of(getActivityType(configService.getStatus().activityType), "Starting..."))
                    .setBulkDeleteSplittingEnabled(false)
                    .setLargeThreshold(50)
                    .build();

            jda.awaitReady();
            ready = true;

            logger.info("Bot connected successfully");

        }
        catch (Exception e)
        {
            logger.error("Failed to connect: {}", e.getMessage(), e);
        }
    }

    public JDA getClient()
    {
        return jda;
    }

    public void updatePresence(String status)
    {
        if (isReady())
        {
            jda.getPresence().setActivity(Activity.of(getActivityType(configService.getStatus().activityType), status));
        }
    }

    public boolean isReady()
    {
        return ready && jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    void onShutdown(@Observes ShutdownEvent event)
    {
        logger.info("Quarkus ShutdownEvent triggered. Stopping bot gracefully...");
        if (jda != null)
        {
            try
            {
                jda.shutdown();
                if (!jda.awaitShutdown(5, TimeUnit.SECONDS))
                {
                    logger.warn("Timeout waiting for JDA to shut down — forcing close.");
                    jda.shutdownNow();
                }
                else
                {
                    logger.info("Discord bot stopped successfully.");
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (Exception e)
            {
                logger.error("Error during shutdown: {}", e.getMessage());
            }
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
}