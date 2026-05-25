# Phase 6 Decompose Real XML Candidate Audit

Date: May 25, 2026
Unit of Work: UOW-892
Status: Audit complete; no Java runtime artifact captured

## Purpose

Audit Java `decomposable_items.xml` and `item_templates.xml` for real selectable-decompose template ids that could drive the live-server capture scenarios without adding test-only Java static data.

Java remains the source of truth. This document does not prove parity. It identifies fixture candidates and explains why the current C# logical fixture still needs either a Java static-data override or a future comparison/projection enhancement before real Java XML counts can be compared cleanly.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java observer/runtime capture | `PacketSendUtility`, live Java server, artifact files | none | Runtime Validation | No | High | Requires Java 25/Maven/live runtime unavailable locally. |
| B | Real XML template-id audit | Java decomposable/item XML and loader classes | docs only | Java Analysis / Documentation | Yes | Low | Read-only source/static-data analysis that supports future live capture. |
| C | Real-count C# projection support | guarded C# comparison helper | test file | Test Infrastructure | No | Medium | Shared comparison helper; should follow this audit if real XML counts are used. |
| D | Non-decompose gameplay slice | isolated code/test files | TBD | Implementation | Maybe | Medium | Possible, but current handoff still points at decompose capture readiness. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Real Java XML selectable-decompose candidate audit | Java Analysis / Documentation | Java XML/source reads; `docs/Phase-6-Decompose-Real-XML-Candidate-Audit.md`; progress and handoff docs | Production Java/C# code changes | Capture contract, SQL fixture appendix, Java XML/static-data loader source | Candidate ids, deterministic-count findings, mapping guidance, and next implementation options |

No sub-agents were used because progress and handoff docs are shared documentation owned by the orchestrator for this unit.

## Source Anchors

- `game-server/data/static_data/decomposable_items/decomposable_items.xml`
- `game-server/data/static_data/decomposable_items/decomposable_items.xsd`
- `game-server/data/static_data/items/item_templates.xml`
- `com.aionemu.gameserver.dataholders.DecomposableItemsData`
- `com.aionemu.gameserver.model.templates.item.DecomposableItemInfo`
- `com.aionemu.gameserver.model.templates.item.ResultedItem`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE`

## Java Loader Findings

`DecomposableItemsData.afterUnmarshal` separates selectable entries from regular decomposable entries:

- if `decomposable.selectable == true`, Java stores `itemGroups.get(0).getItems()` in `selectableDecomposables`
- only the first `<items>` group is used for selectable rewards
- `getSelectableItems(itemId)` returns a new list copy

`ResultedItem.afterUnmarshal` normalizes and validates counts:

- missing `min_count` defaults to `1`
- missing `max_count` stays `0` during JAXB load, then becomes `min_count`
- `min_count <= 0` is invalid
- nonzero `max_count < min_count` is invalid

`ResultedItem.isObtainableFor(player)` filters by:

- `player_classes`, when present
- `race`, unless `PC_ALL`

First live-server candidates should avoid both `race` and `player_classes` attributes so reward availability is independent of the capture character.

## Audit Method

The audit inspected all Java XML entries matching:

```xpath
//decomposable[@selectable="true"]
```

Findings:

- Selectable entries found: `187`
- The audit looked for neutral deterministic rewards: no `race`, no `player_classes`, and normalized `min_count == max_count`.
- The audit specifically checked for any real selectable entry that could mirror the existing C# logical fixture exactly:
  - index `0` reward count `2`
  - index `1` reward count `3`
- Result: no selectable Java XML entry matched those exact first-two reward counts.

This matters because the current guarded C# comparison compares decoded reward `count` fields exactly. Real Java XML ids can be mapped through `fixture.id_mapping`, but real Java reward counts are not currently parameterized in the C# projection.

## Recommended Real XML Candidate

Best low-noise candidate found:

| Role | Java Template Id | Name | Source |
|---|---:|---|---|
| Selectable source | `188051516` | Smart Greater Scroll Bundle | `decomposable_items.xml` around line 11588; `item_templates.xml` line 909922 |
| Reward index 0 | `164000076` | Greater Running Scroll | `decomposable_items.xml` line 11590; `item_templates.xml` line 832793 |
| Reward index 1 | `164000073` | Greater Courage Scroll | `decomposable_items.xml` line 11591; `item_templates.xml` line 832775 |

XML excerpt:

```xml
<decomposable item_id="188051516" selectable="true"><!-- Smart Greater Scroll Bundle -->
  <items>
    <item id="164000076" min_count="100" /><!-- Greater Running Scroll -->
    <item id="164000073" min_count="100" /><!-- Greater Courage Scroll -->
    <item id="164000079" min_count="100" /><!-- Greater Raging Wind Scroll -->
    <item id="164000134" min_count="100" /><!-- Greater Awakening Scroll -->
    <item id="164000257" min_count="50" /><!-- Fine Strike Resist Scroll -->
    <item id="164000258" min_count="50" /><!-- Fine Spell Resist Scroll -->
    <item id="164000255" min_count="100" /><!-- Fine Crit Strike Scroll -->
    <item id="164000256" min_count="100" /><!-- Fine Crit Spell Scroll -->
  </items>
