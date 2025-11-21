package com.danielele.config;

import com.danielele.config.legacy.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Отвечает за загрузку и сохранение конфигурации из/в файлы
 */
public class ConfigLoader
{
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String BACKUP_SUFFIX = ".backup";
    private static final String CORRUPTED_BACKUP_SUFFIX = ".corrupted";

    private final ObjectMapper mapper;
    private final String configFileName;

    public ConfigLoader(ObjectMapper mapper, String configFileName)
    {
        this.mapper = mapper;
        this.configFileName = configFileName;
    }

    public ConfigService.AppConfig loadFromFile(File file) throws IOException
    {
        JsonNode rootNode = mapper.readTree(file);
        return mapper.treeToValue(rootNode, ConfigService.AppConfig.class);
    }

    public AppConfig loadLegacyFromFile(File file) throws IOException
    {
        JsonNode rootNode = mapper.readTree(file);
        return mapper.treeToValue(rootNode, AppConfig.class);
    }

    public void saveConfig(ConfigService.AppConfig config) throws IOException
    {
        File file = new File(configFileName);
        saveConfigWithComments(file, config);
    }

    public void backupAndSave(File configFile, ConfigService.AppConfig config)
    {
        try
        {
            File backupFile = new File(configFileName + BACKUP_SUFFIX);
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created backup: {}", backupFile.getName());

            saveConfigWithComments(configFile, config);
            logger.info("Config updated to version {}", config.version);
        }
        catch (IOException e)
        {
            logger.error("Failed to backup/save config: {}", e.getMessage());
        }
    }

    public void backupCorruptedConfig(File configFile, Exception ex)
    {
        try
        {
            File backupFile = new File(configFileName + CORRUPTED_BACKUP_SUFFIX);
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.warn("Created backup of corrupted file: {}", backupFile.getName());

            var lines = Files.readAllLines(configFile.toPath());
            var output = new StringBuilder();
            boolean pointerInserted = false;

            if (ex instanceof com.fasterxml.jackson.core.JsonParseException parseEx)
            {
                pointerInserted = annotateJsonParseError(lines, parseEx, output);
            }
            else if (ex instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife)
            {
                pointerInserted = annotateInvalidFormatError(lines, ife, output);
            }
            else
            {
                annotateGenericError(lines, ex, output);
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

    private boolean annotateJsonParseError(
            java.util.List<String> lines,
            com.fasterxml.jackson.core.JsonParseException parseEx,
            StringBuilder output
    )
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
            }
        }

        output.append("\n// Parsing error: ").append(parseEx.getOriginalMessage())
                .append("\n// Location: line ").append(line).append(", column ").append(column);
        return true;
    }

    private boolean annotateInvalidFormatError(
            java.util.List<String> lines,
            com.fasterxml.jackson.databind.exc.InvalidFormatException ife,
            StringBuilder output
    )
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

        boolean pointerInserted = false;
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

        return pointerInserted;
    }

    private void annotateGenericError(java.util.List<String> lines, Exception ex, StringBuilder output)
    {
        for (String line : lines)
        {
            output.append(line).append(System.lineSeparator());
        }

        output.append("\n// Unknown config error:")
                .append("\n// ").append(ex.getClass().getName())
                .append("\n// Message: ").append(ex.getMessage());
    }

    private void saveConfigWithComments(File file, ConfigService.AppConfig config) throws IOException
    {
        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(config);

        StringBuilder withComments = new StringBuilder();
        withComments.append("// Discord Bot Configuration\n");
        withComments.append("// For more info visit: https://github.com/DaniilOpryshko/DiscordOnlineDayzBot\n");
        withComments.append("//\n");
        withComments.append("// This config now supports multiple bot instances!\n");
        withComments.append("// Each instance can monitor a different server with its own settings.\n");
        withComments.append("//\n");
        withComments.append("// Available placeholders for status.message:\n");
        withComments.append("//   ${emoji.player} - Player emoji\n");
        withComments.append("//   ${emoji.daytime} - Day/Night emoji\n");
        withComments.append("//   ${emoji.queue} - Queue emoji\n");
        withComments.append("//   ${online} - Current online players\n");
        withComments.append("//   ${max} - Max players\n");
        withComments.append("//   ${time} - In-game time\n");
        withComments.append("//   ${queue} - Queue size\n");
        withComments.append("//   ${status.queueBlock} - Queue block (if enabled)\n");
        withComments.append("//\n");
        withComments.append("//   Possible values for activityType: PLAYING, LISTENING, WATCHING, COMPETING, CUSTOM_STATUS\n");
        withComments.append("//\n");
        withComments.append("// Note: Changes require bot restart\n");
        withComments.append("//\n\n");
        withComments.append(json);

        Files.writeString(file.toPath(), withComments.toString());
    }
}