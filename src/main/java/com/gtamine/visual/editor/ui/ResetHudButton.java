package com.gtamine.visual.editor.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Chat action with a translucent background and normal keyboard/narrator support. */
public final class ResetHudButton extends ButtonWidget {
    public ResetHudButton(net.minecraft.text.Text message, PressAction action) {
        super(0, 0, 1, 18, message, action, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(getX(), getY(), getRight(), getBottom(), isSelected() ? 0xA0404040 : 0x80000000);
        var renderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(renderer, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - renderer.fontHeight) / 2, 0xFFFFFFFF);
    }
}
