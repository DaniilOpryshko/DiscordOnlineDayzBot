package com.danielele.provider.a2s;

import com.danielele.ServerOnlineFun;
import com.danielele.config.ConfigService;
import com.danielele.provider.OnlineProvider;
import com.danielele.provider.OnlineProviderAnnot;
import com.danielele.provider.OnlineProviderType;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@OnlineProviderAnnot(value = OnlineProviderType.A2S)
public class A2SOnlineProvider implements OnlineProvider
{
    private static final Logger logger = LoggerFactory.getLogger(A2SOnlineProvider.class);

    private static final int TIMEOUT_MS = 5000;
    private static final int BUFFER_SIZE = 4096;

    private static final int HEADER_SIMPLE = 0xFFFFFFFF;
    private static final int HEADER_MULTI  = 0xFFFFFFFE;

    private static final byte[] HEADER =
            {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x54};
    private static final byte[] PAYLOAD =
            "Source Engine Query\0".getBytes(StandardCharsets.US_ASCII);

    private static final byte EDF_PORT      = (byte) 0x80;
    private static final byte EDF_STEAM_ID  = 0x10;
    private static final byte EDF_SOURCE_TV = 0x40;
    private static final byte EDF_KEYWORDS  = 0x20;
    private static final byte EDF_GAME_ID   = 0x01;

    private static final byte TYPE_INFO_RESPONSE      = 0x49;
    private static final byte TYPE_CHALLENGE_RESPONSE = 0x41;

    private static final int MAX_STRING_LENGTH = 4096;

    @Override
    public ServerOnlineFun getServerOnline(ConfigService.ServerConfig serverConfig)
    {
        try
        {
            A2SServerInfo info = queryServerInfo(serverConfig.ip, serverConfig.steamQueryPort);
            return new A2SServerOnline(info);
        }
        catch (Exception e)
        {
            logger.error("Exception while getting server info from A2S: {}, {}. Probably server is offline.",
                    e.getClass().getSimpleName(), e.getMessage());
            return new A2SServerOnline(null);
        }
    }

    private A2SServerInfo queryServerInfo(String ip, int port)
    {
        byte[] lastResponse = null;

        try (DatagramSocket socket = new DatagramSocket())
        {
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress address = InetAddress.getByName(ip);

            lastResponse = sendRequest(socket, address, port, null);

            byte type = getResponseType(lastResponse);
            int retries = 0;

            while (type == TYPE_CHALLENGE_RESPONSE && retries < 2)
            {
                int challenge = readChallenge(lastResponse);
                logger.debug("Received A2S challenge {} from {}:{}", challenge, ip, port);

                lastResponse = sendRequest(socket, address, port, challenge);
                type = getResponseType(lastResponse);
                retries++;
            }

            if (type == TYPE_CHALLENGE_RESPONSE)
            {
                throw new QueryException(
                        "Server keeps responding with challenge to A2S_INFO: " + ip + ":" + port,
                        null
                );
            }

            A2SServerInfo info = parseResponse(lastResponse);
            if (logger.isDebugEnabled())
            {
                logger.debug("A2S_INFO {}:{} -> {}", ip, port, info);
            }

            return info;
        }
        catch (Exception e)
        {
            if (lastResponse != null)
            {
                logRawResponse(lastResponse);
            }

            if (e instanceof SocketTimeoutException)
            {
                throw new QueryException("Server timeout: " + ip + ":" + port, e);
            }

            if (e instanceof UnknownHostException)
            {
                throw new QueryException("Unknown host: " + ip, e);
            }

            throw new QueryException("Query failed: " + e.getMessage(), e);
        }
    }

    private byte[] sendRequest(DatagramSocket socket,
                               InetAddress address,
                               int port,
                               Integer challenge) throws IOException
    {
        byte[] request = buildRequest(challenge);
        DatagramPacket packet =
                new DatagramPacket(request, request.length, address, port);
        socket.send(packet);

        return receiveResponse(socket);
    }

    private byte[] receiveResponse(DatagramSocket socket) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        int length = packet.getLength();
        byte[] data = Arrays.copyOf(buffer, length);

