# Phase 6 - Pet Feed Unusual Storage Java Observer Placement

Date: May 27, 2026
Unit of Work: UOW-1346

## Scope

This docs-only unit records the safest Java hook placement for future unusual-storage rejected-food runtime artifact generation. It does not edit Java network core.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.utils.PacketSendUtility`
- `com.aionemu.gameserver.network.aion.AionConnection.writeData`
- `com.aionemu.gameserver.network.aion.AionServerPacket.write`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob`

## Observed Java Send Path

`PetService.checkFeeding` rejected-food branch:

1. calls `ItemPacketService.sendItemUnlockPacket(player, item)`;
2. sends `SM_PET(5, 0, 0, pet)`;
3. sends `SM_EMOTION(player, END_FEEDING, 0, player.getObjectId())`;
4. sends `SM_SYSTEM_MESSAGE.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR(...)`.

For unusual storage ids, `sendItemUnlockPacket` delegates to `sendStorageUpdatePacket`, which queues:

1. `SM_WAREHOUSE_ADD_ITEM`;
2. `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

`PacketSendUtility.sendPacket` is queue-only:

```text
player.getClientConnection().sendPacket(packet)
```

`AionConnection.writeData` is the dequeue/serialization point:

```text
AionServerPacket packet = sendMsgQueue.removeFirst()
packet.write(this, data)
```

`AionServerPacket.write` is the clear-byte construction point:

```text
buf.putShort((short) 0)
writeOP()
writeImpl(con)
buf.flip()
buf.putShort((short) buf.limit())
ByteBuffer b = buf.slice()
buf.position(0)
con.encrypt(b)
```

This means the last deterministic clear payload exists after `writeImpl(con)` and length stamping, but before `con.encrypt(b)`.

## Recommended Two-Stage Hook Placement

Future artifact generation should use two disabled-by-default hooks: a narrow construction/context hook plus a generic serialization/bytes hook.

### 1. Construction Context Hook

Place the narrow context hook in `ItemPacketService.sendStorageUpdatePacket(Player, StorageType, Item, ItemAddType)`.

Trigger only when:

- `addType == ItemAddType.ALL_SLOT`;
- `storageType` is unusual storage:
  - pet bags with ids `32` through `43`;
  - house storage ids `60` through `79`;
  - `BROKER`;
  - `MAILBOX`.

Reasons:

- This method has `Player`, `StorageType`, `Item`, and `ItemAddType`.
- It chooses Java's actual packet order:
  - `SM_WAREHOUSE_ADD_ITEM`;
  - `SM_CUBE_UPDATE.cubeSize(storageType, player)`.
- It can register construction-time route facts before the packet objects are serialized later.

### 2. Serialization Bytes Hook

Use a no-op-by-default observer around `AionServerPacket.write` after `buf.putShort((short) buf.limit())`, before `con.encrypt(b)`.

Reasons:

- It captures encode-time state, including Java's live `Item` reads inside `SM_WAREHOUSE_ADD_ITEM.writeImpl`.
- It sees final clear bytes after packet length and obfuscated opcode are written.
- It avoids queue-time false snapshots from `PacketSendUtility`.
- It can extract:
  - body hex as `buf` bytes after the 7-byte frame header;
  - canonical payload hex as little-endian opcode plus body, matching the C# comparator convention;
  - frame length/header bytes if needed for diagnostics;
  - packet class/opcode/player context.
- It is packet-agnostic and can support subtype-7, unusual-storage, and future vector artifacts through filters.

Recommended future classes:

- `game-server/src/com/aionemu/gameserver/network/aion/capture/ServerPacketCaptureObserver.java`
- `game-server/src/com/aionemu/gameserver/network/aion/capture/NoOpServerPacketCaptureObserver.java`
- `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedUnusualStorageArtifactCapture.java`

The network-core hook should not write artifacts directly. It should call an observer that defaults to no-op; enabled capture should copy bytes immediately and hand them to a bounded async writer.

## Guarding Requirements

The hook must default to disabled and must not allocate/copy bytes when disabled.

Recommended guard layers:

- config flag such as `PARITY_PACKET_CAPTURE_ENABLED = false`;
- optional packet class allow-list;
- optional player/account allow-list;
- optional scenario/session id;
- bounded output directory under `parity-artifacts/...`;
- exception isolation so observer failures cannot block packet send;
- bounded async artifact writing outside the dispatcher thread;
- no encryption-key, credential, or raw client input capture.

## Artifact Capture Responsibilities

The generic `AionServerPacket.write` hook can capture packet bytes, but unusual-storage schema-v1 also needs scenario facts that bytes alone do not provide.

Future implementation should combine:

- a generic serialization observer event from `AionServerPacket.write`;
- a narrow pet-feed scenario context established around `PetService.checkFeeding` or `ItemPacketService.sendStorageUpdatePacket`.

Scenario context should provide:

- storage id, storage type name, and Java ordinal;
- expected reachability and normal UI flow flag;
- feed item lookup phase and unlock decision phase;
- construction-time warehouse type/add type/add mask;
- encode-time item snapshot fields;
- blob entry ids/order and decoded item-blob fields;
- normalized clock facts for expiration and dye remaining seconds.

## Do Not Hook First

Do not start with `PacketSendUtility.sendPacket` for byte parity artifacts. It only observes queued packet instances before encode-time item/blob reads.

Do not start with `AionConnection.writeData` alone unless the observer can inspect the post-write clear bytes before encryption. `writeData` owns queue order and connection context, but `AionServerPacket.write` has the cleaner single packet serialization boundary.

Do not place unusual-storage-only logic directly inside `SM_WAREHOUSE_ADD_ITEM` or `SM_CUBE_UPDATE` first. That would duplicate packet-specific logic and miss future reusable packet vector needs.

Do not reuse gameplay `ObserveController` patterns for this network hook. Existing observer patterns are gameplay-local and are not suitable as-is for serialization capture.

## Migration Parity Table - UOW-1346

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rejected-food branch | future Java scenario context; C# artifact reader | Service Flow / Observer Context | Not Started | Manual Only | Needs Verification | Source review identifies packet order and delayed mutable item timing. No Java context hook implemented. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` default branch | future `PetFeedUnusualStorageArtifactCapture`; C# artifact reader | Service Flow / Observer Context | Not Started | Manual Only | Needs Verification | Recommended construction context hook point because it has player, storage type, item, add type, and packet order. No Java context hook implemented. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | future Java observer design notes | Queue Boundary | Not Started | Manual Only | Needs Verification | Confirmed queue-only and unsuitable for final byte capture. Useful only for optional scenario correlation. |
| `com.aionemu.gameserver.network.aion.AionConnection.writeData` | future Java observer design notes | Serialization Dispatcher | Not Started | Manual Only | Needs Verification | Confirms packet dequeue and call to `packet.write`. Can provide connection/player context, but clear bytes are easiest inside `AionServerPacket.write`. |
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | future Java packet serialization observer | Serialization Hook | Not Started | Manual Only | Needs Verification | Recommended no-op-by-default hook point after length stamping and before encryption. No code added. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet / Runtime Artifact Target | Partial | Unit Tested reader only | Needs Verification | Observer placement preserves encode-time item/blob reads. Java runtime bytes still absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `Aion.GameServer.Tests.PetFeedUnusualStorageJavaVectorArtifactReaderTests` | Packet / Runtime Artifact Target | Partial | Unit Tested reader only | Needs Verification | Observer placement can capture final zero-count cube-update bytes. Java runtime bytes still absent. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Design | Java network serialization source review | Defines safe observer placement and guard requirements. | Manual source review only. | No Java hook, generated artifacts, or runtime comparison. |

## Remaining Risks

- Java observer hook is not implemented.
- Scenario context writer is not implemented.
- Artifact writer/JSON serializer is not implemented.
- Byte-copying at the wrong buffer position could accidentally capture encrypted bytes or include mutable buffer tail data.
- Observer exceptions must be isolated from packet sending.
- Artifact writing from the dispatcher thread could hurt packet throughput unless snapshots are queued to a bounded async writer.
- Full `ItemInfoBlob` decoded-entry generation remains unspecified.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 observer placement document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java observer hook, scenario context writer, artifact writer, item blob decoder, live unusual-storage adapter, socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Implement the disabled-by-default Java packet serialization observer shell around `AionServerPacket.write`, without enabling capture by default and without unusual-storage scenario logic yet. Add minimal tests or compile validation if Java tooling is available.
