package com.danielele.provider;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterForReflection
public class CFToolsResponse
{

    private Map<String, ServerData> servers = new HashMap<>();
    private boolean status;

    @JsonAnySetter
    public void setServer(String key, ServerData value)
    {
        servers.put(key, value);
    }

    public ServerData getServer(String serverGameId)
    {
        return servers.get(serverGameId);
    }

    public boolean isStatus()
    {
        return status;
    }

    public void setStatus(boolean status)
    {
        this.status = status;
    }

    public static class ServerData
    {
        @JsonProperty("_object")
        private ObjectInfo objectInfo;
        private Attributes attributes;
        private Environment environment;
        private int game;
        private Geolocation geolocation;
        private Host host;
        private String map;
        private List<Mod> mods;
        private String name;
        private boolean offline;
        private boolean online;
        private Publisher publisher;
        private int rank;
        private String rating;
        private Security security;
        private List<String> signatures;
        private Status status;
        private String version;

        public ObjectInfo getObjectInfo()
        {
            return objectInfo;
        }

        public void setObjectInfo(ObjectInfo objectInfo)
        {
            this.objectInfo = objectInfo;
        }

        public Attributes getAttributes()
        {
            return attributes;
        }

        public void setAttributes(Attributes attributes)
        {
            this.attributes = attributes;
        }

        public Environment getEnvironment()
        {
            return environment;
        }

        public void setEnvironment(Environment environment)
        {
            this.environment = environment;
        }

        public int getGame()
        {
            return game;
        }

        public void setGame(int game)
        {
            this.game = game;
        }

        public Geolocation getGeolocation()
        {
            return geolocation;
        }

        public void setGeolocation(Geolocation geolocation)
        {
            this.geolocation = geolocation;
        }

        public Host getHost()
        {
            return host;
        }

        public void setHost(Host host)
        {
            this.host = host;
        }

        public String getMap()
        {
            return map;
        }

        public void setMap(String map)
        {
            this.map = map;
        }

        public List<Mod> getMods()
        {
            return mods;
        }

        public void setMods(List<Mod> mods)
        {
            this.mods = mods;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public boolean isOffline()
        {
            return offline;
        }

        public void setOffline(boolean offline)
        {
            this.offline = offline;
        }

        public boolean isOnline()
        {
            return online;
        }

        public void setOnline(boolean online)
        {
            this.online = online;
        }

        public Publisher getPublisher()
        {
            return publisher;
        }

        public void setPublisher(Publisher publisher)
        {
            this.publisher = publisher;
        }

        public int getRank()
        {
            return rank;
        }

        public void setRank(int rank)
        {
            this.rank = rank;
        }

        public String getRating()
        {
            return rating;
        }

        public void setRating(String rating)
        {
            this.rating = rating;
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            this.security = security;
        }

        public List<String> getSignatures()
        {
            return signatures;
        }

        public void setSignatures(List<String> signatures)
        {
            this.signatures = signatures;
        }

        public Status getStatus()
        {
            return status;
        }

        public void setStatus(Status status)
        {
            this.status = status;
        }

        public String getVersion()
        {
            return version;
        }

        public void setVersion(String version)
        {
            this.version = version;
        }
    }

    public static class ObjectInfo
    {
        @JsonProperty("created_at")
        private String createdAt;
        private String error;
        @JsonProperty("updated_at")
        private String updatedAt;

        public String getCreatedAt()
        {
            return createdAt;
        }

        public void setCreatedAt(String createdAt)
        {
            this.createdAt = createdAt;
        }

        public String getError()
        {
            return error;
        }

        public void setError(String error)
        {
            this.error = error;
        }

        public String getUpdatedAt()
        {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt)
        {
            this.updatedAt = updatedAt;
        }
    }

    public static class Attributes
    {
        private String description;
        private boolean dlc;
        private Dlcs dlcs;
        private boolean experimental;
        private String hive;
        private boolean modded;
        private boolean official;
        private String shard;
        private boolean whitelist;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public boolean isDlc()
        {
            return dlc;
        }

        public void setDlc(boolean dlc)
        {
            this.dlc = dlc;
        }

        public Dlcs getDlcs()
        {
            return dlcs;
        }

        public void setDlcs(Dlcs dlcs)
        {
            this.dlcs = dlcs;
        }

        public boolean isExperimental()
        {
            return experimental;
        }

        public void setExperimental(boolean experimental)
        {
            this.experimental = experimental;
        }

        public String getHive()
        {
            return hive;
        }

        public void setHive(String hive)
        {
            this.hive = hive;
        }

        public boolean isModded()
        {
            return modded;
        }

        public void setModded(boolean modded)
        {
            this.modded = modded;
        }

        public boolean isOfficial()
        {
            return official;
        }

        public void setOfficial(boolean official)
        {
            this.official = official;
        }

        public String getShard()
        {
            return shard;
        }

        public void setShard(String shard)
        {
            this.shard = shard;
        }

        public boolean isWhitelist()
        {
            return whitelist;
        }

        public void setWhitelist(boolean whitelist)
        {
            this.whitelist = whitelist;
        }
    }

    public static class Dlcs
    {
        private boolean livonia;
        private boolean sakhal;

        public boolean isLivonia()
        {
            return livonia;
        }

        public void setLivonia(boolean livonia)
        {
            this.livonia = livonia;
        }

