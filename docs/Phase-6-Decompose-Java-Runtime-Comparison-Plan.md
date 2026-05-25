# Phase 6 Decompose Java Runtime Comparison Plan

Date: May 25, 2026
Scope: Phase 6 decompose packet order, crypt bytes, and runtime side effects
Status: Planning complete; runtime harness not implemented

## Goal

Produce objective Java-vs-C# evidence for the decompose paths currently covered by C# socket-loop tests:

- selectable `CM_SELECT_DECOMPOSABLE`
- normal `CM_USE_ITEM` scheduled source decrement
- normal `CM_USE_ITEM` scheduled source delete

The comparison must not replace the Java source of truth. It should capture Java runtime output for the same deterministic inventory/static-data scenarios and compare it against the C# socket-loop observer/packet bytes.

## Current C# Evidence

The C# tests in `GameServerConnectionInventoryExpansionUseItemTests` now cover:

- `RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection`
- `RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose`
- `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward`

These tests provide deterministic C# runtime evidence through:

- real `TcpClient` / `NetworkStream`
- initial `SmKey`
- encrypted client frames for opcodes `236` and `37`
- `GameServerConnection.ReadPacketAsync`
- `GameClientPacketFactory.TryCreatePacket`
- connection handler dispatch
- C# scheduler completion for normal decompose
- packet observer order

They do not compare against Java runtime output, encrypted server bytes, Java scheduler timing, Java known-list broadcast fanout, or Java crypt key evolution.

## Java Artifacts To Compare

| Java Artifact | Behavior To Capture | Current C# Counterpart |
|---|---|---|
| `com.aionemu.gameserver.network.aion.AionConnection.initialized` | Initial `SM_KEY` send before client packet handling. | `GameServerConnection.RunAsync` / `SmKey` |
| `com.aionemu.gameserver.network.Crypt` | First server packet unencrypted; later server/client packet crypt with generated key. | `GameCrypt` |
| `com.aionemu.gameserver.network.EncryptionKeyPair` | Client decrypt validation, server encrypt, and key increment by packet body length. | `GameEncryptionKeyPair` |
| `com.aionemu.gameserver.network.aion.AionServerPacket.write` | Frame length, server opcode transform, static code `0x44`, flipped opcode, and body encryption. | `GameServerPacket.SerializeFrame` |
| `com.aionemu.gameserver.network.aion.AionConnection.processData` | Skip pre-key packets, decrypt, reject fake packets, factory dispatch, packet processor enqueue. | `GameServerConnection.ReadPacketAsync` / `ProcessPacketAsync` |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | Read object id, unknown dword, index; broadcast usage animation; success message; source decrease; secondary clear; reward add. | `CmSelectDecomposable` / `HandleSelectDecomposableAsync` |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | Read object id/type; route to item actions after restrictions and quest hook. | `CmUseItem` / `HandleUseItemAsync` |
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `canAct`, selectable first-show branch, 3000ms normal action, cancel observer, scheduled post-validate, success message, reward add. | `DecomposeService` / `HandleDecomposeUseItemAsync` / `CompleteDecomposeUseItemAsync` |
| `com.aionemu.gameserver.utils.PacketSendUtility` | Self-send and known-list broadcast semantics for usage animation and direct packets. | `SendPacketAsync` / `BroadcastItemUsageAnimationAsync` |
| `com.aionemu.gameserver.services.item.ItemService` / `ItemPacketService` | Source decrement/delete packet side effects and decompose reward add packet type. | `InventoryAddService` / inventory packet writers |

## Scenarios

### Scenario A: Selectable Decompose

Input:

- active player object id `1001`
- source item object id `5001`
- source item template id `101`
- source count `2`
- selectable reward index `1`
- reward item `202 x3`
- no nearby visible players

Expected Java packet order to capture:

1. `SM_KEY`
2. `SM_ITEM_USAGE_ANIMATION` from `broadcastPacketAndReceive`
3. `SM_SYSTEM_MESSAGE.STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`
4. source inventory decrement packet from `Inventory.decreaseByObjectId`
5. `SM_SECONDARY_SHOW_DECOMPOSABLE` with empty list
6. reward add packet from `ItemService.addItem(... ItemAddType.DECOMPOSABLE, ItemUpdateType.INC_ITEM_COLLECT)`

### Scenario B: Normal Decompose Source Decrement

Input:

- active player object id `1001`
- source item object id `5001`
- source item template id `100`
- source count `2`
- deterministic reward item `200 x1`
- no nearby visible players

