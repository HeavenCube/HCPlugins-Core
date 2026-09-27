package fr.noltox.hcplugins.core.api.config;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HCPluginFilesTest {

    @TempDir
    Path directory;

    @Test
    void createsSharedDefaultsWithoutReplacingAdministratorChanges() throws Exception {
        Plugin plugin = plugin();
        Path configuration = HCPluginFiles.singleConfiguration(plugin);
        assertEquals(directory.resolve("plugins/HCPlugins/HCItemFrame.yml"), configuration);
        assertEquals(directory.resolve("plugins/HCPlugins/HCItemFrame"),
                HCPluginFiles.pluginDirectory(plugin));

        HCPluginFiles.copyDefault(plugin, "config.yml", configuration);
        assertEquals("value: default\n", Files.readString(configuration));
        Files.writeString(configuration, "value: custom\n");
        HCPluginFiles.copyDefault(plugin, "config.yml", configuration);
        assertEquals("value: custom\n", Files.readString(configuration));
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getDataFolder" -> directory.resolve("plugins/HCItemFrame").toFile();
                    case "getName" -> "HCItemFrame";
                    case "getResource" -> new ByteArrayInputStream(
                            "value: default\n".getBytes(StandardCharsets.UTF_8));
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
