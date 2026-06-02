# Phase 6 Session 2112 Completion - FindGroup Action 12 Alliance Helper Evidence

Date: 2026-06-02
Unit of Work: UOW-2112
Status: Completed

## Scope

- Added disabled `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` evidence for Java `CM_FIND_GROUP` action `12` accepted alliance invite.
- Proved the connection helper can resolve the applicant through `IGameClientConnectionRegistry.ForEachOnlinePlayer`.
- Proved the disabled boundary can compose the action `12` alliance invite request when the responder's instance group has `minMembers > 6`.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action `12` parses `playerOrTeamId` and `instanceApplicationReply`, then calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplicationResult` resolves the applicant from `World.getInstance().getPlayer(applicantId)`.
  - Reply `1` checks the responder's instance group and calls `PlayerGroupService.inviteToGroup` when `minMembers <= 6`; otherwise it calls `PlayerAllianceService.inviteToAlliance`.

## What Changed

- Added `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptAllianceUsesConnectionResolverAndRuntimesWithoutLiveDispatch`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record that accepted action `12` group and alliance invite branches now have disabled connection-helper evidence.

## Validation

- Changed surface:
  - Test-only connection-boundary evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests" --no-restore`
  - Final result: passed, 15 tests.
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `12` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateDisabledFindGroupBoundaryPlan`; `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet / Connection Helper | Partial | Unit Tested | Partial Parity | Disabled connection helper parses action `12`, resolves the applicant, and composes non-live accepted invite side effects. Live `ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` accepted alliance branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundaryDispatchAdapterService`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Service Method / Boundary Adapter | Partial | Unit Tested | Partial Parity | Focused evidence covers accepted alliance invite composition with `minMembers > 6` through the disabled connection helper. Declined whisper connection-helper evidence, live invite packet side effects, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.services.PlayerAllianceService.inviteToAlliance` call site from FindGroup accept | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.SendInvite` | Invite Service | Partial | Unit Tested | Partial Parity | Disabled connection helper reaches alliance invite request setup and records the pending request/question. Live response execution, membership mutation, requester message sends, and Java runtime comparison remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptAllianceUsesConnectionResolverAndRuntimesWithoutLiveDispatch` | Unit | Java `CM_FIND_GROUP.runImpl` action `12`; `FindGroupService.sendInstanceApplicationResult` accept branch | Disabled connection helper resolves applicant, composes alliance invite request for `minMembers > 6`, records no direct/broadcast intents, and sends no live packets | Focused C# unit test plus reviewed Java source | No live `ProcessPacketAsync`, encrypted socket, Java runtime trace, or declined whisper connection-helper coverage |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionTwelveInviteWithRuntimeComposesDisabledInvitePlan` | Unit | Java `FindGroupService.sendInstanceApplicationResult` accept branch | Adapter composes action `12` invite plan when supplied resolver and runtimes | Existing focused C# unit test plus reviewed Java source | Adapter-level group branch only |
| `FindGroupInstanceApplicationInviteDispatchPlanServiceTests.CreateDisabledPlan_AllianceInviteIntentRegistersAllianceQuestionWithoutLiveDispatch` | Unit | Java `PlayerAllianceService.inviteToAlliance` call site from FindGroup accept branch | Disabled invite planner registers alliance invite question without live dispatch | Existing focused C# unit test plus reviewed Java source | Does not execute live alliance membership mutation |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` accepted group/alliance invite has disabled connection-helper evidence, but declined whisper still needs connection-helper evidence.
- Action `12` live invite packet sends, requester message sends, question response execution, group/alliance membership mutation, Java runtime traces, real-client behavior, and socket-level order remain unverified.
- Direct packet sends, race-filtered world broadcasts, visibility filtering, singleton mutation ordering, enumeration snapshots, and concurrency remain unverified for live dispatch.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2112-Completion.md`
- `docs/Phase-6-Session-2112-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add disabled action `12` connection-helper evidence for the declined whisper direct packet intent.

Safe alternative candidates:

- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add non-live failure result evidence for missing applicant or missing responder instance group through the connection helper.
