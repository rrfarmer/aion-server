# C# Port Handoff Plan

## Migration Progress

| Phase | Status | Completion Date | Notes |
|-------|--------|-----------------|-------|
| Phase 0: Starter Workspace | COMPLETE | May 18, 2026 | Solution structure, projects, and entry points scaffolded. See [PHASE-0-COMPLETION.md](PHASE-0-COMPLETION.md) |
| Phase 1: Parity Harness | COMPLETE | May 18, 2026 | Packet tests, config tests, database fixtures, XML tool. See [PHASE-1-COMPLETION.md](PHASE-1-COMPLETION.md) |
| Phase 2: Port Commons | COMPLETE | May 18, 2026 | Logging, crypto, socket server base, scheduler. 56 tests passing. See [PHASE-2-COMPLETION.md](PHASE-2-COMPLETION.md) |
| Phase 3: Port Login Server | COMPLETE | May 19, 2026 | Authentication, game-server registration, session management, Java GS mixed mode, and real-client login/create/logout validated. See [PHASE-3-COMPLETION.md](PHASE-3-COMPLETION.md) |
| Phase 4: Port Chat Server | COMPLETE | May 20, 2026 | Chat protocol, channels, game-server bridge, DB logging, handler pipeline, and real-client mixed mode validated. See [PHASE-4-COMPLETION.md](PHASE-4-COMPLETION.md) |
| Phase 5: Port Game Infrastructure | COMPLETE | May 20, 2026 | Game socket, bridges, bootstrap, ID factory, static data, and character-selection infrastructure. See [PHASE-5-PROGRESS.md](PHASE-5-PROGRESS.md) |
| Phase 6: Port Game Core | IN PROGRESS | - | Characters, movement, combat, loot, quests. Use the latest `Phase-6-Session-*-Handoff.md` for current context; [PHASE-6-PROGRESS.md](PHASE-6-PROGRESS.md) is a historical archive. |
| Phase 7: Port Dynamic Handlers | PENDING | - | Commands, zones, instances, AI, quests |
| Phase 8: Replacement Readiness | PENDING | - | Docker, soak tests, rollback plan |

## Goal

Port the current Aion server from Java to C#/.NET with behavior kept as close to 1:1 as practical. The current Java implementation works and should remain the correctness reference until the C# implementation proves parity.

Preserve the existing three-process architecture:

- `login-server`
- `game-server`
- `chat-server`

Do not change these contracts unless a later plan explicitly approves it:

- MySQL/MariaDB schemas and existing SQL behavior
- `.properties` config keys and override files
- Binary packet wire formats
- XML/static data shape and validation expectations
- Docker-oriented deployment flow
- Login/game/chat service boundaries

The port should favor direct, boring equivalence over redesign. Make the C# code understandable for a non-Java maintainer, but do not use the migration as a chance to re-architect gameplay systems before parity exists.

## Current Repo Facts

This repository is a Maven multi-module Java project:

- `commons`: shared logging, config, database, networking, scripting, utilities
- `login-server`: login/authentication server and game-server registration
- `chat-server`: chat service using Netty plus shared server networking
- `game-server`: main game simulation, persistence, packet protocol, static data, geo, services, quests, AI, instances, commands

The game server is the largest migration risk. It includes thousands of Java source files, runtime Java handlers under `game-server/data/handlers`, JAXB XML data loading, custom NIO packet handling, geo data, many scheduled services, and a large amount of gameplay state.

Runtime handlers are part of the real application, not optional extras. The C# port must account for:

- Admin/player/console commands
- AI handlers
- Instance handlers
- Quest handlers
- Zone handlers

The Java implementation is the oracle. When behavior is unclear, inspect and match Java behavior instead of guessing.

## Target C# Workspace

Use `dotnetConversion/` as the C# workspace. Keep Java and C# side by side until the C# server is production-ready.

Current layout:

```text
dotnetConversion/
  AionServer.slnx
  src/
    Aion.Commons/
    Aion.LoginServer/
    Aion.ChatServer/
    Aion.GameServer/
  tests/
    Aion.Commons.Tests/
    Aion.LoginServer.Tests/
    Aion.ChatServer.Tests/
    Aion.GameServer.Tests/
  tools/
    Aion.PortParity/
```

Target `.NET 10 LTS`.

Use pragmatic minimal dependencies:

