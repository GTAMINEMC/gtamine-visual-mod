package com.gtamine.visual.editor.nminimap;

/** CPU copy of one server-provided marker, with shader metadata pixels removed. */
public record MinimapSprite(int width, int height, double scaleX, double scaleY, int[] pixels) { }
