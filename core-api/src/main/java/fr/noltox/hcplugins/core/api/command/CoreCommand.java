package fr.noltox.hcplugins.core.api.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface CoreCommand {

    void execute(CommandSourceStack source, String[] args);

    default Collection<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }
}
