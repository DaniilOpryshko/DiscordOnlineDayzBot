package com.danielele.provider.a2s;

import com.danielele.ServerOnlineFun;

public class A2SServerOnline implements ServerOnlineFun
{
    private final A2SServerInfo serverInfo;

    public A2SServerOnline(A2SServerInfo serverInfo)
    {
        this.serverInfo = serverInfo;
    }

    @Override
    public Integer getCurrentPlayers()
    {
        return serverInfo != null ? serverInfo.getPlayers() : null;
    }

    @Override
    public Integer getMaxPlayers()
    {
        return serverInfo != null ? serverInfo.getMaxPlayers() : null;
    }

    @Override
    public String getServerTime()
    {
        return serverInfo != null ? serverInfo.getTime() : null;
    }

    @Override
    public Integer getQueueSize()
    {
        if (serverInfo == null || serverInfo.getQueue() == null)
        {
            return null;
        }

        try
        {
            return Integer.parseInt(serverInfo.getQueue());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    @Override
    public Boolean isQueueActive()
    {
        Integer queueSize = getQueueSize();
        return queueSize != null && queueSize > 0;
    }

    @Override
    public Boolean isOnline()
    {
        return serverInfo != null;
    }
}