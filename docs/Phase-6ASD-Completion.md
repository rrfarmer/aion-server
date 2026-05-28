# Phase 6ASD Completion - Target Selection Packets

Date: 2026-05-28
Unit of Work: UOW-1662
Status: Complete after focused packet tests

## Scope

This unit added isolated C# packet ports for Java target-selection packet bodies.

The C# code now has packet classes for target selected and target update payloads. This does not enable live `PlayerController.onTargetChanged` dispatch, owner sends, sighted-player broadcasts, or live Java object hierarchy traversal.

## Completed Work

- Added `SmTargetSelected` with opcode `41`.
- Added `TargetSelectedSnapshot` for non-live target payload inputs.
- Added `SmTargetUpdate` with opcode `81`.
- Modeled Java null target primitive defaults for `SM_TARGET_SELECTED`.
- Modeled Java non-creature object-id-only target payload behavior.
- Modeled Java creature stat payload order: object id, level, max/current HP, max/current MP.
- Modeled Java target update payload: player object id and target object id, using zero for no target.
- Added focused unit tests for both packet classes.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TARGET_SELECTED.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TARGET_UPDATE.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/Creature.java`
- Java `SM_TARGET_SELECTED(VisibleObject)` leaves all primitive fields at zero for null targets. For non-null targets it records target object id, and only fills level/HP/MP when the target is a `Creature`.
- Java `SM_TARGET_UPDATE(Player)` writes the player object id and `player.getTarget() == null ? 0 : player.getTarget().getObjectId()`.
- Java `PlayerController.onTargetChanged` sends `SM_TARGET_SELECTED` to the owner and broadcasts `SM_TARGET_UPDATE` to known/sighted players. That controller behavior remains unported in this unit.
- C# uses `TargetSelectedSnapshot` instead of claiming the Java `VisibleObject`/`Creature` hierarchy is fully ported.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmTargetPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 245 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Target selection packet ports | target packet classes/tests | Low | Yes | Small missing packet bodies with clear Java source and deterministic payload order. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but less direct than packet parity. |
| Fly-time packet audit | existing packet/service tests | Low | No | Existing coverage exists; target packets had clearer missing implementation. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Target packet classes, tests, docs, commit | `SmTargetSelected.cs`, `SmTargetUpdate.cs`, `SmTargetPacketsTests.cs`, progress/handoff docs | Java source writes, live target dispatch, unrelated services/tests | Implemented and documented UOW-1662. |
| Sub-agents | None | None | All files | Not spawned because selected work was a small packet pair plus shared docs. |

No sub-agent was spawned for UOW-1662 because the selected packet pair and tests were small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `SmTargetSelected_WritesZeroPayloadForNullTargetLikeJavaPrimitiveDefaults` | Added | Null target produces all-zero payload fields. | Static source review of Java `SM_TARGET_SELECTED` constructor and primitive default behavior; deterministic C# packet regression. |
| `SmTargetSelected_WritesOnlyObjectIdForNonCreatureTargetLikeJava` | Added | Non-creature target writes object id with zero stats. | Static source review of Java `target instanceof Creature` branch. |
| `SmTargetSelected_WritesCreatureStatsLikeJava` | Added | Creature snapshot writes target id, level, HP, and MP in Java order. | Static source review of Java `Creature` stat extraction. |
| `SmTargetUpdate_WritesPlayerAndTargetObjectIdsLikeJava` | Added | Player id and target id payload. | Static source review of Java `SM_TARGET_UPDATE.writeImpl`. |
| `SmTargetUpdate_UsesZeroTargetWhenPlayerHasNoTargetLikeJava` | Added | No-target player payload writes zero target id. | Static source review of Java null-target ternary. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED` | `Aion.GameServer.Network.Aion.ServerPackets.SmTargetSelected` | Server Packet | Complete | Unit Tested | Partial Parity | C# models packet opcode `41` and payload fields: target id, level, max/current HP, max/current MP. Null target and non-creature target behavior are covered. It does not yet accept live `VisibleObject`/`Creature` objects or runtime-compare Java frames. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmTargetUpdate` | Server Packet | Complete | Unit Tested | Partial Parity | C# models packet opcode `81`, player object id, and target object id with zero for no target. It does not use Java object identity, live `Player.getTarget()`, or broadcast dispatch. |
| `com.aionemu.gameserver.controllers.PlayerController.onTargetChanged` | future target-change dispatch; packet classes only in this unit | Controller Boundary | Not Started | No Tests | Needs Verification | Java sends `SM_TARGET_SELECTED` to the owner and broadcasts `SM_TARGET_UPDATE` to sighted players. C# currently only has packet classes; live controller/known-list dispatch remains unported. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `TargetSelectedSnapshot` | DTO Projection | Partial | Unit Tested | Partial Parity | C# uses an explicit snapshot to avoid pretending the Java visible-object hierarchy is ported. Non-creature target id-only behavior is covered; reflection/inheritance behavior is not modeled. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `TargetSelectedSnapshot` | DTO Projection | Partial | Unit Tested | Partial Parity | C# snapshot carries level and HP/MP stats used by the packet. It does not model live `Creature.getLifeStats()`, stat recalculation, threading, or null life-stat behavior. |

## Remaining Risks

- Live `PlayerController.onTargetChanged` dispatch is not ported; no owner send or sighted-player broadcast occurs.
- C# uses `TargetSelectedSnapshot` instead of the Java `VisibleObject`/`Creature` inheritance hierarchy.
- Live `Creature.getLifeStats()` HP/MP extraction, stat recalculation timing, null object behavior, and threading remain unverified.
- No Java runtime golden frame or encrypted frame comparison was produced for target packets.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 2 server packets plus 1 DTO projection and 5 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 1 grouped row explicitly marked Needs Verification; remaining rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live target-change dispatch, Java object hierarchy/runtime stat extraction, Java runtime packet capture, encrypted frame comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Target-change plan/factory boundary | non-live controller helper/tests | Use the new target packets to model the Java send/broadcast decision without enabling live dispatch. |
| Another isolated packet parity unit | packet class/tests | Add C# payload/factory tests only if Java packet shape is small and source-derived. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add a non-live target-change plan/factory boundary for `PlayerController.onTargetChanged` using the new target packets.
- Why: target packet bodies are now covered, but the Java controller send/broadcast boundary remains unmodeled.
- Files: likely no file changes for DB execution; otherwise a focused target-change planner/helper, its tests, and docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Target-change boundary analysis | read-only Java `PlayerController.onTargetChanged`, C# player/controller packet services | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, target-change boundary, or next isolated packet unit | exact selected files | Java writes, unrelated shared files |
| Read-only Agent | Audit target-change controller or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared packet helper/test fixtures and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1662] Add target selection packet bodies
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTargetSelected.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmTargetUpdate.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmTargetPacketsTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASD-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
