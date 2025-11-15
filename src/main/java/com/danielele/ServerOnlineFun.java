package com.danielele;

public interface ServerOnlineFun
{
    Integer getCurrentPlayers();
    Integer getMaxPlayers();
    String getServerTime();
    Integer getQueueSize();
    Boolean isQueueActive();
    Boolean isOnline();
}
