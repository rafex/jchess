package dev.rafex.jchess.domain.model;

public enum MachineGameMode {
    CASUAL,
    COMPETITIVE;

    public static MachineGameMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return CASUAL;
        }
        return MachineGameMode.valueOf(value.trim().toUpperCase());
    }
}
