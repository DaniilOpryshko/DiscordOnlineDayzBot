package com.danielele;

import com.danielele.config.ConfigService;
import com.danielele.events.BotsReadyEvent;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
public class DiscordBotService
{
    private static final Logger logger = LoggerFactory.getLogger(DiscordBotService.class);

    @Inject
    ConfigService configService;
    @Inject
    Event<BotsReadyEvent> botsReadyEvent;

    private List<DiscordBot> bots = new ArrayList<>();

    @PostConstruct
    public void init()
    {
        try
        {
            List<CompletableFuture<DiscordBot>> futures = new ArrayList<>();

            for (ConfigService.BotInstance instance : configService.getInstances())
            {
                CompletableFuture<DiscordBot> future = CompletableFuture.supplyAsync(() ->
                {
                    try
                    {
                        return createBot(instance);
                    }
                    catch (Exception e)
                    {
                        logger.error("Failed to create bot: {}", e.getMessage(), e);
                        return null;
                    }
                });

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            for (CompletableFuture<DiscordBot> future : futures)
            {
                DiscordBot bot = future.join();
                if (bot != null)
                {
                    bots.add(bot);
                }
            }

            logger.info("All bots connected successfully. Total: {}", bots.size());

            botsReadyEvent.fire(new BotsReadyEvent(bots));
        }
        catch (Exception e)
        {
            logger.error("Failed to initialize bots: {}", e.getMessage(), e);
        }
    }

    private DiscordBot createBot(ConfigService.BotInstance instance) throws Exception
    {
        String token = instance.discord.token;

        if (token == null || token.equals("YOUR_BOT_TOKEN_HERE"))
        {
            logger.error("========================================");
            logger.error("INVALID TOKEN!");
            logger.error("Please set discord.token in config.json");
            logger.error("Application will shut down in 5 seconds...");
            logger.error("========================================");

            try (ExecutorService executor = Executors.newSingleThreadExecutor())
            {
                executor.submit(() ->
                {
                    try
                    {
                        Thread.sleep(5000);
                        logger.info("Shutting down application...");
                        Quarkus.asyncExit(1);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        logger.info("Connecting bot to Discord...");

        JDA jda = JDABuilder.create(token, EnumSet.noneOf(GatewayIntent.class))
                .disableCache(CacheFlag.ACTIVITY,
                        CacheFlag.VOICE_STATE,
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ONLINE_STATUS,
                        CacheFlag.SCHEDULED_EVENTS
                )
                .enableIntents(GatewayIntent.GUILD_PRESENCES)
                .setActivity(Activity.of(getActivityType(instance.status.activityType), "Starting..."))
                .setBulkDeleteSplittingEnabled(false)
                .setLargeThreshold(50)
                .build();

        jda.awaitReady();

        DiscordBot discordBot = new DiscordBot(jda, instance);
        logger.info("Bot connected successfully");

        return discordBot;
    }

    void onShutdown(@Observes ShutdownEvent event)
    {
        logger.info("Quarkus ShutdownEvent triggered. Stopping bot gracefully...");

        for (DiscordBot bot : bots)
        {
            JDA jda = bot.getJda();

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