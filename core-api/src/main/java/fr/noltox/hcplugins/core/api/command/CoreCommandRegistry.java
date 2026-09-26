package fr.noltox.hcplugins.core.api.command;

import org.bukkit.plugin.Plugin;

import java.util.Collection;

@FunctionalInterface
public interface CoreCommandRegistry {

    CoreCommandRegistration register(
            Plugin owner,
            String module,
            String description,
            Collection<String> accessPermissions,
            CoreCommand command
    );
}
