package com.dougg865.bloomberto.api.scheduler;

/**
 * The shared schedulers, ticked by Bloomberto. Use {@link #server()} for logic that touches the
 * server and {@link #client()} for client-only logic.
 */
public final class Schedulers {
    private static final Scheduler CLIENT = new Scheduler();
    private static final Scheduler SERVER = new Scheduler();

    private Schedulers() {
    }

    /** Ticked at the end of every client tick. Only use from the client thread. */
    public static Scheduler client() {
        return CLIENT;
    }

    /** Ticked at the end of every server tick. Only use from the server thread. */
    public static Scheduler server() {
        return SERVER;
    }
}
