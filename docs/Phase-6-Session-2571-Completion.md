# Phase 6 Session 2571 Completion

## UOW

[Phase 6] UOW-2571: Port quest-template share metadata (`cannot_share`, `target`) into the nearby quest-template holder

## Status

Completed and validated with focused .NET tests (5/5 in the edited extractor test class, including the real-data
audit). This is the foundational, non-live, data-only prerequisite for wiring CM_QUEST_SHARE; the live handler is
deliberately deferred to a follow-up unit (see Next Recommended UOW).

## Work Discovery (UOW-2571 gate from Session 2570 handoff)

The 2570 handoff said to **verify first** whether a quest-template share-metadata loader is reachable before
attempting CM_QUEST_SHARE. Findings:

- The CM_QUEST_SHARE **parser** is already ported (`CmQuestShare.cs`); dispatch is deferred at
  `GameServerConnection.cs` (the `CmQuestShare` switch case).
- `QuestService.checkStartConditions` is already ported as
  `NearbyQuestStartConditionService.CheckNearbyStartConditions`, backed by `NearbyQuestTemplateTable` /
  `NearbyQuestTemplateSummary`.
- `SM_QUEST_ACTION` is ported (`SmQuestAction`) but **only** the `ADD`/`UPDATE` variants. The quest-share
  constructor `SM_QUEST_ACTION(questId, sharerId, shareInAlliance)` → `ActionType.SHARE` (wire:
  `writeC(5); writeD(questId); writeD(sharerId); writeD(shareInAlliance?1:0)`) is **not yet ported**.
- The two quest-template share fields (`QuestTemplate.cannotShare`, `QuestTemplate.target`) had **no** C#
  equivalent. The nearby quest-template holder (`NearbyQuestTemplateSummary`) is reachable and is the right home.

Decision: this unit adds the two share fields only (the explicitly-suggested "scope to adding the two share fields
to whatever quest holder is reachable" path). The live wire (new SHARE packet variant + group/range/conditions
fanout + 5 system messages + dispatch tests) is a separate, larger follow-up unit.

## Java Source Reviewed

- `com.aionemu.gameserver.model.templates.QuestTemplate`:
  - `@XmlAttribute(name = "cannot_share") private boolean cannotShare;` (default `false`) → `isCannotShare()`.
  - `@XmlAttribute(name = "target") private QuestTarget target = QuestTarget.NONE;` → `getTarget()`.
- `com.aionemu.gameserver.model.templates.quest.QuestTarget` enum: `NONE, AREA, LEAGUE, ALLIANCE`.
- `com.aionemu.gameserver.network.aion.clientpackets.CM_QUEST_SHARE.runImpl` (consumer of both fields — see
  Next Recommended UOW for the full behavior breakdown).
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION` (the `SHARE` constructor + wire format).

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs` — added `bool CannotShare = false`
  and `string Target = "NONE"` to `NearbyQuestTemplateSummary` (trailing optional record params; existing callers
  unaffected).
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs` — read `cannot_share`
  (`ReadBoolAttribute`) and `target` (`ReadStringAttribute`, default `"NONE"`).
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs` — field assertions in the
  explicit-fields test (`cannot_share="true" target="ALLIANCE"`), defaults test (`false` / `"NONE"`), and three
  real-data audit counts.

## Implementation Notes

- `cannot_share` maps to `ReadBoolAttribute` (matches the existing `can_report` boolean handling). The real data
  uses only `cannot_share="true"` (no `"false"` literals); absent ⇒ Java default `false`. `ReadBoolAttribute` uses
  `bool.TryParse`, which is sufficient because the XML uses the `true` literal (no `1`/`0` forms present).
- `target` is stored as the raw uppercase string (`NONE`/`AREA`/`LEAGUE`/`ALLIANCE`). This matches JAXB enum
  names exactly and keeps the holder a plain DTO; a future consumer can compare against `"ALLIANCE"` etc. or map
  to an enum if needed. Absent ⇒ `"NONE"` (Java default).
- No live behavior changed: the new fields are not yet read by any dispatch path. CM_QUEST_SHARE remains deferred.

## Validation Decision (UOW-2571)

```text
Validation decision:
- Changed surface: C# non-live dataholder (DTO + XML extractor) plus its extractor unit tests. No live dispatch,
  no shared runtime state, no packet primitive change.
- Specific behavior/contract: the nearby quest-template holder exposes QuestTemplate.cannotShare and
  QuestTemplate.target with Java defaults (false / NONE), and the real quest_data.xml parses to the exact Java
  attribute counts.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj
  --filter "FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests"  → 5/5 passed.
- Focused Java/Maven command: none. No matching narrow Java fixture exists; parity is asserted by the real-data
  audit counts derived directly from game-server/data/static_data/quest_data/quest_data.xml
  (cannot_share="true"=4876; target ALLIANCE=159, AREA=30, LEAGUE=59), which are the Java source-of-truth values.
- Broad-validation trigger: none. Non-live DTO/extractor change behind a filtered test.
- Broad .NET decision: skipped. The filtered test built Aion.GameServer + the test project (compile signal).
- Why this scope is sufficient: the extractor test exercises the new fields for explicit, default, and real-data
  cases; nothing reads the fields at runtime yet, so blast radius is limited to the holder.
```

