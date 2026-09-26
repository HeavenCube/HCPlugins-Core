package fr.noltox.hcplugins.core.command;

import fr.noltox.hcplugins.core.api.command.CoreCommand;
import fr.noltox.hcplugins.core.api.command.CoreCommandRegistration;
import fr.noltox.hcplugins.core.api.command.CoreCommandRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public final class CoreCommandRegistryImpl implements CoreCommandRegistry, AutoCloseable {

    private static final Pattern MODULE_NAME = Pattern.compile("[a-z][a-z0-9-]*");

    private final BooleanSupplier primaryThread;
    private final Map<String, Registration> registrations = new LinkedHashMap<>();
    private boolean closed;

    public CoreCommandRegistryImpl() {
        this(Bukkit::isPrimaryThread);
    }

    CoreCommandRegistryImpl(BooleanSupplier primaryThread) {
        this.primaryThread = Objects.requireNonNull(primaryThread, "primaryThread");
    }

    @Override
    public synchronized CoreCommandRegistration register(
            Plugin owner,
            String module,
            String description,
            Collection<String> accessPermissions,
            CoreCommand command
    ) {
        requirePrimaryThread();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(accessPermissions, "accessPermissions");
        Objects.requireNonNull(command, "command");

        if (closed) {
            throw new IllegalStateException("Le registre de commandes HCCore est fermé.");
        }

        validateModule(module);
        if (description.isBlank()) {
            throw new IllegalArgumentException("La description du module ne peut pas être vide.");
        }
        if (registrations.containsKey(module)) {
            throw new IllegalStateException("Le module de commande '" + module + "' est déjà enregistré.");
        }

        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        for (String permission : accessPermissions) {
            if (permission == null || permission.isBlank()) {
                throw new IllegalArgumentException("Une permission d'accès au module est vide.");
            }
            permissions.add(permission);
        }

        Registration registration = new Registration(
                this, owner, module, description, Set.copyOf(permissions), command
        );
        registrations.put(module, registration);
        return registration;
    }

    synchronized List<RegistrationView> visibleTo(CommandSender sender) {
        return registrations.values().stream()
                .filter(registration -> registration.canUse(sender))
                .map(Registration::view)
                .toList();
    }

    synchronized RegistrationView findVisible(CommandSender sender, String module) {
        Registration registration = registrations.get(module);
        return registration != null && registration.canUse(sender) ? registration.view() : null;
    }

    public synchronized void unregisterAll(Plugin owner) {
        requirePrimaryThread();
        Objects.requireNonNull(owner, "owner");
        registrations.values().stream()
                .filter(registration -> registration.owner == owner)
                .toList()
                .forEach(this::unregister);
    }

    @Override
    public synchronized void close() {
        requirePrimaryThread();
        if (closed) {
            return;
        }
        closed = true;
        registrations.values().forEach(Registration::markClosed);
        registrations.clear();
    }

    private synchronized void unregister(Registration registration) {
        requirePrimaryThread();
        if (registrations.get(registration.module) != registration) {
            registration.markClosed();
            return;
        }
        registrations.remove(registration.module);
        registration.markClosed();
    }

    private void requirePrimaryThread() {
        if (!primaryThread.getAsBoolean()) {
            throw new IllegalStateException("Le registre HCCore doit être modifié sur le thread serveur.");
        }
    }

    private static void validateModule(String module) {
        if (!module.equals(module.toLowerCase(Locale.ROOT)) || !MODULE_NAME.matcher(module).matches()) {
            throw new IllegalArgumentException(
                    "Le module doit respecter [a-z][a-z0-9-]* et être en minuscules : " + module
            );
        }
        if ("hcplugins".equals(module)) {
            throw new IllegalArgumentException("Le module ne peut pas s'appeler 'hcplugins'.");
        }
    }

    record RegistrationView(
            String module,
            String description,
            Set<String> permissions,
            CoreCommand command
    ) {
    }

    private static final class Registration implements CoreCommandRegistration {

        private final CoreCommandRegistryImpl registry;
        private final Plugin owner;
        private final String module;
        private final String description;
        private final Set<String> permissions;
        private final CoreCommand command;
        private volatile boolean registered = true;

        private Registration(
                CoreCommandRegistryImpl registry,
                Plugin owner,
                String module,
                String description,
                Set<String> permissions,
                CoreCommand command
        ) {
            this.registry = registry;
            this.owner = owner;
            this.module = module;
            this.description = description;
            this.permissions = permissions;
            this.command = command;
        }

        private boolean canUse(CommandSender sender) {
            return permissions.isEmpty() || permissions.stream().anyMatch(sender::hasPermission);
        }

        private RegistrationView view() {
            return new RegistrationView(module, description, permissions, command);
        }

        @Override
        public boolean isRegistered() {
            return registered;
        }

        @Override
        public void close() {
            if (registered) {
                registry.unregister(this);
            }
        }

        private void markClosed() {
            registered = false;
        }
    }
}
