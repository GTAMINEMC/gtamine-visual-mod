package com.gtamine.visual.module;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owns module lifecycle on the Minecraft client thread. */
public final class ModuleManager {
    private final List<VisualModule> modules;
    private boolean active;

    public ModuleManager(List<VisualModule> modules) {
        this.modules = List.copyOf(modules);
        Set<String> ids = new HashSet<>();
        for (VisualModule module : this.modules) {
            if (module.id() == null || module.id().isBlank() || !ids.add(module.id())) {
                throw new IllegalArgumentException("Module IDs must be nonblank and unique");
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        for (VisualModule module : modules) {
            if (active) {
                module.enable();
            } else {
                module.disable();
            }
        }
    }
}
