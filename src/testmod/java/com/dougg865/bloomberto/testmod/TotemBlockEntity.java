package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.Bloomberto;
import com.dougg865.bloomberto.api.animation.model.Animatable;
import com.dougg865.bloomberto.api.animation.model.AnimationController;
import com.dougg865.bloomberto.api.animation.model.ModelLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The block entity of {@link TotemBlock}. Its animation controller is created on first use, which only happens on the client. */
public final class TotemBlockEntity extends BlockEntity implements Animatable {
    private AnimationController controller;

    public TotemBlockEntity(BlockPos pos, BlockState state) {
        super(TestMod.TOTEM_BE.get(), pos, state);
    }

    @Override
    public AnimationController animationController() {
        if (controller == null) {
            controller = ModelLibrary.animations(Bloomberto.id(TestMod.MOD_ID, "animations/totem.animation.json")).controller();
            controller.play("animation.totem.idle");
        }
        return controller;
    }
}
