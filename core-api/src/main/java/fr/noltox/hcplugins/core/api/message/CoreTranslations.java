package fr.noltox.hcplugins.core.api.message;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;

/** Server-wide messages owned by HCCore and configured in plugins/HCPlugins/translations.yml. */
public interface CoreTranslations {

    Component render(String key, Map<String, String> placeholders);

    default Component render(String key) {
        return render(key, Map.of());
    }

    default Component reloadSuccess(Plugin plugin, long elapsedNanos) {
        String duration = String.format(Locale.FRANCE, "%.2f s", Math.max(0L, elapsedNanos) / 1_000_000_000.0);
        return render("reload.success", Map.of("plugin", plugin.getName(), "duration", duration));
    }

    default Component reloadFailure(Plugin plugin) {
        return render("reload.failure", Map.of("plugin", plugin.getName()));
    }

    default Component operatorOnly() {
        return render("command.operator-only");
    }

    default Component playersOnly() {
        return render("command.players-only");
    }

    default Component noPermission() {
        return render("command.no-permission");
    }
}
