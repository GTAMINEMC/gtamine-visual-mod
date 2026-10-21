package com.gtamine.visual.editor.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;

/** Chat action with a translucent background and normal keyboard/narrator support. */
public final class ResetHudButton extends ButtonWidget {
    public ResetHudButton(net.minecraft.text.Text message, PressAction action) {
        super(0, 0, 1, 18, message, action, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        // Chat uses arrows for history and cursor movement; preserve other navigation paths.
        return navigation instanceof GuiNavigation.Arrow ? null : super.getNavigationPath(navigation);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Enter (including keypad Enter) belongs to chat even when this button has focus.
        // Space still activates the action for keyboard users.
        return !input.isEnter() && super.keyPressed(input);
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(getX(), getY(), getRight(), getBottom(), isSelected() ? 0xA0404040 : 0x80000000);
        var renderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(renderer, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - renderer.fontHeight) / 2, 0xFFFFFFFF);
    }
}
