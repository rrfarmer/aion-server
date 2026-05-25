# Phase 6 Decompose Java Packet Observer Design

Date: May 25, 2026
Unit of Work: UOW-888
Status: Design complete; Java observer not implemented

## Purpose

Define a small, temporary Java packet observer for producing Level 2 selectable-decompose artifacts under `docs/parity-artifacts/java/decompose/selectable/`.

Java remains the source of truth. This document does not prove parity by itself. It is an implementation guide for a Java-capable environment that can run the live server or loopback proof and emit packet order plus decoded fields without relying on manual transcription.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java proof/runbook execution | `LoopbackCaptureProof`, `AionConnection`, `Crypt` | none | Runtime Validation | No | High | Requires Java 25/Maven and a controlled runtime environment unavailable locally. |
| B | Java packet-observer design | `PacketSendUtility`, `AionConnection`, decompose server packets | docs only | Documentation Update | Yes | Low | Settles the diagnostic observation shape without production C# or Java changes. |
| C | Object-id mapping support | Java artifacts and guarded C# comparison helper | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Infrastructure | No | Medium | Shared comparison helper should be edited sequentially and depends on final artifact object-id shape. |
| D | SQL fixture appendix | `Storage`, `ItemService`, fixture DB paths | docs only | Documentation Update | Yes | Low-Medium | Useful supporting work, but less urgent than defining how packet fields will be observed. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Java packet-observer design notes | Documentation Update | `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`; progress and handoff docs | Production Java/C# changes | Java capture contract, live-server runbook, loopback design, Java packet source review | Observer insertion point, decoded field extractors, artifact schema additions, stop conditions, and parity risks |

No sub-agents were used because the progress and handoff files are shared documentation owned by the orchestrator for this unit.

## Source Anchors

- `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE`
- `com.aionemu.gameserver.utils.PacketSendUtility`
- `com.aionemu.gameserver.network.aion.AionConnection`
- `com.aionemu.gameserver.network.aion.AionServerPacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.model.items.storage.Storage`

## Observed Java Send Path

`CM_SELECT_DECOMPOSABLE.runImpl` reads:

- `objectId = readD()`
- `unk = readD()`
- `index = readUC()`

The handler then:

1. resolves the active player and source item
2. fetches selectable rewards from `DataManager.DECOMPOSABLE_ITEMS_DATA`
3. filters rewards by `ResultedItem.isObtainableFor(player)`
4. returns without packets if the selectable list is absent or the index is out of range
5. calls `PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), objectId, item.getItemId()))`
6. calls `PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED(item.getL10n()))`
7. calls `player.getInventory().decreaseByObjectId(objectId, 1)`
8. sends `new SM_SECONDARY_SHOW_DECOMPOSABLE(objectId, Collections.emptyList())`
9. creates the reward count with `Rnd.get(minCount, maxCount)`
10. calls `ItemService.addItem(..., new ItemUpdatePredicate(ItemAddType.DECOMPOSABLE, ItemUpdateType.INC_ITEM_COLLECT))`

`PacketSendUtility.broadcastPacketAndReceive(VisibleObject, AionServerPacket)` sends to the player first when the visible object is a `Player`, then broadcasts to known players. First captures must keep known-list fanout empty so the observed packet order is self-send only.

