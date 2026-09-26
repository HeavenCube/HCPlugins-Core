package fr.noltox.hcplugins.core.command;

import fr.noltox.hcplugins.core.api.command.CoreCommandRegistration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreCommandRegistryImplTest {

    @Test
    void registrationIsIdempotentlyCloseable() {
        CoreCommandRegistryImpl registry = new CoreCommandRegistryImpl(() -> true);
        CoreCommandRegistration registration = registry.register(
                plugin(), "glowing", "Glow cosmetics", List.of(), (source, args) -> { }
        );

        assertTrue(registration.isRegistered());
        registration.close();
        registration.close();
        assertFalse(registration.isRegistered());
    }

    @Test
    void duplicateModuleIsRejected() {
        CoreCommandRegistryImpl registry = new CoreCommandRegistryImpl(() -> true);
        registry.register(plugin(), "itemframe", "Frames", List.of(), (source, args) -> { });

        assertThrows(
                IllegalStateException.class,
                () -> registry.register(plugin(), "itemframe", "Duplicate", List.of(), (source, args) -> { })
        );
    }

    @Test
    void invalidModuleIsRejected() {
        CoreCommandRegistryImpl registry = new CoreCommandRegistryImpl(() -> true);
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(plugin(), "Invalid_Name", "Invalid", List.of(), (source, args) -> { })
        );
    }

    @Test
    void mutationOffPrimaryThreadIsRejected() {
        AtomicBoolean primary = new AtomicBoolean(true);
        CoreCommandRegistryImpl registry = new CoreCommandRegistryImpl(primary::get);
        primary.set(false);

        assertThrows(
                IllegalStateException.class,
                () -> registry.register(plugin(), "module", "Module", List.of(), (source, args) -> { })
        );
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{Plugin.class},
                (_, _, _) -> null
        );
    }
}
