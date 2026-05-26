# Quest Bonus Static Data Bridge Audit

Date: May 26, 2026

## Scope

This audit reviews how Java loads `ItemGroupsData` and how the current C# static-data loader could expose `QuestBonusItemGroupTable` without enabling production quest-finish bonus execution.

No C# loader, `StaticData`, `DataManager`, quest-finish call site, RNG, live reward mutation, packet send, persistence, or rollback behavior is changed by this audit.

## Java Source Findings

| Java Artifact | Finding |
|---|---|
| `game-server/data/static_data/static_data.xml` | Imports `items/item_groups.xml` as part of the main static-data import graph. |
| `game-server/data/static_data/static_data.xsd` | Includes `items/item_groups.xsd` and allows top-level `<item_groups>`. |
| `com.aionemu.gameserver.dataholders.StaticData` | Has `@XmlElement(name = "item_groups") public ItemGroupsData itemGroupsData;`. |
| `com.aionemu.gameserver.dataholders.DataManager` | Assigns `ITEM_GROUPS_DATA = data.itemGroupsData;` during static-data bootstrap. |
| `com.aionemu.gameserver.dataholders.ItemGroupsData.afterUnmarshal` | Builds ordered lists for craft, manastone, medal, food, medicine, gather, enchant, event, and boss groups. It also builds pet-food lookup sets and clears temporary food item lists. |
| `com.aionemu.gameserver.services.reward.BonusService.getBonusGroups` | Uses only `EVENTS`, `FOOD`, `MANASTONE`, `MEDICINE`, `MEDAL`, and `TASK` for quest bonuses. `GATHER`, `BOSS`, and `ENCHANT` branches are commented out; `MOVIE` and `NONE` return an empty list; unknown types warn and return empty. |

## C# Source Findings

| C# Artifact | Finding |
|---|---|
| `Aion.GameServer.Dataholders.DataManager` | Loads `StaticData` through `XmlDataLoader.LoadStaticDataAsync`; it exposes one `StaticData` instance, not Java-style static fields. |
| `Aion.GameServer.Dataholders.LoadingUtils.XmlMerger` / `XmlDataLoader` | Merges `static_data.xml` imports into the cache XML before `StaticData.LoadFromCacheAsync` parses it. This means imported `items/item_groups.xml` is already present in the flattened cache. |
| `Aion.GameServer.Dataholders.StaticData.LoadFromCacheAsync` | Uses a single streaming `XmlReader` pass over the flattened cache and manually collects supported tables. It currently counts/top-level tracks `<item_groups>` but does not project its child groups into a typed table. |
| `Aion.GameServer.Services.QuestBonusItemGroupXmlProjectionExtractor` | Already knows how to project Java-live quest bonus group families from an XML document or stream, but it currently expects an `XDocument`/stream and is not integrated into the streaming `StaticData` parse. |
| `Aion.GameServer.Services.QuestBonusItemGroupTable` | Wraps supported quest bonus projections, preserves projected order, indexes by normalized bonus type, and has real-data count coverage. It is not exposed from `StaticData`. |
| `Aion.GameServer.Services.QuestBonusRewardPlanningInputAdapterService` | Still requires caller-supplied `IReadOnlyList<QuestBonusItemGroupProjection>`; it does not read `DataManager` or `StaticData`. |

## Candidate Bridge

The safest bridge is a narrow `StaticData` property, not production quest-finish wiring:

1. Add `QuestBonusItemGroupTable QuestBonusItemGroups` to `StaticData`.
2. During `StaticData.LoadFromCacheAsync`, collect supported quest bonus groups while the existing streaming reader is inside top-level `<item_groups>`.
3. Reuse the same projection semantics as `QuestBonusItemGroupXmlProjectionExtractor`: supported group names only, raw item attributes only, no RNG and no live reward behavior.
4. Pass `new QuestBonusItemGroupTable(collectedGroups.AsReadOnly())` into the `StaticData` constructor.
5. Add focused loader tests that verify:
   - real static-data load exposes 12 supported quest bonus groups;
   - real load exposes 4,701 supported items;
   - `TASK`, `MANASTONE`, `MEDAL`, `FOOD`, `MEDICINE`, and `EVENTS` counts match current extractor tests;
   - `BOSS`, `GATHER`, and `ENCHANT` are not exposed through the quest bonus table.

This bridge should keep `QuestBonusRewardPlanningInputAdapterService` explicit for now. A later unit can choose whether a caller passes `staticData.QuestBonusItemGroups.Groups` into the adapter.

## Why Not Broad `ItemGroupsData` Yet

Java `ItemGroupsData` also owns pet-food lookups and broader group families. The current quest bonus work only needs the Java-live `BonusService` group families. A broad port would need additional artifacts:

- feed/pet-food group modeling;
- `FoodType` parity and `isFood` behavior;
- gather/enchant/boss group consumers;
- JAXB after-unmarshal side effects, including clearing temporary food lists;
- broader validation around unsupported consumers.

Those are outside the current quest bonus reward-planning unit and should not be pulled in just to supply item groups to the disabled bonus adapter.

## Remaining Risks

- A streaming parser implementation must preserve current static-data load performance and avoid changing existing table counts.
- `QuestBonusItemGroupXmlProjectionExtractor` currently uses `XDocument`; duplicating projection logic in `StaticData` risks drift unless shared carefully.
- `item_groups.xsd` validation still runs only through the existing cache validation path when enabled; direct table construction will not prove JAXB-equivalent invalid XML behavior.
- Java `ItemGroupsData.afterUnmarshal` creates ordered lists with possible null entries if XML omits a group; current real data has all supported groups, but missing-group behavior is not runtime-compared.
- Pet-food and non-quest item-group consumers remain unported and should not be represented by the quest bonus table.
- Production quest-finish adapter invocation, dynamic handler dispatch, Java RNG/Chance behavior, selected `QuestItems`, random count rolls, live item mutation, packet sends, persistence, rollback, and Java runtime comparison remain disabled.

## Next Recommended Unit Of Work

Add the narrow `StaticData.QuestBonusItemGroups` bridge with focused loader tests. Keep `QuestBonusRewardPlanningInputAdapterService` explicit and keep production quest-finish invocation disabled.
