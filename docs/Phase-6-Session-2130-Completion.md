# Phase 6 Session 2130 Completion - FindGroup Action 12 Failure Result Evidence

Date: 2026-06-02
Unit of Work: UOW-2130
Status: Completed

## Scope

- Added focused disabled failure-result evidence for action `12` instance-application dispatch paths before any live `ProcessPacketAsync` wiring.
- Covered the declined-whisper direct packet path when the applicant recipient is no longer resolvable.
- Covered the accepted-invite path when the responder/inviter is no longer resolvable.
- Kept all evidence disabled and non-live.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplicationResult` resolves the applicant through `World.getInstance().getPlayer(applicantId)`.
  - Decline sends `new SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)` to the applicant only when the applicant exists.
  - Accept invokes `PlayerGroupService.inviteToGroup(responder, applicant)` or `PlayerAllianceService.inviteToAlliance(responder, applicant)` only after applicant and responder-side instance-group state exist.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `runImpl` action `12` calls `FindGroupService.getInstance().sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.

## What Changed

- Added `CreateDisabledPlan_DeclinedActionTwelveMissingApplicantRecipientRecordsFailureWithoutLiveDispatch`.
- Added `CreateDisabledPlan_MissingResponderSkipsWithoutQuestionMutation`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record missing-recipient and missing-invite-player failure-result evidence.

## Validation

- Changed surface:
  - Test-only disabled failure-result evidence plus documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: passed, 79 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `12` and `FindGroupService.sendInstanceApplicationResult`; no focused Java test target was identified for this disabled C# failure-result evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped failure-result evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `12` | `Aion.GameServer.Services.FindGroupInstanceApplicationDirectDispatchPlanService`; `Aion.GameServer.Services.FindGroupInstanceApplicationInviteDispatchPlanService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused disabled evidence records failure-result surfaces for declined-whisper missing direct recipient and accepted-invite missing inviter/responder. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; disabled direct/invite dispatch plans | Service Method | Partial | Unit Tested | Partial Parity | Java source reviewed for applicant lookup and invite/decline branching. C# disabled tests surface missing recipient/player status and prove no direct packet audit or invite question mutation is produced in these failure paths. Java runtime/socket trace remains unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupInstanceApplicationDirectDispatchPlanServiceTests.CreateDisabledPlan_DeclinedActionTwelveMissingApplicantRecipientRecordsFailureWithoutLiveDispatch` | Unit | Java `FindGroupService.sendInstanceApplicationResult`; Java `World.getPlayer` lookup guard | Declined action `12` whisper intent surfaces `SkippedMissingRecipient`, records the missing applicant id, and plans no direct packet audit when the recipient cannot be resolved | Focused C# unit test plus reviewed Java source | Does not prove live socket behavior or Java runtime trace |
| `FindGroupInstanceApplicationInviteDispatchPlanServiceTests.CreateDisabledPlan_MissingResponderSkipsWithoutQuestionMutation` | Unit | Java `FindGroupService.sendInstanceApplicationResult`; Java group/alliance invite call shape | Accepted action `12` invite dispatch surfaces `SkippedMissingPlayer` with `MissingInviter`, and does not mutate applicant response-request state | Focused C# unit test plus reviewed Java source | Does not prove live socket behavior, Java runtime trace, or all group/alliance invite service branches |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes, 2 methods/branches.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` failure-result behavior is covered only through disabled direct/invite dispatch plans.
- Java runtime/socket trace was not produced in this UOW.
- Broader invite request behavior and live connection behavior remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationDirectDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationInviteDispatchPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2130-Completion.md`
- `docs/Phase-6-Session-2130-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review multi-step mutation ordering under concurrent singleton callers before live dispatch, or add one more adapter-result evidence slice for direct packet/world broadcast execution failures if a concrete missing status is not yet surfaced.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add live-readiness tests around connection-registry direct packet ordering relative to the triggering client packet.
- Inventory remaining `CM_FIND_GROUP` blockers into a concise go/no-go checklist before any live `ProcessPacketAsync` wiring.
