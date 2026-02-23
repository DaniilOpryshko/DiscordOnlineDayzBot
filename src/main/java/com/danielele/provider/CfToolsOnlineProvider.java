package com.danielele.provider;

import com.danielele.ServerOnlineFun;
import com.danielele.config.ConfigService;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@OnlineProviderAnnot(value = OnlineProviderType.CF_TOOLS)
public class CfToolsOnlineProvider implements OnlineProvider
{
    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(CfToolsOnlineProvider.class);
    private static final long FAILURE_LOG_COOLDOWN_MS = 60_000L;

    private final Map<String, String> serverIdCache = new ConcurrentHashMap<>();
    private final Map<String, CFToolsResponse.ServerData> lastKnownServerData = new ConcurrentHashMap<>();
    private final Map<String, Long> nextFailureLogAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> suppressedFailures = new ConcurrentHashMap<>();

    public CfToolsOnlineProvider(WebClient webClient)
    {
        this.webClient = webClient;
    }

    @Override
    public ServerOnlineFun getServerOnline(ConfigService.ServerConfig serverConfig)
    {
        String serverKey = serverConfig.ip + ":" + serverConfig.port;
        String gameServerId = serverIdCache.computeIfAbsent(serverKey, (k) -> toSHA1("1" + serverConfig.ip + serverConfig.port));

        try
        {
            HttpResponse<Buffer> response = webClient.getAbs("https://data.cftools.cloud/v1/gameserver/" + gameServerId)
                    .send().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            if (response.statusCode() != 200)
            {
                return handleFailure(
                        serverKey,
                        "HTTP " + response.statusCode() + " " + response.statusMessage(),
                        null
                );
            }

            if (response.bodyAsJsonObject() == null)
            {
                return handleFailure(serverKey, "empty JSON body", null);
            }

            CFToolsResponse cfToolsResponse = response.bodyAsJsonObject().mapTo(CFToolsResponse.class);
            CFToolsResponse.ServerData server = cfToolsResponse.getServer(gameServerId);
            if (server == null)
            {
                return handleFailure(serverKey, "missing server data in payload", null);
            }

            lastKnownServerData.put(serverKey, server);
            clearFailureState(serverKey);
            return new CfToolsServerOnline(server);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return handleFailure(serverKey, "request interrupted", e);
        }
        catch (Exception e)
        {
            return handleFailure(serverKey, "request failed", e);
        }
    }

    private ServerOnlineFun handleFailure(String serverKey, String reason, Exception exception)
    {
        CFToolsResponse.ServerData cachedServerData = lastKnownServerData.get(serverKey);
        logFailureThrottled(serverKey, reason, exception, cachedServerData != null);
        return new CfToolsServerOnline(cachedServerData);
    }

    private void clearFailureState(String serverKey)
    {
        Long hadFailureWindow = nextFailureLogAt.remove(serverKey);
        Integer suppressed = suppressedFailures.remove(serverKey);
        if (hadFailureWindow != null)
        {
            int suppressedCount = suppressed != null ? suppressed : 0;
            if (suppressedCount > 0)
            {
                logger.info("CFTools recovered for {} ({} repeated errors were suppressed).", serverKey, suppressedCount);
            }
            else
            {
                logger.info("CFTools recovered for {}.", serverKey);
            }
        }
    }

    private void logFailureThrottled(String serverKey, String reason, Exception exception, boolean usingCachedData)
    {
        long now = System.currentTimeMillis();
        Long nextAllowed = nextFailureLogAt.get(serverKey);

        if (nextAllowed == null || now >= nextAllowed)
        {
            int suppressedCount = suppressedFailures.getOrDefault(serverKey, 0);
            String suppressionPart = suppressedCount > 0
                    ? " Suppressed repeated errors: " + suppressedCount + "."
                    : "";
            String cachePart = usingCachedData
                    ? " Using last known data to avoid false offline."
                    : " No cached data available.";

            if (exception == null)
            {
                logger.error("CFTools query failed for {}: {}.{}{}", serverKey, reason, cachePart, suppressionPart);
            }
            else
            {
                logger.error("CFTools query failed for {}: {} ({}: {}).{}{}",
                        serverKey,
                        reason,
                        exception.getClass().getSimpleName(),
                        exception.getMessage(),
                        cachePart,
                        suppressionPart);
            }

            nextFailureLogAt.put(serverKey, now + FAILURE_LOG_COOLDOWN_MS);
            suppressedFailures.put(serverKey, 0);
            return;
        }

        suppressedFailures.merge(serverKey, 1, Integer::sum);
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
}
