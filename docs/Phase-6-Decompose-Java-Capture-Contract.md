# Phase 6 Decompose Java Capture Contract

Date: May 25, 2026
Unit of Work: UOW-877
Status: Contract complete; Java capture artifact not implemented

## Purpose

Define the fixture contract and output artifact schema for Java runtime capture of selectable decompose packet order. This document is the bridge between the feasibility audit and any future Java loopback/live-server capture work.

Java remains the source of truth. This contract does not prove parity by itself.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Capture contract/schema | `CM_SELECT_DECOMPOSABLE`, `PacketSendUtility`, `Storage`, `ItemService`, `ItemPacketService` | docs only | Documentation Update | Yes, orchestrator-owned | Low | No production files; output is a shared contract doc. |
| B | Java loopback socket design | `AionConnection`, dispatcher classes, packet factory | docs only / read-only Java | Java Analysis | Yes | Medium | Can proceed after contract, but implementation path remains broad. |
| C | Live-server runbook | Java launch/config/DB fixture | docs only | Documentation Update | Yes | Medium | Useful fallback, but depends on fixture values defined here. |
| D | C# comparison schema | C# decompose tests and possible fixture data | docs/test data later | Parity Verification | Maybe later | Medium | Should wait until Java artifact schema is stable. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Decompose Java capture fixture contract | Documentation Update | `docs/Phase-6-Decompose-Java-Capture-Contract.md`; progress and handoff docs | Production Java/C# code changes | UOW-876 feasibility audit and existing C# selectable-decompose tests | Scenario ids, fixture values, artifact schema, pass/fail criteria, and implementation checkpoint |

No sub-agents were used because the write targets are shared documentation and progress/handoff files.

## Capture Level Definitions

| Level | Name | Required For First Java Artifact? | Description |
|---|---|---|---|
| 0 | Source Reviewed | Yes | Java source path and method sequence documented. No runtime output. |
| 1 | Packet Class Order | Yes | Ordered Java server packet class names and recipient ids. |
| 2 | Decoded Fields | Yes | Important packet fields decoded into stable JSON values. |
| 3 | Unencrypted Body Bytes | Optional for first artifact | Serialized packet payload/body bytes before `Crypt.encrypt`, if accessible without broad Java changes. |
| 4 | Encrypted Frame Bytes | Deferred | Full framed/encrypted bytes, only after deterministic Java `SM_KEY`/`Crypt` path is controlled. |

The first acceptable Java capture artifact must reach Level 2 for both selectable scenarios. Level 3 and Level 4 are valuable but not required to unblock C# packet-order comparison.

## Scenario Matrix

| Scenario Id | Name | Java Handler | Client Opcode | Source Count | Select Index | Expected Reward | Capture Levels | Mirrors Existing C# Test |
|---|---|---|---|---:|---:|---|---|---|
| `JD-SEL-DEC-001` | Selectable decompose source decrement | `CM_SELECT_DECOMPOSABLE.runImpl` | `236` | 2 | 1 | item `202 x3` | 0, 1, 2 | `HandleSelectDecomposableAsync_SelectableRewardConsumesSourceAndAddsReward` |
| `JD-SEL-DEL-001` | Selectable decompose source delete | `CM_SELECT_DECOMPOSABLE.runImpl` | `236` | 1 | 0 | item `201 x2` | 0, 1, 2 | `HandleSelectDecomposableAsync_SelectableRewardDeletesSingleCountSource` |

Deferred scenarios:

- normal scheduled `CM_USE_ITEM` decompose decrement
- normal scheduled `CM_USE_ITEM` decompose delete
- selectable invalid index no-op
- selectable reward stack merge
- selectable full-cube overflow behavior

## Shared Fixture Values