</decomposable>
```

Why this candidate is useful:

- selectable source exists in Java XML
- first two rewards are neutral: no race restriction and no class restriction
- counts are deterministic because `max_count` is omitted and Java normalizes it to `min_count`
- source and rewards are `race="PC_ALL"` in item templates
- source and first two rewards are stackable according to `max_stack_count`

Why this candidate does not directly match the current C# logical fixture:

- C# logical delete scenario expects reward index `0` count `2`; Java candidate gives `100`
- C# logical decrement scenario expects reward index `1` count `3`; Java candidate gives `100`
- A direct live Java artifact from this candidate will fail the current guarded comparison on reward counts unless C# projection input is parameterized or Java static data is overridden.

## Mapping Guidance

If this real XML candidate is used, future artifacts should map ids like this:

```json
{
  "logical_source_item_id": 101,
  "java_source_item_id": 188051516,
  "logical_reward_index_0": 201,
  "java_reward_index_0": 164000076,
  "logical_reward_index_1": 202,
  "java_reward_index_1": 164000073
}
```

Do not map Java reward counts through the broad numeric `id_mapping` normalizer. Counts are behavioral values, not ids. If real Java counts are used, add an explicit projection/capture fixture field for reward counts before comparing.

Recommended future fixture fields:

```json
{
  "logical_reward_index_0_count": 2,
  "java_reward_index_0_count": 100,
  "logical_reward_index_1_count": 3,
  "java_reward_index_1_count": 100
}
```

These count fields should not be consumed by the existing id-mapping normalizer. They should drive scenario-specific C# projection inputs or a narrowly scoped count comparison adapter.

## Alternative: Java Static-Data Override

The cleanest runtime comparison remains a dedicated Java capture static-data override that mirrors the existing C# logical fixture exactly:

- source item id `101`
- reward index `0`: item `201 x2`
- reward index `1`: item `202 x3`

This keeps the current C# projection unchanged and avoids count translation. The downside is that the Java capture environment must load test-only static data or a capture profile without polluting production Java data.

## Recommended Next Decision

Choose one path before generating artifacts:

| Path | Use When | Work Needed | Risk |
|---|---|---|---|
| Test static-data override | Java capture environment can safely load dedicated fixture XML | Add/enable fixture data for ids `101`, `201`, `202`; run observer capture | Lowest comparison ambiguity, but requires Java static-data setup |
| Real XML candidate with parameterized C# projection | Java live server should run unmodified static data | Extend C# capture projection to accept scenario source/reward ids and counts, then use `188051516`, `164000076`, `164000073` | Preserves Java data, but requires test-helper changes before artifacts pass |
| Real XML candidate with broad count mapping | Not recommended | Map `100 -> 2/3` through generic id mapping | Too risky; can mask unrelated numeric mismatches |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` / guarded comparison fixture | Data Holder | Partial | Manual Only for audit; Regression Tested in C# comparison helper | Needs Verification | Java loader source reviewed: selectable entries use only first `<items>` group and return copied reward lists. Real XML runtime loading was not executed locally. |
| `com.aionemu.gameserver.model.templates.item.DecomposableItemInfo` | `Aion.GameServer.Data.StaticData` decomposable item model | DTO / Static Data Model | Partial | Manual Only | Needs Verification | XML attributes `item_id` and `selectable` reviewed. C# static-data equivalent was not changed in this unit. |
| `com.aionemu.gameserver.model.templates.item.ResultedItem` | `Aion.GameServer.Data.StaticData` resulted-item model / decompose fixture projection | DTO / Static Data Model | Partial | Manual Only | Needs Verification | Java count normalization and race/class filtering reviewed. No runtime comparison; count behavior remains a future projection concern if real XML ids are used. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / guarded comparison helper | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for XML audit | Partial Parity | Audit selects possible real source/reward ids for future handler capture. No Java runtime artifact generated. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Static Data Audit | Java XML, Java XSD, `DecomposableItemsData`, `ResultedItem` source review | Identifies real selectable XML candidates and documents count mismatch with current C# logical fixture. | Static source/XML inspection only. | No Java XML loader execution, no runtime artifact, no C# projection change, no Java/C# comparison. |

## Remaining Risks

- Java runtime capture remains unavailable locally.
- The recommended real XML candidate has deterministic counts, but they differ from the current C# logical fixture.
- Item template existence was checked by source text, not Java runtime static-data loading.
- Real live-server behavior may still be affected by item use restrictions, inventory capacity, event systems, login rewards, or server configuration.
- Count parameterization must be carefully scoped; broad numeric normalization could hide real parity mismatches.
- Byte-level payload/frame parity, full item-info blob parity, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 0 production code artifacts; 1 real XML candidate audit document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, Java static-data override setup, C# real-count projection support, SQL fixture execution, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit improves capture planning but adds no runtime evidence.

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, choose between Java static-data override and real XML candidate capture, then execute the live-server runbook.

If tooling remains blocked, add narrowly scoped C# projection support for real Java selectable-decompose source/reward ids and counts so a future artifact using `188051516` can be compared without abusing id mapping.
