# Phase 6 Session 2573 Completion

## UOW

[Phase 6] UOW-2573: Wire CM_QUEST_SHARE live (quest-share decision planner + dispatch handler)

## Status

Completed and validated with a focused planner unit suite (12/12 in `QuestSharePlanServiceTests`). CM_QUEST_SHARE
is now live: the client packet is decoded, the share decision is computed, and SM_QUEST_ACTION.SHARE / system
messages are fanned out. This completes the three-UOW quest-share arc (UOW-2571 template metadata, UOW-2572 SHARE
packet, UOW-2573 live wiring).

## Java Source Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_QUEST_SHARE.runImpl` (full behavior):
  - `questTemplate == null || isCannotShare()` ⇒ SM_SYSTEM_MESSAGE **1100001**, return.
  - `questState == null || status == COMPLETE` ⇒ return (no message).
  - `membersToShareWith` = `currentGroup.filterMembers(allExcept(player) AND ONLINE AND
    PositionUtil.isInRange(member, player, GroupConfig.GROUP_MAX_DISTANCE))`; `currentGroup == null` ⇒ empty.
  - empty ⇒ **1100005** if `getTarget() == ALLIANCE` else **1100000**, return.
  - per member: `QuestService.checkStartConditions(member, questId, false)` false ⇒ **1100003** `member.getName()`
    to sharer; true ⇒ `SM_QUEST_ACTION(questId, player.getObjectId(), member.isInAlliance())` to member +
    **1100002** `member.getName()` to sharer.
- `GroupConfig.GROUP_MAX_DISTANCE` = `gameserver.playergroup.maxdistance` default **100** (group.properties uses 100).
- `Player.isInAlliance()` = `playerAllianceGroup != null` (member belongs to an alliance).
- `PositionUtil.isInRange(VisibleObject, VisibleObject, float)` ⇒ `isInRange(.., true)`: requires same
  worldId + instanceId, then squared-distance `< range²`.

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/QuestSharePlanService.cs` — new pure planner. Given the sharer,
  quest template/state, the resolved current-team online members, and a `canStartConditions` delegate, it returns
  an ordered `QuestSharePlan` of `QuestShareInstruction(recipientObjectId, packet)`. Encodes the full runImpl
  decision: cannot-share/null-template ⇒ 1100001; null/COMPLETE state ⇒ empty; self-exclusion + range filter
  (`GROUP_MAX_DISTANCE`=100, same world+instance); empty ⇒ 1100005 (ALLIANCE target) / 1100000; per member ⇒
  1100003 fail, or `SmQuestAction.Share` + 1100002.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`:
  - `CmQuestShare` dispatch case now binds the packet and calls `HandleQuestShareAsync(_activePlayer, QuestId)`
    (was a deferred `break;`).
  - new `HandleQuestShareAsync`: resolves the template from `_runtimeContext.DataManager.StaticData.NearbyQuestTemplates`,
    the quest state from `player.Quests`, and the current-team online members (Group ⇒
    `_playerGroupRuntime.GetOnlineMemberPlayers`; Alliance ⇒ `_playerAllianceRuntime.GetOnlineMemberPlayers`;
    None ⇒ empty), then executes `QuestSharePlanService.Plan` (with `canStartConditions` =
    `NearbyQuestStartConditionService.CheckNearbyStartConditions(member, questId, table).CanStart`) and fans out
    each instruction via `SendToPlayerOrActiveAsync`.
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestSharePlanServiceTests.cs` — new, 12 cases.

## Implementation Notes

- `getCurrentGroup()` returns the whole group OR the whole alliance (not the alliance sub-group), so the handler
  uses `GetOnlineMemberPlayers` (whole-team) for both Group and Alliance — distinct from CM_GROUP_DISTRIBUTION
  partyType-1-in-alliance, which uses the sub-group. Solo players (membership None) ⇒ empty members ⇒ 1100000.
- `member.isInAlliance()` ⇒ `member.TeamMembership == PlayerTeamMembership.Alliance` for the SHARE alliance flag.
- The range gate mirrors `isInRange(member, player, 100, centerToCenter=true)`: same world+instance then
  `PositionUtilService.IsInRange(..)` squared-distance `< 100²`.
- `Target` is compared against the literal `"ALLIANCE"` (the raw string stored in UOW-2571).
- The handler emits packets only — it does not mutate kinah, inventory, or quest state — so the live wiring's
  blast radius is limited to packet fanout.

## Validation Decision (UOW-2573)

```text
Validation decision:
- Changed surface: production-code — a new pure decision planner (QuestSharePlanService) plus a thin live dispatch
  handler (HandleQuestShareAsync) that wires CM_QUEST_SHARE. The handler emits packets only; no shared runtime
  state (kinah/inventory/quest state) is mutated.
- Specific behavior/contract: CM_QUEST_SHARE.runImpl decision parity — 1100001 on cannot-share/null template;
  silent return on null/COMPLETE quest state; (group OR alliance) ∩ ONLINE ∩ in-range membership with self
  excluded; empty ⇒ 1100005 (Target==ALLIANCE) / 1100000; per member ⇒ 1100003 fail, or SM_QUEST_ACTION.SHARE +
  1100002.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj
  --filter "FullyQualifiedName~QuestSharePlanServiceTests"  → 12/12 passed. (The SHARE packet wire bytes are
  already golden-tested in UOW-2572 GamePacketTests.)
