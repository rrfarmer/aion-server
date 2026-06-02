# Phase 6 Session 2113 Completion - FindGroup Action 12 Declined Whisper Evidence

Date: 2026-06-02
Unit of Work: UOW-2113
Status: Completed

## Scope

- Added disabled `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` evidence for Java `CM_FIND_GROUP` action `12` declined whisper.
- Proved the connection helper can resolve the applicant through `IGameClientConnectionRegistry.ForEachOnlinePlayer`.
- Proved the disabled boundary exposes the declined `SM_MESSAGE` direct packet intent and does not compose an invite plan.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action `12` parses `playerOrTeamId` and `instanceApplicationReply`, then calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Non-accept replies send `new SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)` to the resolved applicant.

## What Changed

- Added `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveDeclineComposesWhisperIntentWithoutLiveDispatch`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record that action `12` accepted invite and declined whisper branches now have disabled connection-helper evidence.

## Validation

- Changed surface:
  - Test-only connection-boundary evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests" --no-restore`
  - First run failed at compile time because the new test used `Assert.Empty` against `QuestionResponseRegistry`; the assertion was corrected to use `ResponseRequester.Count`.
  - Final result: passed, 16 tests.
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `12` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateDisabledFindGroupBoundaryPlan`; `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet / Connection Helper | Partial | Unit Tested | Partial Parity | Disabled connection helper parses action `12`, resolves the applicant, and composes non-live accepted invite or declined direct-packet side effects. Live `ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` declined branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupConnectionBoundaryDispatchAdapterService` | Service Method / Boundary Adapter | Partial | Unit Tested | Partial Parity | Focused evidence covers declined whisper direct packet intent through the disabled connection helper. Live send execution and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE` declined whisper call site | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage`; `FindGroupDirectPacketIntent` | Server Packet Intent | Partial | Unit Tested | Partial Parity | Disabled helper records an `SmMessage` direct packet intent with Java source breadcrumb. Packet bytes, live socket order, and real-client behavior remain unverified for this branch. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveDeclineComposesWhisperIntentWithoutLiveDispatch` | Unit | Java `CM_FIND_GROUP.runImpl` action `12`; `FindGroupService.sendInstanceApplicationResult` declined branch | Disabled connection helper resolves applicant, composes declined `SmMessage` direct packet intent, skips invite plan, and sends no live packets | Focused C# unit test plus reviewed Java source | No live `ProcessPacketAsync`, encrypted socket, Java runtime trace, or packet-byte comparison for the whisper |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` accepted group/alliance invite and declined whisper have disabled connection-helper evidence, but live invite/direct packet execution remains unverified.
- Action `12` missing applicant and missing responder instance-group paths still need connection-helper evidence.
- Java runtime traces, real-client behavior, socket-level order, singleton mutation ordering, enumeration snapshots, and concurrency remain unverified for live dispatch.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2113-Completion.md`
- `docs/Phase-6-Session-2113-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add disabled action `12` connection-helper evidence for missing applicant and missing responder instance-group paths.

Safe alternative candidates:

- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
