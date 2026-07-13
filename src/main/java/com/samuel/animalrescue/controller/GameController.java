package com.samuel.animalrescue.controller;

import com.samuel.animalrescue.dto.GameOptionsResponse;
import com.samuel.animalrescue.dto.GameActionRequest;
import com.samuel.animalrescue.dto.GameStateResponse;
import com.samuel.animalrescue.dto.LeaderboardResponse;
import com.samuel.animalrescue.dto.MoveRequest;
import com.samuel.animalrescue.dto.StartGameRequest;
import com.samuel.animalrescue.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import com.samuel.animalrescue.model.Difficulty;
import com.samuel.animalrescue.model.GameMode;

@Validated
@RestController
@RequestMapping("/api/v1")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Start a temporary guest game")
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public GameStateResponse start(@Valid @RequestBody StartGameRequest request) {
        return gameService.start(request);
    }

    @Operation(summary = "Get the authoritative state of a game")
    @GetMapping("/games/{sessionId}")
    public GameStateResponse get(@PathVariable UUID sessionId) {
        return gameService.get(sessionId);
    }

    @Operation(summary = "Move the player by one maze tile")
    @PostMapping("/games/{sessionId}/moves")
    public GameStateResponse move(@PathVariable UUID sessionId,
                                  @Valid @RequestBody MoveRequest request) {
        return gameService.move(sessionId, request);
    }

    @Operation(summary = "Use the animal ability or drop a banana decoy")
    @PostMapping("/games/{sessionId}/actions")
    public GameStateResponse action(@PathVariable UUID sessionId,
                                    @Valid @RequestBody GameActionRequest request) {
        return gameService.action(sessionId, request);
    }

    @Operation(summary = "Abandon an active game")
    @DeleteMapping("/games/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@PathVariable UUID sessionId) {
        gameService.abandon(sessionId);
    }

    @Operation(summary = "Get the temporary leaderboard")
    @GetMapping("/leaderboard")
    public LeaderboardResponse leaderboard(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) GameMode mode,
            @RequestParam(required = false) Difficulty difficulty) {
        return gameService.leaderboard(limit, mode, difficulty);
    }

    @Operation(summary = "Get frontend game options and map encoding")
    @GetMapping("/game-options")
    public GameOptionsResponse options() {
        return gameService.options();
    }
}
