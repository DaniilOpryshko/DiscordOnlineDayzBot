package com.danielele.provider.a2s;

public class A2SServerInfo
{
    private final String name;
    private final String map;
    private final String game;
    private final int players;
    private final int maxPlayers;
    private final int bots;
    private final String serverType;
    private final String environment;
    private final boolean vac;
    private final String version;

    private Integer port;
    private Long steamId;
    private Long gameId;
    private String queue;
    private String time;

    public A2SServerInfo(String name,
                         String map,
                         String game,
                         int players,
                         int maxPlayers,
                         int bots,
                         String serverType,
                         String environment,
                         boolean vac,
                         String version)
    {
        this.name = name;
        this.map = map;
        this.game = game;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.bots = bots;
        this.serverType = serverType;
        this.environment = environment;
        this.vac = vac;
        this.version = version;
    }

    public String getName()
    {
        return name;
    }

    public String getMap()
    {
        return map;
    }

    public String getGame()
    {
        return game;
    }

    public int getPlayers()
    {
        return players;
    }

    public int getMaxPlayers()
    {
        return maxPlayers;
    }

    public int getBots()
    {
        return bots;
    }

    public String getServerType()
    {
        return serverType;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public boolean isVacEnabled()
    {
        return vac;
    }

    public String getVersion()
    {
        return version;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public Long getSteamId()
    {
        return steamId;
    }

    public void setSteamId(Long steamId)
    {
        this.steamId = steamId;
    }

    public Long getGameId()
    {
        return gameId;
    }

    public void setGameId(Long gameId)
    {
        this.gameId = gameId;
    }

    public String getQueue()
    {
        return queue;
    }

    public void setQueue(String queue)
    {
        this.queue = queue;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    @Override
    public String toString()
    {
        return "ServerInfo{" + "\n" +
                "name='" + name + '\'' + "\n" +
                "map='" + map + '\'' + "\n" +
                "game='" + game + '\'' + "\n" +
                "players=" + players + "\n" +
                "maxPlayers=" + maxPlayers + "\n" +
                "bots=" + bots + "\n" +
                "serverType='" + serverType + '\'' + "\n" +
                "environment='" + environment + '\'' + "\n" +
                "vac=" + vac + "\n" +
                "version='" + version + '\'' + "\n" +
                "port=" + port + "\n" +
                "steamId=" + steamId + "\n" +
                "gameId=" + gameId + "\n" +
                "queue='" + queue + '\'' + "\n" +
                "time='" + time + '\'' + "\n" +
                '}';
    }
}
