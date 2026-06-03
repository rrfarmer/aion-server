# Phase 6 Session 2572 Completion

## UOW

[Phase 6] UOW-2572: Port the `SM_QUEST_ACTION` SHARE variant (`ActionType.SHARE`, id 5)

## Status

Completed and validated with a focused packet golden test (286/286 in `GamePacketTests`). This is the second of
three CM_QUEST_SHARE dependencies; the live handler wiring (UOW-2573) remains the only outstanding piece.

## Java Source Reviewed

- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION`:
  - Constructor `SM_QUEST_ACTION(int questId, int sharerId, boolean shareInAlliance)` ⇒ `ActionType.SHARE`.
  - `writeImpl`: looks up `DataManager.QUEST_DATA.getQuestById(questId)`; returns before writing if
    `questTemplate != null && questTemplate.getExtraCategory() != QuestExtraCategory.NONE`.
  - Common prefix `writeC(actionType.getId()); writeD(questId)`, then `case SHARE: writeD(sharerId);
    writeD(shareInAlliance ? 1 : 0)` (0 = group, 1 = alliance).
  - `ActionType.SHARE` id = 5.

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestAction.cs` — added `ShareActionId = 5`,
  a `Share(int questId, int sharerId, bool shareInAlliance, bool suppressForExtraCategory = false)` factory, and a
  `_actionId` switch in `WritePayload`. Restructured the private ctor to store `_questId` + nullable `_questState`
  + `_sharerId` + `_shareInAlliance` so Add/Update stay byte-identical and SHARE writes its own three fields.
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` — three SHARE golden assertions: group
  (`shareInAlliance=false`), alliance (`true`), and `suppressForExtraCategory: true` ⇒ empty payload.

## Implementation Notes

- Add/Update output is unchanged: `Add`/`Update` now pass `questState.QuestId` as `_questId`, and `WritePayload`
  writes the same `WriteC(actionId); WriteD(questId); WriteC(status); WriteC(0); WriteD(vars); WriteH(0)` for the
  Add/Update cases. Verified against the pre-existing Update golden `02640000000500220000020000`.
- SHARE golden bytes: group = `0564000000C800000000000000`, alliance = `0564000000C800000001000000`
  (actionId 05, questId 100, sharerId 200, shareInAlliance 0|1), matching Java `writeImpl` SHARE branch.
- The `extraCategory != NONE` early-return is kept as the caller-supplied `suppressForExtraCategory` flag (same
  pattern as Add/Update). For the live CM_QUEST_SHARE wire (UOW-2573), confirm against Java whether the sharer
  resolves this guard before sending; default is `false`.

## Validation Decision (UOW-2572)

```text
Validation decision:
- Changed surface: a single C# server-packet shape (SM_QUEST_ACTION SHARE variant) + its golden test.
- Specific behavior/contract: SHARE writes actionId=5, questId, sharerId, shareInAlliance(0|1) byte-for-byte like
  Java writeImpl; Add/Update remain byte-identical; suppressForExtraCategory yields an empty payload.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj
  --filter "FullyQualifiedName~GamePacketTests"  → 286/286 passed.
- Focused Java/Maven command: none. No regenerated Java packet golden was needed; the expected bytes derive
  directly from the reviewed Java writeImpl SHARE branch (manual deterministic golden).
- Broad-validation trigger: none. Single server-packet shape behind a filtered golden test.
- Broad .NET decision: skipped. The filtered test built Aion.GameServer + the test project (compile signal).
- Why this scope is sufficient: the golden test pins the exact SHARE wire bytes for both alliance flag values plus
  the extra-category suppression path, and re-asserts the unchanged Update golden as a regression guard.
