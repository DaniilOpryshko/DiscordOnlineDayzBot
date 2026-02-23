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
        CFToolsResponse.Status status = getStatus();
        if (status == null)
        {
            return 0;
        }
        return status.getPlayers();
    }

    @Override
    public Integer getMaxPlayers()
    {
        CFToolsResponse.Status status = getStatus();
        if (status == null)
        {
            return 0;
        }
        return status.getSlots();
    }

    @Override
    public String getServerTime()
    {
        CFToolsResponse.Environment environment = getEnvironment();
        if (environment == null || environment.getTime() == null || environment.getTime().isBlank())
        {
            return "00:00";
        }
        return environment.getTime();
    }

    @Override
    public Integer getQueueSize()
    {
        CFToolsResponse.Queue queue = getQueue();
        if (queue == null)
        {
            return 0;
        }
        return queue.getSize();
    }

    @Override
    public Boolean isQueueActive()
    {
        CFToolsResponse.Queue queue = getQueue();
        if (queue == null)
        {
            return false;
        }
        return queue.isActive();
    }

    @Override
    public Boolean isOnline()
    {
        if (serverData == null)
        {
            return false;
        }

        if (serverData.isOffline())
        {
            return false;
        }

        if (serverData.isOnline())
        {
            return true;
        }

        return serverData.getStatus() != null;
    }

    private CFToolsResponse.Status getStatus()
    {
        if (serverData == null)
        {
            return null;
        }
        return serverData.getStatus();
    }

    private CFToolsResponse.Environment getEnvironment()
    {
        if (serverData == null)
        {
            return null;
        }
        return serverData.getEnvironment();
    }

    private CFToolsResponse.Queue getQueue()
    {
        CFToolsResponse.Status status = getStatus();
        if (status == null)
        {
            return null;
        }
        return status.getQueue();
    }
}
