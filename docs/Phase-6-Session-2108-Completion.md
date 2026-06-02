# Phase 6 Session 2108 Completion - FindGroup Connection Adapter Consumer Evidence

Date: 2026-06-02
Unit of Work: UOW-2108
Status: Completed

## Scope

- Added non-live `GameServerConnection` consumer evidence for parsed `CmFindGroup` packets.
- Threaded injected `FindGroupConnectionClientActionCompositionPlanService` and `FindGroupConnectionBoundaryDispatchAdapterService` from `GameClientSocketServer` into new `GameServerConnection` instances.
- Kept live `case CmFindGroup` deferred in `ProcessPacketAsync`.
- Updated readiness reports and the live-dispatch design note to distinguish disabled connection adapter-consumer evidence from live dispatch readiness.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `readImpl` parses actions `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `15`, `17`, `20`, and `25`.
  - `runImpl` dispatches actions `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `15`, and `17` to `FindGroupService.getInstance()`.
  - Actions `20` and `25` are parsed but have no `runImpl` branch.

## What Changed

- `GameServerConnection` now accepts optional injected FindGroup composition and boundary adapter services.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` composes a non-live boundary plan from the connection active player and parsed `CmFindGroup` packet.
- The helper returns `null` when the connection lacks the injected services, preserving existing deferred/ad hoc connection behavior.
- `GameClientSocketServer` now stores the injected boundary services and passes them to each created connection.
- `FindGroupServiceCollectionExtensionsTests` now proves the production socket server receives the singleton composition and adapter services.
- New focused connection tests prove disabled boundary-plan composition creates direct packet intent evidence without writing to the live connection.

## Validation

- Changed surface:
  - Production-code, connection-adjacent non-live helper and socket constructor injection.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupServiceCollectionExtensionsTests" --no-restore`
  - Final result: passed, 37 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed; this UOW reviewed Java `CM_FIND_GROUP` source and added C# non-live connection adapter-consumer evidence around already-modeled branches.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the new connection helper, adjacent adapter/composition services, DI graph, and readiness reports.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateDisabledFindGroupBoundaryPlan`; `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet Boundary | Partial | Unit Tested | Partial Parity | Java `readImpl`/`runImpl` reviewed. C# connection can compose a disabled boundary plan from parsed packet and active player, but `ProcessPacketAsync` still defers live `CmFindGroup`; no live sends or runtime comparison. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundaryDispatchAdapterService`; `FindGroupRecruitmentPlanService` | Service Boundary / Adapter | Partial | Unit Tested | Partial Parity | C# disabled adapter composes direct packet, broadcast, invite, no-op, missing-player, and missing-runtime intents. Live Java singleton side effects remain blocked at connection dispatch. |
| `com.aionemu.gameserver.network.aion.GameConnectionListener` / `AionConnection` creation path | `Aion.GameServer.Network.Aion.GameClientSocketServer`; `Aion.GameServer.Network.Aion.GameServerConnection` | Socket Boundary | Partial | Unit Tested | Partial Parity | C# socket server now passes injected non-live FindGroup boundary services into created connections. Actual live client packet processing still does not invoke the helper. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ComposesAdapterPlanWithoutLiveDispatch` | Unit | Java `CM_FIND_GROUP.runImpl` source review | Connection can compose a disabled action `0` `SmFindGroup` direct packet intent from an active player without live sends | Focused C# unit test plus reviewed Java source | Does not execute live dispatch or encrypted socket writes |
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_UnconfiguredConnectionPreservesDeferredBoundary` | Unit | Java boundary review and current C# deferred behavior | Existing ad hoc connections without injected services remain deferred by returning `null` from the helper | Focused C# unit test | Does not validate production DI graph |
| `FindGroupServiceCollectionExtensionsTests.AddFindGroupSingletonGraph_RegistersSharedSocketRuntimeServices` | Unit | Java singleton service call-site review | Production service graph provides shared runtime, invite, composition, and adapter services to `GameClientSocketServer` | Focused C# DI unit test | Does not start the listener or process live `CmFindGroup` |
| `FindGroupConnectionBoundaryReadinessAggregateServiceTests.CreateReport_KeepsCmFindGroupBoundaryBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl` source review | Readiness report records disabled connection adapter-consumer evidence while keeping live boundary blocked | Focused C# unit test | Report evidence only |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_RecordsLifecycleObserverEvidenceWithoutMarkingLiveDispatchReady` | Unit | Java `FindGroupService` and `CM_FIND_GROUP` source review | Live readiness report includes connection helper evidence but remains blocked | Focused C# unit test | Report evidence only |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1 primary packet boundary plus the Java connection creation shape by analogy.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `CreateDisabledFindGroupBoundaryPlan` is not invoked by packet processing.
- Direct packet sends, race-filtered world broadcasts, action 11/12 side effects, socket-level order, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2108-Completion.md`
- `docs/Phase-6-Session-2108-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use, focusing on Java `FindGroupService` map mutation order, enumeration snapshots, and cross-caller cleanup interactions.

Safe alternative candidates:

- Add connection-registry ordering audit evidence for future direct sends and race-filtered world broadcasts before any live `ProcessPacketAsync` call.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a disabled action 12 connection-helper test using the connection resolver and injected group/alliance runtimes.
