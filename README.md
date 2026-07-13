# Animal Rescue Maze Backend

Production-structured Spring Boot backend for a quick guest-play maze game. A player chooses an animal, rescues trapped animals, collects stars and reaches home before the authoritative 90-second server timer expires.

No account or database is required. Sessions and leaderboard scores live in memory and are completely deleted every 48 hours. Restarting the application also deletes them.

## Included

- Random perfect-maze generation with guaranteed reachable home and collectibles
- Temporary UUID guest sessions
- Server-controlled timer, movement validation and collision detection
- Star collection, animal rescue and scoring
- Easy, medium and hard difficulty levels
- Slow monkey patrols with short-range, three-move chases and a protected start zone
- Anti-trap collisions: the monkey resets, the player stays put and gains three seconds of protection
- Bush hiding, banana decoys, a key-operated shortcut and rescued-animal followers
- Extra-time, star-magnet, wall-vision, speed and shield power-ups
- A different active ability for every playable animal with a move-based cooldown
- Combo multipliers for quickly collected objectives
- Deterministic UTC daily challenges with filtered daily leaderboards
- REST and raw WebSocket movement interfaces
- Temporary leaderboard with bounded memory usage
- Automatic timeout processing and 48-hour full reset
- Request validation and RFC 9457 problem responses
- Swagger/OpenAPI, Actuator health endpoint, tests, Docker and CI

## Requirements

- Java 17+
- Maven 3.9+, or Docker

## Run

With Maven:

```bash
mvn spring-boot:run
```

Or with Docker:

```bash
docker compose up --build
```

Open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Game flow

1. `POST /api/v1/games` starts a guest session.
2. Render the returned maze. `#` is a wall and `.` is a walkable tile.
3. Render the player, home, remaining stars and rescue targets using their separate coordinates.
4. Send one `UP`, `DOWN`, `LEFT` or `RIGHT` command at a time.
5. The backend validates the move and returns the complete authoritative state.
6. Reaching home changes the status to `WON` and records the score.

Home only opens after every star has been collected and every trapped animal has been rescued. Reaching home early keeps the game active and returns `HOME_LOCKED`.

## REST API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/games` | Start a guest game |
| `GET` | `/api/v1/games/{sessionId}` | Get current state |
| `POST` | `/api/v1/games/{sessionId}/moves` | Move by one tile |
| `POST` | `/api/v1/games/{sessionId}/actions` | Use an ability or drop a banana |
| `DELETE` | `/api/v1/games/{sessionId}` | Abandon the game |
| `GET` | `/api/v1/leaderboard?limit=10` | Get top temporary scores |
| `GET` | `/api/v1/game-options` | Get animals and game configuration |

Start request:

```json
{
  "nickname": "Little Hero",
  "animal": "PANDA",
  "difficulty": "MEDIUM",
  "mode": "NORMAL"
}
```

Move request:

```json
{
  "direction": "RIGHT"
}
```

Action request:

```json
{
  "action": "DROP_BANANA"
}
```

Use `USE_ABILITY` to activate the selected animal&apos;s special move.

Supported player animals are `FOX`, `PANDA`, `RABBIT`, `LION_CUB` and `PUPPY`.

Difficulty settings:

| Difficulty | Maze | Time | Stars | Rescues | Enemies | Power-ups |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Easy | 11×11 | 120s | 6 | 2 | 0 | 2 |
| Medium | 15×15 | 90s | 10 | 3 | 1 | 4 |
| Hard | 19×19 | 75s | 14 | 4 | 2 | 5 |

`DAILY` mode uses the same UTC-date seed for every player on the selected difficulty. `NORMAL` mode generates a fresh maze.

## WebSocket movement

Connect after creating a game:

```text
ws://localhost:8080/ws/games/{sessionId}
```

Send the same move or action JSON used by REST. Each accepted message receives a complete `GameStateResponse`. Game errors use this shape:

```json
{
  "type": "GAME_ERROR",
  "message": "This game is no longer active. Current status: WON"
}
```

## Scoring

| Action | Points |
| --- | ---: |
| Collect a star | 100 |
| Rescue an animal | 500 |
| Reach home | 1,000 |
| Each remaining second | 10 |
| Rescue every animal | 1,000 bonus |
| Collect every star | 500 bonus |
| Finish without a monkey hit | 1,000 bonus |

Only completed games appear on the leaderboard.

## Configuration

Game defaults are in `src/main/resources/application.yml`. `PORT` changes the HTTP port. `ALLOWED_ORIGINS` accepts a comma-separated list of frontend origin patterns.

The default full reset interval is `172800000` milliseconds, exactly 48 hours. Because storage is process memory, deploy one backend instance. Multiple instances would have independent sessions and leaderboards unless a shared store is added later.

## Verification

```bash
mvn clean verify
```

The tests cover maze determinism and reachability, wall collision, successful completion, server-side expiration and complete temporary-data reset.
