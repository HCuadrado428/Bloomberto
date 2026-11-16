package com.dougg865.bloomberto.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client-only helpers, kept in their own class so server code never loads client classes. */
public final class ClientMessages {
    private ClientMessages() {
    }

    /** Shows a line in the chat of the local player, on the client thread. */
    public static void chat(String text) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.gui.getChat().addMessage(Component.literal(text));
            }
        });
    }
}
