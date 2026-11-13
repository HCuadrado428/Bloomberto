package com.dougg865.bloomberto.api.animation.model;

/** Implemented by entities and block entities that are drawn with an animated Blockbench model. */
public interface Animatable {
    /** The controller that decides which animation plays; create it once and keep it. */
    AnimationController animationController();
}
