# Discord Online DayZ Bot

This project is based on the original work from the author of:
<https://github.com/DaniilOpryshko/DiscordOnlineDayzBot>

It keeps the same idea (Discord status with DayZ online data), and extends it with:

- container/cloud-oriented environment variable overrides (neccessary for some services, like GCPs CloudRun; which does not allow you logging directly into container running your image → [_this results in being unable to override default config file_])
- `/health` endpoint for probes

## Requirements

- Java 21
- Maven wrapper (`mvnw.cmd` / `./mvnw`)
- Discord bot token(s)
- DayZ server info (`ip`, `port`, optionally `steamQueryPort` for A2S)

## Quick start (local)

1. Build:

```bash
./mvnw -DskipTests package
```

2. On first run, config file is created:

- `OnlineBot_Config.json`

3. Fill token/server values in JSON and run:

```bash
java -jar target/discord-online-dayz-bot-1.0-SNAPSHOT-runner.jar
```

## Configuration model

Config version is `5`, with `instances[]`.
Each entry is one independent monitored server + one Discord bot session.

Minimal example:

```json
{
  "version": 5,
  "instances": [
    {
      "discord": {
        "token": "YOUR_BOT_TOKEN_HERE"
      },
      "server": {
        "ip": "127.0.0.1",
        "port": 2302,
        "steamQueryPort": 27015,
        "onlineProvider": "CFTOOLS"
      },
      "emojis": {
        "player": "PLAYER",
        "day": "DAY",
        "night": "NIGHT",
        "queue": "QUEUE"
      },
      "updater": {
        "intervalSeconds": 10
      },
      "status": {
        "message": "${emoji.player} ${online} / ${max} ${emoji.daytime} ${time} ${status.queueBlock}",
        "queueBlock": "${emoji.queue} ${queue}",
        "showQueueIfNotActive": true,
        "activityType": "PLAYING",
        "serverOfflineMessage": "Server offline"
      }
    }
  ]
}
```

## Environment variables (cloud/container mode; especially **CloudRun**)

Running in CloudRun PaaS requires you to provide the instance with `environmental variables`:

- `SERVER_IP`     (_supported only for single instance_)
- `DISCORD_TOKEN` (_supported only for single instance_)

If you want to monitor more game servers this way you can apply to the following naming convention:

- `INSTANCE_0_SERVER_IP`
- `INSTANCE_0_DISCORD_TOKEN`
- `INSTANCE_1_SERVER_IP`
- `INSTANCE_1_DISCORD_TOKEN`
- `INSTANCE_N_SERVER_IP`
- `INSTANCE_N_DISCORD_TOKEN`

Shortcuts for first instance (`N=0`) only:


### Important rule

Override for an instance through a `sysenv` is applied only when **BOTH** values are present for that same instance:

- `INSTANCE_N_SERVER_IP`
- `INSTANCE_N_DISCORD_TOKEN`

If only one of the pair is set, app logs a warning and falls back to JSON for that instance.

If no complete pair exists, app uses JSON for all instances.

At this moment, only `server.ip` and `discord.token` are available via environment variables. All other instance settings must come from `OnlineBot_Config.json`.

## Multi-server behavior

`instances[]` controls how many servers are monitored inside a single process:

- 1 entry = 1 Discord bot session + 1 scheduler thread
- 3 entries = 3 independent monitoring loops in one container/process

***This is separate from Cloud Run container scaling!***

## Status placeholders

Available in `status.message`:

- `${emoji.player}`
- `${online}`
- `${max}`
- `${emoji.daytime}`
- `${time}`
- `${status.queueBlock}`

Queue block:

- `${emoji.queue}`
- `${queue}`

## Activity types

- `PLAYING`
- `LISTENING`
- `WATCHING`
- `COMPETING`
- `CUSTOM_STATUS`

## Runtime flow and key logs

Startup config path:

- `Initializing configuration...`
- `Loading config from file: OnlineBot_Config.json`
- `Instance[N]: using environment variables '...' and '...'` (ENV mode)
- `Instance[N]: ENV naming for override is '...' + '...'` (JSON mode hint)
- `Cloud Run detected and no complete ENV override pairs were found...` (cloud warning)

Discord/bot path:

- `Connecting bot to Discord...`
- `Login Successful!`
- `Bot connected successfully`
- `Bots are ready. Starting schedulers for X bots...`
- `Scheduler started for bot <ip:port>: interval=<Ns>`

Provider path:

- CFTools failures are throttled and can reuse cached data
- recovery log: `CFTools recovered for <ip:port>...`

Health endpoint:

- `GET /health` returns `ok` (plain text)

## Docker

Build image:

```bash
docker build -t discord-online-dayz-bot:local .
```

Run image (example):

```bash
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e INSTANCE_0_SERVER_IP=127.0.0.1 \
  -e INSTANCE_0_DISCORD_TOKEN=YOUR_TOKEN \
  discord-online-dayz-bot:local
```

## Cloud Run notes

For this bot pattern (long-lived Discord connection + scheduler):

- use one service instance for stable behavior (`max instances = 1`)
- keep it warm if you need continuous monitoring (`min instances = 1`)
- for this always-on bot workload, prefer instance-based billing (per instance)
- set full ENV pairs for instances you want to override

If you deploy without ENV pairs, app can still run from JSON values that are present in the image filesystem.

## Troubleshooting

- `INVALID TOKEN FOR INSTANCE[N]!`
  - set both `INSTANCE_N_SERVER_IP` and `INSTANCE_N_DISCORD_TOKEN`, or set JSON token locally
- `partial environment override detected`
  - you set only one variable from required pair
- frequent CFTools errors
  - check outbound network/DNS/TLS to `https://data.cftools.cloud`

## Native build (optional)

```bash
./mvnw package -Dnative
```

or container-based native build:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```
