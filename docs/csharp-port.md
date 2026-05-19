# C# Port Handoff Plan

## Migration Progress

| Phase | Status | Completion Date | Notes |
|-------|--------|-----------------|-------|
| Phase 0: Starter Workspace | ✅ COMPLETE | May 18, 2026 | Solution structure, projects, and entry points scaffolded. See [PHASE-0-COMPLETION.md](PHASE-0-COMPLETION.md) |
| Phase 1: Parity Harness | ✅ COMPLETE | May 18, 2026 | Packet tests (16✓), config tests (12✓), database fixtures, XML tool. See [PHASE-1-COMPLETION.md](PHASE-1-COMPLETION.md) |
| Phase 2: Port Commons | 🔄 IN PROGRESS | — | Logging, crypto, threading, scheduler primitives |
  | Phase 2: Port Commons | ✅ COMPLETE | May 18, 2026 | Logging, crypto (XOR cipher), socket server base, scheduler. 56 tests passing. See [PHASE-2-COMPLETION.md](PHASE-2-COMPLETION.md) |
  | Phase 3: Port Login Server | 🔄 IN PROGRESS | — | Authentication, server registration, session management |
| Phase 4: Port Chat Server | ⏳ PENDING | — | Chat protocol, channels, player messaging |
| Phase 5: Port Game Infrastructure | ⏳ PENDING | — | World, scheduler, object factory, data loading |
| Phase 6: Port Game Core | ⏳ PENDING | — | Characters, movement, combat, loot, quests |
| Phase 7: Port Dynamic Handlers | ⏳ PENDING | — | Commands, zones, instances, AI, quests |
| Phase 8: Replacement Readiness | ⏳ PENDING | — | Docker, soak tests, rollback plan |

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

Use `csharp/` as the C# workspace. Keep Java and C# side by side until the C# server is production-ready.

Suggested layout:

```text
csharp/
  AionServer.sln
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

Create the initial `.NET` solution under `csharp/`.

Deliverables:

- `AionServer.sln`
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

- Login client crypto, RSA key/modulus handling, encrypted frame read/write, and Java-generated crypto golden vectors are in place.
- DB-backed account authentication, external auth, brute-force ban escalation, and core account-time/penalty behavior are ported.
- Game-server registration, login handoff, reconnect, account-list sync, server-list refresh fanout, character-count fanout, ping/pong, ban lists, account controls, and player-transfer bridge slices are ported.
- Full .NET suite passes with 108 tests; the opt-in MySQL schema integration test passes against a Dockerized MySQL 8.4 instance.

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

- C# login server can interoperate with the existing Java game server.
- Real client can authenticate and receive server-list responses.
- Existing login database schema works without migration.
- Packet golden tests pass for login protocol packets.

### Phase 4: Port Chat Server

Port chat-server after login.

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
2. Inspect the Java modules and current Docker config.
3. Create `csharp/` only when beginning Phase 0.
4. Keep the Java project building and runnable.
5. Build the parity harness before porting large gameplay areas.
6. Start with `Aion.Commons`, then login-server.
7. Add short implementation notes as each phase discovers differences from Java.

