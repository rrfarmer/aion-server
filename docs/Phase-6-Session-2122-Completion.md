# Phase 6 Session 2122 Completion - FindGroup Boundary Multi-Direct Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2122
Status: Completed

## Scope

- Added focused disabled-boundary execution-order assertions for `CM_FIND_GROUP` actions `2` and `6`.
- Covered Java's posted-message-before-refresh behavior for `FindGroupService.addRecruitment` and `FindGroupService.addApplication`.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` action `2` calls `FindGroupService.addRecruitment(player, message, groupType)`.
  - Java `runImpl` action `6` calls `FindGroupService.addApplication(player, message, groupType, classId, level)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `addRecruitment` stores the recruitment, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED`, then calls `showRecruitments(player)`.
  - `showRecruitments` sends `new SM_FIND_GROUP(0, recruitments)`.
  - `addApplication` stores the application, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED`, then calls `showApplications(player)`.
  - `showApplications` sends `new SM_FIND_GROUP(4, applications)`.

## What Changed

- Extended `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTwoAddRecruitmentAsPostedMessageAndShowList` to assert execution sequence:
  - sequence `1`: `SmSystemMessage` posted-message direct packet.
  - sequence `2`: `SmFindGroup` refreshed recruitment-list direct packet.
- Extended `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSixAddApplicationAsPostedMessageAndShowList` to assert execution sequence:
  - sequence `1`: `SmSystemMessage` posted-message direct packet.
  - sequence `2`: `SmFindGroup` refreshed application-list direct packet.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record disabled-boundary multi-direct ordering evidence.

## Validation

- Changed surface:
  - Test-only ordering evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore`
  - Final result: passed, 26 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl`, `FindGroupService.addRecruitment`, `FindGroupService.showRecruitments`, `FindGroupService.addApplication`, and `FindGroupService.showApplications`; no focused Java test target was identified for this disabled C# boundary ordering evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped ordering evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` actions `2` and `6` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync`; `FindGroupSideEffectDispatchExecutorService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence covers disabled-boundary direct-packet execution order for posted message before refreshed show-list. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment`; `showRecruitments` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddRecruitment`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence records `STR_PARTY_MATCH_OFFER_PARTY_POSTED` before `SM_FIND_GROUP(0, recruitments)` at the disabled boundary. Live socket order and Java runtime trace remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication`; `showApplications` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddApplication`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence records `STR_PARTY_MATCH_SEEK_PARTY_POSTED` before `SM_FIND_GROUP(4, applications)` at the disabled boundary. Live socket order and Java runtime trace remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTwoAddRecruitmentAsPostedMessageAndShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment`; `FindGroupService.showRecruitments` | Disabled boundary execution order sends posted-message packet before refreshed recruitment-list packet | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSixAddApplicationAsPostedMessageAndShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addApplication`; `FindGroupService.showApplications` | Disabled boundary execution order sends posted-message packet before refreshed application-list packet | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes, 5 methods/branches.
- Total artifacts ported or represented in this UOW: 2 C# evidence surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Disabled-boundary action `2`/`6` direct-packet order is now covered, but live socket ordering relative to the triggering client packet remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2122-Completion.md`
- `docs/Phase-6-Session-2122-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add disabled-boundary execution-order assertions for action `10` mask-list-before-show-list when form-anywhere is enabled.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
