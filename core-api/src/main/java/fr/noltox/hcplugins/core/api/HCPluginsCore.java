package fr.noltox.hcplugins.core.api;

import fr.noltox.hcplugins.core.api.command.CoreCommandRegistry;
import fr.noltox.hcplugins.core.api.message.CoreTranslations;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class HCPluginsCore {

    public static final String PLUGIN_NAME = "HCCore";

    private HCPluginsCore() {
    }

    public static CoreCommandRegistry require(Plugin consumer) {
        Objects.requireNonNull(consumer, "consumer");
        CoreCommandRegistry registry = consumer.getServer()
                .getServicesManager()
                .load(CoreCommandRegistry.class);
        if (registry == null) {
            throw new IllegalStateException(
                    PLUGIN_NAME + " est requis et doit être activé avant " + consumer.getName() + '.'
            );
        }
        return registry;
    }

    public static CoreTranslations translations(Plugin consumer) {
        Objects.requireNonNull(consumer, "consumer");
        CoreTranslations translations = consumer.getServer()
                .getServicesManager()
                .load(CoreTranslations.class);
        if (translations == null) {
            throw new IllegalStateException(
                    PLUGIN_NAME + " doit fournir les traductions avant " + consumer.getName() + '.'
            );
        }
        return translations;
    }
}
