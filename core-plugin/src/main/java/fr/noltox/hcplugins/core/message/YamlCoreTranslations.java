package fr.noltox.hcplugins.core.message;

import fr.noltox.hcplugins.core.api.config.BukkitYaml;
import fr.noltox.hcplugins.core.api.config.HCPluginFiles;
import fr.noltox.hcplugins.core.api.message.CoreTranslations;
import fr.noltox.hcplugins.core.api.message.MiniMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads a validated candidate before replacing the live shared translations. */
public final class YamlCoreTranslations implements CoreTranslations {

    private static final String RESOURCE = "translations.yml";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z][a-z0-9_-]*)}");
    private static final Set<String> REQUIRED_KEYS = Set.of(
            "reload.success", "reload.failure", "command.operator-only",
            "command.players-only", "command.no-permission"
    );

    private final Plugin plugin;
    private final Path path;
    private volatile Map<String, String> templates = Map.of();

    public YamlCoreTranslations(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.path = HCPluginFiles.root(plugin).resolve(RESOURCE);
    }

    public void reload() {
        HCPluginFiles.copyDefault(plugin, RESOURCE, path);
        YamlConfiguration yaml = BukkitYaml.load(path, plugin.getResource(RESOURCE));
        Map<String, String> candidate = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key)) {
                continue;
            }
            Object value = yaml.get(key);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new IllegalStateException("Traduction manquante ou invalide : " + key);
            }
            Set<String> allowed = switch (key) {
                case "reload.success" -> Set.of("plugin", "duration");
                case "reload.failure" -> Set.of("plugin");
                case "command.operator-only", "command.players-only", "command.no-permission" -> Set.of();
                default -> null;
            };
            if (allowed != null) {
                Set<String> names = new HashSet<>();
                Matcher placeholders = PLACEHOLDER.matcher(text);
                while (placeholders.find()) {
                    names.add(placeholders.group(1));
                }
                if (!allowed.containsAll(names)) {
                    throw new IllegalStateException("Placeholder inconnu dans la traduction " + key);
                }
            }
            String probe = PLACEHOLDER.matcher(text).replaceAll("exemple");
            MiniMessages.parseStrict(probe);
            candidate.put(key, text);
        }
        if (!candidate.keySet().containsAll(REQUIRED_KEYS)) {
            throw new IllegalStateException("Traductions communes manquantes dans " + RESOURCE);
        }
        templates = Map.copyOf(candidate);
    }

    @Override
    public Component render(String key, Map<String, String> placeholders) {
        Objects.requireNonNull(placeholders, "placeholders");
        String template = templates.get(Objects.requireNonNull(key, "key"));
        if (template == null) {
            throw new IllegalArgumentException("Traduction inconnue : " + key);
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder parsed = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!placeholders.containsKey(name)) {
                throw new IllegalArgumentException("Valeur absente pour {" + name + "} dans " + key);
            }
            matcher.appendReplacement(parsed, Matcher.quoteReplacement("<" + name + ">"));
        }
        matcher.appendTail(parsed);
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        return MiniMessages.parse(parsed.toString(), resolvers);
    }
}
