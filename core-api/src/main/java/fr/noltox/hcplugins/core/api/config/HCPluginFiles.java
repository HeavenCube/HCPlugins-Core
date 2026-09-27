package fr.noltox.hcplugins.core.api.config;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Common file layout for HeavenCube plugins under the server's plugins/HCPlugins directory. */
public final class HCPluginFiles {

    private HCPluginFiles() {
    }

    public static Path root(Plugin plugin) {
        return Objects.requireNonNull(plugin, "plugin")
                .getDataFolder().toPath().resolveSibling("HCPlugins");
    }

    /** The sole administrator configuration of a plugin, for example HCItemFrame.yml. */
    public static Path singleConfiguration(Plugin plugin) {
        return root(plugin).resolve(plugin.getName() + ".yml");
    }

    /** Files belonging to one plugin when it needs more than one file. */
    public static Path pluginDirectory(Plugin plugin) {
        return root(plugin).resolve(plugin.getName());
    }

    /** Copies a bundled resource into the shared layout only when the target is absent. */
    public static void copyDefault(Plugin plugin, String resourceName, Path target) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(target, "target");
        try (InputStream resource = plugin.getResource(resourceName)) {
            if (resource == null) {
                throw new IllegalStateException("Missing bundled resource: " + resourceName);
            }
            if (Files.exists(target)) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(resource, target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create " + target + '.', exception);
        }
    }
}
