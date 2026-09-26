package fr.noltox.hcplugins.core.command;

import fr.noltox.hcplugins.core.command.CoreCommandRegistryImpl.RegistrationView;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class HCPluginsRootCommand implements BasicCommand {

    private final CoreCommandRegistryImpl registry;

    public HCPluginsRootCommand(CoreCommandRegistryImpl registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            sendModuleList(source);
            return;
        }

        RegistrationView registration = registry.findVisible(source.getSender(), args[0]);
        if (registration == null) {
            source.getSender().sendMessage(Component.text(
                    "Module inconnu ou inaccessible : " + args[0],
                    NamedTextColor.RED
            ));
            return;
        }

        registration.command().execute(source, tail(args));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            return registry.visibleTo(source.getSender()).stream()
                    .map(RegistrationView::module)
                    .filter(module -> module.startsWith(prefix))
                    .toList();
        }

        RegistrationView registration = registry.findVisible(source.getSender(), args[0]);
        if (registration == null) {
            return List.of();
        }
        return registration.command().suggest(source, tail(args));
    }

    private void sendModuleList(CommandSourceStack source) {
        List<RegistrationView> visible = registry.visibleTo(source.getSender());
        if (visible.isEmpty()) {
            source.getSender().sendMessage(Component.text(
                    "Aucun module HCPlugins ne vous est accessible.",
                    NamedTextColor.YELLOW
            ));
            return;
        }

        source.getSender().sendMessage(Component.text("Modules HCPlugins :", NamedTextColor.AQUA));
        for (RegistrationView module : visible) {
            source.getSender().sendMessage(
                    Component.text(" - " + module.module(), NamedTextColor.WHITE)
                            .append(Component.text(" — " + module.description(), NamedTextColor.GRAY))
            );
        }
    }

    private static String[] tail(String[] args) {
        return args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
    }
}
