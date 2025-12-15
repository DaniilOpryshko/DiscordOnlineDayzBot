package com.danielele.events;

import com.danielele.DiscordBot;

import java.util.List;

public class BotsReadyEvent
{
    private final List<DiscordBot> bots;
    
    public BotsReadyEvent(List<DiscordBot> bots)
    {
        this.bots = bots;
    }

    public List<DiscordBot> getBots()
    {
        return bots;
    }
}