- `MySqlConnector` for MySQL/MariaDB access
- Microsoft logging/hosting/configuration packages where useful
- Roslyn for dynamic handler compilation/loading
- A scheduler package only if the built-in .NET timer/background-service model is not enough

Avoid ORM adoption during parity work. Keep SQL close to the Java DAO layer so behavior can be compared directly.

## Migration Phases

### Phase 0: Starter Workspace

Create the initial `.NET` solution under `dotnetConversion/`.

Deliverables:

- `AionServer.slnx`
- Empty service entrypoints for login, chat, and game
- Shared `Aion.Commons` project
- Test projects
- Basic build script or documented `dotnet build` command
- Initial Docker plan, but no production replacement yet

Rules:

- Do not remove or rewrite Java modules.
- Do not change existing Docker files for Java unless the change is explicitly scoped.
- Keep C# project names stable from this phase onward.

### Phase 1: Parity Harness

Build tests and tooling before porting deep gameplay logic.

Deliverables:

- Packet golden-test infrastructure for read/write byte parity
- Config-loading tests for default properties plus `my*.properties` overrides
- Database fixture tests against fresh schemas
- XML/static-data load-count comparison tests
- Mixed Java/C# smoke-test scripts or documented runbooks

Acceptance:

- A C# test can assert exact packet bytes for representative login, game, chat, and inter-server packets.
- A C# tool can compare major XML data counts against Java startup logs or Java-generated reference output.
- A developer can run parity tests without replacing the working Java server.

### Phase 2: Port Commons

Port the shared infrastructure needed by all three servers.

Subsystems:

- Logging bootstrap and structured log conventions
- `.properties` loader with default and override precedence
- Config binding for static-like config classes
- Database connection factory and direct SQL helpers
- Little-endian packet buffer helpers
- Base packet abstractions
- Shared socket server primitives
- Threading/scheduler abstractions
- XML utilities and schema validation helpers
- Crypto helpers, including any custom Aion packet crypto
- Dynamic handler loading foundation

Rules:

- Keep config keys identical.
- Keep direct SQL style.
- Use direct C# equivalents for Java synchronization/concurrency concepts.
- Add tests for each primitive before using it in server ports.

### Phase 3: Port Login Server

Port login-server first because it is relatively small and protocol-heavy.

Current status:

- Phase 3 is complete for known login-server parity work as of May 19, 2026.
- Login client crypto, RSA key/modulus handling, encrypted frame read/write, and Java-generated crypto/RSA/`SM_INIT` plus currently modeled login-client and game-server bridge server-packet golden vectors are in place.
- DB-backed account authentication, Java database config/`DatabaseFactory` startup initialization, Java-shaped account insert and auto-create defaults, startup-loaded banned-IP controller semantics, opt-in DB-backed encrypted login socket smoke, normal and `-loginex` RSA credential layouts, external auth, brute-force ban escalation, core account-state/account-time/penalty behavior, and mixed-mode Java chat+GS / C# LS validation setup are ported and smoke-validated.
- Game-server registration, GS auth failure/duplicate close behavior, Java-style malformed packet read/default handling for live login/GS buffers, hosted-service startup ordering, player-transfer scheduler startup/shutdown ordering, encrypted fake-auth login socket smoke, Java-style live client checksum verification from captured `CM_AUTH_GG`, Java-style `CM_AUTH_GG`/`CM_LOGIN`/`CM_SERVER_LIST` session-id rejection, encrypted account-banned and duplicate-login client handling, encrypted `CM_UPDATE_SESSION` reconnect success/failure, no-server-list and offline-server-list behavior, fake-GS server-list/play/account-auth socket smoke, `CM_PLAY` failure branch coverage, login handoff, reconnect, account reconnect/disconnect lifecycle, toll/allowed-HDD/account-list sync, server-list refresh fanout, character-count fanout, ping/pong live loop, IP/MAC/HDD ban lists with Java-style startup cleanup plus lazy map load, account controls, player-transfer bridge slices, pre-client GS bridge behavior coverage for LS control/ban/premium/player-transfer/account-list/account-connection/MAC-HDD ban side effects, client and game-server packet parser parity audits, connection shutdown send/close protection, and loopback listener smoke coverage are ported.
- Full .NET suite passes with 180 tests; the opt-in MySQL schema integration tests pass against a Dockerized MySQL 8.4 instance.
- Mixed-mode validation passed with Java chat and game servers in Docker, C# login server locally, and a real client completing login, character creation, and logout.

