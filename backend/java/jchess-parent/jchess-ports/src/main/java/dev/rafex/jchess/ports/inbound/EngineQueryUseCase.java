package dev.rafex.jchess.ports.inbound;

import dev.rafex.jchess.domain.model.EngineInfo;
import dev.rafex.jchess.domain.model.Position;

public interface EngineQueryUseCase {

    EngineInfo engineInfo();

    Position initialPosition();
}
