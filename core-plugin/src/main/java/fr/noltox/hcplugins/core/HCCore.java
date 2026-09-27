package fr.noltox.hcplugins.core;

import fr.noltox.hcplugins.core.api.command.CoreCommandRegistry;
import fr.noltox.hcplugins.core.api.message.CoreTranslations;
import fr.noltox.hcplugins.core.command.CoreTranslationsCommand;
import fr.noltox.hcplugins.core.command.CoreCommandRegistryImpl;
import fr.noltox.hcplugins.core.command.HCPluginsRootCommand;
import fr.noltox.hcplugins.core.message.YamlCoreTranslations;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.List;

public final class HCCore extends JavaPlugin implements Listener {

    private CoreCommandRegistryImpl commandRegistry;

    @Override
    public void onEnable() {
        try {
            YamlCoreTranslations translations = new YamlCoreTranslations(this);
            translations.reload();
            commandRegistry = new CoreCommandRegistryImpl();
            getServer().getServicesManager().register(
                    CoreCommandRegistry.class,
                    commandRegistry,
                    this,
                    ServicePriority.Normal
            );
            getServer().getServicesManager().register(
                    CoreTranslations.class, translations, this, ServicePriority.Normal
            );
            commandRegistry.register(this, "core", "Traductions partagées", List.of(),
                    new CoreTranslationsCommand(this, translations));
            getServer().getPluginManager().registerEvents(this, this);

            getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                    event.registrar().register(
                            "hcplugins",
                            "Commandes des plugins HeavenCube",
                            new HCPluginsRootCommand(commandRegistry)
                    )
            );
        } catch (RuntimeException | LinkageError exception) {
            getLogger().log(Level.SEVERE, "Impossible d'initialiser HCCore. Désactivation du plugin.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        CoreCommandRegistryImpl registry = commandRegistry;
        if (registry == null || event.getPlugin() == this) {
            return;
        }
        try {
            registry.unregisterAll(event.getPlugin());
        } catch (RuntimeException exception) {
            getLogger().log(
                    Level.SEVERE,
                    exception,
                    () -> "Impossible de retirer les services de " + event.getPlugin().getName() + '.'
            );
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        CoreCommandRegistryImpl registry = commandRegistry;
        commandRegistry = null;
        if (registry != null) {
            registry.close();
        }
    }
}
