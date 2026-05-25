# Phase 6 Java Decompose Harness Feasibility Audit

Date: May 25, 2026
Unit of Work: UOW-876
Status: Read-only feasibility audit complete; harness not implemented

## Scope

This audit checks whether selectable-decompose packet order can be captured from the Java runtime without changing production Java.

Target scenario:

- Java `CM_SELECT_DECOMPOSABLE`
- active player object id `1001`
- source item object id `5001`
- selectable item template `101`
- reward item `202 x3`
- no nearby visible players

The goal is packet class/order capture first. Encrypted frame bytes remain a later concern because deterministic Java `Crypt` state is a separate blocker.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Java connection capture feasibility | `AionConnection`, `AConnection`, `AionServerPacket` | none | Java Analysis | Yes, read-only | Medium | Source-only analysis has no file ownership conflict, but output doc is shared. |
| B | Java player/inventory fixture feasibility | `Player`, `PlayerStorage`, `Storage`, `ItemService`, `ItemPacketService`, `ItemFactory` | none | Java Analysis | Yes, read-only | High | Fixture dependencies touch global data, ID factory, storage packet side effects, and player online state. |
| C | Selectable decompose handler path | `CM_SELECT_DECOMPOSABLE`, `DecomposableItemsData`, `PacketSendUtility` | none | Java Analysis | Yes, read-only | Medium | Handler is narrow, but sends through static packet utility and mutable global data holders. |
| D | Fixture schema documentation | docs only | docs only | Documentation Update | Yes after analysis | Low | Can be documented separately, but progress/handoff docs remain orchestrator-owned. |
| E | Java vector/capture implementation | possible Java test utility or live capture script | TBD | Test Creation / Parity Verification | No for this unit | High | Exact implementation path depends on this audit and may need shared build/test decisions. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Read-only Java harness feasibility audit | Java Analysis / Documentation | Java source reads; `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`; progress and handoff docs | Production Java/C# code changes | Latest Phase 6 handoff and comparison plan | Conservative feasibility decision and next unit scope |

No sub-agents were used because the only write targets are shared documentation and progress/handoff files.

## Findings

### AionConnection Capture

Relevant Java artifacts:

- `com.aionemu.commons.network.AConnection`
- `com.aionemu.gameserver.network.aion.AionConnection`
- `com.aionemu.gameserver.network.aion.AionServerPacket`
- `com.aionemu.gameserver.network.Crypt`

Important constraints:

- `AConnection.sendPacket` is `final`, so a capture connection cannot override packet enqueue behavior.
- `sendPacket` writes into `getSendMsgQueue()` only when `isConnected()` is true, then touches a private `SelectionKey` through `key.interestOps(...)` and `key.selector().wakeup()`.
- `AionConnection.getSendMsgQueue`, `processData`, and `writeData` are `final`.
- `AionConnection` owns a private final `Crypt`; deterministic encrypted-byte capture would require either real `SM_KEY` capture plus replay, reflection, or live socket capture.
- `AionConnection` constructs `ConnectionAliveChecker`, which schedules a repeating ping check via `ThreadPoolManager` in the constructor.
- `AionServerPacket.write` is usable only with an `AionConnection` because it calls `con.encrypt(...)`.

Implication:

A no-socket fake `AionConnection` is not a clean in-process seam. Packet capture through normal `PacketSendUtility.sendPacket` likely needs either:

1. a real registered Java socket connection under the dispatcher, or
2. reflection-heavy setup of private `key`/connection state and direct queue inspection, with scheduler cleanup.

Both are broader than a small unit test and should not be treated as low-risk.

### Player and Inventory Fixture

Relevant Java artifacts:

