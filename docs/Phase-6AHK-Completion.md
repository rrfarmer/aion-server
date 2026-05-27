# Phase 6AHK Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1383
Latest Commit: included in `[Phase 6][UOW-1383] Document unusual storage runtime activation plan`
Status: Runtime activation plan exists. Java runtime artifact generation is still blocked locally by missing Maven/JDK tooling.

## What Changed

- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeActivationPlan.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.

## Code Changed

- No Java or C# source code was changed in this unit.

## Documentation Changed

- Documented local-only capture config overrides.
- Documented startup/shutdown capture boundaries.
- Documented rejected-food pet-feed scenario trigger.
- Documented output path/filename expectations.
- Documented required artifact field checks before C# reader work starts.

## Validation Completed

- Ran read-only source discovery over Java pet-feed, item-packet, startup, shutdown, config, and capture sources.
- No Java compile was required for this docs-only activation plan.
- Java validation remains blocked because `mvn` is not available on PATH and no Maven wrapper exists in the repository.

No Java runtime artifact, C# reader/schema behavior, warehouse-add byte comparison, live storage lookup, inventory mutation, or packet send was enabled.

## Migration Parity Table - UOW-1383

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService` | future C# pet-feed live adapter | Service | Partial | Manual Only | Needs Verification | Runtime plan uses the Java rejected-food branch in `checkFeeding(...)` as the source scenario. No runtime artifact generated locally. Date/time delay and scheduler behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | future C# item packet/live adapter | Service | Partial | Manual Only | Needs Verification | Runtime plan follows `sendItemUnlockPacket` to `sendStorageUpdatePacket(..., ALL_SLOT)` and the warehouse-add/cube-update packet order. No Java/C# byte comparison yet. |
| `com.aionemu.gameserver.services.toypet.PetFeedUnusualStorageArtifactCapture` | future C# artifact/schema reader | Utility / Capture Context | Partial | Manual Only | Needs Verification | Runtime activation plan documents config, scenario, output path, and artifact field checks. Writer exists but local runtime validation is blocked by missing Maven/JDK tooling. |
| `com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig` | future C# artifact fixture path/config | Config | Partial | Manual Only | Needs Verification | Capture remains disabled by default. Local overrides must not be committed. |
| `com.aionemu.gameserver.GameServer` | future C# game-server startup capture hook | Bootstrap | Partial | Manual Only | Needs Verification | Startup calls `installIfEnabled()` before NIO startup. Runtime ordering has not been tested locally. |
| `com.aionemu.gameserver.ShutdownHook` | future C# shutdown capture cleanup hook | Shutdown | Partial | Manual Only | Needs Verification | Shutdown resets observer before shutdown packet fanout. Runtime cleanup has not been tested locally. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Read-only activation plan | Java pet-feed/item-packet/startup/shutdown source review | Documents how to generate and manually inspect the first Java unusual-storage artifact once tooling is available. | Source inspection only. | No Java runtime artifact, no C# reader validation, no Java/C# byte comparison, and no compile validation. |

## Remaining Risks

- Java compile/runtime validation remains blocked locally by missing Maven/Java 25 tooling.
- Scenario requires real or controlled client access to a pet-food rejected-item path and an item in unusual storage.
- `itemBlob.packetBodyVerification` can fail if encode-time localized name or mutable item fields drift.
- File output can expose player/item identifiers and must remain local/opt-in.
- C# artifact reader/schema validation, runtime artifact ingestion, and warehouse-add byte comparison remain missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only runtime activation plan
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java compile/runtime validation, runtime artifact generation, C# artifact reader/schema validation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Prepare C# schema-v1 artifact reader validation for the new Java fields.
- Scope:
  - locate existing guarded unusual-storage artifact reader tests;
  - add expectations for `itemBlob.hex`, `itemBlob.packetBodyVerification`, packet `bodyHex`, and packet `canonicalPayloadHex`;
  - keep warehouse-add byte comparison guarded until a real Java artifact exists;
  - do not require generated Java artifacts in the default test run.

## Safe Parallel Candidates

- Read-only audit: inspect C# item-blob serializer gaps for `STAT_BONUSES`, plume tempering, cleanup/seal static data, and dye/expiration timing.
- Read-only audit: inspect runtime activation risks for enabling the Java observer in a local Java tooling environment.
- Java tooling task: in an environment with Maven/JDK tools, run compile and `javap` feature inspection only, with no source edits.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | C# reader test prep | isolated C# artifact reader test files only | Java source, shared docs unless assigned |
| Agent B | C# item-blob gap audit | read-only C# item-blob serializer files and Java iteminfo files | all writes |
| Orchestrator | Docs/parity integration | shared docs and final review | source files unless selected unit requires them |

## Do Not Parallelize

- C# reader changes with Java artifact schema mutations.
- Runtime Java artifact generation with source changes.
- Shared progress/handoff docs between agents.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`
  - `game-server/src/com/aionemu/gameserver/configs/main/PetFeedUnusualStorageArtifactCaptureConfig.java`
  - `game-server/src/com/aionemu/gameserver/GameServer.java`
  - `game-server/src/com/aionemu/gameserver/ShutdownHook.java`
- Docs:
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeActivationPlan.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageJsonWriterImplementation.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageRuntimeArtifactSchema.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
  - `docs/PHASE-6-PROGRESS.md`
