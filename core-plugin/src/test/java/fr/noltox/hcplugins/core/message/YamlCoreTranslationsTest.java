package fr.noltox.hcplugins.core.message;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlCoreTranslationsTest {

    @TempDir
    Path directory;

    @Test
    void reloadKeepsThePreviousCatalogueWhenTheFileIsInvalid() throws Exception {
        YamlCoreTranslations translations = new YamlCoreTranslations(plugin());
        translations.reload();
        Path file = directory.resolve("plugins/HCPlugins/translations.yml");
        assertTrue(Files.exists(file));

        String previous = plain(translations.operatorOnly());
        Files.writeString(file, "reload: [invalid\n");
        assertThrows(IllegalStateException.class, translations::reload);
        assertEquals(previous, plain(translations.operatorOnly()));
    }

    @Test
    void placeholdersRenderAsTextAndChangedTranslationsReload() throws Exception {
        YamlCoreTranslations translations = new YamlCoreTranslations(plugin());
        translations.reload();
        assertEquals("Le plugin <red>Exemple a été rechargé en 250ms !",
                plain(translations.render("reload.success",
                        Map.of("plugin", "<red>Exemple", "duration", "250ms"))));
        assertEquals("Le plugin HCCore a été rechargé en 250ms !",
                plain(translations.reloadSuccess(plugin(), 250_000_000L)));

        Path file = directory.resolve("plugins/HCPlugins/translations.yml");
        Files.writeString(file, Files.readString(file).replace(
                "Cette commande est réservée aux opérateurs.", "Accès opérateur uniquement.")
                + "\nfuture:\n  shared: \"<yellow>Bonjour {name}</yellow>\"\n");
        translations.reload();
        assertEquals("Accès opérateur uniquement.", plain(translations.operatorOnly()));
        assertEquals("Bonjour Alex", plain(translations.render("future.shared", Map.of("name", "Alex"))));
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getDataFolder" -> directory.resolve("plugins/HCCore").toFile();
                    case "getName" -> "HCCore";
                    case "getResource" -> getClass().getClassLoader().getResourceAsStream((String) arguments[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static String plain(net.kyori.adventure.text.Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
