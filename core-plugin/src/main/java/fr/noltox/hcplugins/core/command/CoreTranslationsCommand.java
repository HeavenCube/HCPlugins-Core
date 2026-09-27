package fr.noltox.hcplugins.core.command;

import fr.noltox.hcplugins.core.api.command.CoreCommand;
import fr.noltox.hcplugins.core.message.YamlCoreTranslations;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

/** Reloads the shared translation catalogue with /hcplugins core reload. */
public final class CoreTranslationsCommand implements CoreCommand {

    private final Plugin plugin;
    private final YamlCoreTranslations translations;

    public CoreTranslationsCommand(Plugin plugin, YamlCoreTranslations translations) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.translations = Objects.requireNonNull(translations, "translations");
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage(translations.operatorOnly());
            return;
        }
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            source.getSender().sendMessage(Component.text("Utilisation : /hcplugins core reload", NamedTextColor.RED));
            return;
        }
        long started = System.nanoTime();
        try {
            translations.reload();
            source.getSender().sendMessage(translations.reloadSuccess(plugin, System.nanoTime() - started));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de recharger translations.yml.", exception);
            source.getSender().sendMessage(translations.reloadFailure(plugin));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().isOp() || args.length > 1) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return "reload".startsWith(prefix) ? List.of("reload") : List.of();
    }
}