- `com.aionemu.gameserver.model.gameobjects.player.Player`
- `com.aionemu.gameserver.model.items.storage.PlayerStorage`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.services.item.ItemFactory`
- `com.aionemu.gameserver.utils.idfactory.IDFactory`

Important constraints:

- `Player` constructor builds real `Equipment`, `PlayerStorage`, cooldown lists, stats, and controller/move-controller state.
- `Player.isOnline()` is true only when `getClientConnection() != null`; `PacketSendUtility.sendPacket` drops packets unless the player is online.
- `Player` constructor passes `position = null`; code paths that call `isSpawned()` or world position need an explicit world/position setup.
- `PlayerService` normally installs `new KnownList(player)`. For direct selectable decompose, the harness can install a `KnownList` manually to support `broadcastPacketAndReceive`.
- `Storage.decreaseByObjectId` sends item update/delete packets through `ItemPacketService`.
- `ItemService.addItem` uses `DataManager.ITEM_DATA`, `ItemFactory`, `IDFactory`, `ExpireTimerTask`, storage capacity checks, and `ItemPacketService`.
- `ItemFactory.newItem` calls `IDFactory.getInstance().nextId()`, and `IDFactory` initializes from multiple DAOs.

Implication:

The selectable-decompose handler can probably be exercised in-process only if the harness accepts significant real server infrastructure:

- loaded item/decomposable static data, or reflective minimal data holders
- real or test-safe database/DAO configuration for ID allocation, unless item objects are inserted manually and reward add is captured through a broader service path
- an online player with a real or convincingly registered connection
- a known-list fixture returning no other players

This is feasible as an integration-style Java harness, not as a tiny unit test.

### Selectable Decompose Handler

Relevant Java artifacts:

- `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE`
- `com.aionemu.gameserver.dataholders.DecomposableItemsData`
- `com.aionemu.gameserver.utils.PacketSendUtility`

Handler packet order from Java source:

1. `PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(...))`
2. `PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED(...))`
3. `player.getInventory().decreaseByObjectId(objectId, 1)`
4. `PacketSendUtility.sendPacket(player, new SM_SECONDARY_SHOW_DECOMPOSABLE(objectId, Collections.emptyList()))`
5. `ItemService.addItem(... ItemAddType.DECOMPOSABLE, ItemUpdateType.INC_ITEM_COLLECT)`

Important constraints:

- `DecomposableItemsData.getSelectableItems` returns a copy, so `removeIf` does not mutate static data.
- Index validation is 1-based against size but uses zero-based retrieval: `if (index + 1 > size) return; selectedItems.get(index)`.
- Reward count is random through `Rnd.get(min, max)`; deterministic fixture should use `min == max`.
- `broadcastPacketAndReceive` sends to self first, then iterates known players. An empty known list should produce exactly one self packet.
- Source decrement packet can be either update or delete depending on source stack count.
- Reward add packet type is controlled by `ItemAddType.DECOMPOSABLE` and `ItemUpdateType.INC_ITEM_COLLECT`.

Implication:

The source-reviewed packet class sequence is clear, but runtime capture needs real side-effect paths to prove Java output. A class/order-only capture is easier than byte capture, but still depends on connection/player fixture viability.

## Feasibility Decision

| Capture Path | Feasibility | Pros | Blockers / Risks | Recommendation |
|---|---|---|---|---|
| Direct no-socket fake `AionConnection` | Low | Would be fast if possible | `sendPacket` is final, private `SelectionKey` is touched, constructor starts heartbeat, private final `Crypt` blocks deterministic byte control | Do not start here. |
| Reflection-heavy in-process queue capture | Medium | Could capture server packet classes without live client traffic | Fragile private `key`/queue/crypt state, scheduler cleanup, test may break with Java internals | Only use if a separate spike proves key/selector setup can be made stable. |
| Real Java loopback socket capture | Medium | Exercises actual dispatcher, queue, `SM_KEY`, crypt, and packet writes | Requires Java server networking/test bootstrap and deterministic fixture account/player/static data | Best Java-runtime direction if live fixture can be controlled. |
| Live Java server capture | Medium-High | Highest fidelity for runtime packet order/bytes | Needs DB/static-data setup, fixture inventory, timing isolation, client script, and noise filtering | Preferred fallback if in-process fixture becomes too broad. |
| Source-only confirmation | Complete for planning, insufficient for parity | Fast and already done | Cannot prove runtime packet order, serialization bytes, scheduler behavior, or encryption | Keep as `Partial Parity` only. |

Conclusion:

An in-process Java selectable-decompose harness without production changes is possible only as an integration-style harness with real or heavily reflected connection state. It is not a small isolated unit test. The next safe unit should specify the exact fixture/capture contract before implementation, then choose either a loopback Java socket harness or a live-server capture.

## Recommended Next Unit

Create a decompose Java capture fixture contract document and skeleton artifact schema before writing harness code.

Minimum output:

- scenario id and Java revision
- static-data fixture inputs for source/reward item templates and decomposable data
- player/account/inventory setup requirements
- packet capture level:
  - packet class/order
  - decoded important fields
  - unencrypted frame bytes only if practical
  - encrypted bytes only with captured deterministic key path
- exact pass/fail criteria for selectable source decrement and selectable source delete variants
- decision checkpoint between Java loopback socket capture and live-server capture

Do not claim verified parity from this audit. It is source inspection and feasibility planning only.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.commons.network.AConnection` | `Aion.Commons.Network` connection primitives / `Aion.GameServer.Network.Aion.GameServerConnection` | Connection Base | Partial | Manual Only | Needs Verification | Java `sendPacket` is final and touches private `SelectionKey`; this blocks a clean fake connection capture seam. C# socket behavior remains tested separately, but Java runtime comparison is not implemented. Reflection differences and threading/scheduler side effects are unresolved. |
| `com.aionemu.gameserver.network.aion.AionConnection` | `Aion.GameServer.Network.Aion.GameServerConnection` | Game Connection | Partial | Manual Only | Needs Verification | Java constructor schedules heartbeat and owns private final `Crypt`; queue capture likely needs real dispatcher/socket or fragile reflection. No Java runtime capture exists yet. Threading and encryption-state differences remain risks. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Network.Aion.GameServerPacket` | Packet Serialization | Partial | Manual Only | Needs Verification | Java packet serialization requires an `AionConnection` because `write` calls `con.encrypt`. Broader frame-byte parity and deterministic Java crypt capture remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for Java audit | Partial Parity | Java packet order was source-reviewed: usage animation, success message, source decrement/delete side effect, secondary clear, reward add. Runtime Java packet capture and byte comparison remain missing. Random reward count must be fixed by min=max fixture. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player.Player` | Domain Model | Partial | Manual Only | Needs Verification | Java online state requires a non-null client connection; constructor initializes many real subsystems and leaves world position null. Harness needs explicit connection, known-list, and possibly position/world setup. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | `Aion.GameServer.Model.Items.Storage.PlayerStorage` / inventory services | Storage | Partial | Manual Only | Needs Verification | Java storage routes add/decrease/delete through owner-aware packet side effects. Source decrement/delete runtime capture remains unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Model.Items.Storage.Storage` / inventory services | Storage | Partial | Manual Only | Needs Verification | Java `decreaseByObjectId` updates item counts, persistent state, deleted-items queue, quest removal callback, and item packets. Persistence/DAO and side-effect parity are not verified by this audit. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.Items.InventoryAddService` / item services | Service | Partial | Manual Only | Needs Verification | Java reward add uses `DataManager.ITEM_DATA`, `ItemFactory`, `IDFactory`, expirable registration, storage capacity, and item packet add/update side effects. Runtime fixture needs deterministic static data and ID allocation. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers | Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Java delete/update/add packet type selection was source-reviewed, including `ItemAddType.DECOMPOSABLE` and `ItemUpdateType.INC_ITEM_COLLECT`. Exact packet bytes and source-delete/decrement runtime order remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` decomposable item data | Data Holder | Partial | Manual Only | Needs Verification | Java selectable data returns a copy, so handler filtering is isolated. Harness needs either real XML load or minimal reflective fixture data. Serialization/XML defaults were not verified in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send/broadcast helpers | Utility | Partial | Manual Only | Needs Verification | Java `sendPacket` drops packets unless player is online; `broadcastPacketAndReceive` sends self before known-list players. Empty known-list fixture is required to make order deterministic. |
| `com.aionemu.gameserver.services.item.ItemFactory` | `Aion.GameServer.Services.Items.ItemFactory` / item creation services | Utility | Partial | Manual Only | Needs Verification | Java reward creation calls global `IDFactory` and `DataManager.ITEM_DATA`; this can pull DAO/database dependencies into an in-process harness. |
| `com.aionemu.gameserver.utils.idfactory.IDFactory` | `Aion.GameServer.Services.IdFactory` / object id allocation | Utility | Partial | Manual Only | Needs Verification | Java singleton initializes used IDs from multiple DAOs. Test fixture must avoid accidental production DB assumptions or explicitly configure a test DB. |

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Java Analysis | Source review of Java connection, packet, player, storage, item-service, decomposable-data, and packet-send artifacts | Documents whether selectable-decompose packet-order capture can be implemented without production Java changes. | Static source inspection only. | No Java harness, no Java runtime output, no golden packet class sequence, no byte vectors, no live-server capture. |

## Remaining Risks

- Java runtime comparison remains unimplemented.
- In-process capture may require fragile reflection against `AConnection.key`, `AionConnection.sendMsgQueue`, private final `Crypt`, or scheduler state.
- Live Java socket capture may require DB/static-data/player fixture setup beyond the current C# test fixtures.
- Java `IDFactory` and `ItemFactory` can pull DAO/database dependencies into reward-add capture.
- Decompose reward count must be deterministic (`min == max`) before comparing output.
- Encryption byte parity cannot be claimed until Java key generation/capture is controlled.
- Threading differences remain unresolved: Java dispatcher/packet processor/scheduler ordering differs from C# async/socket-loop execution.
- Serialization differences remain unresolved for item update/add/delete packet bytes.

## Summary Metrics

- Total Java artifacts discovered: 13
- Total artifacts ported: 0 code artifacts; 1 feasibility audit document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 13
- Total blocked artifacts: 8 blocked/not-started categories, including Java runtime harness, deterministic connection capture, Java crypt byte vectors, fixture DB/static-data setup, source decrement/delete runtime comparison, reward-add packet byte comparison, broadcast fanout comparison, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete; this unit clarifies the Java runtime comparison path but does not increase runtime parity coverage.
