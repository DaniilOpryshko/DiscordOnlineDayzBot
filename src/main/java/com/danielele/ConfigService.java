package com.danielele;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.Interceptor;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@Startup
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
@ApplicationScoped
public class ConfigService
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private AppConfig config;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String CONFIG_FILE = "OnlineBot_Config.json";
    private static final String BACKUP_SUFFIX = ".backup";
    private static final String CORRUPTED_BACKUP_SUFFIX = ".corrupted";
    private static final int CONFIG_VERSION = 2;

    public ConfigService()
    {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
    }

    @PostConstruct
    public void init()
    {
        logger.info("Initializing configuration...");
        loadConfig();
        logger.info("Configuration loaded successfully");
        printConfig();
    }

    private void loadConfig()
    {
        File externalFile = new File(CONFIG_FILE);
        try
        {

            if (!externalFile.exists())
            {
                useDefaultConfig();
                return;
            }

            logger.info("Loading config from file: {}", CONFIG_FILE);

            JsonNode rootNode = mapper.readTree(externalFile);

            AppConfig loadedConfig = mapper.treeToValue(rootNode, AppConfig.class);

            validateAndFixConfig(loadedConfig);

            config = loadedConfig;

            if (config.version == null || config.version < CONFIG_VERSION)
            {
                logger.warn("Config version mismatch: {} < {}", config.version, CONFIG_VERSION);
                config = mergeWithDefaults(config);
                backupAndSaveConfig(externalFile);
            }

            logger.info("Config loaded and validated successfully");
        }
        catch (Exception e)
        {
            logger.error("Failed to load config: {}", e.getMessage());
            backupCorruptedConfig(externalFile, e);
            useDefaultConfig();
        }
    }

    private void validateAndFixConfig(AppConfig cfg)
    {
        AppConfig defaults = createDefaultConfig();

        if (cfg.server == null)
        {
            logger.warn("Server section missing, using defaults");
            cfg.server = defaults.server;
        }
        else
        {
            if (cfg.server.ip == null || cfg.server.ip.isBlank())
            {
                logger.warn("Invalid server IP, using default");
                cfg.server.ip = defaults.server.ip;
            }
            if (cfg.server.port <= 0 || cfg.server.port > 65535)
            {
                logger.warn("Invalid server port '{}', using default", cfg.server.port);
                cfg.server.port = defaults.server.port;
            }
        }

        if (cfg.emojis == null)
        {
            logger.warn("Emojis section missing, using defaults");
            cfg.emojis = defaults.emojis;
        }
        else
        {
            if (cfg.emojis.player == null || cfg.emojis.player.isEmpty()) cfg.emojis.player = defaults.emojis.player;
            if (cfg.emojis.day == null || cfg.emojis.day.isEmpty()) cfg.emojis.day = defaults.emojis.day;
            if (cfg.emojis.night == null || cfg.emojis.night.isEmpty()) cfg.emojis.night = defaults.emojis.night;
            if (cfg.emojis.queue == null || cfg.emojis.queue.isEmpty()) cfg.emojis.queue = defaults.emojis.queue;
        }

        if (cfg.updater == null)
        {
            logger.warn("Updater section missing, using defaults");
            cfg.updater = defaults.updater;
        }
        else if (cfg.updater.intervalSeconds <= 0)
        {
            logger.warn("Invalid updater interval '{}', using default", cfg.updater.intervalSeconds);
            cfg.updater.intervalSeconds = defaults.updater.intervalSeconds;
        }

        if (cfg.discord == null)
        {
            logger.warn("Discord section missing, using defaults");
            cfg.discord = defaults.discord;
        }
        else if (cfg.discord.token == null || cfg.discord.token.isBlank())
        {
            logger.warn("Discord token is empty — bot will not connect!");
        }

        if (cfg.status == null)
        {
            logger.warn("Status section missing, using defaults");
            cfg.status = defaults.status;
        }
        else
        {
            if (cfg.status.message == null || cfg.status.message.isBlank())
            {
                logger.warn("Invalid status message, using default");
                cfg.status.message = defaults.status.message;
            }

            if (cfg.status.queueBlock == null)
            {
                logger.warn("Invalid queue block, using default");
                cfg.status.queueBlock = defaults.status.queueBlock;
            }

            if (cfg.status.activityType == null)
            {
                logger.warn("No activity type, using default (PLAYING)");
                cfg.status.activityType = "PLAYING";
            }
            else
            {
                logger.warn("Invalid activity type, using default (PLAYING)");
                boolean b = Arrays.stream(Activity.ActivityType.values())
                        .map(Activity.ActivityType::name)
                        .anyMatch(name -> name.equals(cfg.status.activityType));

                if (!b)
                {
                    cfg.status.activityType = "PLAYING";
                }
            }
        }

        if (cfg.version == null)
        {
            logger.warn("Missing config version, setting to {}", CONFIG_VERSION);
            cfg.version = CONFIG_VERSION;
        }
    }


    private AppConfig mergeWithDefaults(AppConfig oldConfig)
    {
        logger.info("Merging old config with new defaults...");
        AppConfig defaultConfig = createDefaultConfig();

        if (oldConfig.server != null)
        {
            defaultConfig.server.ip = oldConfig.server.ip != null ? oldConfig.server.ip : defaultConfig.server.ip;
            defaultConfig.server.port = oldConfig.server.port;
        }

        if (oldConfig.emojis != null)
        {
            defaultConfig.emojis.player = oldConfig.emojis.player != null ? oldConfig.emojis.player : defaultConfig.emojis.player;
            defaultConfig.emojis.day = oldConfig.emojis.day != null ? oldConfig.emojis.day : defaultConfig.emojis.day;
            defaultConfig.emojis.night = oldConfig.emojis.night != null ? oldConfig.emojis.night : defaultConfig.emojis.night;
            defaultConfig.emojis.queue = oldConfig.emojis.queue != null ? oldConfig.emojis.queue : defaultConfig.emojis.queue;
        }

        if (oldConfig.updater != null)
        {
            defaultConfig.updater.intervalSeconds = oldConfig.updater.intervalSeconds;
        }

        if (oldConfig.discord != null && oldConfig.discord.token != null)
        {
            defaultConfig.discord.token = oldConfig.discord.token;
        }

        if (oldConfig.status != null)
        {
            defaultConfig.status.message = oldConfig.status.message != null ? oldConfig.status.message : defaultConfig.status.message;
            defaultConfig.status.queueBlock = oldConfig.status.queueBlock != null ? oldConfig.status.queueBlock : defaultConfig.status.queueBlock;
            defaultConfig.status.showQueueIfNotActive = oldConfig.status.showQueueIfNotActive;
            defaultConfig.status.activityType = oldConfig.status.activityType;
        }

        logger.info("Config merged successfully");
        return defaultConfig;
    }

    private void backupAndSaveConfig(File configFile)
    {
        try
        {
            File backupFile = new File(CONFIG_FILE + BACKUP_SUFFIX);
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created backup: {}", backupFile.getName());

            saveConfigWithComments(configFile);
            logger.info("Config updated to version {}", CONFIG_VERSION);
        }
        catch (IOException e)
        {
            logger.error("Failed to backup/save config: {}", e.getMessage());
        }
    }

    private void backupCorruptedConfig(File configFile, Exception ex)
    {
        try
        {
            File backupFile = new File(CONFIG_FILE + CORRUPTED_BACKUP_SUFFIX);
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.warn("Created backup of corrupted file: {}", backupFile.getName());

            var lines = Files.readAllLines(configFile.toPath());
            var output = new StringBuilder();

            boolean pointerInserted = false;

            if (ex instanceof com.fasterxml.jackson.core.JsonParseException parseEx)
            {
                int line = parseEx.getLocation().getLineNr();
                int column = parseEx.getLocation().getColumnNr();

                logger.warn("JSON syntax error at line {}, column {}", line, column);

                for (int i = 0; i < lines.size(); i++)
                {
                    output.append(lines.get(i)).append(System.lineSeparator());
                    if (i == line - 1)
                    {
                        output.append(" ".repeat(Math.max(0, column - 1)))
                                .append("↑ JSON PARSE ERROR HERE")
                                .append(System.lineSeparator());
                        pointerInserted = true;
                    }
                }

                output.append("\n// Parsing error: ").append(parseEx.getOriginalMessage())
                        .append("\n// Location: line ").append(line).append(", column ").append(column);
            }

            else if (ex instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife)
            {
                String fieldPath = ife.getPathReference();
                String badValue = String.valueOf(ife.getValue());
                String targetType = ife.getTargetType().getSimpleName();
                String fieldName = null;

                if (!ife.getPath().isEmpty() && ife.getPath().get(ife.getPath().size() - 1).getFieldName() != null)
                {
                    fieldName = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
                }

                logger.warn("Type mismatch at {}: cannot map '{}' to {}", fieldPath, badValue, targetType);

                for (int i = 0; i < lines.size(); i++)
                {
                    String line = lines.get(i);
                    output.append(line).append(System.lineSeparator());

                    if (!pointerInserted && fieldName != null && line.contains("\"" + fieldName + "\""))
                    {
                        int pos = line.indexOf(fieldName);
                        output.append(" ".repeat(Math.max(0, pos)))
                                .append("↑ TYPE ERROR HERE (expected ").append(targetType).append(")")
                                .append(System.lineSeparator());
                        pointerInserted = true;
                    }
                }

                output.append("\n// Mapping error:")
                        .append("\n// Field: ").append(fieldPath)
                        .append("\n// Invalid value: ").append(badValue)
                        .append("\n// Expected type: ").append(targetType)
                        .append("\n// Message: ").append(ife.getOriginalMessage());
            }

            else
            {
                for (String line : lines)
                {
                    output.append(line).append(System.lineSeparator());
                }

                output.append("\n// Unknown config error:")
                        .append("\n// ").append(ex.getClass().getName())
                        .append("\n// Message: ").append(ex.getMessage());
            }

            if (!pointerInserted)
            {
                output.append("\n\n// ⚠️ Could not determine exact error line — please check JSON formatting above.");
            }

            Files.writeString(backupFile.toPath(), output.toString());
            logger.info("Annotated corrupted config backup written successfully");
        }
        catch (IOException e)
        {
            logger.error("Failed to backup corrupted config: {}", e.getMessage());
        }
    }


    private void useDefaultConfig()
    {
        config = createDefaultConfig();
        logger.info("Using default configuration");

        try
        {
            saveConfigWithComments(new File(CONFIG_FILE));
            logger.info("Created default {} file", CONFIG_FILE);
        }
        catch (IOException e)
        {
            logger.error("Failed to create default config file: {}", e.getMessage());
        }
    }

    private AppConfig createDefaultConfig()
    {
        AppConfig cfg = new AppConfig();
        cfg.version = CONFIG_VERSION;

        cfg.server = new ServerConfig();
        cfg.server.ip = "127.0.0.1";
        cfg.server.port = 2302;

        cfg.emojis = new EmojisConfig();
        cfg.emojis.player = "👥";
        cfg.emojis.day = "☀️";
        cfg.emojis.night = "🌙";
        cfg.emojis.queue = "👥";

        cfg.updater = new UpdaterConfig();
        cfg.updater.intervalSeconds = 10;

        cfg.discord = new DiscordConfig();
        cfg.discord.token = "YOUR_BOT_TOKEN_HERE";

        cfg.status = new StatusConfig();
        cfg.status.message = "${emoji.player} ${online} / ${max} ${emoji.daytime} ${time} ${status.queueBlock} ";
        cfg.status.queueBlock = "${emoji.queue} ${queue}";
        cfg.status.showQueueIfNotActive = true;
        cfg.status.activityType = "PLAYING";

        return cfg;
    }

    private void saveConfigWithComments(File file) throws IOException
    {
        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(config);

        StringBuilder withComments = new StringBuilder();
        withComments.append("// Discord Bot Configuration\n");
        withComments.append("// For more info visit: https://github.com/DaniilOpryshko/DiscordOnlineDayzBot\n");
        withComments.append("//\n");
        withComments.append("// Available placeholders for status.message:\n");
        withComments.append("//   ${emoji.player} - Player emoji\n");
        withComments.append("//   ${emoji.daytime} - Day/Night emoji\n");
        withComments.append("//   ${emoji.queue} - Queue emoji\n");
        withComments.append("//   ${online} - Current online players\n");
        withComments.append("//   ${max} - Max players\n");
        withComments.append("//   ${time} - In-game time\n");
        withComments.append("//   ${queue} - Queue size\n");
        withComments.append("//   ${status.queueBlock} - Queue block (if enabled)\n\n");
        withComments.append("//   Possible values for activityType: PLAYING, LISTENING, WATCHING, COMPETING, CUSTOM\n");
        withComments.append("//\n");
        withComments.append("// Note: Changes require bot restart\n");
        withComments.append("//\n\n");
        withComments.append(json);

        Files.writeString(file.toPath(), withComments.toString());
    }

    private void printConfig()
    {
        logger.info("Config version: {}", config.version);
        logger.info("Server: {}:{}", config.server.ip, config.server.port);
        logger.info("Update interval: {}s", config.updater.intervalSeconds);
        logger.info("Emojis loaded: {} {} {} {}", config.emojis.player,
                config.emojis.day, config.emojis.night, config.emojis.queue);
    }

    public ServerConfig getServer()
    {
        return config.server;
    }

    public EmojisConfig getEmojis()
    {
        return config.emojis;
    }

    public StatusConfig getStatus()
    {
        return config.status;
    }

    public UpdaterConfig getUpdater()
    {
        return config.updater;
    }

    public DiscordConfig getDiscord()
    {
        return config.discord;
    }

    @RegisterForReflection
    public static class AppConfig
    {
        public Integer version;
        public ServerConfig server;
        public EmojisConfig emojis;
        public UpdaterConfig updater;
        public DiscordConfig discord;
        public StatusConfig status;
    }

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