package dev.rafex.jchess.adapter.sqlite;

import dev.rafex.jchess.core.engine.FenCodec;
import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.GameSession;
import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.GameStatus;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.RecordedMove;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.ports.outbound.GameRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteGameRepository implements GameRepository {
    private final String jdbcUrl;

    public SqliteGameRepository(Path databasePath) {
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    @Override
    public void initialize() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("pragma journal_mode = wal");
            statement.executeUpdate("create table if not exists schema_version (version integer not null)");
            migrate(connection);
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to initialize sqlite schema", ex);
        }
    }

    @Override
    public void save(GameState gameState) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            upsertSession(connection, gameState, false, gameState.session().version());
            replaceMoves(connection, gameState);
            connection.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to save game state", ex);
        }
    }

    @Override
    public boolean saveIfVersionMatches(GameState gameState, long expectedVersion) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            boolean updated = upsertSession(connection, gameState, true, expectedVersion);
            if (!updated) {
                connection.rollback();
                return false;
            }
            replaceMoves(connection, gameState);
            connection.commit();
            return true;
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to save game state", ex);
        }
    }

    @Override
    public Optional<GameState> findById(UUID sessionId) {
        try (Connection connection = openConnection()) {
            GameSession session = loadSession(connection, sessionId);
            if (session == null) {
                return Optional.empty();
            }
            return Optional.of(new GameState(session, loadMoves(connection, sessionId)));
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to load game state", ex);
        }
    }

    private void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists game_session (
                        session_id text primary key,
                        white_participant text not null,
                        black_participant text not null,
                        preferred_human_side text,
                        llm_provider text,
                        current_fen text not null,
                        status text not null,
                        result text not null default 'IN_PROGRESS',
                        end_reason text not null default 'NONE',
                        white_player_id text not null default '',
                        black_player_id text not null default '',
                        white_player_name text not null default 'white',
                        black_player_name text not null default 'black',
                        white_player_token text not null default '',
                        black_player_token text not null default '',
                        version integer not null default 0,
                        created_at text not null,
                        updated_at text not null default '1970-01-01T00:00:00Z'
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists game_move (
                        session_id text not null,
                        ply integer not null,
                        side text not null,
                        submitted_notation text not null,
                        canonical_notation text not null,
                        uci text not null,
                        fen_before text not null default '',
                        fen_after text not null default '',
                        played_at text not null,
                        primary key (session_id, ply)
                    )
                    """);
            statement.executeUpdate("create index if not exists idx_game_move_session on game_move(session_id)");
            ensureColumn(statement, "game_session", "result", "text not null default 'IN_PROGRESS'");
            ensureColumn(statement, "game_session", "end_reason", "text not null default 'NONE'");
            ensureColumn(statement, "game_session", "white_player_id", "text not null default ''");
            ensureColumn(statement, "game_session", "black_player_id", "text not null default ''");
            ensureColumn(statement, "game_session", "white_player_name", "text not null default 'white'");
            ensureColumn(statement, "game_session", "black_player_name", "text not null default 'black'");
            ensureColumn(statement, "game_session", "white_player_token", "text not null default ''");
            ensureColumn(statement, "game_session", "black_player_token", "text not null default ''");
            ensureColumn(statement, "game_session", "version", "integer not null default 0");
            ensureColumn(statement, "game_session", "updated_at", "text not null default '1970-01-01T00:00:00Z'");
            ensureColumn(statement, "game_move", "fen_before", "text not null default ''");
            ensureColumn(statement, "game_move", "fen_after", "text not null default ''");
        }
    }

    private void ensureColumn(Statement statement, String table, String column, String ddl) throws SQLException {
        try {
            statement.executeUpdate("alter table " + table + " add column " + column + " " + ddl);
        } catch (SQLException ignored) {
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private boolean upsertSession(Connection connection, GameState gameState, boolean checkVersion, long expectedVersion)
            throws SQLException {
        String sql = """
                insert into game_session (
                    session_id,
                    white_participant,
                    black_participant,
                    preferred_human_side,
                    llm_provider,
                    current_fen,
                    status,
                    result,
                    end_reason,
                    white_player_id,
                    black_player_id,
                    white_player_name,
                    black_player_name,
                    white_player_token,
                    black_player_token,
                    version,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict(session_id) do update set
                    white_participant = excluded.white_participant,
                    black_participant = excluded.black_participant,
                    preferred_human_side = excluded.preferred_human_side,
                    llm_provider = excluded.llm_provider,
                    current_fen = excluded.current_fen,
                    status = excluded.status,
                    result = excluded.result,
                    end_reason = excluded.end_reason,
                    white_player_id = excluded.white_player_id,
                    black_player_id = excluded.black_player_id,
                    white_player_name = excluded.white_player_name,
                    black_player_name = excluded.black_player_name,
                    white_player_token = excluded.white_player_token,
                    black_player_token = excluded.black_player_token,
                    version = excluded.version,
                    created_at = excluded.created_at,
                    updated_at = excluded.updated_at
                """ + (checkVersion ? " where game_session.version = ?" : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            GameSession session = gameState.session();
            statement.setString(1, session.sessionId().toString());
            statement.setString(2, session.whiteParticipant().name());
            statement.setString(3, session.blackParticipant().name());
            statement.setString(4, session.preferredHumanSide() == null ? null : session.preferredHumanSide().name());
            statement.setString(5, session.llmProvider() == null ? null : session.llmProvider().name());
            statement.setString(6, FenCodec.toFen(session.currentPosition()));
            statement.setString(7, session.status().name());
            statement.setString(8, session.result().name());
            statement.setString(9, session.endReason().name());
            statement.setString(10, session.whitePlayerId().toString());
            statement.setString(11, session.blackPlayerId().toString());
            statement.setString(12, session.whitePlayerName());
            statement.setString(13, session.blackPlayerName());
            statement.setString(14, session.whitePlayerToken());
            statement.setString(15, session.blackPlayerToken());
            statement.setLong(16, session.version());
            statement.setString(17, session.createdAt().toString());
            statement.setString(18, session.updatedAt().toString());
            if (checkVersion) {
                statement.setLong(19, expectedVersion);
            }
            return statement.executeUpdate() > 0;
        }
    }

    private void replaceMoves(Connection connection, GameState gameState) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement("delete from game_move where session_id = ?")) {
            deleteStatement.setString(1, gameState.session().sessionId().toString());
            deleteStatement.executeUpdate();
        }

        try (PreparedStatement insertStatement = connection.prepareStatement("""
                insert into game_move (
                    session_id,
                    ply,
                    side,
                    submitted_notation,
                    canonical_notation,
                    uci,
                    fen_before,
                    fen_after,
                    played_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (RecordedMove move : gameState.moves()) {
                insertStatement.setString(1, gameState.session().sessionId().toString());
                insertStatement.setInt(2, move.ply());
                insertStatement.setString(3, move.side().name());
                insertStatement.setString(4, move.submittedNotation());
                insertStatement.setString(5, move.canonicalNotation());
                insertStatement.setString(6, move.uci());
                insertStatement.setString(7, move.fenBefore());
                insertStatement.setString(8, move.fenAfter());
                insertStatement.setString(9, move.playedAt().toString());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private GameSession loadSession(Connection connection, UUID sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    session_id,
                    white_participant,
                    black_participant,
                    preferred_human_side,
                    llm_provider,
                    current_fen,
                    status,
                    result,
                    end_reason,
                    white_player_id,
                    black_player_id,
                    white_player_name,
                    black_player_name,
                    white_player_token,
                    black_player_token,
                    version,
                    created_at,
                    updated_at
                from game_session
                where session_id = ?
                """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new GameSession(
                        UUID.fromString(resultSet.getString("session_id")),
                        ParticipantType.valueOf(resultSet.getString("white_participant")),
                        ParticipantType.valueOf(resultSet.getString("black_participant")),
                        sideOrNull(resultSet.getString("preferred_human_side")),
                        llmProviderOrNull(resultSet.getString("llm_provider")),
                        FenCodec.parse(resultSet.getString("current_fen")),
                        GameStatus.valueOf(resultSet.getString("status")),
                        GameResult.valueOf(resultSet.getString("result")),
                        GameEndReason.valueOf(resultSet.getString("end_reason")),
                        UUID.fromString(resultSet.getString("white_player_id")),
                        UUID.fromString(resultSet.getString("black_player_id")),
                        resultSet.getString("white_player_name"),
                        resultSet.getString("black_player_name"),
                        resultSet.getString("white_player_token"),
                        resultSet.getString("black_player_token"),
                        resultSet.getLong("version"),
                        Instant.parse(resultSet.getString("created_at")),
                        Instant.parse(resultSet.getString("updated_at"))
                );
            }
        }
    }

    private List<RecordedMove> loadMoves(Connection connection, UUID sessionId) throws SQLException {
        List<RecordedMove> moves = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                select ply, side, submitted_notation, canonical_notation, uci, fen_before, fen_after, played_at
                from game_move
                where session_id = ?
                order by ply asc
                """)) {
            statement.setString(1, sessionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    moves.add(new RecordedMove(
                            resultSet.getInt("ply"),
                            Side.valueOf(resultSet.getString("side")),
                            resultSet.getString("submitted_notation"),
                            resultSet.getString("canonical_notation"),
                            resultSet.getString("uci"),
                            resultSet.getString("fen_before"),
                            resultSet.getString("fen_after"),
                            Instant.parse(resultSet.getString("played_at"))
                    ));
                }
            }
        }

        return List.copyOf(moves);
    }

    private Side sideOrNull(String value) {
        return value == null ? null : Side.valueOf(value);
    }

    private LlmProvider llmProviderOrNull(String value) {
        return value == null ? null : LlmProvider.valueOf(value);
    }
}
