package com.danielele;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
public class OnlineUpdater
{
    private final DiscordBotService discordBotService;
    private final ConfigService configService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;
    private String gameServerId = null;

    private static final Logger logger = LoggerFactory.getLogger(OnlineUpdater.class);

    @Inject
    Vertx vertx;

    public OnlineUpdater(DiscordBotService discordBotService, ConfigService configService)
    {
        this.discordBotService = discordBotService;
        this.configService = configService;
    }

    @PostConstruct
    public void startScheduler()
    {
        System.setProperty("file.encoding", "UTF-8");
        scheduleUpdate();
        logger.info("Scheduler started with interval: {}s", configService.getUpdater().intervalSeconds);
    }

    private void scheduleUpdate()
    {
        int interval = configService.getUpdater().intervalSeconds;

        if (currentTask != null && !currentTask.isCancelled())
        {
            currentTask.cancel(false);
        }

        currentTask = scheduler.scheduleAtFixedRate(
                this::updateOnlineStats,
                0,
                interval,
                TimeUnit.SECONDS
        );
    }

    void updateOnlineStats()
    {
        if (gameServerId == null)
        {
            gameServerId = toSHA1("1" + configService.getServer().ip + configService.getServer().port);
        }

        if (!discordBotService.isReady())
        {
            return;
        }

        WebClient client = WebClient.create(vertx);

        client.getAbs("https://data.cftools.cloud/v1/gameserver/" + gameServerId)
                .send()
                .onSuccess(resp ->
                {
                    if (resp.statusCode() != 200)
                    {
                        discordBotService.updatePresence("Offline");
                        logger.info("CF Tools API returned {}. Probably server is offline or server ip is incorrect", resp.statusCode());
                        return;
                    }
                    handleResponse(resp.bodyAsJsonObject());
                })
                .onFailure(failure ->
                        {
                            discordBotService.updatePresence("Offline");
                            logger.error("Error: {}", failure.getMessage());
                        }
                );
    }

    private void handleResponse(JsonObject json)
    {
        try
        {
            CFToolsResponse response = json.mapTo(CFToolsResponse.class);
            CFToolsResponse.ServerData server = response.getServer(gameServerId);

            if (server == null)
            {
                logger.error("Server not found: {}", gameServerId);
                return;
            }

            String presence = formatPresenceMessage(server);

            discordBotService.updatePresence(presence);

            logger.info("Updated presence: {}", presence);
        }
        catch (Exception e)
        {
            logger.error("Parsing error: {}", e.getMessage(), e);
        }
    }

    private String formatPresenceMessage(CFToolsResponse.ServerData server)
    {
        String timeEmoji = getTimeEmoji(server.getEnvironment().getTime());

        String message = configService.getStatus().message;

        return message
                .replace("${emoji.player}", configService.getEmojis().player)
                .replace("${online}", String.valueOf(server.getStatus().getPlayers()))
                .replace("${max}", String.valueOf(server.getStatus().getSlots()))
                .replace("${emoji.daytime}", timeEmoji)
                .replace("${time}", server.getEnvironment().getTime())
                .replace("${status.queueBlock}", getQueueBlock(server));
    }

    private String getQueueBlock(CFToolsResponse.ServerData server)
    {
        boolean shouldShow = server.getStatus().getQueue().isActive()
                || configService.getStatus().showQueueIfNotActive;

        if (!shouldShow)
        {
            return "";
        }

        return configService.getStatus().queueBlock
                .replace("${emoji.queue}", configService.getEmojis().queue)
                .replace("${queue}", String.valueOf(server.getStatus().getQueue().getSize()));
    }

    private String getTimeEmoji(String serverTime)
    {
        try
        {
            int hour = Integer.parseInt(serverTime.split(":")[0]);
            if (hour >= 6 && hour < 20)
            {
                return configService.getEmojis().day;
            }
            else
            {
                return configService.getEmojis().night;
            }
        }
        catch (Exception e)
        {
            return configService.getEmojis().day;
        }
    }

    private String toSHA1(String input)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    void onShutdown(@Observes ShutdownEvent event)
    {
        logger.info("Quarkus ShutdownEvent triggered. Stopping scheduler gracefully...");
        if (currentTask != null)
        {
            currentTask.cancel(true);
        }
    }
}