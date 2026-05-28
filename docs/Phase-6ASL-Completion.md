# Phase 6ASL Completion - NPC Target Heading Overload

Date: 2026-05-28
Unit of Work: UOW-1670
Status: Complete after focused composition tests

## Scope

This unit wired the Java-derived `PositionUtilService.GetHeadingTowards` helper into the non-live NPC target-change packet planner.

The C# planner now has a coordinate-input overload that calculates heading toward a non-self target before creating `SmLookAtObject`. It still does not integrate with live NPC controllers, mutate live heading, schedule AI, or broadcast packets.

## Completed Work

- Added `NpcTargetChangeCoordinatePacketPlanInput`.
- Added `NpcTargetChangePacketPlanService.CreatePlan(NpcTargetChangeCoordinatePacketPlanInput)`.
- Used `PositionUtilService.GetHeadingTowards` for non-null, non-self target heading calculation.
- Preserved current heading for target-clear and self-target branches.
- Added focused composition test asserting calculated heading and packet payload.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`
  - `game-server/src/com/aionemu/gameserver/utils/PositionUtil.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LOOKATOBJECT.java`
- Java recalculates NPC heading only when `newTarget != null && !getOwner().equals(newTarget)`.
- Java then broadcasts `new SM_LOOKATOBJECT(getOwner())`.
- C# coordinate overload models the heading calculation and packet creation with snapshots only.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NpcTargetChangePacketPlanServiceTests|FullyQualifiedName~PositionUtilServiceTests|FullyQualifiedName~SmLookAtObjectPacketTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 274 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| NPC heading coordinate overload | NPC target service/tests | Low | Yes | Removes the precomputed-heading dependency from non-live NPC target-change planning. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after heading overload is documented. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside heading overload scope. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | NPC heading overload, tests, docs, commit | `NpcTargetChangePacketPlanService.cs`, `NpcTargetChangePacketPlanServiceTests.cs`, progress/handoff docs | Java source writes, live NPC controller/broadcast integration, unrelated services/tests | Implemented and documented UOW-1670. |
| Sub-agents | None | None | All files | Not spawned because selected work touched one existing service/test pair plus shared docs. |

No sub-agent was spawned for UOW-1670 because the selected overload was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_WithCoordinatesCalculatesHeadingTowardNonSelfTargetLikeJavaPositionUtil` | Added | Coordinate overload calculates heading `105` and creates `SmLookAtObject` with that heading. | Java `NpcController.onTargetChanged` plus `PositionUtil.getHeadingTowards`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.NpcController.onTargetChanged` | `NpcTargetChangePacketPlanService.CreatePlan(NpcTargetChangeCoordinatePacketPlanInput)` | Controller Boundary | Partial | Unit Tested | Partial Parity | C# non-live planner can now calculate heading from NPC/target coordinates before creating `SmLookAtObject`. It still does not mutate live NPC heading, clear attacked count, update game stats, schedule AI, or broadcast. |
| `com.aionemu.gameserver.utils.PositionUtil.getHeadingTowards` | `Aion.GameServer.Services.PositionUtilService.GetHeadingTowards` | Utility | Complete | Regression Tested | Partial Parity | Reused by NPC target-change coordinate overload. Source-derived tests cover representative coordinate cases, but no Java runtime vector/golden comparison was produced. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LOOKATOBJECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmLookAtObject` | Server Packet | Complete | Regression Tested | Partial Parity | Coordinate overload creates packet payload with calculated heading. No Java runtime golden/encrypted frame comparison. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `NpcTargetChangeCoordinatePacketPlanInput` | Model Boundary | Partial | Unit Tested | Partial Parity | C# uses coordinate/id snapshots instead of live NPC and target object references. Live object equality, world/instance, z-coordinate, object templates, and heading mutation remain unported. |

## Remaining Risks

- Live `NpcController.onTargetChanged` integration remains unported; no actual NPC state mutation, heading update, scheduler, AI think, or broadcast occurs.
- Coordinate overload still uses snapshots and does not inspect live `VisibleObject`/`Npc` references or object equality.
- Java runtime vector/golden comparison for heading precision remains missing.
- Broadcast recipient selection, packet ordering, source inclusion, visibility, and threading remain unverified.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 1 coordinate-input DTO/overload plus 1 focused composition regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented live/runtime gaps.
- Total blocked artifacts: live NPC controller integration, live object-reference heading calculation, broadcast utility integration, Java runtime vector comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Java runtime heading vectors | parity/golden utility tests | Generate or encode Java runtime vectors for `PositionUtil` heading precision. |
| Another isolated packet parity unit | packet class/tests | Continue small packet-body parity while live dispatch remains incomplete. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise continue with another isolated packet parity unit or add Java runtime/golden vector coverage for `PositionUtil`.
- Why: NPC target-change planning now computes heading non-live; live integration remains risky until object references and broadcast semantics are stronger.
- Files: likely no file changes for DB execution; otherwise exact packet files or focused vector tests/docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Packet audit | read-only Java/C# packet comparison | Low | Pick a small deterministic missing packet only. |
| B | PositionUtil vector design | read-only Java helper/tests | Low | Use if adding Java runtime/golden vectors. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, packet unit, or heading vectors | exact selected files | Java writes unless vector generation is explicitly scoped, live NPC broadcast, unrelated shared files |
| Read-only Agent | Audit next packet or vector source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only unless deliberately generating runtime vectors in a scoped way.
- Live target dispatch, live NPC target broadcast, live scheduler mutation, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1670] Add NPC target heading overload
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/NpcTargetChangePacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcTargetChangePacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASL-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