Deliverables:

- Login client socket listener
- Game-server socket listener
- Account authentication flow
- Game-server registration/authentication
- Session keys and reconnect behavior
- Ban checks and account-time behavior
- Login/game inter-server packets
- Shutdown behavior

Validation:

- C# login server interoperates with the existing Java game server.
- Real client authenticates, receives server-list/play responses, creates a character through the Java game server, and logs out cleanly.
- Existing login database schema works without migration.
- Packet golden tests pass for login protocol packets.

### Phase 4: Port Chat Server

Port chat-server after login.

Current status:

- Phase 4 is complete as of May 20, 2026.
- C# chat loads Java config/database settings, accepts client chat connections on `10241`, accepts Java/C# game-server bridge connections on `9021`, and uses the existing `aion_cs.chatlog` schema.
- Chat packet models, frame codec behavior, channel logic, localized job-channel aliases, flood/filter/logging handlers, hosted listener smoke tests, opt-in live DB validation, and C# login + Java game + C# chat real-client mixed mode are validated.

Deliverables:

- Client chat socket listener
- Game-server chat bridge
- Channel create/join/leave/message behavior
- Player chat auth and player info handling
- Ping/keepalive behavior
- Chat packet encoder/decoder parity

Validation:

- C# chat server can interoperate with Java login/game where applicable.
- Real client can join channels and send/receive messages.
- Existing chat database schema works without migration.

### Phase 5: Port Game Infrastructure

Start the game server with infrastructure before gameplay.

Deliverables:

- Game client socket listener
- Login-server connector
- Chat-server connector
- Packet processor with per-connection ordering guarantees
- Object ID factory
- Game startup sequence shell
- Static XML data merge/cache/load path
- XML validation path
- World/bootstrap containers
- Scheduler/thread pool equivalent
- Basic shutdown and persistence hooks

Validation:

- C# game server starts far enough to load config and static data.
- XML load counts match Java for major data holders.
- Login/chat bridge packets match golden tests.
- Empty or limited world bootstrap can run without replacing Java production.

### Phase 6: Port Game Core

Port gameplay systems in dependency order, validating continuously.

Subsystem order:

- Account and character list flow
- Character create/delete/restore
- Player enter-world flow
- Inventory and equipment
- Movement and known-list updates
- NPC and spawn engine basics
- Skills and effects
- Combat and damage
- Loot and item use
- Quest state persistence
- Player logout/save flow
- Periodic saves and recovery behavior

Validation:

- Real client can enter world, move, chat, fight a basic NPC, loot, persist, logout, restart, and re-enter.
- Java and C# database writes remain compatible.
- Packet golden tests grow with each subsystem.

### Phase 7: Port Dynamic Handlers

Port runtime handlers after the core APIs they depend on exist.

Order:

1. Console/player/admin commands
2. Zone handlers
3. Instance handlers
4. AI handlers
5. Quest handlers

Rules:

- Keep handler public names close to Java names.
- Preserve registration behavior and event callbacks.
- Prefer mechanical direct ports.
- Do not simplify quest or AI behavior unless a failing parity test proves Java behavior is wrong and the change is approved.

Validation:

- Handler counts match expected Java load counts.
- Representative commands, zones, instances, AI, and quests pass functional tests.
- Missing handler coverage is tracked explicitly.

### Phase 8: Replacement Readiness

Only consider replacing Java after staged parity is proven.

Deliverables:

- Full Docker setup for C# services
- Runbook for Java/C# mixed mode and full C# mode
- Soak-test results
- Known gaps list
- Rollback plan
- Backup/restore checklist for databases

Acceptance:

- Login flow works with a real client.
- Server list works.
- Character creation, deletion, and restore work.
- Enter world works.
- Movement works.
- Chat works.
- Combat works.
- Loot works.
- Quest dialog works.
- Logout and restart persistence work.
- XML load counts match Java for major datasets.
- Packet golden tests pass.
- Mixed Java/C# compatibility passes during staged replacement.

## Agent Rules

