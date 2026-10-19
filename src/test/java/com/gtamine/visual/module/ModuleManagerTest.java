package com.gtamine.visual.module;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModuleManagerTest {
    @Test
    void transitionsOnlyOnceAndSupportsReconnects() {
        CountingModule module = new CountingModule();
        ModuleManager manager = new ModuleManager(List.of(module));
        assertFalse(manager.isActive());
        manager.setActive(false);
        manager.setActive(true);
        manager.setActive(true);
        assertTrue(manager.isActive());
        manager.setActive(false);
        manager.setActive(false);
        manager.setActive(true);
        assertEquals(2, module.enables);
        assertEquals(1, module.disables);
    }

    @Test
    void rejectsDuplicateIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModuleManager(List.of(new CountingModule(), new CountingModule())));
    }

    private static final class CountingModule implements VisualModule {
        int enables;
        int disables;
        public String id() { return "test"; }
        public void enable() { enables++; }
        public void disable() { disables++; }
    }
}
