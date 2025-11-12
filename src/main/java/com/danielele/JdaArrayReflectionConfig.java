package com.danielele;

import io.quarkus.runtime.annotations.RegisterForReflection;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.managers.AudioManager;

@RegisterForReflection(
    targets = {
        Guild[].class,
        User[].class,
        Member[].class,
        Role[].class,
        AudioManager[].class,
        GuildSticker[].class,
        RichCustomEmoji[].class,
        ForumTag[].class,
        ScheduledEvent[].class,

    },
    unsafeAllocated = true
)
public class JdaArrayReflectionConfig {
}