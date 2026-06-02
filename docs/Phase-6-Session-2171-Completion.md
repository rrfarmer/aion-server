# Phase 6 Session 2171 Completion - FindGroup Mutation Runtime Fixture Contract

Date: 2026-06-02
Unit of Work: UOW-2171
Status: Completed

## Scope

This unit added a blocked runtime-comparison fixture contract row for Java `CM_FIND_GROUP` mutation-post actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `2` calls `FindGroupService.addRecruitment(player, message, groupType)`, mutates recruitment state, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED()`, then refreshes recruitments with `SM_FIND_GROUP` action `0`.
- Action `6` calls `FindGroupService.addApplication(player, message, groupType, classId, level)`, mutates application state, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()`, then refreshes applications with `SM_FIND_GROUP` action `4`.

This UOW does not generate Java trace artifacts, does not capture live C# runtime traces, does not enable live `CM_FIND_GROUP` dispatch, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupRuntimeComparisonFixtureContractRow`.
- Added `FindGroupRuntimeComparisonFixtureContractStatus`.
- Extended `FindGroupRuntimeComparisonPreflightContract` with `RequiredFixtureRows`.
- Added a blocked `mutation-post-actions-2-6` fixture row.
- The fixture row ties:
  - actions `2` and `6`,
  - trace name `cm-find-group-direct-mutation-post-boundary`,
  - Java `CM_FIND_GROUP.runImpl` and `FindGroupService.addRecruitment/addApplication`,
  - C# `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.CreateExportFromDisabledPlan`.
- Tightened `IsReadyForRuntimeComparison` so future readiness also requires fixture rows to be ready.
- Updated design notes to record the fixture row while keeping runtime comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production runtime preflight contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this contract row uses reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture exists yet; this UOW documents that future fixture target.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the edited runtime preflight contract plus adjacent mutation-post schema/projection and singleton-readiness surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 15
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService` | Runtime Comparison Fixture Contract | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post runtime fixture row is represented and tied to the trace schema/projection, but no Java/C# runtime trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Runtime Comparison Fixture Contract / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message ordering, refreshed show-list ordering, and post-mutation visible ids are named as future fixture requirements. No Java artifact, live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRuntimeComparisonPreflightContractServiceTests.Create_AddsMutationPostFixtureRowWithoutMarkingRuntimeComparisonReady` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment/addApplication` source review | Runtime preflight includes a blocked action `2`/`6` mutation-post fixture row tied to the trace name and C# projection helper while keeping runtime comparison not ready. | Focused non-live contract assertion. | Does not generate Java artifacts or execute live C# traces. |
| `FindGroupRuntimeComparisonPreflightContractServiceTests.Create_KeepsRuntimeComparisonBlockedAndNonLive` | Unit | Java runtime-comparison source review | Runtime preflight remains blocked/non-live and requires Java/C# traces plus encrypted socket capture. | Focused preflight assertion. | Does not capture runtime evidence. |
| `FindGroupRuntimeComparisonPreflightContractServiceTests.Create_CoversFindGroupScenarioMatrixConservatively` | Unit | Java `CM_FIND_GROUP.readImpl/runImpl`; `FindGroupService` source review | Scenario matrix still covers show-list, mutation-direct, world-broadcast, instance application, action `12`, parsed-only no-run, and shared-singleton lifecycle groups. | Focused contract assertion. | Scenario coverage is planning metadata, not runtime comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 runtime comparison trace-capture gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The fixture row is non-live contract metadata; no Java trace artifact, C# live trace row, encrypted socket capture, or real-client runtime comparison has executed.
- The fixture row depends on the disabled C# projection helper; it does not prove `GameServerConnection` invokes the executor from the live boundary.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a Java trace artifact schema or validator target for the action `2`/`6` mutation-post fixture row, without enabling live C# dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a non-live trace export projection for another direct-packet subgroup only if a stable schema already exists.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRuntimeComparisonPreflightContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRuntimeComparisonPreflightContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2171-Completion.md`
- `docs/Phase-6-Session-2171-Handoff.md`