```

## Migration Parity Table (UOW-2572)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `SM_QUEST_ACTION(int,int,boolean)` (SHARE ctor) | `SmQuestAction.Share` | Packet | Complete | Golden File Tested | Verified Parity | Wire bytes pinned for group + alliance + suppressed |
| `SM_QUEST_ACTION.writeImpl` (SHARE case) | `SmQuestAction.WritePayload` (ShareActionId) | Packet | Complete | Golden File Tested | Verified Parity | actionId 5; writeD(sharerId); writeD(0|1) |
| `SM_QUEST_ACTION.ActionType.SHARE` | `SmQuestAction.ShareActionId` (=5) | Const | Complete | Golden File Tested | Verified Parity | — |

## Summary Metrics (conservative)

- New automated coverage: 3 SHARE golden assertions added to the existing `GamePacketTests` (286 total pass).
- Two of three CM_QUEST_SHARE dependencies (template metadata UOW-2571, SHARE packet UOW-2572) are ported +
  verified. Only the live handler wiring (UOW-2573) remains.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- CM_QUEST_SHARE still not live: UOW-2573 must wire the handler (group resolution + ONLINE + range filter +
  per-member `checkStartConditions` + 5 system messages + SHARE packet fanout).
- The `suppressForExtraCategory` guard for SHARE is caller-resolved; confirm against Java that the sharer computes
  `extraCategory` before sending (default false in the port).
- Carried: League partyType-3 distribution; canTrade gate; in-memory kinah persistence; legion-history live
  response; exchange + legion-rank DB reads unit-only; XP/level-up not ported.

## Next Recommended UOW

1. **UOW-2573: Wire CM_QUEST_SHARE live.** Behavior from `CM_QUEST_SHARE.runImpl`:
   - `questTemplate == null || CannotShare` ⇒ SM_SYSTEM_MESSAGE **1100001** (cannot share), return.
   - `questState == null || status == COMPLETE` ⇒ return (no message). (Quest state via
     `player.Quests.FirstOrDefault(q => q.QuestId == questId)`.)
   - Build share list from the current group (null ⇒ empty): `allExcept(player) AND ONLINE AND
     PositionUtil.isInRange(member, player, GroupConfig.GROUP_MAX_DISTANCE)`.
   - Empty ⇒ **1100005** if `Target == "ALLIANCE"` else **1100000**, return.
   - Per member: `NearbyQuestStartConditionService.CheckNearbyStartConditions` false ⇒ **1100003**
     `member.Name` to sharer; true ⇒ `SmQuestAction.Share(questId, player.ObjectId, member.IsInAlliance)` to
     member + **1100002** `member.Name` to sharer.
   - **Verify first**: `PlayerGroupRuntime` online-member enumeration API, the GROUP_MAX_DISTANCE config value,
     `PositionUtilService` range API signature, `member.IsInAlliance`, and that system-message ids
     1100000/1100001/1100002/1100003/1100005 exist with a `%0` name param.
2. **Alt**: a deferred handler whose deps are ported — Work Discovery on the `deferred` switch cases in
   `GameServerConnection.cs`.

### Focused validation recipe for UOW-2573 (live CM_QUEST_SHARE)

- Behavior/contract: CM_QUEST_SHARE emits 1100001 (cannot share / null template), returns silently on
  null/COMPLETE quest state, 1100000/1100005 on no eligible members (by Target), and per eligible member either
  1100003 (failed start conditions) or SM_QUEST_ACTION.SHARE + 1100002.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~<CmQuestShareDispatchTests>|FullyQualifiedName~GamePacketTests"` (new dispatch test class +
  the SHARE golden; narrow to the dispatch class once green).
- Java/Maven: not expected unless quest XML or a Java fixture changes.
- Broad-validation trigger: none unless the wire crosses shared group/world state in a way focused tests expose.

## Context Needed By Next Session

- CM_QUEST_SHARE dependencies now ported: template metadata (`NearbyQuestTemplateSummary.CannotShare` / `.Target`,
  UOW-2571) and the SHARE packet (`SmQuestAction.Share`, UOW-2572). Only the live handler wiring remains.
- CM_QUEST_SHARE parser is ported (`CmQuestShare.cs`, exposes `QuestId`); dispatch is still `break;` in
  `GameServerConnection.cs` (`CmQuestShare` case).
- `QuestService.checkStartConditions` ⇒ `NearbyQuestStartConditionService.CheckNearbyStartConditions(player,
  questId, NearbyQuestTemplateTable, ...)` → `NearbyQuestStartConditionResult.CanStart`.
- `SmQuestAction.Share(questId, sharerId, shareInAlliance)` produces the share question-window packet.
- Group online-member enumeration + range helpers exist (`PlayerGroupRuntime`, `PositionUtilService`); confirm
  exact APIs + the GROUP_MAX_DISTANCE config value before wiring.