`ItemPacketService.sendItemDeletePacket` sends `SM_DELETE_ITEM` for cube items, then always sends `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

`ItemPacketService.sendStorageUpdatePacket` sends `SM_INVENTORY_ADD_ITEM` for new cube items, then sends `SM_CUBE_UPDATE.cubeSize(storageType, player)`. The current first-scenario comparison only expects the reward add packet, so a Java observer must explicitly record whether this trailing cube update is emitted during the selectable reward add. If it is emitted, treat the C# expectation as a parity gap to investigate, not a Java deviation.

## Recommended Observer Insertion

Use a temporary Java-only diagnostic hook at `PacketSendUtility.sendPacket(Player, AionServerPacket)`.

Recommended shape:

```java
public static void sendPacket(Player player, AionServerPacket packet) {
	if (player.isOnline()) {
		DecomposeCaptureObserver.observe(player, packet);
		player.getClientConnection().sendPacket(packet);
	}
}
```

The observer must be disabled by default and enabled only by an explicit capture flag, for example `-Daion.capture.decompose.enabled=true`.

Why this hook:

- It preserves the real Java handler, storage, item service, packet service, and connection send path.
- It has the recipient `Player`, which `AionConnection.sendPacket` does not directly expose.
- It observes both direct sends and broadcast self-send calls because `broadcastPacketAndReceive` funnels through `sendPacket`.
- It avoids depending on encrypted frame decoding for the first Level 2 artifacts.

Do not keep this hook as a production feature unless it is converted into a tightly scoped diagnostic facility with review. For the migration artifact, a local temporary patch is acceptable if the artifact records the exact patch or commit.

## Observer Filtering

The observer should record only packets for the capture player and active scenario.

Minimum filters:

- enabled flag is true
- recipient player object id matches the capture player
- scenario id is set before sending `CM_SELECT_DECOMPOSABLE`
- packet Java class is in the allowed decompose packet set

Allowed packet classes for first artifacts:

- `SM_ITEM_USAGE_ANIMATION`
- `SM_SYSTEM_MESSAGE`
- `SM_INVENTORY_UPDATE_ITEM`
- `SM_DELETE_ITEM`
- `SM_CUBE_UPDATE`
- `SM_SECONDARY_SHOW_DECOMPOSABLE`
- `SM_INVENTORY_ADD_ITEM`

If any other item, cube, reward, survey, mailbox, event, or login packet appears between the client packet and final reward side effect, stop the capture and fix the fixture isolation before writing an artifact.

## Decoded Field Strategy

Prefer reflection-based field extraction for the observer, not hand-written packet reserialization.

Reason:

- First artifacts require Level 2 decoded fields, not byte-level parity.
- Most packet fields are private and do not expose getters.
- Calling `writeImpl` manually risks mutating Java runtime state. For example, `SM_ITEM_USAGE_ANIMATION.writeImpl` can set the player's current using item when `time > 0`.
- `AionServerPacket.write` encrypts through the connection and belongs to the real send path.

Reflection rules:

- Reflection is diagnostic-only and must be documented in `capture_method_details`.
- Field names must be read from the exact Java revision recorded by the artifact.
- If a private field cannot be read, include the packet class and missing field in `unsupported`; do not infer it.
- Do not mutate packet, player, item, inventory, storage, or static-data objects.
- Do not mark reflection-derived artifacts as byte parity evidence.

## Packet Field Extractors

### SM_ITEM_USAGE_ANIMATION

Read private fields:

- `playerObjId`
- `targetObjId`
- `itemObjId`
- `itemId`
- `time`
- `end`
- `unk`
- `unk1`
- `unk2`
- `unk3`

Expected constructor defaults for selectable decompose:

- `time = 0`
- `end = 1`
- `unk = 0`
- `unk1 = 0`
- `unk2 = 1`
- `unk3 = 1`

### SM_SYSTEM_MESSAGE

Record:

- numeric message id
- Java factory name when known
- parameter list

For selectable success, expected factory is `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`, message id `1400452`.

If the observer cannot identify factory names directly from the packet instance, map only reviewed numeric ids:

- `1400452` -> `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`
- `1300447` -> `STR_DECOMPOSE_ITEM_INVENTORY_IS_FULL`

Any other system message id should have `factory_name = null` and a note.

### SM_INVENTORY_UPDATE_ITEM

Read private fields:

- `player`
- `item`
- `updateType`

Record decoded values from the item and update type:

- `object_id`
- `item_id`
- `item_name`
- `count`
- `update_type_mask`
- `update_type_name`

For selectable source decrement, expected update type is `DEC_ITEM_USE`, mask `0x16`.

### SM_DELETE_ITEM

Read private fields:

- `itemObjectId`
- `deleteType`

Record:

- `object_id`
- `delete_type`
- `delete_type_name`

For selectable source delete, expected delete type is `USE`, mask `0x17`.

### SM_CUBE_UPDATE

Read private fields:

- `action`
- `actionValue`
- `itemsCount`
- `npcExpands`
- `questExpands`
- `itemExpands`

Record:

- `action`
- `storage_type_ordinal`
- `storage_type_name` if known from `actionValue`
- `items_count`
- `npc_expands`
- `quest_expands`
- `item_expands`

For cube-size updates, expected `action = 0`.

### SM_SECONDARY_SHOW_DECOMPOSABLE

Read private fields:

- `objectId`
- `itemsCollections`

Record:

- `object_id`
- `unknown_dword = 0`
- `reward_count`
- reward entries if non-empty

For selectable completion, Java currently sends an empty reward list after source mutation.

### SM_INVENTORY_ADD_ITEM

Read private fields:

- `items`
- `player`
- `addType`

For each item, record:

- `object_id`
- `item_id`
- `item_name`
- `count`
- `slot`
- `cloth_flag`

Record packet-level fields:

- `add_type_mask`
- `add_type_name`
- `packet_item_count`

For selectable reward add, expected add type is `DECOMPOSABLE`, mask `0x50`.

Generated reward object ids must be recorded. If the C# comparison cannot yet normalize generated object ids, store them in the artifact and mark comparison as needing follow-up object-id mapping support.

## Artifact Additions

Use the existing capture contract schema and add an optional `capture_method_details` object:

```json
{
  "capture_method": "live-java-server",
  "capture_method_details": {
    "observer": "PacketSendUtility.sendPacket diagnostic hook",
    "observer_enabled_flag": "aion.capture.decompose.enabled",
    "decoded_fields_method": "reflection",
    "reflection_scope": [
      "SM_ITEM_USAGE_ANIMATION",
      "SM_SYSTEM_MESSAGE",
      "SM_INVENTORY_UPDATE_ITEM",
      "SM_DELETE_ITEM",
      "SM_CUBE_UPDATE",
      "SM_SECONDARY_SHOW_DECOMPOSABLE",
      "SM_INVENTORY_ADD_ITEM"
    ],
    "java_patch_reference": "local patch or commit sha"
  }
}
```

Use `id_mapping` for Java item ids that differ from logical C# fixture ids:

```json
{
  "logical_source_item_id": 101,
  "java_source_item_id": 188052590,
  "logical_reward_index_0": 201,
  "java_reward_index_0": 188052591,
  "logical_reward_index_1": 202,
  "java_reward_index_1": 188052592
}
```

For object ids, prefer explicit logical mappings once the C# comparison supports them:

```json
{
  "logical_source_object_id": 5001,
  "java_source_object_id": 70001,
  "logical_reward_index_0_object_id": 6001,
  "java_reward_index_0_object_id": 70002
}
```

Do not use broad object-id normalization until the artifact names the logical and Java ids precisely.

## Pass/Fail Gate

An observer-generated artifact is acceptable for first C# comparison only when:

- it records the exact Java Git revision
- it records the observer hook and reflection scope
- it records both packet class order and decoded fields
- it records all packets sent to the capture player during the scenario, including any trailing `SM_CUBE_UPDATE` after reward add
- packet order is observed at `sendPacket`, not inferred from source
- item ids and object ids are explicit and mapped only by declared artifact fields
- unsupported byte capture is listed in `unsupported`
- reflection limitations are listed in `risks` or `unsupported`

Do not mark C# parity as verified from observer output alone. Parity can only be upgraded after C# tests compare the Java artifact against C# output and pass deterministically for the artifact scope.

## Stop Conditions

Stop and document the blocker if:

- the observer must mutate Java packet or player state
- the observer cannot filter to one capture player
- unrelated runtime packets appear during the scenario
- reflection cannot read required Level 2 fields
- packet order differs from source-reviewed expectations and no runtime explanation is available
- the diagnostic patch becomes broader than `PacketSendUtility.sendPacket` plus standalone observer code
- object-id or item-id mappings are ambiguous
- reward count is nondeterministic because Java XML data has different min/max values

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / selectable-decompose tests | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Java source reviewed for send order and no-op branches. Observer design does not run Java or compare artifacts. Invalid index, missing selectable data, and reward RNG remain runtime-unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send/broadcast helpers | Utility | Partial | Manual Only | Needs Verification | Recommended observer hook is `sendPacket(Player, AionServerPacket)` before the real connection send. Broadcast threading/fanout remains unverified; known-list must be empty for first captures. |
| `com.aionemu.gameserver.network.aion.AionConnection` | `Aion.GameServer.Network.Aion.GameServerConnection` | Game Connection | Partial | Manual Only | Needs Verification | Design avoids `AionConnection` as first hook because recipient player context is easier at `PacketSendUtility`. Java encryption/frame bytes remain unverified. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Network.Aion.GameServerPacket` | Packet Serialization | Partial | Manual Only | Needs Verification | Observer records reflected packet fields before Java serialization. This is not byte-level parity and does not verify `writeImpl` output ordering beyond source-reviewed field names. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan covers Java private constructor defaults including `unk2 = 1` and `unk3 = 1`. Runtime Java artifact and bytes remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Design maps only reviewed decompose message ids to factory names. Broader system-message reflection/factory coverage is incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan records source count and update type. Full `ItemInfoBlob` serialization remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan records object id and delete type. Runtime Java artifact still needed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Design calls out delete-path cube update and possible reward-add trailing cube update as packets to record. Existing C# expectations may need adjustment after Java artifact evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSecondaryShowDecomposable` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan records source object id and reward count. Non-empty selectable-list display remains outside first artifacts. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Design records add type, reward ids/counts, slot, cloth flag, and generated object ids. Object-id comparison support remains a known follow-up. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested in C#; Manual Only for observer design | Needs Verification | Observer records high-level item fields but does not decode or compare full Java blob bytes. Serialization/optional-entry parity remains incomplete. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / connection item side effects | Service / Packet Side Effects | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Java source shows delete sends `SM_DELETE_ITEM` then `SM_CUBE_UPDATE`; storage add sends `SM_INVENTORY_ADD_ITEM` then `SM_CUBE_UPDATE`. C# comparison must treat observed trailing cube updates as evidence, not noise. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` / item services | Service | Partial | Regression Tested in C#; Manual Only for observer design | Needs Verification | Observer design records reward packet side effects but does not verify Java item creation, IDFactory allocation, expirable registration, or persistence behavior. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Regression Tested in C#; Manual Only for observer design | Needs Verification | Observer design relies on Java storage/service side effects for source decrement/delete. Persistence, quest callbacks, and transaction behavior remain unverified. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Java Design | Java source review of selectable decompose handler, send utility, packet services, and server packets | Defines a temporary Java observer for Level 2 artifact generation. | Static source inspection only. | No Java observer was implemented or run; no artifact exists; no C# comparison against Java runtime output. |

