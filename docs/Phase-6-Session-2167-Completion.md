# Phase 6 Session 2167 Completion - FindGroup Mutation Direct Trace Scaffold

Date: 2026-06-02
Unit of Work: UOW-2167
Status: Completed

## Scope

This unit added a non-live live-boundary trace scaffold for mutating direct-packet `CM_FIND_GROUP` actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `2` parses `playerOrTeamId`, `message`, and `groupType`, then calls `FindGroupService.addRecruitment(player, message, groupType)`.
- `addRecruitment` stores recruitment state keyed by current team object id when present, otherwise player object id; sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED()`; then calls `showRecruitments(player)`.
- Action `6` parses `playerOrTeamId`, `message`, `groupType`, `classId`, and `level`, then calls `FindGroupService.addApplication(player, message, groupType, classId, level)`.
- `addApplication` stores application state keyed by player object id; sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()`; then calls `showApplications(player)`.

This UOW does not enable live `CM_FIND_GROUP` dispatch, does not invoke live sends, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService`.
- The scaffold covers actions `2` and `6` only.
- The scaffold excludes direct-packet actions `0`, `4`, `8`, `9`, `10`, `11`, `13`, `15`, and `17`.
- Required ordered milestones now include:
  - triggering client packet accepted,
  - shared singleton mutation plan composed,
  - state mutation recorded before direct sends,
  - posted system message intent materialized,
  - refreshed show-list intent materialized,
  - direct-packet executor invoked from the boundary,
  - registry send ordering observed,
  - boundary trace captured.
- Recorded Java posted system message ids:
  - action `2`: `1400392`,
  - action `6`: `1400393`.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to surface the mutation-post scaffold as non-live evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/scaffold service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live scaffold used reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new scaffold plus adjacent readiness and existing action `2`/`6` opt-in composition evidence, and the filtered command built the affected project/dependencies.

Result:

- Passed: 30
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService` | Runtime Comparison Readiness / Trace Scaffold | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post trace milestones are represented, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Direct Packet Readiness / Service Planner | Partial | Unit Tested | Partial Parity | Java add-recruitment/add-application mutation and direct-send ordering are represented in non-live scaffold and existing opt-in composition tests. No live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests.Create_KeepsMutationPostScaffoldBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Scaffold remains blocked/non-live and documents Java add-recruitment/add-application sources. | Focused non-live scaffold assertion. | Does not capture runtime traces. |
| `FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests.Create_ScopesOnlyJavaMutationPostDirectActions` | Unit | Java action dispatch source review | Scaffold covers only actions `2` and `6` and excludes other direct-packet actions. | Focused action-scope assertion. | Other mutating direct actions still need their own live evidence. |
| `FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests.Create_RequiresOrderedMutationPostTraceMilestonesBeforeLiveReadiness` | Unit | Java add-recruitment/add-application source review | Required trace milestone order, singleton mutation, state mutation, posted-message, refreshed-list, registry-ordering, and boundary-trace requirements. | Focused non-live milestone assertion. | No `ProcessPacketAsync` boundary trace exists yet. |
| `FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests.Create_RecordsJavaPostedSystemMessageIds` | Unit | C# planner and Java system-message call-site review | Action `2` and `6` posted system message ids are surfaced as stable trace evidence. | Focused DTO assertion. | Message payload byte parity remains outside this scaffold. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct-send source review | Readiness report includes mutation-post scaffold evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsNextRequiredLiveEvidence` | Unit | Java direct-send source review | Next-required evidence includes action `2`/`6` mutation-post scaffold use. | Focused readiness-report assertion. | Does not create live trace evidence. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 trace-capture gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The mutation-post scaffold is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Show-list traces, mutating direct-packet traces, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live trace export population helper for action `0`/`4` disabled boundary plans using the existing show-list trace schema.

Safe candidates:

- Add a trace schema/export DTO for action `2`/`6` mutation-post boundary traces.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2167-Completion.md`
- `docs/Phase-6-Session-2167-Handoff.md`