## Migration Parity Table (UOW-2571)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestTemplate.cannotShare` / `isCannotShare()` | `NearbyQuestTemplateSummary.CannotShare` | DTO field | Complete | Unit Tested | Verified Parity | `cannot_share` attr; default false; real-data count 4876 matches XML |
| `QuestTemplate.target` / `getTarget()` | `NearbyQuestTemplateSummary.Target` | DTO field | Complete | Unit Tested | Verified Parity | `target` attr as raw string; default "NONE"; counts ALLIANCE=159/AREA=30/LEAGUE=59 match XML |
| `QuestTarget` (enum) | `string` ("NONE"/"AREA"/"LEAGUE"/"ALLIANCE") | Enum→string | Partial | Unit Tested | Intentional Difference | Held as raw uppercase string in the DTO; no enum type introduced (matches JAXB names) |

## Summary Metrics (conservative)

- New automated coverage: 3 real-data audit assertions + 2 explicit field/default assertions across the existing
  5-test extractor class (now exercising the two new fields).
- CM_QUEST_SHARE is now unblocked at the template-metadata layer; the live handler remains deferred (needs the
  SM_QUEST_ACTION SHARE variant + group/range fanout).
- Overall Phase 6 completion estimate: incremental; one of the three remaining CM_QUEST_SHARE dependencies (the
  share template metadata) is now ported and verified.

## Remaining Risks

- CM_QUEST_SHARE still deferred: needs (1) the `SM_QUEST_ACTION` SHARE variant packet (new wire format, golden
  test required) and (2) the live handler wiring (group resolution + range filter + `checkStartConditions` per
  member + 5 system messages). Tracked as UOW-2572 / UOW-2573 below.
- `Target` is a raw string, not an enum; a future consumer must compare against the uppercase literals. Documented
  as an Intentional Difference.
- Carried from 2570: League distribution (partyType 3) deferred; canTrade gate not modeled; kinah persistence
  in-memory; legion-history live response blocked on a data source; exchange + legion-rank DB reads unit-only.

## Next Recommended UOW

1. **UOW-2572: Port the `SM_QUEST_ACTION` SHARE variant** (`SM_QUEST_ACTION(questId, sharerId, shareInAlliance)`
   → `ActionType.SHARE`). Small, self-contained packet port with a golden test.
   - Java wire (after the shared `writeC(actionType.id); writeD(questId)` prefix, and the early-return when
     `QuestTemplate.extraCategory != NONE`): `case SHARE: writeD(sharerId); writeD(shareInAlliance ? 1 : 0)`.
   - C#: add a `Share(int questId, int sharerId, bool shareInAlliance)` factory to `SmQuestAction` (ActionType id
     5). Note: the existing `SmQuestAction` takes a `PlayerQuestState`; the SHARE variant needs only ids + a flag,
     so add a parallel construction path. The extra-category early-return uses the quest template; for SHARE the
     sharer already validated the template, so confirm how `extraCategory` is reachable (likely not gated for
     SHARE in practice — verify against Java before adding the guard).
2. **UOW-2573: Wire CM_QUEST_SHARE live** once UOW-2572 lands. Behavior from `CM_QUEST_SHARE.runImpl`:
   - `questTemplate == null || isCannotShare()` ⇒ SM_SYSTEM_MESSAGE **1100001** (cannot share), return.
   - `questState == null || status == COMPLETE` ⇒ return (no message).
   - Build share list from `player.getCurrentGroup()` (null ⇒ empty): members `allExcept(player) AND ONLINE AND
     PositionUtil.isInRange(member, player, GroupConfig.GROUP_MAX_DISTANCE)`.
   - Empty list ⇒ **1100005** if `target == ALLIANCE` else **1100000**, return.
   - Per member: `QuestService.checkStartConditions(member, questId, false)` (=
     `NearbyQuestStartConditionService.CheckNearbyStartConditions`) false ⇒ **1100003** `member.getName()` to
     sharer; true ⇒ SM_QUEST_ACTION.SHARE to member + **1100002** `member.getName()` to sharer.
   - Deps to confirm reachable first: group runtime online-member enumeration (`PlayerGroupRuntime`), range
     check (`PositionUtilService` + GROUP_MAX_DISTANCE config value), `member.isInAlliance()`, and the four
     system-message ids with a `%0` name param.
3. **UOW-2571-alt** (if quest-share stalls): a deferred handler whose deps are ported — Work Discovery on the
   `deferred` switch cases in `GameServerConnection.cs`.

### Focused validation recipe for UOW-2572 (SM_QUEST_ACTION SHARE variant)

- Behavior/contract: `SM_QUEST_ACTION.SHARE` writes `actionType=5, questId, sharerId, shareInAlliance(0|1)`
  byte-for-byte like Java `writeImpl` (post extra-category guard).
- Focused C# command:
  `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests"`
  (the SM_QUEST_ACTION golden lives in `GamePacketTests`; narrow further to the SHARE case if slow).
- Java/Maven: not expected unless a Java packet golden is regenerated.
- Broad-validation trigger: none (single server-packet shape).

## Context Needed By Next Session

- Quest-share template metadata is ported: `NearbyQuestTemplateSummary.CannotShare` (bool) and `.Target`
  (string: NONE/AREA/LEAGUE/ALLIANCE). Defaults false / "NONE".
- CM_QUEST_SHARE parser is ported (`CmQuestShare.cs`); dispatch is still a `break;` in `GameServerConnection.cs`.
- `QuestService.checkStartConditions` ⇒ `NearbyQuestStartConditionService.CheckNearbyStartConditions(player,
  questId, NearbyQuestTemplateTable, ...)` returning `NearbyQuestStartConditionResult.CanStart`.
- `SmQuestAction` has `Add`/`Update` only; the SHARE variant (ActionType 5) is the next packet to port.
- Group online-member enumeration + range helpers exist (`PlayerGroupRuntime`, `PositionUtilService`); confirm
  exact APIs before the UOW-2573 wire.
