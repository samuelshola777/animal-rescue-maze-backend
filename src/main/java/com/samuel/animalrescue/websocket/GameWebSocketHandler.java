package com.samuel.animalrescue.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.samuel.animalrescue.dto.GameActionRequest;
import com.samuel.animalrescue.dto.MoveRequest;
import com.samuel.animalrescue.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final String GAME_ID = "gameId";

    private final GameService gameService;
    private final ObjectMapper objectMapper;

    public GameWebSocketHandler(GameService gameService, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            UUID gameId = extractGameId(session.getUri());
            gameService.get(gameId);
            session.getAttributes().put(GAME_ID, gameId);
        } catch (Exception exception) {
            sendError(session, exception.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            UUID gameId = (UUID) session.getAttributes().get(GAME_ID);
            if (gameId == null) throw new IllegalArgumentException("WebSocket game session is missing");
            JsonNode payload = objectMapper.readTree(message.getPayload());
            Object responseState;
            if (payload.hasNonNull("action")) {
                GameActionRequest request = objectMapper.treeToValue(payload, GameActionRequest.class);
                responseState = gameService.action(gameId, request);
            } else {
                MoveRequest request = objectMapper.treeToValue(payload, MoveRequest.class);
                if (request.direction() == null) throw new IllegalArgumentException("direction is required");
                responseState = gameService.move(gameId, request);
            }
            String response = objectMapper.writeValueAsString(responseState);
            session.sendMessage(new TextMessage(response));
        } catch (Exception exception) {
            sendError(session, exception.getMessage());
        }
    }

    private UUID extractGameId(URI uri) {
        if (uri == null) throw new IllegalArgumentException("WebSocket URI is missing");
        String path = uri.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        return UUID.fromString(id);
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "GAME_ERROR",
                    "message", message == null ? "Game request failed" : message
            ))));
        }
    }
}
