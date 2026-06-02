# Phase 6 Session 2134 Completion - FindGroup World Broadcast Fanout Readiness

Date: 2026-06-02
Unit of Work: UOW-2134
Status: Completed

## Scope

- Added a readiness report for FindGroup world-broadcast race filtering and fanout ordering.
- Linked the new report into the existing live-dispatch readiness rollup.
- Kept live `GameServerConnection.ProcessPacketAsync` `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `broadcastToWorld(packet, filter)` iterates `World.forEachPlayer` and sends only to players accepted by the predicate.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `removeRecruitment` broadcasts action `1` to players matching `recruitment.getRace()`.
  - `removeApplication` broadcasts action `5` to players matching `application.getPlayer().getRace()`.

## C# Source Reviewed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
  - `BroadcastToWorldAsync` iterates active player connections, skips missing active players, applies the optional filter, and sends packets to accepted recipients.
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupSideEffectDispatchExecutorService.cs`
  - Applies FindGroup world-broadcast race filters through `IGameClientConnectionRegistry.BroadcastToWorldAsync` when explicitly invoked.

## What Changed

- Added `FindGroupWorldBroadcastFanoutReadinessService`.
- Added focused tests that:
  - keep live world-broadcast fanout blocked;
  - separate reviewed Java race-filter behavior and C# opt-in executor evidence from live boundary proof;
  - record next evidence for actions `1` and `5`.
- Updated `FindGroupLiveDispatchReadinessReportService` to surface the world-broadcast fanout blocker and readiness evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` with the new readiness report.

## Validation

- Changed surface:
  - Production readiness-report services plus focused tests and documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupWorldBroadcastFanoutReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore`
  - Final result: passed, 15 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `PacketSendUtility.broadcastToWorld` and `FindGroupService` action `1`/`5` race-filter call paths; no focused Java test target was identified for this C# readiness-report surface.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped readiness-report surface.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld` | `Aion.GameServer.Services.FindGroupWorldBroadcastFanoutReadinessService`; `GameClientSocketServer.BroadcastToWorldAsync` | Readiness Report | Partial | Unit Tested | Partial Parity | Report records reviewed Java predicate fanout and C# registry evidence, but live `CM_FIND_GROUP` fanout remains unproven. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment/removeApplication` | `Aion.GameServer.Services.FindGroupWorldBroadcastFanoutReadinessService`; `FindGroupSideEffectDispatchExecutorService` | Readiness Report | Partial | Unit Tested | Partial Parity | Actions `1` and `5` race-filter evidence exists in disabled/opt-in paths only; live boundary fanout remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupWorldBroadcastFanoutReadinessServiceTests.CreateReport_KeepsLiveWorldBroadcastFanoutBlocked` | Unit | Java `PacketSendUtility.broadcastToWorld`; Java `FindGroupService` | Live world-broadcast fanout remains blocked because C# still defers `CmFindGroup` | Focused C# unit test plus reviewed Java source | Does not execute live dispatch |
| `FindGroupWorldBroadcastFanoutReadinessServiceTests.CreateReport_SeparatesJavaRaceFilterAndCSharpExecutorEvidenceFromLiveProof` | Unit | Java `PacketSendUtility.broadcastToWorld`; Java FindGroup action `1`/`5` branches | Java race-filter review and opt-in C# fanout evidence are not treated as live proof | Focused C# unit test plus reviewed Java source | Does not prove live recipient filtering |
| `FindGroupWorldBroadcastFanoutReadinessServiceTests.CreateReport_RecordsNextRequiredEvidenceForActionsOneAndFive` | Unit | Java FindGroup action `1`/`5` branches | Next required live evidence includes same-race recipients, opposite-race exclusion, and action `1`/`5` ordering | Focused C# unit test plus reviewed Java source | Does not provide the ordered live trace |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_RecordsLifecycleObserverEvidenceWithoutMarkingLiveDispatchReady` | Unit | Java `CM_FIND_GROUP.runImpl`; Java `FindGroupService` | Rollup includes the world-broadcast fanout blocker while keeping live dispatch blocked | Focused C# unit test plus reviewed Java source | Does not execute live dispatch |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes.
- Total artifacts ported or represented in this UOW: 2 C# readiness-report surfaces plus one reviewed registry implementation.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The world-broadcast report is readiness evidence only; it does not execute live sends or prove client-visible behavior.
- Actions `1` and `5` still need live boundary evidence for same-race recipient fanout and opposite-race exclusion.
- Direct-packet ordering relative to the triggering client packet remains unverified.
- Shared singleton lifecycle still needs live `CM_FIND_GROUP` execution proof against the same C# state store.
- Invite request mutation and runtime/socket comparison remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastFanoutReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastFanoutReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2134-Completion.md`
- `docs/Phase-6-Session-2134-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review multi-step mutation ordering under concurrent singleton callers before live dispatch.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a disabled boundary ordered-trace test shape that models the eventual live trigger/send observer without invoking `ProcessPacketAsync`.
- Add a compact readiness report for action `12` invite request mutation ordering across direct whisper and invite branches.