- Keep Java behavior as the source of truth.
- Prefer direct ports over redesign.
- Do not change packet wire format.
- Do not change database schema.
- Do not change XML shape.
- Do not change config keys.
- Add tests before or alongside each subsystem port.
- Keep C# public names close to Java names until parity is complete.
- Keep the Java server runnable throughout the migration.
- Treat runtime handlers as application code, not data.
- Record every intentional behavior difference in the handoff notes or a dedicated gaps file.

## Implementation Guidance

Use the Java source to drive naming and behavior:

- Java `commons` maps to `Aion.Commons`.
- Java `login-server` maps to `Aion.LoginServer`.
- Java `chat-server` maps to `Aion.ChatServer`.
- Java `game-server` maps to `Aion.GameServer`.

Recommended C# patterns:

- Use `Span<byte>`, `Memory<byte>`, `BinaryPrimitives`, and pooled buffers for packet work.
- Use `CancellationToken` for shutdown-aware background tasks.
- Use `Task`, `Channel<T>`, or dedicated worker queues where they preserve Java packet ordering.
- Use direct ADO.NET-style SQL with `MySqlConnector`.
- Use `AssemblyLoadContext` for dynamically compiled handlers.
- Use tests to freeze byte-level packet behavior before refactoring ergonomics.

Risk areas to handle carefully:

- Blowfish/custom packet crypto
- Packet length/opcode encoding
- Per-client packet execution order
- Java synchronized/volatile semantics translated to C#
- JAXB field/attribute defaults and post-unmarshal hooks
- XML merge/cache validation behavior
- Runtime handler reload/unload behavior
- Login/game/chat inter-server protocol compatibility
- Long-running scheduled game services
- Database writes that depend on Java transaction/autocommit behavior

## Acceptance Checklist

The C# port is not considered ready until all of these are true:

- Real Aion client can log in through C# login server.
- Client can see server list and select the game server.
- Client can create and delete characters.
- Client can enter world.
- Movement is visible and persisted correctly.
- Chat works through the C# chat path.
- Combat and skill use work for representative player/NPC cases.
- Loot and inventory persistence work.
- Quest dialog and quest progress work for representative quests.
- Logout saves state.
- Restart and re-entry preserve player state.
- Major XML data counts match Java.
- Packet golden tests pass.
- Mixed Java/C# mode has passed for login, chat, and game boundaries.
- Full C# Docker mode has passed smoke and soak tests.

## Explicit Non-Goals For Initial Port

- Do not redesign gameplay systems.
- Do not replace MySQL/MariaDB with another database.
- Do not introduce Entity Framework for the parity port.
- Do not convert XML data formats to JSON/YAML.
- Do not merge login, game, and chat into a single process.
- Do not remove dynamic handler support unless a separate plan replaces it with an equivalent workflow.

## Starting Instructions For The Next Agent

1. Read this document fully.
2. Treat Phase 3 login-server parity as complete unless new client or Java GS testing exposes a mismatch.
3. Treat Phase 4 chat-server parity as complete unless new client or Java GS testing exposes a mismatch.
4. Treat Phase 5 game-server infrastructure as complete unless optional real-client shell validation exposes a mismatch.
5. Continue with Phase 6: port game core behavior into `dotnetConversion/src/Aion.GameServer`.
6. Keep the Java project building and runnable for mixed-mode validation.
7. Add or extend parity tests before changing shared protocol behavior.
8. Add short implementation notes as each phase discovers differences from Java.
9. Use focused test selection from `orchestration-rules.md` for ordinary Phase 6 units; reserve full .NET suite/build runs for documented broad-validation triggers.
10. In each completion/handoff document, record the exact focused validation command and the reason any full .NET suite/build was skipped or run.
11. Treat a passing filtered `dotnet test` command as the compile signal for its affected project/dependencies unless a named broad-validation trigger requires a wider project test or solution build.
12. For documentation-only Phase 6 units, prefer `git diff --check` and skip runtime tests unless the docs alter generated artifacts, test scripts, or run scripts.
13. Do not run full .NET tests or full solution builds during startup or ordinary handoff review; choose the narrowest service, packet, parser, or documentation hygiene command after Work Discovery.
14. Do not run an unfiltered project test, full solution test, or full solution build unless the active completion/handoff notes already name the broad-validation trigger from `orchestration-rules.md`.
15. When a focused filtered test passes, do not follow it with a full solution build merely to confirm compilation; the filtered test has already built the affected project and dependencies.