| Field | Value | Notes |
|---|---:|---|
| `java_revision` | current repo commit at capture time | Record exact Git SHA in each artifact. |
| `player.object_id` | `1001` | Must match existing C# test expectations where practical. |
| `player.name` | `CapturePlayer` | Exact name only matters for messages if serialized. |
| `player.level` | `10` or higher | Reward obtainability should not be level-blocked. |
| `player.race` | `ELYOS` | Selectable fixture rewards should be race-neutral; still record the value. |
| `source.object_id` | `5001` | Matches C# selectable tests. |
| `source.item_id` | `101` | Test-only/simple selectable source id used by C# fixtures. If Java static XML cannot supply this id, record the actual fixture id and map it explicitly. |
| `source.location` | cube storage | Java should route through `StorageType.CUBE`. |
| `select.unknown_dword` | `0` | Java reads and ignores it. |
| `known_list.visible_players` | empty | Ensures `broadcastPacketAndReceive` sends only to self. |
| `reward.add_type` | `DECOMPOSABLE` | Java `ItemPacketService.ItemAddType.DECOMPOSABLE`, mask `0x50`. |
| `reward.update_type` | `INC_ITEM_COLLECT` | Java `ItemPacketService.ItemUpdateType.INC_ITEM_COLLECT`, mask `0x19`. |

## Static Data Fixture

Preferred fixture data should use test-only static-data loading or a dedicated capture profile. If the Java runtime must use real XML ids instead of the simple C# ids, the artifact must include an explicit `id_mapping` section.

Required item templates:

| Logical Id | Template Id | Stackable | Max Stack | Extra Inventory | Name | Notes |
|---|---:|---|---:|---:|---|---|
| `selectable_source` | `101` | true | at least 2 | 0 | `Selectable Source` | Source item being decomposed. |
| `reward_index_0` | `201` | true | at least 2 | 0 | `Reward 201` | Used by delete scenario. |
| `reward_index_1` | `202` | true | at least 3 | 0 | `Reward 202` | Used by decrement scenario. |

Required decomposable data:

```json
{
  "item_id": 101,
  "selectable": true,
  "rewards": [
    {
      "index": 0,
      "item_id": 201,
      "min_count": 2,
      "max_count": 2,
      "obtainable_for_player": true
    },
    {
      "index": 1,
      "item_id": 202,
      "min_count": 3,
      "max_count": 3,
      "obtainable_for_player": true
    }
  ]
}
```

Determinism rule:

- `min_count` must equal `max_count` for first captures.
- No class/race/restriction gate should block either reward.
- If Java real XML data is used, choose real rewards that satisfy the same deterministic rule, then record the real ids/counts.

## Scenario Inputs

### JD-SEL-DEC-001

```json
{
  "scenario_id": "JD-SEL-DEC-001",
  "client_packet": {
    "class": "CM_SELECT_DECOMPOSABLE",
    "opcode": 236,
    "payload_fields": {
      "object_id": 5001,
      "unknown_dword": 0,
      "index": 1
    }
  },
  "initial_inventory": [
    {
      "object_id": 5001,
      "item_id": 101,
      "count": 2,
      "location": "CUBE",
      "equipped": false
    }
  ],
  "expected_inventory": [
    {
      "object_id": 5001,
      "item_id": 101,
      "count": 1,
      "location": "CUBE"
    },
    {
      "item_id": 202,
      "count": 3,
      "location": "CUBE"
    }
  ]
}
```

Expected Java packet class order:

1. `SM_ITEM_USAGE_ANIMATION`
2. `SM_SYSTEM_MESSAGE`
3. `SM_INVENTORY_UPDATE_ITEM`
4. `SM_SECONDARY_SHOW_DECOMPOSABLE`
5. `SM_INVENTORY_ADD_ITEM`

### JD-SEL-DEL-001

```json
{
  "scenario_id": "JD-SEL-DEL-001",
  "client_packet": {
    "class": "CM_SELECT_DECOMPOSABLE",
    "opcode": 236,
    "payload_fields": {
      "object_id": 5001,
      "unknown_dword": 0,
      "index": 0
    }
  },
  "initial_inventory": [
    {
      "object_id": 5001,
      "item_id": 101,
      "count": 1,
      "location": "CUBE",
      "equipped": false
    }
  ],
  "expected_inventory": [
    {
      "item_id": 201,
      "count": 2,
      "location": "CUBE"
    }
  ]
}
```

Expected Java packet class order:

1. `SM_ITEM_USAGE_ANIMATION`
2. `SM_SYSTEM_MESSAGE`
3. `SM_DELETE_ITEM`
4. `SM_CUBE_UPDATE`
5. `SM_SECONDARY_SHOW_DECOMPOSABLE`
6. `SM_INVENTORY_ADD_ITEM`

