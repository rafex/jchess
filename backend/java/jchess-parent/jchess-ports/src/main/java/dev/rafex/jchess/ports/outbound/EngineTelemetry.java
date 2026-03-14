package dev.rafex.jchess.ports.outbound;

public interface EngineTelemetry {

    void record(String event, String detail);
}
