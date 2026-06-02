# Phase 6 Session 2118 Completion - FindGroup Logout Cross-Caller Singleton Evidence

Date: 2026-06-02
Unit of Work: UOW-2118
Status: Completed

## Scope

- Added focused evidence that `PlayerEnterWorldService.LeaveWorldAsync` removes FindGroup state created earlier through the disabled `CM_FIND_GROUP` client-action planner when both use the same injected `FindGroupRecruitmentPlanService`.
- Covered recruitment, application, and instance-group entries keyed by the logging-out player's object id.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` actions `2`, `6`, and `8` mutate the singleton `FindGroupService` state for recruitment, application, and instance group entries.
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld` calls `FindGroupService.getInstance().onLogout(player)` before `player.getResponseRequester().denyAll()`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onLogout` removes `recruitments`, `applications`, and `instanceGroups` entries keyed by `player.getObjectId()` without packet fanout.

## What Changed

- Added `PlayerEnterWorldServiceTests.LeaveWorld_RemovesFindGroupStateCreatedByDisabledClientActionPlannerLikeJavaSingleton`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record disabled client-action-to-logout singleton cleanup evidence.

## Validation

- Changed surface:
  - Test-only cross-caller singleton evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore`
  - Final result: passed, 87 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl`, `PlayerLeaveWorldService.leaveWorld`, and `FindGroupService.onLogout`; no focused Java test target was identified for this disabled C# singleton-cleanup evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped cross-caller behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` actions `2`, `6`, `8` | `Aion.GameServer.Services.FindGroupClientActionPlanService.Plan`; `FindGroupRecruitmentPlanService` | Client Action Planner | Partial | Unit Tested | Partial Parity | Focused evidence covers disabled planner mutation of recruitment, application, and instance-group singleton state consumed by logout cleanup. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` FindGroup cleanup | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Lifecycle Service | Partial | Unit Tested | Partial Parity | Focused evidence covers cleanup against state created through the same injected FindGroup service and preserves disabled no-packet cleanup. Broader Java logout side effects and real runtime ordering remain partially covered. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers recruitment/application/instance-group removal by player object id, including state created through disabled client-action planner. Concurrent callers and live singleton dispatch remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerEnterWorldServiceTests.LeaveWorld_RemovesFindGroupStateCreatedByDisabledClientActionPlannerLikeJavaSingleton` | Unit | Java `CM_FIND_GROUP.runImpl`; `PlayerLeaveWorldService.leaveWorld`; `FindGroupService.onLogout` | Disabled client-action planner writes recruitment/application/instance-group state to the injected FindGroup service, then logout removes that same singleton state | Focused C# unit test plus reviewed Java source | Does not prove live packet dispatch, Java runtime trace, or concurrent singleton behavior |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 3.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Disabled client-action-to-logout singleton cleanup now has focused evidence, but live socket dispatch, Java runtime traces, real-client behavior, and concurrent singleton mutation ordering remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2118-Completion.md`
- `docs/Phase-6-Session-2118-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add focused cross-caller evidence for group or alliance disband cleanup against FindGroup state created through the disabled client-action planner, matching Java `PlayerGroupService.disband` or `PlayerAllianceService.disband`.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.
