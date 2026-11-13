package com.dougg865.bloomberto.api.animation;

/** Something that advances one game tick at a time until it finishes. */
public interface Animation {
    /** Advances by one tick. @return {@code true} once the animation has finished. */
    boolean tick();

    boolean isFinished();

    /** Rewinds to the start so it can play again. */
    void reset();
}
