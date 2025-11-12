package com.danielele;

import io.quarkus.runtime.annotations.RegisterForReflection;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

@RegisterForReflection(targets = {
        Guild.class,
        User.class,
        Member.class,
        Role.class,
        Channel.class,
        RichCustomEmoji.class,
        Message.class
})
public class JdaReflectionConfig {
}