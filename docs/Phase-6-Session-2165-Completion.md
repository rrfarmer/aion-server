# Phase 6 Session 2165 Completion - FindGroup Direct Show-List Trace Schema

Date: 2026-06-02
Unit of Work: UOW-2165
Status: Completed

## Scope

This unit added a stable non-live trace schema/export DTO for future Java/C# `CM_FIND_GROUP` direct show-list boundary traces for actions `0` and `4`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `0` calls `FindGroupService.showRecruitments(player)` and sends one `SM_FIND_GROUP` action `0` packet to the triggering player.
- Action `4` calls `FindGroupService.showApplications(player)` and sends one `SM_FIND_GROUP` action `4` packet to the triggering player.
- Both branches filter visible entries by active player race, materialize the list, and do not world-broadcast or dispatch invites.

This UOW does not capture Java/C# traces, does not enable live `CM_FIND_GROUP` dispatch, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupDirectPacketShowListBoundaryTraceSchemaService`.
- Added schema records:
  - `FindGroupDirectPacketShowListBoundaryTraceSchema`,
  - `FindGroupDirectPacketShowListActionSchema`,
  - `FindGroupDirectPacketShowListBoundaryTraceField`,
  - `FindGroupDirectPacketShowListBoundaryTraceExport`.
- Schema version is `1`.
- Stable export fields include parsed action, boundary acceptance, active player object id/race, server epoch seconds, list kind, visible entry object ids, direct packet recipient/type/action, boundary executor status, registry-send observation, and zero world-broadcast/invite counts.
- Added sample export shapes for action `0` recruitment show-list and action `4` application show-list.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to surface the trace schema as non-live evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/schema service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live schema used reviewed Java `CM_FIND_GROUP.runImpl` actions `0`/`4` plus `FindGroupService.showRecruitments/showApplications` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new schema plus adjacent show-list scaffold and direct-packet readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 10
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService` | Trace Schema / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `0`/`4` trace export fields are represented, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Trace Schema / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java show-list filtering and direct-send comparison fields are represented. No live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests.CreateSchema_DefinesStableVersionAndJavaShowListMappings` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Schema version, trace name, action `0`/`4` list-kind mappings, and Java method/packet labels. | Focused non-live schema assertion. | Does not capture runtime traces. |
| `FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests.CreateSchema_RequiresStableTraceFieldOrder` | Unit | Java show-list source review | Stable export field order and required no-broadcast/no-invite fields. | Focused non-live schema assertion. | No Java/C# trace file exists yet. |
| `FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests.CreateSampleExport_ProjectsActionSpecificComparisonShape` | Unit | Java action `0`/`4` source review | Sample export shape for recruitments/applications keeps live boundary, executor, registry, broadcast, and invite counts inactive. | Focused non-live export DTO assertion. | Sample only; no capture populated it. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct-send source review | Readiness report surfaces the trace schema as non-live evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 trace-capture gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Trace schema is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutating direct-packet actions, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-packet live-boundary trace scaffolding for mutating direct-packet actions `2` and `6` without live dispatch.

Safe candidates:

- Add a non-live trace export population helper for action `0`/`4` disabled boundary plans using the schema.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketShowListBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2165-Completion.md`
- `docs/Phase-6-Session-2165-Handoff.md`
