package com.dougg865.bloomberto.api.ui;

import com.dougg865.bloomberto.Bloomberto;
import net.minecraft.resources.ResourceLocation;

/** The textures a set of widgets is drawn with. Each texture is a 9-slice. */
public record UiTheme(ResourceLocation button, ResourceLocation buttonHover, ResourceLocation buttonDisabled,
                      ResourceLocation panel, ResourceLocation sliderTrack, ResourceLocation sliderKnob,
                      NineSlice slice, int textColor, int disabledTextColor) {
    /** Bloomberto's own dark theme. */
    public static final UiTheme DEFAULT = new UiTheme(
            tex("button"), tex("button_hover"), tex("button_disabled"), tex("panel"), tex("slider_track"), tex("slider_knob"),
            new NineSlice(16, 16, 3), 0xFFFFFFFF, 0xFF909090);

    private static ResourceLocation tex(String name) {
        return Bloomberto.id("textures/gui/" + name + ".png");
    }
}