Expected Java packet order to capture:

1. `SM_KEY`
2. start `SM_ITEM_USAGE_ANIMATION` with `time=3000`, `end=0`
3. `SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_SUCCEED`
4. source inventory update packet from `Inventory.decreaseByObjectId`
5. final `SM_ITEM_USAGE_ANIMATION` with `time=0`, `end=1`
6. reward add packet from `ItemService.addItem(... ItemAddType.DECOMPOSABLE, ItemUpdateType.INC_ITEM_COLLECT)`

### Scenario C: Normal Decompose Source Delete

Input:

- same as Scenario B, but source count `1`

Expected Java packet order to capture:

1. `SM_KEY`
2. start `SM_ITEM_USAGE_ANIMATION` with `time=3000`, `end=0`
3. `SM_SYSTEM_MESSAGE.STR_DECOMPOSE_ITEM_SUCCEED`
4. `SM_DELETE_ITEM` with use-delete type from Java `ItemPacketService`
5. final `SM_ITEM_USAGE_ANIMATION` with `time=0`, `end=1`
6. reward add packet from `ItemService.addItem(... ItemAddType.DECOMPOSABLE, ItemUpdateType.INC_ITEM_COLLECT)`

## Recommended Harness Path

### Preferred: Java In-Process Packet Capture Harness

Create a Java-only test harness that instantiates or subclasses the minimum runtime objects needed to execute packet handlers and capture packets before socket encryption.

Required seams:

- a test `AionConnection` with:
  - deterministic `Crypt` key or captured generated key
  - active `Player`
  - packet send queue capture
- a minimal online `Player` with:
  - client connection
  - inventory entries
  - known-list that returns no nearby players
  - spawned/alive flags for `DecomposeAction.canAct`
- deterministic data holders for:
  - item templates `100`, `101`, `200`, `202`
  - decomposable item collections
- deterministic random outcome:
  - use fixed min/max reward counts first
  - avoid random collections until a later unit

Capture levels:

1. pre-encryption packet class/order and decoded field values
2. serialized unencrypted frame body after `AionServerPacket.write` with crypt disabled or first-packet bypass controlled
3. encrypted frame bytes with deterministic `Crypt` key

Risks:

- Java `AionConnection` owns a private final `Crypt` with random key generation.
- Java `PacketSendUtility` requires `player.isOnline()` and a real client connection.
- Java packet writes may rely on global static `DataManager` state.
- Java item services may touch DAO/autocommit paths unless item storage is fully test-backed.
- Scheduled completion uses `ThreadPoolManager`, so the harness needs either a real scheduler wait or a seam around scheduled execution.

### Fallback: Live Java Server Capture

Run the Java game server with a deterministic fixture account/player and capture packet order/bytes from a script or lightweight client.

Benefits:

- exercises real Java runtime, schedulers, item services, and packet encryption

Costs:

- needs live DB/static-data setup
- harder to force deterministic inventory and reward data
- harder to isolate packet order from unrelated login/enter-world traffic
- real-client timing and visibility state can add noise

### Not Recommended: Source-Only Manual Confirmation

Source review is already enough for conservative `Partial Parity` notes, but it is not enough to claim Java runtime comparison or byte parity for these paths.

## Proposed Outputs

Add a harness or captured artifact that records:

- scenario id
- Java commit/source revision
- Java packet class sequence
- decoded important fields per packet
- unencrypted payload bytes where deterministic
- encrypted frame bytes where deterministic
- scheduler wait/capture method
- static-data fixture inputs
- known unsupported comparisons

Then add C# tests that compare:

- packet class sequence
- important decoded field values
- unencrypted payload bytes for stable packets
- encrypted frame bytes only when key and frame sequence are deterministic

## Recommended Next Unit

Start with a Java analysis/harness spike that does not change production Java:

1. Identify whether Java `AionConnection` can be subclassed or wrapped cleanly enough to capture `sendMsgQueue`.
2. Identify the smallest set of `Player`, `Inventory`, and static `DataManager` setup needed for `CM_SELECT_DECOMPOSABLE`.
3. Decide whether deterministic `Crypt` requires test-only reflection or a small copied vector generator under `dotnetConversion/tools`.
4. Produce one captured selectable-decompose packet-order artifact before attempting normal scheduled decompose.

If this is too broad, switch to the smaller C# verification fallback: add C# tests for corrupt encrypted packet threshold and multi-packet key evolution, while keeping Java runtime comparison as unresolved.
