package com.danielele;

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
        return serverData.getStatus().getPlayers();
    }

    @Override
    public Integer getMaxPlayers()
    {
        return serverData.getStatus().getSlots();
    }

    @Override
    public String getServerTime()
    {
        return serverData.getEnvironment().getTime();
    }

    @Override
    public Integer getQueueSize()
    {
        return serverData.getStatus().getQueue().getSize();
    }

    @Override
    public Boolean isQueueActive()
    {
        return serverData.getStatus().getQueue().isActive();
    }

    @Override
    public Boolean isOnline()
    {
        return serverData != null && serverData.isOnline();
    }


}
