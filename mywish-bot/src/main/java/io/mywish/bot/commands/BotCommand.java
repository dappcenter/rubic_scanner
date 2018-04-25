package io.mywish.bot.commands;

import io.mywish.bot.service.ChatContext;
import org.telegram.telegrambots.bots.AbsSender;

import java.util.List;

public interface BotCommand {
    void execute(ChatContext context, List<String> args);
    String getName();
}
