package com.dougg865.bloomberto.platform.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client-only helper, only loaded when a packet is handled on the client. */
final class ForgeClientNetwork {
    private ForgeClientNetwork() {
    }

    static Player localPlayer() {
        return Minecraft.getInstance().player;
    }
}
