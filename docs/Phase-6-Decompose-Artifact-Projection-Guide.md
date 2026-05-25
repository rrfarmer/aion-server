# Phase 6 Decompose Artifact Projection Guide

Date: May 25, 2026
Unit of Work: UOW-894
Status: Guide complete; no Java runtime artifact captured

## Purpose

Define how selectable-decompose Java artifacts should declare whether they use the simple logical fixture or the real Java XML candidate.

Java remains the source of truth. This guide does not prove parity. It prevents future artifacts from mixing item-id mapping, reward-count projection, and runtime evidence in a way that could hide differences.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java observer/runtime capture | `PacketSendUtility`, live Java server, artifact files | none | Runtime Validation | No | High | Still blocked locally by Java 8 and missing Maven. |
| B | Projection guide | capture contract, runbook, real XML audit | docs only | Documentation Update | Yes, orchestrator-owned | Low | Clarifies artifact shape after C# real-count projection support. |
| C | Non-decompose gameplay slice | TBD | isolated code/test files | Implementation | Maybe | Medium | Useful, but decompose handoff explicitly recommends this schema note while tooling is blocked. |
| D | Java static-data override profile | Java static data / runtime config | docs or Java fixture files | Design / Runtime Setup | Maybe | Medium | Needs Java-capable environment before execution. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Decompose artifact projection guide | Documentation Update | this guide; runbook link; progress and handoff docs | Production Java/C# code changes | UOW-892 real XML audit and UOW-893 C# projection support | Clear artifact-mode rules, JSON shape examples, comparison expectations, and stop conditions |

No sub-agents were used because progress and handoff docs are shared documentation owned by the orchestrator for this unit.

## Artifact Modes

Selectable-decompose Java artifacts must declare one of these modes in `fixture.projection.mode`.

| Mode | Use When | C# Projection | `fixture.id_mapping` | Reward Count Handling | Preferred For |
|---|---|---|---|---|---|
| `logical_static_override` | Java capture environment loads a test-only fixture matching C# ids/counts | default `101 -> 201 x2 / 202 x3` | optional, usually absent | exact comparison, no count mapping | First clean parity artifact if Java static-data override is available |
| `real_java_xml_candidate` | Java live server uses unmodified real XML source `188051516` | `SelectableDecomposeTestData.RealJavaXmlCandidate` | optional only for object ids or if logical labels are still included | exact comparison against `100` reward counts | First artifact from unmodified Java static data |
| `custom_java_fixture` | Java uses another deterministic selectable source | explicit future C# projection data required first | required for every non-default id | exact comparison against explicitly projected counts | Future broadened coverage |

Do not use a broad numeric mapping to translate reward counts. Counts are behavior, not identifiers.

## Logical Static-Override Artifact

Use this mode when Java is configured with test-only selectable static data:

| Role | Id | Count |
|---|---:|---:|
| Source | `101` | scenario-specific source count |
| Reward index 0 | `201` | `2` |
| Reward index 1 | `202` | `3` |

Required JSON fragment:

```json
{
  "fixture": {
    "projection": {
      "mode": "logical_static_override",
      "source": {
        "item_id": 101
      },
      "rewards": [
        {
          "index": 0,
          "item_id": 201,
          "count": 2
        },
        {
          "index": 1,
          "item_id": 202,
          "count": 3
        }
      ]
    }
  }
}
```

Comparison expectation:

- C# should call the default selectable-decompose projection.
- Reward ids and counts are compared exactly.
- Object ids may still need `fixture.id_mapping` if Java allocated different item object ids.

## Real Java XML Candidate Artifact

Use this mode when Java runs against the audited real XML candidate:

| Role | Id | Count | Java Source |
|---|---:|---:|---|
| Source | `188051516` | scenario-specific source count | `decomposable_items.xml`, Smart Greater Scroll Bundle |
| Reward index 0 | `164000076` | `100` | Greater Running Scroll |
| Reward index 1 | `164000073` | `100` | Greater Courage Scroll |

Required JSON fragment:

```json
{
  "fixture": {
    "projection": {
      "mode": "real_java_xml_candidate",
      "source": {
        "item_id": 188051516,
        "java_name": "Smart Greater Scroll Bundle"
      },
      "rewards": [
        {
          "index": 0,
          "item_id": 164000076,
          "count": 100,
          "java_name": "Greater Running Scroll"
        },
        {
          "index": 1,
          "item_id": 164000073,
          "count": 100,
          "java_name": "Greater Courage Scroll"
        }
      ]
    }
  }
}
```

Comparison expectation:

