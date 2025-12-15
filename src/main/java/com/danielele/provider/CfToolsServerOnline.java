package com.danielele.provider;

import com.danielele.ServerOnlineFun;

public class CfToolsServerOnline implements ServerOnlineFun
{
    private final CFToolsResponse.ServerData serverData;

    public CfToolsServerOnline(CFToolsResponse.ServerData serverData)
    {
        this.serverData = serverData;
    }

    @Override
    public Integer getCurrentPlayers()
    {
        if (serverData == null) return 0;
        return serverData.getStatus().getPlayers();
    }

    @Override
    public Integer getMaxPlayers()
    {
        if (serverData == null) return 0;
        return serverData.getStatus().getSlots();
    }

    @Override
    public String getServerTime()
    {
        if (serverData == null) return "00:00";
        return serverData.getEnvironment().getTime();
    }

    @Override
    public Integer getQueueSize()
    {
        if (serverData == null) return 0;
        return serverData.getStatus().getQueue().getSize();
    }

    @Override
    public Boolean isQueueActive()
    {
        if (serverData == null) return false;
        return serverData.getStatus().getQueue().isActive();
    }

    @Override
    public Boolean isOnline()
    {
        return serverData != null;
    }
}
