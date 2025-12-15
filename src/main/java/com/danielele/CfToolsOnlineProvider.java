package com.danielele;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
@OnlineProviderAnnot(value = OnlineProviderType.CF_TOOLS)
public class CfToolsOnlineProvider implements OnlineProvider
{
    private final WebClient webClient;
    private final ConfigService configService;

    private String gameServerId = null;

    public CfToolsOnlineProvider(WebClient webClient,
                                 ConfigService configService)
    {
        this.webClient = webClient;
        this.configService = configService;
    }

    @Override
    public ServerOnlineFun getServerOnline()
    {
        try
        {
            if (gameServerId == null)
            {
                gameServerId = toSHA1("1" + configService.getServer().ip + configService.getServer().port);
            }

            HttpResponse<Buffer> response = webClient.getAbs("https://data.cftools.cloud/v1/gameserver/" + gameServerId)
                    .send().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);

            if (response.statusCode() != 200)
            {
                return new CfToolsServerOnline(null);
            }

            CFToolsResponse cfToolsResponse = response.bodyAsJsonObject().mapTo(CFToolsResponse.class);
            CFToolsResponse.ServerData server = cfToolsResponse.getServer(gameServerId);

            return new CfToolsServerOnline(server);
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            throw new RuntimeException(e);
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
}
