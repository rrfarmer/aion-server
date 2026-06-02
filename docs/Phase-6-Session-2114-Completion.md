# Phase 6 Session 2114 Completion - FindGroup Action 12 No-Side-Effect Evidence

Date: 2026-06-02
Unit of Work: UOW-2114
Status: Completed

## Scope

- Added disabled `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` evidence for Java `CM_FIND_GROUP` action `12` no-side-effect paths.
- Covered missing applicant resolution.
- Covered accept reply with missing responder instance-group registration.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action `12` parses `playerOrTeamId` and `instanceApplicationReply`, then calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - If `World.getInstance().getPlayer(applicantId)` returns null, the method sends no packet and plans no invite.
  - If reply `1` is accepted but `instanceGroups.get(responder.getObjectId())` is null, the method sends no packet and plans no invite.

## What Changed

- Added `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveMissingApplicantComposesNoSideEffects`.
- Added `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptMissingInstanceGroupComposesNoSideEffects`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record disabled connection-helper evidence for action `12` accepted invite, declined whisper, missing applicant, and missing instance-group branches.

## Validation

- Changed surface:
  - Test-only connection-boundary evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests" --no-restore`
  - Final result: passed, 18 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP` and `FindGroupService.sendInstanceApplicationResult`; no focused Java test target was identified for this disabled C# connection-helper evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped connection helper plus adjacent adapter/invite planner behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `12` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateDisabledFindGroupBoundaryPlan`; `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet / Connection Helper | Partial | Unit Tested | Partial Parity | Disabled connection helper parses action `12`, resolves the applicant when online, and composes non-live accepted invite, declined direct-packet, or no-side-effect outcomes. Live `ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` missing applicant branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundaryDispatchAdapterService` | Service Method / Boundary Adapter | Partial | Unit Tested | Partial Parity | Focused evidence covers missing applicant no-side-effect result through the disabled connection helper. Java runtime comparison remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` missing instance-group branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundaryDispatchAdapterService` | Service Method / Boundary Adapter | Partial | Unit Tested | Partial Parity | Focused evidence covers accepted reply with no responder instance-group registration as a no-side-effect result through the disabled connection helper. Java runtime comparison remains unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveMissingApplicantComposesNoSideEffects` | Unit | Java `FindGroupService.sendInstanceApplicationResult` missing applicant guard | Disabled connection helper produces no direct packets, no broadcasts, and no invite plan when applicant resolution fails | Focused C# unit test plus reviewed Java source | No live `ProcessPacketAsync`, encrypted socket, or Java runtime trace |
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptMissingInstanceGroupComposesNoSideEffects` | Unit | Java `FindGroupService.sendInstanceApplicationResult` missing `instanceGroups` guard | Disabled connection helper produces no direct packets, no broadcasts, and no invite plan when accept reply has no responder instance group | Focused C# unit test plus reviewed Java source | No live `ProcessPacketAsync`, encrypted socket, or Java runtime trace |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` connection-helper evidence now covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, and missing responder instance-group branches, but live invite/direct packet execution remains unverified.
- Java runtime traces, real-client behavior, socket-level order, singleton mutation ordering, enumeration snapshots, and concurrency remain unverified for live dispatch.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2114-Completion.md`
- `docs/Phase-6-Session-2114-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.
