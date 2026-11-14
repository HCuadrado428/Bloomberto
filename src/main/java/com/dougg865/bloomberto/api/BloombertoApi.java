package com.dougg865.bloomberto.api;

/**
 * The API level of the installed Bloomberto. It goes up whenever a public class or method of {@code com.dougg865.bloomberto.api}
 * is added, removed or changes behavior in a way that matters to dependent mods (see {@code CHANGELOG.md}, which lists the level
 * of every release). Jars with the same version number can differ while a release is in development, so check the level instead
 * of trusting the version.
 *
 * <p>Check it as early as possible, before touching any other Bloomberto class:
 *
 * <pre>{@code
 * // 1. A jar that is too old to have this class at all (safe on a dedicated server: the class is only looked up, not loaded)
 * if (MyMod.class.getClassLoader().getResource("com/dougg865/bloomberto/api/BloombertoApi.class") == null) {
 *     throw new IllegalStateException("MyMod needs Bloomberto 0.2.0 or newer; the installed jar is too old");
 * }
 * // 2. A jar that has it but is older than what you use
 * BloombertoApi.require("MyMod", 2);
 * }</pre>
 */
public final class BloombertoApi {
    /** The level of this build. Read it with {@link #level()}: the compiler would copy the constant into your mod. */
    private static final int LEVEL = 2;

    private BloombertoApi() {
    }

    /** The API level of the installed Bloomberto. */
    public static int level() {
        return LEVEL;
    }

    /** {@code true} if the installed Bloomberto has at least API level {@code minLevel}. */
    public static boolean has(int minLevel) {
        return LEVEL >= minLevel;
    }

    /**
     * Throws a clear error if the installed Bloomberto is older than {@code minLevel}.
     *
     * @param modName the mod that needs it, for the message
     */
    public static void require(String modName, int minLevel) {
        if (LEVEL < minLevel) {
            throw new IllegalStateException(modName + " needs Bloomberto API level " + minLevel + " or newer, but the installed "
                    + "jar has level " + LEVEL + ". Replace the Bloomberto jar in your mods folder with the latest release.");
        }
    }
}