- C# should call `SelectableDecomposeTestData.RealJavaXmlCandidate`.
- Reward ids and counts are compared exactly as Java values.
- Do not include `logical_reward_index_0_count`, `java_reward_index_0_count`, or similar broad count translation fields unless a future comparison helper explicitly consumes them.
- If object ids differ, include only object-id mappings in `fixture.id_mapping`.

## `fixture.id_mapping` Rules

Allowed mappings:

- source item id when the artifact intentionally labels both logical and Java ids
- reward item ids when comparing a logical static-data artifact against non-default Java ids
- source object id
- reward object id

Disallowed mappings:

- reward counts
- packet sequence numbers
- opcode values
- update/add/delete masks
- message ids
- slot values
- cube item counts

Reason: the C# comparison helper normalizes numeric values by mapping table. Adding count-like fields to `fixture.id_mapping` can accidentally hide unrelated numeric parity gaps.

## Scenario Expectations

### `JD-SEL-DEC-001`

| Mode | Source Count | Select Index | Expected Reward Packet |
|---|---:|---:|---|
| `logical_static_override` | `2` | `1` | `SM_INVENTORY_ADD_ITEM` item `202`, count `3` |
| `real_java_xml_candidate` | `2` | `1` | `SM_INVENTORY_ADD_ITEM` item `164000073`, count `100` |

### `JD-SEL-DEL-001`

| Mode | Source Count | Select Index | Expected Reward Packet |
|---|---:|---:|---|
| `logical_static_override` | `1` | `0` | `SM_INVENTORY_ADD_ITEM` item `201`, count `2` |
| `real_java_xml_candidate` | `1` | `0` | `SM_INVENTORY_ADD_ITEM` item `164000076`, count `100` |

## Stop Conditions

Stop and document the artifact as unusable for parity comparison if:

- `fixture.projection.mode` is missing or ambiguous.
- Reward counts require broad numeric normalization to pass.
- Java emits a selectable reward from a different index than the requested `client_packet.payload_fields.index`.
- Java emits a random reward count because `min_count != max_count`.
- Java filters out the reward because of race/class restrictions.
- Packet order includes a reward-add trailing `SM_CUBE_UPDATE`; keep the artifact, but expect the UOW-891 C# diagnostic to report a parity gap.
- Unrelated login/event packets appear in the captured packet list.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `game-server/data/static_data/decomposable_items/decomposable_items.xml` | `docs/Phase-6-Decompose-Artifact-Projection-Guide.md` / `SelectableDecomposeTestData.RealJavaXmlCandidate` | Static Data / Fixture Contract | Partial | Manual Only for guide; Unit Tested in C# projection | Needs Verification | Guide documents exact real-candidate ids/counts and forbids count normalization through id mapping. Java runtime static-data loading remains unverified. |
| `game-server/data/static_data/items/item_templates.xml` | `docs/Phase-6-Decompose-Artifact-Projection-Guide.md` / generated C# fixture templates | Static Data / Item Template Contract | Partial | Manual Only for guide; Unit Tested in C# projection | Needs Verification | Guide records names and ids needed by future artifacts. Full template attributes and serialization effects remain unverified. |
| `com.aionemu.gameserver.model.templates.item.ResultedItem` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests.SelectableDecomposeTestData` | DTO / Static Data Model | Partial | Manual Only for guide; Unit Tested in C# projection | Needs Verification | Guide relies on source-reviewed Java `max_count` normalization to explain `x100` rewards. Runtime JAXB behavior, random ranges, and race/class filtering remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / artifact comparison contract | Client Packet Handler | Partial | Manual Only for guide; Regression Tested in C# projection | Partial Parity | Guide defines artifact projection modes for the existing two selectable scenarios. No live Java handler artifact exists. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Contract Guide | Java XML/source audit and C# UOW-893 projection test | Documents artifact projection mode rules and count-mapping stop conditions. | Static source review plus previously passing C# projection tests. | No new runtime validation; no Java artifact; no packet byte comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The guide is only as useful as future artifact authors following `fixture.projection.mode`.
- Real XML candidate behavior may still be affected by live item restrictions, inventory capacity, event systems, server config, or packet side effects.
- Full item-info blob serialization, byte-level payload/frame parity, object-id allocation, persistence, threading, date/time, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 0 production code artifacts; 1 artifact projection guide added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, Java static-data override setup, SQL fixture execution, reward-add trailing cube update implementation decision, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, generate selectable-decompose artifacts using this guide and run the guarded C# comparison.

If tooling remains blocked, continue an isolated non-decompose Phase 6 gameplay slice that avoids shared decompose comparison helpers and progress docs until commit time.
