package com.example.demo.common;

import java.util.prefs.Preferences;

/**
 * Simple persistent storage for the JWT used by the desktop client.
 *
 * <p>The token is stored in the user's {@link Preferences} node so that
 * sessions can survive application restarts. A small in-memory cache is
 * used to avoid hitting the preferences API on every request.</p>
 */

public final class TokenStore {

    /** Preference key under which the JWT is stored. */

    private static final String KEY = "jwt";

    /**
     * Preferences node used by the client application.
     *
     * <p>The node path ({@code "com.example.demo"}) should match the
     * application's package name or another stable identifier.</p>
     */

    private static final Preferences PREF = Preferences.userRoot().node("com.example.demo");
    private static String cached;

    /**
     * Utility class; not meant to be instantiated.
     */

    private TokenStore() {}

    /**
     * Stores the given JWT and updates the in-memory cache.
     *
     * <p>If the provided token is {@code null} or blank, an empty string
     * is stored, effectively clearing the token.</p>
     *
     * @param token the JWT to store, or {@code null} to clear it
     */

    public static void set(String token) {
        cached = token;
        try { PREF.put(KEY, token == null ? "" : token); } catch (Exception ignored) {}
    }

    /**
     * Returns the currently stored JWT.
     *
     * <p>If a non-blank value is present in the in-memory cache, it is
     * returned directly. Otherwise the value is loaded from
     * {@link Preferences}, cached and then returned.</p>
     *
     * @return the stored token, or an empty string if none is stored
     */

    public static String get() {
        if (cached != null && !cached.isBlank()) return cached;
        String v = PREF.get(KEY, "");
        cached = v;
        return v;
    }

    /**
     * Checks whether a non-blank token is currently stored.
     *
     * @return {@code true} if {@link #get()} returns a non-null, non-blank value;
     *         {@code false} otherwise
     */

    public static boolean hasToken() { return get() != null && !get().isBlank(); }

    /**
     * Clears the stored token.
     *
     * <p>Internally this is equivalent to calling {@link #set(String)} with
     * an empty string.</p>
     */
    public static void clear()
    {
        set("");
    }
}
