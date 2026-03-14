package dev.rafex.jchess.ports.outbound;

import dev.rafex.jchess.domain.model.Position;

import java.util.Optional;

public interface PositionRepository {

    Optional<Position> loadInitialPosition();
}
