package com.danielele;

import com.danielele.events.BotsReadyEvent;
import com.danielele.provider.OnlineProviderType;
import com.danielele.provider.OnlineProviderFactory;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

@Startup
@ApplicationScoped
public class OnlineUpdater
{
    private final OnlineProviderFactory onlineProviderFactory;
    private static final Logger logger = LoggerFactory.getLogger(OnlineUpdater.class);

    private final Map<String, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public OnlineUpdater(OnlineProviderFactory onlineProviderFactory)
    {
        this.onlineProviderFactory = onlineProviderFactory;
    }

    void onBotsReady(@Observes BotsReadyEvent event)
    {
        logger.info("Bots are ready. Starting schedulers for {} bots...", event.getBots().size());

        for (DiscordBot bot : event.getBots())
        {
            startSchedulerForBot(bot);
        }
    }

    private void startSchedulerForBot(DiscordBot bot)
    {
        int interval = bot.getBotInstanceConfig().updater.intervalSeconds;

        String botId = bot.getBotInstanceConfig().server.ip + ":" + bot.getBotInstanceConfig().server.port;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r ->
        {
            Thread t = new Thread(r);
            t.setName("updater-" + botId);
            return t;
        });

        schedulers.put(botId, scheduler);

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                () -> updateOnlineStats(bot),
                0,
                interval,
                TimeUnit.SECONDS
        );

        tasks.put(botId, task);

        logger.info("Scheduler started for bot {}: interval={}s", botId, interval);
    }

    void updateOnlineStats(DiscordBot bot)
    {
        try
        {
            ServerOnlineFun serverOnline = onlineProviderFactory.getStrategy(getProviderType(bot.getBotInstanceConfig().server.onlineProvider)).getServerOnline(bot.getBotInstanceConfig().server);
            bot.updatePresence(serverOnline);
        }
        catch (Exception e)
        {
        logger.error("Error while updating presence for bot {}", bot.getBotInstanceConfig().server.ip, e);
        }
    }

    private OnlineProviderType getProviderType(String provider)
    {
        return OnlineProviderType.fromString(provider.toUpperCase());
    }

    void onShutdown(@Observes ShutdownEvent event)
    {
        logger.info("Stopping all schedulers gracefully...");

        tasks.values().forEach(task -> task.cancel(true));

        schedulers.values().forEach(scheduler -> {
            scheduler.shutdown();
            try
            {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
                {
                    scheduler.shutdownNow();
                }
            }
            catch (InterruptedException e)
            {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });

        logger.info("All schedulers stopped");
    }
}