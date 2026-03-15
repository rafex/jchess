package dev.rafex.jchess.transport.api;

public record ConnectionStateView(boolean whiteConnected, boolean blackConnected) {
    public static ConnectionStateView disconnected() {
        return new ConnectionStateView(false, false);
    }
}
