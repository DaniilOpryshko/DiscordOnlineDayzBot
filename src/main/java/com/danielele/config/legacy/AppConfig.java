package com.danielele.config.legacy;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class AppConfig
{
    public Integer version;
    public ServerConfig server;
    public EmojisConfig emojis;
    public UpdaterConfig updater;
    public DiscordConfig discord;
    public StatusConfig status;

    @RegisterForReflection
    public static class ServerConfig
    {
        public String ip;
        public int port;
    }

    @RegisterForReflection
    public static class EmojisConfig
    {
        public String player;
        public String day;
        public String night;
        public String queue;
    }

    @RegisterForReflection
    public static class StatusConfig
    {
        public String message;
        public String queueBlock;
        public boolean showQueueIfNotActive;
        public String activityType;
        public String serverOfflineMessage;
    }

    @RegisterForReflection
    public static class UpdaterConfig
    {
        public int intervalSeconds;
    }

    @RegisterForReflection
    public static class DiscordConfig
    {
        public String token;
    }
}