        ByteBuffer headerBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int header = headerBuf.getInt();

        if (header == HEADER_SIMPLE)
        {
            return data;
        }
        else if (header == HEADER_MULTI)
        {
            return receiveMultiPacket(socket, data, headerBuf);
        }
        else
        {
            throw new IOException(
                    String.format("Unknown A2S header: 0x%08X", header)
            );
        }
    }

    private byte[] receiveMultiPacket(DatagramSocket socket,
                                      byte[] firstData,
                                      ByteBuffer headerBuf) throws IOException
    {
        int id = headerBuf.getInt();

        if ((id & 0x80000000) != 0)
        {
            throw new IOException("Compressed multi-packet A2S responses are not supported");
        }

        int totalPackets = Byte.toUnsignedInt(headerBuf.get());
        int packetIndex = Byte.toUnsignedInt(headerBuf.get());
        headerBuf.getShort();

        if (totalPackets <= 0 || totalPackets > 32)
        {
            throw new IOException("Suspicious totalPackets value: " + totalPackets);
        }

        byte[][] fragments = new byte[totalPackets][];
        fragments[packetIndex] = extractPayload(firstData, headerBuf.position());

        int received = 1;

        while (received < totalPackets)
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            int length = packet.getLength();
            byte[] data = Arrays.copyOf(buffer, length);

            ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int header = bb.getInt();
            if (header != HEADER_MULTI)
            {
                logger.warn("Unexpected header in multi-packet: 0x{}", Integer.toHexString(header));
                continue;
            }

            int thisId = bb.getInt();
            if (thisId != id)
            {
                logger.warn("Mismatched multi-packet id: expected {}, got {}", id, thisId);
                continue;
            }

            int thisTotal = Byte.toUnsignedInt(bb.get());
            int thisIndex = Byte.toUnsignedInt(bb.get());
            bb.getShort();

            if (thisTotal != totalPackets)
            {
                logger.warn("Total packet count differs between packets: {} vs {}", totalPackets, thisTotal);
            }

            if (thisIndex < 0 || thisIndex >= totalPackets)
            {
                logger.warn("Invalid multi-packet index: {}", thisIndex);
                continue;
            }

            if (fragments[thisIndex] != null)
            {
                logger.warn("Duplicate multi-packet fragment index: {}", thisIndex);
                continue;
            }

            fragments[thisIndex] = extractPayload(data, bb.position());
            received++;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < totalPackets; i++)
        {
            if (fragments[i] == null)
            {
                throw new IOException("Missing fragment at index " + i + " in multi-packet response");
            }
            baos.write(fragments[i]);
        }

        return baos.toByteArray();
    }

    private byte[] extractPayload(byte[] data, int offset)
    {
        if (offset >= data.length)
        {
            return new byte[0];
        }
        return Arrays.copyOfRange(data, offset, data.length);
    }

    private byte[] buildRequest(Integer challenge)
    {
        int size = HEADER.length + PAYLOAD.length + (challenge != null ? 4 : 0);
        byte[] request = new byte[size];
        int offset = 0;

        System.arraycopy(HEADER, 0, request, offset, HEADER.length);
        offset += HEADER.length;

        System.arraycopy(PAYLOAD, 0, request, offset, PAYLOAD.length);
        offset += PAYLOAD.length;

        if (challenge != null)
        {
            ByteBuffer.wrap(request, offset, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(challenge);
        }

        return request;
    }

    private byte getResponseType(byte[] response)
    {
        ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN);
        buffer.getInt();
        return buffer.get();
    }

    private int readChallenge(byte[] response)
    {
        ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN);
        buffer.getInt();
        byte type = buffer.get();

        if (type != TYPE_CHALLENGE_RESPONSE)
        {
            throw new IllegalStateException(
                    String.format("Expected challenge response (0x41), got 0x%02X", type)
            );
        }

        return buffer.getInt();
    }

    private A2SServerInfo parseResponse(byte[] response)
    {
        ByteBuffer buffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN);

        int header = buffer.getInt();
        if (header != HEADER_SIMPLE)
        {
            throw new IllegalStateException(
                    String.format("Invalid header: expected 0xFFFFFFFF, got 0x%08X", header)
            );
        }

        byte type = buffer.get();
        if (type != TYPE_INFO_RESPONSE)
        {
            throw new IllegalStateException(
                    String.format("Invalid response type: expected 0x49, got 0x%02X", type)
            );
        }

        buffer.get();

        String name = readString(buffer);
        String map = readString(buffer);
        readString(buffer);
        String game = readString(buffer);

        buffer.getShort();

        int players = Byte.toUnsignedInt(buffer.get());
        int maxPlayers = Byte.toUnsignedInt(buffer.get());
        int bots = Byte.toUnsignedInt(buffer.get());

        String serverType = parseServerType(buffer.get());
        String environment = parseEnvironment(buffer.get());

        buffer.get();
        boolean vac = buffer.get() == 1;

        String version = readString(buffer);

        A2SServerInfo serverInfo = new A2SServerInfo(
                name, map, game,
                players, maxPlayers, bots,
                serverType, environment, vac, version
        );

        if (buffer.hasRemaining())
        {
            byte edf = buffer.get();
            parseEDF(buffer, edf, serverInfo);
        }

        return serverInfo;
    }

    private void parseEDF(ByteBuffer buffer, byte edf, A2SServerInfo serverInfo)
    {
        if ((edf & EDF_PORT) != 0 && buffer.remaining() >= 2)
        {
            int port = Short.toUnsignedInt(buffer.getShort());
            serverInfo.setPort(port);
        }

        if ((edf & EDF_STEAM_ID) != 0 && buffer.remaining() >= 8)
        {
            long steamId = buffer.getLong();
            serverInfo.setSteamId(steamId);
        }

        if ((edf & EDF_SOURCE_TV) != 0 && buffer.remaining() >= 2)
        {
            buffer.getShort();
            if (buffer.hasRemaining())
            {
                readString(buffer);
            }
        }

        if ((edf & EDF_KEYWORDS) != 0 && buffer.hasRemaining())
        {
            String s = readString(buffer);
            String[] keywords = s.split(",");
            List<String> list = Arrays.asList(keywords);

            String queue = list.stream()
                    .filter(k -> k.startsWith("lqs"))
                    .findFirst()
                    .orElse(null);

            if (queue != null && queue.length() > 3)
            {
                serverInfo.setQueue(queue.substring(3));
            }

            if (!list.isEmpty())
            {
                serverInfo.setTime(list.getLast());
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("EDF keywords: {}", s);
            }
        }

        if ((edf & EDF_GAME_ID) != 0 && buffer.remaining() >= 8)
        {
            long gameId = buffer.getLong();
            serverInfo.setGameId(gameId);
        }
    }

    private String readString(ByteBuffer buffer)
    {
        StringBuilder sb = new StringBuilder();
        int read = 0;

        while (buffer.hasRemaining() && read < MAX_STRING_LENGTH)
        {
            byte b = buffer.get();
            read++;

            if (b == 0)
            {
                break;
            }

            sb.append((char) (b & 0xFF));
        }

        if (read >= MAX_STRING_LENGTH)
        {
            while (buffer.hasRemaining())
            {
                byte b = buffer.get();
                if (b == 0)
                {
                    break;
                }
            }
        }

        return sb.toString();
    }

    private String parseServerType(byte type)
    {
        return switch (type)
        {
            case 'd' -> "Dedicated";
            case 'l' -> "Non-dedicated";
            case 'p' -> "Proxy";
            default -> "Unknown";
        };
    }

    private String parseEnvironment(byte env)
    {
        return switch (env)
        {
            case 'l' -> "Linux";
            case 'w' -> "Windows";
            case 'm', 'o' -> "Mac";
            default -> "Unknown";
        };
    }

    private void logRawResponse(byte[] response)
    {
        if (!logger.isTraceEnabled())
        {
            return;
        }

        StringBuilder sb = new StringBuilder(response.length * 3);
        for (byte b : response)
        {
            sb.append(String.format("%02X ", b));
        }
        logger.trace("Raw A2S response ({} bytes): {}", response.length, sb);
    }

    public static class QueryException extends RuntimeException
    {
        public QueryException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}