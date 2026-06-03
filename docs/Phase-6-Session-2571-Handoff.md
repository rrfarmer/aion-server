# Phase 6 Session 2571 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2571: Port quest-template share metadata (`cannot_share`, `target`) into the nearby quest-template
holder. Foundational, non-live prerequisite for CM_QUEST_SHARE. See
[Phase-6-Session-2571-Completion.md](Phase-6-Session-2571-Completion.md) for full detail.

## Session Summary

| UOW | Summary |
|-----|---------|
| 2567 | Legion member rank loaded at enter-world. |
| 2568 | Group kinah-distribution decision planner + split messages. |
| 2569 | Live group kinah distribution (partyType 1, non-alliance). |
| 2570 | Live alliance kinah distribution (partyType 2 + partyType-1-in-alliance sub-group). |
| 2571 | Quest-template share metadata (`CannotShare`, `Target`) added to `NearbyQuestTemplateSummary` + extractor + tests. Unblocks CM_QUEST_SHARE at the data layer; live handler still deferred. |

## What changed (UOW-2571)

Java source of truth:
- `QuestTemplate.cannotShare` (`@XmlAttribute cannot_share`, default false) → `isCannotShare()`.
- `QuestTemplate.target` (`@XmlAttribute target`, `QuestTarget` enum NONE/AREA/LEAGUE/ALLIANCE, default NONE) →
  `getTarget()`.

C# changes:
- `NearbyQuestTemplateSummary` gained `bool CannotShare = false` and `string Target = "NONE"` (trailing optional
  record params).
- `NearbyQuestTemplateXmlExtractor` now reads `cannot_share` and `target`.
- Extractor tests: explicit-field, defaults, and 3 real-data audit counts (cannot_share=4876; ALLIANCE=159,
  AREA=30, LEAGUE=59).

### Documented gaps (carried + new)

- CM_QUEST_SHARE still deferred — needs the `SM_QUEST_ACTION` SHARE variant (UOW-2572) + live wiring (UOW-2573).
- `Target` held as raw uppercase string, not an enum (Intentional Difference).
- Carried: League distribution (partyType 3) deferred; canTrade gate not modeled; kinah persistence in-memory;
  legion-history live response blocked on a data source; exchange + legion-rank DB reads unit-only.

## Validation Decision (UOW-2571)

- Changed surface: C# non-live dataholder (DTO + XML extractor) + extractor unit tests.
- Specific behavior/contract: holder exposes `cannotShare`/`target` with Java defaults; real quest_data.xml parses
  to exact Java attribute counts.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests"` → 5/5.
- Focused Java/Maven command: none (no narrow Java fixture; parity asserted via real-data audit counts taken
  directly from quest_data.xml).
- Broad-validation trigger: none.
- Broad .NET decision: skipped (filtered test built `Aion.GameServer` + test project).
- Why sufficient: the new fields are exercised for explicit/default/real-data cases and are not read at runtime
  yet, so blast radius is limited to the holder.

## Files Changed (UOW-2571)

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs` — 2 new record fields.
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs` — read the 2 attributes.
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs` — field + audit assertions.

## Migration Parity Table (UOW-2571)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestTemplate.cannotShare` | `NearbyQuestTemplateSummary.CannotShare` | DTO field | Complete | Unit Tested | Verified Parity | `cannot_share` attr; default false; real-data count 4876 |
| `QuestTemplate.target` | `NearbyQuestTemplateSummary.Target` | DTO field | Complete | Unit Tested | Verified Parity | `target` attr as raw string; default "NONE"; ALLIANCE=159/AREA=30/LEAGUE=59 |
| `QuestTarget` (enum) | `string` literals | Enum→string | Partial | Unit Tested | Intentional Difference | Held as uppercase string; no enum type introduced |

## Summary Metrics (conservative)

- New automated coverage: 5 assertions across the existing 5-test extractor class now cover the two new fields.
- One of the three remaining CM_QUEST_SHARE dependencies (share template metadata) is ported + verified.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- CM_QUEST_SHARE not yet live (SHARE packet variant + handler wiring outstanding).
- `Target` is a raw string (Intentional Difference) — consumers compare against uppercase literals.
- Carried: League partyType-3 distribution; canTrade gate; in-memory kinah persistence; legion-history live
  response; exchange + legion-rank DB reads unit-only; XP/level-up not ported.

## Next Recommended UOW

1. **UOW-2572: Port the `SM_QUEST_ACTION` SHARE variant** (ActionType id 5).
   - Java wire (after `writeC(actionType.id); writeD(questId)` and the `extraCategory != NONE` early-return):
     `writeD(sharerId); writeD(shareInAlliance ? 1 : 0)`.
   - C#: add a `Share(int questId, int sharerId, bool shareInAlliance)` factory to `SmQuestAction`. The existing
     ctor takes a `PlayerQuestState`; SHARE needs only ids + a flag, so add a parallel path. Verify whether the
     `extraCategory != NONE` early-return is reachable/relevant for SHARE before adding the guard.
2. **UOW-2573: Wire CM_QUEST_SHARE live** (depends on UOW-2572). Full behavior breakdown is in the 2571 Completion
   doc ("Next Recommended UOW" item 2): 1100001 cannot-share; return on null/COMPLETE quest state; group +
   ONLINE + range filter; empty ⇒ 1100005 (ALLIANCE target) / 1100000; per-member checkStartConditions ⇒ 1100003
   fail / (SM_QUEST_ACTION.SHARE + 1100002) success.
3. **Alt** (if quest-share stalls): a deferred handler whose deps are ported — Work Discovery on the `deferred`
   switch cases in `GameServerConnection.cs`.

### Focused validation recipe for UOW-2572

- Behavior/contract: `SM_QUEST_ACTION.SHARE` writes `actionType=5, questId, sharerId, shareInAlliance(0|1)`
  byte-for-byte like Java `writeImpl`.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter
  "FullyQualifiedName~GamePacketTests"` (the SM_QUEST_ACTION golden lives there; narrow to the SHARE case if slow).
- Java/Maven: not expected unless a Java packet golden is regenerated.
- Broad-validation trigger: none (single server-packet shape).

## Context Needed By Next Session

- Quest-share template metadata is ported: `NearbyQuestTemplateSummary.CannotShare` (bool, default false) and
  `.Target` (string NONE/AREA/LEAGUE/ALLIANCE, default "NONE").
- CM_QUEST_SHARE parser is ported (`CmQuestShare.cs`); dispatch is still `break;` in `GameServerConnection.cs`
  (`CmQuestShare` case).
- `QuestService.checkStartConditions` ⇒ `NearbyQuestStartConditionService.CheckNearbyStartConditions`.
- `SmQuestAction` has `Add`/`Update` only; the SHARE variant (ActionType 5) is the next packet to port.
- Group online-member enumeration + range helpers exist (`PlayerGroupRuntime`, `PositionUtilService`); confirm
  exact APIs and the GROUP_MAX_DISTANCE config value before the UOW-2573 wire.