## Remaining Risks

- Java runtime capture remains blocked locally by missing Java 25/Maven tooling.
- The observer design uses reflection against private packet fields; this is acceptable for diagnostics but not production parity.
- Reflection-derived fields do not prove byte-level serialization parity.
- Java `SM_INVENTORY_ADD_ITEM` storage update may emit a trailing `SM_CUBE_UPDATE`; current C# guarded comparison may need a follow-up once a real artifact confirms the sequence.
- Generated reward object ids need an explicit artifact mapping and C# comparison support before object-id fields can be compared safely.
- Full `ItemInfoBlob` contents, encrypted frames, threading/order under real dispatcher load, persistence, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 15
- Total artifacts ported: 0 production code artifacts; 1 Java packet-observer design document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 15
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, SQL fixture automation, object-id comparison support, full item-info blob comparison, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit reduces artifact-generation ambiguity but adds no runtime evidence.

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, implement the temporary `PacketSendUtility.sendPacket` observer in a Java-capable capture branch and generate `JD-SEL-DEC-001.json` plus `JD-SEL-DEL-001.json`.

If tooling remains blocked, add object-id mapping/comparison support to the guarded C# artifact comparison now that this document defines explicit logical/Java object-id mapping names, or add a SQL fixture appendix for the live-server capture runbook.