Note: the C# focused handler test currently asserts `SM_DELETE_ITEM` and `SM_INVENTORY_ADD_ITEM`; Java `ItemPacketService.sendItemDeletePacket` also sends `SM_CUBE_UPDATE` after delete. If the C# socket/observer path does not surface that packet, treat this as a parity gap to investigate, not as verified behavior.

## Decoded Packet Field Requirements

### SM_ITEM_USAGE_ANIMATION

Required fields:

- recipient object id
- player object id
- target player object id if present in serializer
- item object id
- item id
- time
- end
- remaining trailing bytes/unknown fields as hex or numeric list

Expected first-capture values:

| Scenario | player_object_id | item_object_id | item_id | time | end |
|---|---:|---:|---:|---:|---:|
| `JD-SEL-DEC-001` | 1001 | 5001 | 101 | 0 | source-reviewed Java constructor default |
| `JD-SEL-DEL-001` | 1001 | 5001 | 101 | 0 | source-reviewed Java constructor default |

### SM_SYSTEM_MESSAGE

Required fields:

- message id or enum/factory name
- parameters
- chat type or message category if encoded

Expected:

- factory: `SM_SYSTEM_MESSAGE.STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`
- first parameter: source item l10n/name used by Java runtime

### Source Mutation Packet

For `JD-SEL-DEC-001`:

- packet class: `SM_INVENTORY_UPDATE_ITEM`
- object id: `5001`
- item id: `101`
- count: `1`
- update type: `DEC_ITEM_USE`, mask `0x16`

For `JD-SEL-DEL-001`:

- packet class: `SM_DELETE_ITEM`
- object id: `5001`
- delete type: `USE`, mask `0x17`
- follow-up packet: `SM_CUBE_UPDATE` if Java emits it through `ItemPacketService.sendItemDeletePacket`

### SM_SECONDARY_SHOW_DECOMPOSABLE

Required fields:

- source object id: `5001`
- reward count/list size: `0`
- raw trailing bytes if any

### Reward Mutation Packet

For a new stack:

- packet class: `SM_INVENTORY_ADD_ITEM`
- add type: `DECOMPOSABLE`, mask `0x50`
- reward item id/count
- generated reward object id

For a future merge scenario:

- packet class: `SM_INVENTORY_UPDATE_ITEM`
- update type: `INC_ITEM_COLLECT`, mask `0x19`
- existing object id and new count

## Artifact Schema

First Java capture output should be JSON. Store future artifacts under a stable path such as:

```text
docs/parity-artifacts/java/decompose/selectable/<scenario-id>.json
```

Schema:

```json
{
  "schema_version": 1,
  "scenario_id": "JD-SEL-DEC-001",
  "capture_date": "YYYY-MM-DD",
  "java_revision": "git-sha",
  "capture_method": "java-loopback-socket | live-java-server | reflected-in-process | source-only",
  "capture_levels": [0, 1, 2],
  "fixture": {
    "player": {
      "object_id": 1001,
      "name": "CapturePlayer",
      "level": 10,
      "race": "ELYOS"
    },
    "known_list": {
      "visible_players": []
    },
    "initial_inventory": [],
    "static_data": {
      "item_templates": [],
      "decomposable": {}
    },
    "id_mapping": {}
  },
  "client_packet": {
    "class": "CM_SELECT_DECOMPOSABLE",
    "opcode": 236,
    "payload_fields": {}
  },
  "packets": [
    {
      "sequence": 1,
      "recipient_object_id": 1001,
      "java_class": "SM_ITEM_USAGE_ANIMATION",
      "decoded_fields": {},
      "unencrypted_body_hex": null,
      "encrypted_frame_hex": null,
      "notes": []
    }
  ],
  "final_inventory": [],
  "unsupported": [
    "encrypted frame bytes not captured"
  ],
  "risks": []
}
```

Rules:

- `capture_method` must not be `source-only` for artifacts used as runtime evidence.
- `packets.sequence` must be 1-based and represent observed send order.
- Unknown fields should be kept as numeric values or hex, not discarded.
- If packet bytes are unavailable, set byte fields to `null` and explain why in `unsupported`.
- If real Java XML ids differ from the logical test ids, include an `id_mapping` object and do not silently rewrite the artifact.

