# Phase 6 Session 2089 Completion - Find Group Opt-In Side-Effect Executor

Date: 2026-06-01
Unit of Work: UOW-2089
Status: Completed

## Scope

- Inspected the existing C# `IGameClientConnectionRegistry` direct-send and world-broadcast hooks.
- Added an opt-in Find Group side-effect executor for direct packet and race-filtered world-broadcast intents.
- Preserved live `CM_FIND_GROUP` deferral; the executor is not wired into `GameServerConnection`.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Direct side effects use `PacketSendUtility.sendPacket(targetPlayer, packet)`.
  - World fanout side effects use `PacketSendUtility.broadcastToWorld(packet, p -> p.getRace() == recordedRace)`.
- C# target infrastructure reviewed:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`

## What Changed

- Added `FindGroupSideEffectDispatchExecutorService`.
  - Sends `FindGroupDirectPacketIntent` values through `IGameClientConnectionRegistry.SendPacketToPlayerAsync`.
  - Broadcasts `FindGroupWorldBroadcastIntent` values through `IGameClientConnectionRegistry.BroadcastToWorldAsync`.
  - Applies the Java race filter shape with ordinal race equality.
  - Records per-direct-send success and per-world-broadcast sent counts.
  - Marks `DispatchLiveSideEffects = true` because the executor performs side effects when explicitly invoked.
  - Includes a boundary note that `CM_FIND_GROUP` remains deferred.
- Added focused tests proving:
  - direct packet intents call the connection registry and report success;
  - missing direct recipients report unsent results;
  - world-broadcast intents apply the Java race filter and send only to matching race players;
  - null world-broadcast intents are ignored.
- Updated readiness reporting:
  - `FindGroupLiveDispatchReadinessReportService` now records opt-in executor evidence while preserving global blockers.
  - `FindGroupConnectionBoundaryReadinessAggregateService` now lists both audit and opt-in executor evidence for side-effect dispatch.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests" --no-restore`
  - Result: passed, 49 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added an opt-in C# executor around reviewed Java send/broadcast source behavior. It did not change Java packet parsing, Java runtime behavior, or a Java-executable parity target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: opt-in executor and readiness reporting only; no live `CM_FIND_GROUP` wiring, packet primitive, persistence, crypto, scheduling, world-state mutation, or connection-dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` Find Group call sites | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService` | Opt-In Side-Effect Executor | Partial | Unit Tested | Partial Parity | C# can execute planned direct packet intents through `IGameClientConnectionRegistry.SendPacketToPlayerAsync` when explicitly invoked. It is not wired into `CM_FIND_GROUP`. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld` Find Group race-filter call sites | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService` | Opt-In Side-Effect Executor | Partial | Unit Tested | Partial Parity | C# can execute planned race-filtered world-broadcast intents through `IGameClientConnectionRegistry.BroadcastToWorldAsync` with an ordinal race filter. It is not wired into `CM_FIND_GROUP`. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` live side-effect readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`; `FindGroupConnectionBoundaryReadinessAggregateService` | Readiness Report | Partial | Unit Tested | Partial Parity | Readiness reports now include opt-in side-effect executor evidence while preserving live boundary blockers. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupSideEffectDispatchExecutorServiceTests.ExecuteAsync_SendsDirectPacketIntentThroughConnectionRegistry` | Unit | Java `PacketSendUtility.sendPacket` Find Group source review | Direct packet intent calls `SendPacketToPlayerAsync` and records success | Focused C# unit test | Does not execute encrypted real-client socket |
| `FindGroupSideEffectDispatchExecutorServiceTests.ExecuteAsync_MissingDirectRecipientRecordsUnsentResult` | Unit | Java direct send target can be absent from world/connection state | Missing direct recipient is recorded as unsent | Focused C# unit test | Does not compare live Java runtime |
| `FindGroupSideEffectDispatchExecutorServiceTests.ExecuteAsync_BroadcastsWorldIntentWithJavaRaceFilter` | Unit | Java `broadcastToWorld(..., p -> p.getRace() == race)` source review | World broadcast applies the race filter and sends only to matching players | Focused C# unit test | Does not execute real socket fanout |
| `FindGroupSideEffectDispatchExecutorServiceTests.ExecuteAsync_IgnoresNullWorldBroadcastIntent` | Unit | C# defensive executor behavior | Null optional broadcast intents are skipped safely | Focused C# unit test | Java has no corresponding null helper |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1 Java service plus C# connection infrastructure.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond the race predicate, and concurrency remain unverified.
- Future live dispatch still needs controlled boundary composition and runtime comparison before wiring to `CmFindGroup`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupSideEffectDispatchExecutorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSideEffectDispatchExecutorServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `docs/Phase-6-Session-2089-Completion.md`
- `docs/Phase-6-Session-2089-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect whether `showInstanceGroupMembersInfo` action 15 should receive a direct-dispatch disabled executor slice or remain covered by the generic side-effect executor/audit.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Design a controlled boundary composition test that proves `CmFindGroup` can compose planner plus opt-in executor results without changing the live `GameServerConnection` switch.
