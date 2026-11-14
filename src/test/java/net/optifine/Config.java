package net.optifine;

/** Test stand-in for OptiFine's {@code net.optifine.Config}, which Bloomberto reads by reflection. */
public final class Config {
    public static boolean shaders;

    private Config() {
    }

    public static boolean isShaders() {
        return shaders;
    }
}