- Focused Java/Maven command: none. No matching narrow Java fixture exists; parity derives from the reviewed
  runImpl source plus the GROUP_MAX_DISTANCE=100 config and PositionUtil.isInRange semantics.
- Broad-validation trigger: none. The handler fans out packets only; the decision logic is fully unit-covered in
  the planner, and the group/alliance online-member accessors are shared with the already-tested distribution
  path (UOW-2569/2570).
- Broad .NET decision: skipped. The filtered test built Aion.GameServer (including the wired handler) + the test
  project (compile signal for the glue).
- Why this scope is sufficient: the planner tests exercise every runImpl branch (null/cannot-share template,
  null/COMPLETE state, empty members by target, self-exclusion, out-of-range, different-world, pass/fail start
  conditions, mixed members); the thin handler is a mechanical input-resolution + fanout mirror of the existing
  distribution handler.
```

## Migration Parity Table (UOW-2573)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_QUEST_SHARE.runImpl` (decision) | `QuestSharePlanService.Plan` | Service | Complete | Unit Tested | Verified Parity | All runImpl branches unit-covered |
| `CM_QUEST_SHARE.runImpl` (dispatch) | `GameServerConnection.HandleQuestShareAsync` | Handler | Complete | Manual Only | Partial Parity | Thin glue: resolves template/state/members + fanout; not directly unit-tested (test seam lacks runtime template injection) |
| `PositionUtil.isInRange(VO,VO,float)` | `QuestSharePlanService.IsInGroupRange` | Utility | Complete | Unit Tested | Verified Parity | same world+instance + squared distance < range² |
| `Player.isInAlliance()` | `member.TeamMembership == Alliance` | Predicate | Complete | Unit Tested | Verified Parity | drives SHARE alliance flag |

## Summary Metrics (conservative)

- New automated coverage: 12 planner unit tests covering every CM_QUEST_SHARE.runImpl branch.
- CM_QUEST_SHARE is now live (was deferred). The quest-share arc (UOW-2571/2572/2573) is complete.
- Overall Phase 6 completion estimate: incremental; one previously-blocked client packet is now fully wired.

## Remaining Risks

- The thin dispatch handler (`HandleQuestShareAsync`) is not directly unit-tested: the connection test seam does
  not inject `NearbyQuestTemplates` into `_runtimeContext`, so a dispatch test would only exercise the null-template
  path (already covered). Mitigation: the decision is fully covered by the planner; the input-resolution + fanout
  glue mirrors the unit-tested `HandleGroupDistributionAsync`. A future runtime-context test seam could close this.
- `canTrade`/restriction gates: Java CM_QUEST_SHARE has none (unlike distribution), so no omission here.
- Carried: League partyType-3 distribution; canTrade gate (distribution); in-memory kinah persistence;
  legion-history live response; exchange + legion-rank DB reads unit-only; XP/level-up not ported.

## Next Recommended UOW

1. **UOW-2574: Port `CM_DELETE_QUEST` (quest abandon).** Java
   `network/aion/clientpackets/CM_DELETE_QUEST.runImpl` cancels timed quests and dispatches
   `QuestService.abandonQuest`; dispatch is deferred in `GameServerConnection.cs` (`CmDeleteQuest` case, search
   `CM_DELETE_QUEST`). **Verify first**: whether `QuestService.abandonQuest` semantics (quest state removal/reset +
   SM_QUEST_ACTION.ABANDON + `cannot_giveup` gate via the now-available quest template) are reachable. The
   `QuestTemplate.cannotGiveup` field is the abandon analog of `cannotShare` and is **not** yet on
   `NearbyQuestTemplateSummary` — a small UOW-2571-style extractor add may be a prerequisite.
2. **Alt**: another deferred handler whose deps are ported — Work Discovery on the `deferred` switch cases in
   `GameServerConnection.cs`.

### Focused validation recipe for UOW-2574 (CM_DELETE_QUEST)

- Behavior/contract: CM_DELETE_QUEST honors `cannot_giveup`, removes/resets the quest state, and emits
  SM_QUEST_ACTION.ABANDON. (Confirm exact Java behavior before scoping.)
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~<CmDeleteQuestTests>|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests"` (planner/
  decision tests + extractor if a template field is added).
- Java/Maven: not expected unless quest XML parsing is touched.
- Broad-validation trigger: none unless the abandon path mutates shared persistence.

## Context Needed By Next Session

- CM_QUEST_SHARE is live end-to-end: parser (`CmQuestShare`) → `HandleQuestShareAsync` → `QuestSharePlanService`
  → SM_QUEST_ACTION.SHARE + system messages.
- `QuestSharePlanService.Plan(sharer, questId, template, questState, currentTeamOnlineMembers, canStartConditions,
  groupMaxDistance=100)` is the reusable decision; message-id constants are exposed as `Msg*`.
- Quest template metadata lives on `NearbyQuestTemplateSummary` (`CannotShare`, `Target`); `cannotGiveup` is NOT
  yet ported (next abandon UOW likely needs it).
- `NearbyQuestStartConditionService.CheckNearbyStartConditions` is the `QuestService.checkStartConditions` port.
- Group/alliance whole-team online members: `_playerGroupRuntime.GetOnlineMemberPlayers(teamId)` /
  `_playerAllianceRuntime.GetOnlineMemberPlayers(teamId)`.
