# discord-online-dayz-bot

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

# Usage

After building the jar or native file, run it once it will generate a config file near executable.
Edit the config file with your bot token and your server ip and server port.
Run the executable.

If there will be any errors in config file, the bot will fallback to default config value and will create
*.json.corrupted file with error message. Examine it and fix it.

## Available Placeholders

The following placeholders can be used in the status message:

- `${emoji.player}` - Player emoji from config
- `${online}` - Current online players count
- `${max}` - Maximum server slots
- `${emoji.daytime}` - Day/Night emoji based on server time
- `${time}` - Current server time
- `${status.queueBlock}` - Queue information block (if enabled)

Queue block placeholders:

- `${emoji.queue}` - Queue emoji from config
- `${queue}` - Current queue size

## Activity Types

The bot supports following activity types in config:

- `PLAYING` - Shows as "Playing {status}"
- `LISTENING` - Shows as "Listening to {status}"
- `WATCHING` - Shows as "Watching {status}"
- `COMPETING` - Shows as "Competing in {status}"
- `CUSTOM_STATUS` - Shows as custom status

## Configuration

The config.json file structure:
Go to OnlineBot_Config.json

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/discord-online-dayz-bot-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.
