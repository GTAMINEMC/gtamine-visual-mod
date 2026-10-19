package com.gtamine.visual.module;

/** One visual feature. Register event listeners once; release session state on disable. */
public interface VisualModule {
    String id();

    void enable();

    void disable();
}
