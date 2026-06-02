# Phase 6 Session 2169 Completion - FindGroup Mutation Trace Schema

Date: 2026-06-02
Unit of Work: UOW-2169
Status: Completed

## Scope

This unit added a stable non-live trace schema/export DTO for future Java/C# `CM_FIND_GROUP` mutation-post boundary traces for actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `2` calls `FindGroupService.addRecruitment(player, message, groupType)`, stores the recruitment, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED()`, then refreshes recruitments with `SM_FIND_GROUP` action `0`.
- Action `6` calls `FindGroupService.addApplication(player, message, groupType, classId, level)`, stores the application, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()`, then refreshes applications with `SM_FIND_GROUP` action `4`.

This UOW does not capture Java/C# traces, does not enable live `CM_FIND_GROUP` dispatch, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`.
- Added schema records:
  - `FindGroupDirectPacketMutationPostBoundaryTraceSchema`,
  - `FindGroupDirectPacketMutationPostActionSchema`,
  - `FindGroupDirectPacketMutationPostBoundaryTraceField`,
  - `FindGroupDirectPacketMutationPostBoundaryTraceExport`.
- Schema version is `1`.
- Stable export fields include parsed action, boundary acceptance, active player facts, mutation kind, mutated entry id, mutation-before-direct-send flag, posted system message recipient/type/id, refreshed list recipient/type/action, visible entry ids after mutation, boundary executor status, registry-send ordering observation, and zero world-broadcast/invite counts.
- Supported action mappings:
  - action `2`: recruitment mutation, posted message id `1400392`, refreshed show-list action `0`;
  - action `6`: application mutation, posted message id `1400393`, refreshed show-list action `4`.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to surface the mutation-post trace schema as non-live evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/schema service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live schema used reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new schema plus adjacent mutation-post scaffold and direct-packet readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 11
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Trace Schema / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `2`/`6` trace export fields are represented, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Trace Schema / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message, and refreshed show-list ordering fields are represented. No live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests.CreateSchema_DefinesStableVersionAndJavaMutationPostMappings` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Schema version, trace name, action `2`/`6` mutation mappings, Java methods, posted message ids, and refreshed show-list action ids. | Focused non-live schema assertion. | Does not capture runtime traces. |
| `FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests.CreateSchema_RequiresStableTraceFieldOrder` | Unit | Java mutation-post source review | Stable export field order and required mutation, posted-message, refreshed-list, no-broadcast, and no-invite fields. | Focused non-live schema assertion. | No Java/C# trace file exists yet. |
| `FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests.CreateSampleExport_ProjectsActionSpecificComparisonShape` | Unit | Java action `2`/`6` source review | Sample export shape for recruitment/application mutation-post traces keeps live boundary, executor, registry, broadcast, and invite counts inactive. | Focused non-live export DTO assertion. | Sample only; no capture populated it. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct-send source review | Readiness report surfaces the mutation-post trace schema as non-live evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsNextRequiredLiveEvidence` | Unit | Java direct-send source review | Next-required evidence includes the mutation-post trace schema while keeping live trace requirements blocked. | Focused readiness-report assertion. | Does not create live trace evidence. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 trace-capture gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The mutation-post trace schema is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutation-post trace projection, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live export projection helper for action `2`/`6` mutation-post disabled boundary plans using the new schema.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a runtime comparison fixture contract row for action `2`/`6` only after the projection helper exists.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2169-Completion.md`
- `docs/Phase-6-Session-2169-Handoff.md`
