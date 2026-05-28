# Phase 6ASE Completion - Target Change Packet Plan Boundary

Date: 2026-05-28
Unit of Work: UOW-1663
Status: Complete after focused packet-plan tests

## Scope

This unit added a non-live target-change packet-plan boundary for Java `PlayerController.onTargetChanged`.

The C# code now creates the same two packet objects Java would emit when a player target changes: an owner `SmTargetSelected` packet and a sighted-player `SmTargetUpdate` packet. This unit does not enable live dispatch, target resolution, KnownList visibility checks, or connection-registry broadcasting.

## Completed Work

- Added `PlayerTargetChangePacketPlanService`.
- Added `PlayerTargetChangePacketPlan`.
- Added `PlayerTargetChangePacketPlanStatus`.
- Created owner `SmTargetSelected` packet plans for changed targets.
- Created sighted-player `SmTargetUpdate` packet plans for changed targets.
- Modeled target clearing with Java-shaped zero-target packet payloads.
- Preserved Java `VisibleObject.setTarget` changed-target guard as an object-id approximation until live target references are ported.
- Added focused unit tests for packet-plan creation and blocked/no-change cases.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TARGET_SELECT.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TARGET_SELECTED.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TARGET_UPDATE.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- Java `VisibleObject.setTarget` invokes the controller only when the target reference changes.
- Java `PlayerController.onTargetChanged` sends `SM_TARGET_SELECTED(newTarget)` to the owner and broadcasts `SM_TARGET_UPDATE(getOwner())` to sighted players.
- C# currently approximates reference-change detection with object ids because live `VisibleObject` target references and KnownList resolution are not yet ported.
- Java `CM_TARGET_SELECT` resolution is now the next documented targeting gap.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerTargetChangePacketPlanServiceTests|FullyQualifiedName~SmTargetPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 250 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Target-change packet-plan boundary | target plan service/tests | Low | Yes | Closes the packet-factory side of Java `PlayerController.onTargetChanged` after UOW-1662 packet bodies. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside target-change scope. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after the target-change boundary is documented. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Target-change packet-plan service, tests, docs, commit | `PlayerTargetChangePacketPlanService.cs`, `PlayerTargetChangePacketPlanServiceTests.cs`, progress/handoff docs | Java source writes, live target dispatch, unrelated connection handlers/services/tests | Implemented and documented UOW-1663. |
| Sub-agents | None | None | All files | Not spawned because selected work touched one service/test pair plus shared docs. |

No sub-agent was spawned for UOW-1663 because the selected boundary was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_CreatesOwnerAndSightedPacketsWhenTargetChangesToCreatureLikeJavaController` | Added | Changed creature target creates owner selected-target and sighted-player target-update packets. | Static source review of `VisibleObject.setTarget` and `PlayerController.onTargetChanged`; deterministic packet-plan regression. |
| `CreatePlan_CreatesClearTargetPacketsWhenTargetChangesToNullLikeJavaController` | Added | Clearing a target creates zeroed owner and broadcast target payloads. | Java null-target packet behavior from `SM_TARGET_SELECTED` and `SM_TARGET_UPDATE`. |
| `CreatePlan_DoesNotCreatePacketsWhenTargetIdIsUnchangedLikeVisibleObjectGuard` | Added | Unchanged target id creates no packets. | C# approximation of Java reference guard; explicitly not verified as reference parity. |
| `CreatePlan_CreatesPlanFromPlayerCurrentTargetObjectId` | Added | `Player` overload uses current C# target id and creates a new-target broadcast packet. | C# boundary regression based on Java side-effect shape. |
| `CreatePlan_BlocksPacketsWhenPlayerOwnerIsUnavailable` | Added | Invalid owner id does not create packets. | Non-live C# safety boundary for Java live owner requirement. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.PlayerController.onTargetChanged` | `Aion.GameServer.Services.PlayerTargetChangePacketPlanService` | Controller Boundary | Partial | Unit Tested | Partial Parity | C# creates owner `SmTargetSelected` and sighted-player `SmTargetUpdate` packet objects in Java order. It does not call `PacketSendUtility.sendPacket`, `broadcastToSightedPlayers`, connection registry dispatch, or live KnownList visibility filtering. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.setTarget` | `PlayerTargetChangePacketPlanService.CreatePlan` | Model Boundary | Partial | Unit Tested | Partial Parity | Java guards by object reference before invoking the controller. C# currently approximates this with target object ids because live `VisibleObject` references are not ported into targeting. This is an explicit non-live approximation and needs verification once object references exist. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED` | `Aion.GameServer.Network.Aion.ServerPackets.SmTargetSelected` | Server Packet | Complete | Regression Tested | Partial Parity | Reused from UOW-1662; this unit verifies the controller-boundary factory creates the owner packet and serializes the expected payload. No Java runtime golden/encrypted frame comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmTargetUpdate` | Server Packet | Complete | Regression Tested | Partial Parity | Reused from UOW-1662; this unit verifies the controller-boundary factory creates the sighted-player broadcast packet. Live broadcast recipients and include/exclude source behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TARGET_SELECT` | `GameServerConnection.HandleTargetSelect`; future targeting input plan | Client Packet / Input Boundary | Partial | Manual Only | Needs Verification | Java resolves target-of-target, self target, KnownList object, team member fallback, invisible-target audit, and then calls `player.setTarget`. Existing C# handler only stores a target object id and logs target-of-target as unported. No new live connection integration was added in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToSightedPlayers` | future connection-registry broadcast integration | Utility Boundary | Not Started | No Tests | Needs Verification | Newly documented dependency for live target-change parity. Recipient selection, source exclusion, visibility, threading, and packet ordering remain unported for this path. |

## Remaining Risks

- Live `GameServerConnection.HandleTargetSelect` still does not resolve Java target-of-target, self target, KnownList target, team member fallback, invisible-target audit, or object-reference target assignment.
- Live `PlayerController.onTargetChanged` dispatch remains unported; no owner send or sighted-player broadcast occurs.
- C# changed-target guard uses target ids instead of Java object-reference equality.
- Broadcast recipient selection, packet ordering, threading, and source-player inclusion/exclusion remain unverified.
- No Java runtime golden frame or encrypted frame comparison was produced for target-change packets.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live controller packet-plan service, 2 packet-factory uses, and 5 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live `CM_TARGET_SELECT` target resolution, live target-change dispatch, Java object-reference target storage, sighted-player broadcast recipient selection, Java runtime packet capture, encrypted frame comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| `CM_TARGET_SELECT` input-resolution plan | non-live targeting helper/tests | Model target-of-target, self target, KnownList, team fallback, invisible-target audit, and no-target outcomes without live dispatch. |
| Another isolated packet parity unit | packet class/tests | Add C# payload/factory tests only if Java packet shape is small and source-derived. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add a non-live `CM_TARGET_SELECT` input-resolution plan.
- Why: target-change packet creation is now covered, but Java target selection input resolution remains reduced to direct C# id assignment.
- Files: likely no file changes for DB execution; otherwise a focused targeting input planner, its tests, and docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | `CM_TARGET_SELECT` input-resolution analysis | read-only Java client packet and C# connection handler | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, target input planner, or next isolated packet unit | exact selected files | Java writes, unrelated shared files |
| Read-only Agent | Audit `CM_TARGET_SELECT` or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1663] Add target change packet plan boundary
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerTargetChangePacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerTargetChangePacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASE-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
