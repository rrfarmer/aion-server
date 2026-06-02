# Phase 6 Session 2111 Completion - FindGroup Action 12 Connection Helper Evidence

Date: 2026-06-02
Unit of Work: UOW-2111
Status: Completed

## Scope

- Added disabled `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` evidence for Java `CM_FIND_GROUP` action `12` accept.
- Proved the connection helper can resolve the applicant through `IGameClientConnectionRegistry.ForEachOnlinePlayer`.
- Proved the disabled boundary can compose the action `12` group invite request through injected group/alliance runtimes without live socket sends.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action `12` parses `playerOrTeamId` and `instanceApplicationReply`, then calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplicationResult` resolves the applicant from `World.getInstance().getPlayer(applicantId)`.
  - Reply `1` checks the responder's instance group and calls `PlayerGroupService.inviteToGroup` when `minMembers <= 6`; otherwise it calls `PlayerAllianceService.inviteToAlliance`.
  - Non-accept replies send an `SM_MESSAGE` whisper to the applicant when present.

## What Changed

- Added `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptUsesConnectionResolverAndRuntimesWithoutLiveDispatch`.
- Extended the test fixture so disabled FindGroup connection-boundary tests can pass:
  - an `IGameClientConnectionRegistry` applicant resolver;
  - injected `PlayerGroupRuntime`;
  - injected `PlayerAllianceRuntime`.
- Added a local capturing registry for connection-boundary evidence only.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record this disabled action `12` connection-helper evidence.

## Validation

- Changed surface:
  - Test-only connection-boundary evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests" --no-restore`
  - First run failed at compile time because the new test asserted a non-existent `PendingGroupInviteRequest.InvitedObjectId` property.
  - The assertion was corrected to match the current C# request shape.
  - Final result: passed, 14 tests.
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `12` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateDisabledFindGroupBoundaryPlan`; `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet / Connection Helper | Partial | Unit Tested | Partial Parity | Disabled connection helper parses action `12`, resolves the applicant, and composes non-live side effects. Live `ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` accept branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundaryDispatchAdapterService`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Service Method / Boundary Adapter | Partial | Unit Tested | Partial Parity | Focused evidence covers accepted group invite composition with `minMembers <= 6` through the disabled connection helper. Alliance accept, declined whisper connection-helper evidence, live invite packet side effects, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.world.World.getPlayer` applicant resolution | `IGameClientConnectionRegistry.ForEachOnlinePlayer`; `GameServerConnection.ResolveOnlinePlayerByObjectId` | Runtime Resolver | Partial | Unit Tested | Partial Parity | The disabled connection helper can resolve an online applicant from the connection registry. World/runtime identity, socket order, and real-client behavior remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptUsesConnectionResolverAndRuntimesWithoutLiveDispatch` | Unit | Java `CM_FIND_GROUP.runImpl` action `12`; `FindGroupService.sendInstanceApplicationResult` accept branch | Disabled connection helper resolves applicant, composes group invite request, records no direct/broadcast intents, and sends no live packets | Focused C# unit test plus reviewed Java source | No live `ProcessPacketAsync`, encrypted socket, Java runtime trace, alliance accept, or declined whisper connection-helper coverage |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionTwelveInviteWithRuntimeComposesDisabledInvitePlan` | Unit | Java `FindGroupService.sendInstanceApplicationResult` accept branch | Adapter composes group invite plan when supplied resolver and runtimes | Existing focused C# unit test plus reviewed Java source | Adapter-level only |
| `FindGroupInstanceApplicationInviteDispatchPlanServiceTests.CreateDisabledPlan_GroupInviteIntentRegistersPartyQuestionWithoutLiveDispatch` | Unit | Java `PlayerGroupService.inviteToGroup` call site from FindGroup accept branch | Disabled invite planner registers party invite question without live dispatch | Existing focused C# unit test plus reviewed Java source | Does not execute live group membership mutation |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` accepted group invite has disabled connection-helper evidence, but accepted alliance invite and declined whisper still need connection-helper evidence.
- Action `12` live invite packet sends, question response execution, group/alliance membership mutation, Java runtime traces, real-client behavior, and socket-level order remain unverified.
- Direct packet sends, race-filtered world broadcasts, visibility filtering, singleton mutation ordering, enumeration snapshots, and concurrency remain unverified for live dispatch.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2111-Completion.md`
- `docs/Phase-6-Session-2111-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add disabled action `12` connection-helper evidence for the accepted alliance invite branch (`minMembers > 6`) using the connection resolver and injected group/alliance runtimes.

Safe alternative candidates:

- Add disabled action `12` connection-helper evidence for declined whisper direct packet intent.
- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
