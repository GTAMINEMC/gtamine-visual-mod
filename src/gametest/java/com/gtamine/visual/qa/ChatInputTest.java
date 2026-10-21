package com.gtamine.visual.qa;

import com.gtamine.visual.editor.ui.ResetHudButton;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

/** Verifies vanilla chat history and submission with the editor action present. */
public final class ChatInputTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                client.inGameHud.getChatHud().addToMessageHistory("/help");
                for (int enter : new int[] {GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER}) {
                    var chat = new RecordingChat();
                    client.setScreen(chat);
                    var reset = Screens.getButtons(chat).stream()
                            .filter(button -> button instanceof ResetHudButton).findFirst().orElseThrow();
                    // Singleplayer normally hides the editor; reproduce its visible server state.
                    reset.visible = true;
                    chat.keyPressed(key(GLFW.GLFW_KEY_UP));
                    if (!(chat.getFocused() instanceof TextFieldWidget field)
                            || !field.getText().equals("/help")) {
                        throw new AssertionError("Up must select chat history without focusing reset");
                    }
                    chat.keyPressed(key(enter));
                    assertSent(chat);

                    chat = new RecordingChat();
                    client.setScreen(chat);
                    reset = Screens.getButtons(chat).stream()
                            .filter(button -> button instanceof ResetHudButton).findFirst().orElseThrow();
                    reset.visible = true;
                    chat.keyPressed(key(GLFW.GLFW_KEY_UP));
                    // Chat reserves Tab for completion. Simulate accessibility focus directly.
                    if (reset.getNavigationPath(new GuiNavigation.Tab(true)) == null) {
                        throw new AssertionError("Reset must retain non-arrow navigation");
                    }
                    chat.setFocused(reset);
                    chat.keyPressed(key(enter));
                    assertSent(chat);
                }
            });
        }
    }

    private static KeyInput key(int key) { return new KeyInput(key, 0, 0); }

    private static void assertSent(RecordingChat chat) {
        if (chat.sent != 1 || !"/help".equals(chat.message)) {
            throw new AssertionError("One Enter must submit the command exactly once");
        }
    }

    private static final class RecordingChat extends ChatScreen {
        int sent;
        String message;

        RecordingChat() { super("", false); }

        @Override
        public void sendMessage(String message, boolean addToHistory) {
            this.message = message;
            sent++;
        }
    }
}