        public boolean isSakhal()
        {
            return sakhal;
        }

        public void setSakhal(boolean sakhal)
        {
            this.sakhal = sakhal;
        }
    }

    public static class Environment
    {
        private Perspectives perspectives;
        private String time;
        @JsonProperty("time_acceleration")
        private TimeAcceleration timeAcceleration;

        public Perspectives getPerspectives()
        {
            return perspectives;
        }

        public void setPerspectives(Perspectives perspectives)
        {
            this.perspectives = perspectives;
        }

        public String getTime()
        {
            return time;
        }

        public void setTime(String time)
        {
            this.time = time;
        }

        public TimeAcceleration getTimeAcceleration()
        {
            return timeAcceleration;
        }

        public void setTimeAcceleration(TimeAcceleration timeAcceleration)
        {
            this.timeAcceleration = timeAcceleration;
        }
    }

    public static class Perspectives
    {
        @JsonProperty("1rd")
        private boolean firstPerson;
        @JsonProperty("3rd")
        private boolean thirdPerson;

        public boolean isFirstPerson()
        {
            return firstPerson;
        }

        public void setFirstPerson(boolean firstPerson)
        {
            this.firstPerson = firstPerson;
        }

        public boolean isThirdPerson()
        {
            return thirdPerson;
        }

        public void setThirdPerson(boolean thirdPerson)
        {
            this.thirdPerson = thirdPerson;
        }
    }

    public static class TimeAcceleration
    {
        private double general;
        private double night;

        public double getGeneral()
        {
            return general;
        }

        public void setGeneral(double general)
        {
            this.general = general;
        }

        public double getNight()
        {
            return night;
        }

        public void setNight(double night)
        {
            this.night = night;
        }
    }

    public static class Geolocation
    {
        private boolean available;
        private City city;
        private String continent;
        private Country country;
        private String timezone;

        public boolean isAvailable()
        {
            return available;
        }

        public void setAvailable(boolean available)
        {
            this.available = available;
        }

        public City getCity()
        {
            return city;
        }

        public void setCity(City city)
        {
            this.city = city;
        }

        public String getContinent()
        {
            return continent;
        }

        public void setContinent(String continent)
        {
            this.continent = continent;
        }

        public Country getCountry()
        {
            return country;
        }

        public void setCountry(Country country)
        {
            this.country = country;
        }

        public String getTimezone()
        {
            return timezone;
        }

        public void setTimezone(String timezone)
        {
            this.timezone = timezone;
        }
    }

    public static class City
    {
        private String name;
        private String region;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getRegion()
        {
            return region;
        }

        public void setRegion(String region)
        {
            this.region = region;
        }
    }

    public static class Country
    {
        private String code;
        private String name;

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class Host
    {
        private String address;
        @JsonProperty("game_port")
        private int gamePort;
        private String os;
        @JsonProperty("query_port")
        private int queryPort;

        public String getAddress()
        {
            return address;
        }

        public void setAddress(String address)
        {
            this.address = address;
        }

        public int getGamePort()
        {
            return gamePort;
        }

        public void setGamePort(int gamePort)
        {
            this.gamePort = gamePort;
        }

        public String getOs()
        {
            return os;
        }

        public void setOs(String os)
        {
            this.os = os;
        }

        public int getQueryPort()
        {
            return queryPort;
        }

        public void setQueryPort(int queryPort)
        {
            this.queryPort = queryPort;
        }
    }

    public static class Mod
    {
        @JsonProperty("file_id")
        private long fileId;
        private String name;

        public long getFileId()
        {
            return fileId;
        }

        public void setFileId(long fileId)
        {
            this.fileId = fileId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class Publisher
    {
        private boolean monetization;

        public boolean isMonetization()
        {
            return monetization;
        }

        public void setMonetization(boolean monetization)
        {
            this.monetization = monetization;
        }
    }

    public static class Security
    {
        private boolean battleye;
        private boolean password;
        private boolean vac;

        public boolean isBattleye()
        {
            return battleye;
        }

        public void setBattleye(boolean battleye)
        {
            this.battleye = battleye;
        }

        public boolean isPassword()
        {
            return password;
        }

        public void setPassword(boolean password)
        {
            this.password = password;
        }

        public boolean isVac()
        {
            return vac;
        }

        public void setVac(boolean vac)
        {
            this.vac = vac;
        }
    }

    public static class Status
    {
        private boolean bots;
        private int players;
        private Queue queue;
        private int slots;

        public boolean isBots()
        {
            return bots;
        }

        public void setBots(boolean bots)
        {
            this.bots = bots;
        }

        public int getPlayers()
        {
            return players;
        }

        public void setPlayers(int players)
        {
            this.players = players;
        }

        public Queue getQueue()
        {
            return queue;
        }

        public void setQueue(Queue queue)
        {
            this.queue = queue;
        }

        public int getSlots()
        {
            return slots;
        }

        public void setSlots(int slots)
        {
            this.slots = slots;
        }
    }

    public static class Queue
    {
        private boolean active;
        private int size;

        public boolean isActive()
        {
            return active;
        }

        public void setActive(boolean active)
        {
            this.active = active;
        }

        public int getSize()
        {
            return size;
        }

        public void setSize(int size)
        {
            this.size = size;
        }
    }
}