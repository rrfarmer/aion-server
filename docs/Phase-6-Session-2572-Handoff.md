# Phase 6 Session 2572 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2572: Port the `SM_QUEST_ACTION` SHARE variant (`ActionType.SHARE`, id 5). Second of three
CM_QUEST_SHARE dependencies. See [Phase-6-Session-2572-Completion.md](Phase-6-Session-2572-Completion.md).

## Session Summary

| UOW | Summary |
|-----|---------|
| 2568 | Group kinah-distribution decision planner + split messages. |
| 2569 | Live group kinah distribution (partyType 1, non-alliance). |
| 2570 | Live alliance kinah distribution (partyType 2 + sub-group). |
| 2571 | Quest-template share metadata (`CannotShare`, `Target`) → `NearbyQuestTemplateSummary`. |
| 2572 | `SM_QUEST_ACTION` SHARE variant (`SmQuestAction.Share`, ActionType 5) + golden test. |

## CM_QUEST_SHARE dependency status

| Dependency | Status |
|------------|--------|
| Quest-template share metadata (`cannot_share`, `target`) | Ported (UOW-2571) |
| `SM_QUEST_ACTION` SHARE packet | Ported (UOW-2572) |
| `QuestService.checkStartConditions` | Already ported (`NearbyQuestStartConditionService`) |
| Live handler wiring (group/range/messages fanout) | **Outstanding — UOW-2573** |

## What changed (UOW-2572)

Java source of truth: `SM_QUEST_ACTION(int questId, int sharerId, boolean shareInAlliance)` → `ActionType.SHARE`;
`writeImpl` SHARE branch writes `writeC(5); writeD(questId); writeD(sharerId); writeD(shareInAlliance ? 1 : 0)`
after the `extraCategory != NONE` early-return.

C# changes:
- `SmQuestAction`: added `ShareActionId = 5`, a `Share(questId, sharerId, shareInAlliance, suppressForExtraCategory)`
  factory, and an `_actionId` switch in `WritePayload`. Private ctor restructured (`_questId` + nullable
  `_questState` + `_sharerId` + `_shareInAlliance`); Add/Update remain byte-identical.
- `GamePacketTests`: 3 SHARE golden assertions (group, alliance, suppressed).

### Documented gaps (carried + new)

- CM_QUEST_SHARE still deferred — only the live handler (UOW-2573) remains.
- SHARE `suppressForExtraCategory` is caller-resolved (default false); confirm Java reachability when wiring.
- Carried: League partyType-3 distribution; canTrade gate; in-memory kinah persistence; legion-history live
  response; exchange + legion-rank DB reads unit-only; XP/level-up not ported.

## Validation Decision (UOW-2572)

- Changed surface: single C# server-packet shape (SM_QUEST_ACTION SHARE) + golden test.
- Specific behavior/contract: SHARE writes actionId=5, questId, sharerId, shareInAlliance(0|1) byte-for-byte;
  Add/Update unchanged; suppress ⇒ empty payload.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~GamePacketTests"` → 286/286.
- Focused Java/Maven command: none (manual deterministic golden from reviewed `writeImpl`).
- Broad-validation trigger: none.
- Broad .NET decision: skipped (filtered test built `Aion.GameServer` + test project).
- Why sufficient: golden pins SHARE bytes for both alliance values + the suppression path, and re-asserts the
  unchanged Update golden as a regression guard.

## Files Changed (UOW-2572)

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestAction.cs` — SHARE variant.
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` — 3 SHARE golden assertions.

## Migration Parity Table (UOW-2572)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `SM_QUEST_ACTION(int,int,boolean)` | `SmQuestAction.Share` | Packet | Complete | Golden File Tested | Verified Parity | group + alliance + suppressed pinned |
| `SM_QUEST_ACTION.writeImpl` SHARE case | `SmQuestAction.WritePayload` (ShareActionId) | Packet | Complete | Golden File Tested | Verified Parity | actionId 5; writeD(sharerId); writeD(0|1) |
| `ActionType.SHARE` | `SmQuestAction.ShareActionId` (5) | Const | Complete | Golden File Tested | Verified Parity | — |

## Summary Metrics (conservative)

- New automated coverage: 3 SHARE golden assertions (286 `GamePacketTests` pass).
- Two of three CM_QUEST_SHARE deps ported + verified; only live wiring (UOW-2573) remains.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- CM_QUEST_SHARE not yet live (handler wiring outstanding).
- SHARE `suppressForExtraCategory` caller-resolved; verify Java reachability.
- Carried risks as above.

## Next Recommended UOW

1. **UOW-2573: Wire CM_QUEST_SHARE live.** Full behavior + a verify-first dependency checklist are in
   [Phase-6-Session-2572-Completion.md](Phase-6-Session-2572-Completion.md) ("Next Recommended UOW" item 1).
   Summary: 1100001 cannot-share / null template; silent return on null/COMPLETE quest state; current-group ⋂
   ONLINE ⋂ in-range members; empty ⇒ 1100005 (Target==ALLIANCE) / 1100000; per member checkStartConditions ⇒
   1100003 fail or (`SmQuestAction.Share` + 1100002) success.
2. **Alt**: a deferred handler whose deps are ported — Work Discovery on `deferred` switch cases in
   `GameServerConnection.cs`.

### Focused validation recipe for UOW-2573

- Behavior/contract: CM_QUEST_SHARE emits 1100001 (cannot share / null template), returns silently on
  null/COMPLETE quest state, 1100000/1100005 on no eligible members (by Target), and per eligible member either
  1100003 or SM_QUEST_ACTION.SHARE + 1100002.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~<CmQuestShareDispatchTests>|FullyQualifiedName~GamePacketTests"` (new dispatch class + SHARE
  golden; narrow to the dispatch class once green).
- Java/Maven: not expected unless quest XML or a Java fixture changes.
- Broad-validation trigger: none unless the wire crosses shared group/world state in a way focused tests expose.

## Context Needed By Next Session

- CM_QUEST_SHARE deps ported: template metadata (`NearbyQuestTemplateSummary.CannotShare` / `.Target`) and the
  SHARE packet (`SmQuestAction.Share(questId, sharerId, shareInAlliance)`). Only live wiring remains.
- CM_QUEST_SHARE parser is ported (`CmQuestShare.cs`, exposes `QuestId`); dispatch is still `break;` in
  `GameServerConnection.cs` (`CmQuestShare` case).
- `QuestService.checkStartConditions` ⇒ `NearbyQuestStartConditionService.CheckNearbyStartConditions`.
- Group online-member enumeration + range helpers exist (`PlayerGroupRuntime`, `PositionUtilService`); confirm
  exact APIs + the GROUP_MAX_DISTANCE config value before wiring.
- System messages: verify 1100000/1100001/1100002/1100003/1100005 exist with a `%0` name param before the wire.