## Pass/Fail Criteria

A Java capture is acceptable for first comparison only when all of these are true:

- The artifact records exact Java Git revision.
- The artifact records the capture method.
- Both selectable scenarios reach capture Levels 1 and 2.
- The observed packet class sequence matches the expected Java sequence, or differences are explained with Java source/runtime evidence.
- Reward count is deterministic.
- Known-list fanout is controlled and shows only self-send for the first scenarios.
- Source decrement/delete side effects are captured through Java item packet services, not inferred.
- Missing byte-level capture is explicitly listed as unsupported.

Do not mark C# parity as verified from these artifacts alone unless C# tests compare the captured Java artifact to C# output and pass deterministically.

## Implementation Checkpoint

Before implementing any Java harness, choose one path:

| Path | Choose When | First Implementation Target | Stop Conditions |
|---|---|---|---|
| Java loopback socket capture | Java server networking can be bootstrapped with deterministic player/static-data fixture in a test profile | Level 1/2 artifact for `JD-SEL-DEC-001` | Requires production Java changes, global DB writes cannot be isolated, or startup noise cannot be filtered. |
| Live Java server capture | Existing Java server plus DB fixture is easier than in-process bootstrapping | Level 1/2 artifact for both selectable scenarios | Fixture inventory cannot be set deterministically, unrelated traffic pollutes capture, or reward RNG cannot be controlled. |
| Reflected in-process capture | A small proof shows stable private `SelectionKey`/queue setup and scheduler cleanup | Level 1 artifact only | Reflection becomes broader than documented fields, heartbeat/scheduler leaks, or packet serialization needs uncontrolled crypt state. |

Recommended next implementation direction:

Start with Java loopback socket capture design notes, but keep live-server capture as the fallback. Avoid reflected in-process capture unless a very small proof proves it stable.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for contract | Partial Parity | Contract defines two Java runtime capture scenarios for selectable decrement/delete but no Java artifact has been generated. Runtime comparison remains missing. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection.BroadcastItemUsageAnimationAsync` / send helpers | Utility | Partial | Manual Only | Needs Verification | Contract requires empty known-list self-send capture because Java `broadcastPacketAndReceive` sends self first. Broadcast fanout parity remains unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Manual Only | Needs Verification | Contract requires Java source decrement and delete side effects to be captured through item packet services. Persistence, quest callback, and delete/update packet ordering remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers | Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Contract explicitly expects `SM_CUBE_UPDATE` after Java delete because `sendItemDeletePacket` sends it. Existing C# observer coverage may not surface that packet; this is a parity risk to investigate. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.Items.InventoryAddService` / item services | Service | Partial | Manual Only | Needs Verification | Contract fixes reward counts and add type for deterministic Java capture. Runtime reward-add packet bytes and ID allocation remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` decomposable item data | Data Holder | Partial | Manual Only | Needs Verification | Contract defines minimal selectable static-data fixture and requires explicit id mapping if real Java XML ids differ. XML load/default parity remains unverified. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Contract | Java source review plus existing C# selectable-decompose regression tests | Defines Java capture fixture values, scenario ids, packet fields, artifact schema, and pass/fail criteria. | No runtime evidence; planning only. | No Java capture artifact, no C# comparison test against Java artifact, no byte vectors, no live-client validation. |

## Remaining Risks

- Java runtime capture remains unimplemented.
- The C# selectable delete observer tests may be missing Java's `SM_CUBE_UPDATE` packet after `SM_DELETE_ITEM`; this needs comparison evidence.
- If Java real XML ids must be used, the simple fixture ids (`101`, `201`, `202`) need explicit mapping.
- DB/DAO and `IDFactory` setup may still push first capture toward live-server fixture.
- Level 3/4 byte capture remains deferred until Java connection/crypt state is controlled.
- Threading and packet-processor ordering remain unverified for any implementation path.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 0 code artifacts; 1 capture contract document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, C# artifact comparison tests, delete-path `SM_CUBE_UPDATE` parity, deterministic Java static-data fixture, Java ID allocation fixture, unencrypted byte capture, and encrypted frame capture
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit sharpens the runtime comparison contract but does not add runtime evidence.
