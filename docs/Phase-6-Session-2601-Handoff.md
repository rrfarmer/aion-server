# Phase 6 Session 2601 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2601: Send private-store seller kinah add fanout live. See
[Phase-6-Session-2601-Completion.md](Phase-6-Session-2601-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- `d15dc2f` - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`
- Current commit - `[Phase 6][UOW-2601] Send private-store seller kinah add live`

## Session Summary

- Java review confirmed `Storage.increaseKinah` creates a zero-count kinah item through `Storage.add` when the seller has
  no existing kinah row, then sends the kinah increase update.
- C# private-store purchases already created the seller kinah inventory row, but only sent the final increase update.
- Live C# private-store execution now sends zero-count seller kinah add/cube before the increase update when the row was
  newly created.

## Files Changed In UOW-2601

- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2601-Completion.md`
- `docs/Phase-6-Session-2601-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.model.items.storage.Storage#increaseKinah`
- `com.aionemu.gameserver.model.items.storage.Storage#add`
- `com.aionemu.gameserver.model.items.storage.Storage#increaseItemCount`
- `com.aionemu.gameserver.services.item.ItemPacketService#sendStorageUpdatePacket`
- `com.aionemu.gameserver.services.item.ItemPacketService#sendItemPacket`

## C# Artifacts Touched

- `Aion.GameServer.Services.PrivateStorePurchasePlanService`
- `Aion.GameServer.Services.PrivateStorePurchasePlan`
- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 311/311.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live packet and serializer coverage. The filter built `Aion.GameServer` and directly
covered the modified live `ProcessPacketAsync` branch plus adjacent packet tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage#increaseKinah` | `Aion.GameServer.Services.PrivateStorePurchasePlanService` | Service/plan | Partial | Unit Tested | Partial Parity | Plan now records Java's `kinahItem == null` branch for private-store seller kinah gain; broader inventory service behavior remains incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage#add` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` seller kinah fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | Missing seller kinah row now sends zero-count add and cube update before increase update. |
| `com.aionemu.gameserver.services.item.ItemPacketService#sendStorageUpdatePacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` / `SmCubeUpdate` | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered for the private-store seller kinah add branch only; other storage families and add types are outside this UOW. |
| `com.aionemu.gameserver.services.item.ItemPacketService#sendItemPacket` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered for seller kinah `INC_KINAH_COLLECT` after a newly created kinah row. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `Storage.increaseKinah` + `ItemPacketService` | Seller without kinah receives zero-count kinah add, cube update, and kinah increase update | Java source review + live C# handler assertion | Seller message ordering remains different. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `Storage.increaseKinah` + `ItemPacketService` | Multi-row seller without kinah receives zero-count kinah add/cube before update | Java source review + live C# handler assertion | Does not verify real client behavior. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount` | Unit/live handler | `Storage.increaseKinah` + `ItemPacketService` | Non-closing sale still sends missing-seller-kinah add/cube/update | Java source review + live C# handler assertion | Seller message ordering remains different. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMissingSellerInventoryKeepsStoreItemAndTransfersKinah` | Unit/live handler | `Storage.increaseKinah` | Seller with existing kinah remains update-only | Java source review + live C# handler assertion | Branch is mixed missing-item, not a normal sold item. |

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Seller sale notification ordering likely differs from Java; Java sends the seller system message in the per-bought-item
  loop before buyer/seller kinah updates, while C# still sends seller kinah packets before seller messages.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- Multi-item purchase packet interleaving across seller and buyer recipients remains only partially modeled by tests.
- Insufficient-kinah denial, stale seller item counts, missing item templates, and mixed present/missing seller item
  behavior still need live branch coverage or fixes.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution
  surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Update-Only Seller Kinah Fanout For New Kinah Row

- Java `Storage.increaseKinah` first calls `Storage.add(ItemFactory.newItem(KINAH, 0), actor)` when `kinahItem == null`.
- That add path sends `SM_INVENTORY_ADD_ITEM` and `SM_CUBE_UPDATE` before the later `INC_KINAH_COLLECT` update.
- C# update-only behavior was therefore not Java-equivalent when the seller had no kinah row.

## Next Recommended Runtime UOW

**UOW-2602 candidate: fix seller private-store notification ordering from live `CM_BUY_ITEM` action `0`.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: seller sale notification packets should be sent from the same per-bought-item point Java uses, before the buyer/seller kinah transfer packets.
- Java source method or runtime path: PrivateStoreService.sellStoreItem per-item loop -> decreaseItemFromPlayer -> ItemService.addItem -> PacketSendUtility.sendPacket(seller, STR_MSG_PERSONAL_SHOP_SELL_ITEM[_MULTI]) -> buyer.decreaseKinah -> seller.increaseKinah.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync seller packet fanout ordering for seller messages and kinah packets.
- Client-visible/state effect expected: seller receives sale system messages before seller kinah add/update packets for live private-store purchases, with multi-row messages preserving bought-item order.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real server packets emitted by live CM_BUY_ITEM.
```

Suggested focused validation if this UOW includes a runtime fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Behavior to prove: seller sale messages precede seller kinah add/update packets in direct seller packet order for live
`CM_BUY_ITEM`, including a multi-row purchase with two messages.

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet output changes
if a runtime fix is required; start focused and document any broad skip.

## Safe Runtime Candidates

- Seller sale notification ordering from live private-store `CM_BUY_ITEM`.
- Mixed present/missing private-store purchase branch if discovery finds a live state or packet mismatch beyond UOW-2598.
- Private-store insufficient-kinah or stale seller-count denials only if they require a runtime send/state fix.
- Buyer inventory-full live branch coverage only if paired with a runtime fix; it was previously inspected as already wired.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.
- Java XML/static-data loading only when the data is immediately used by live code.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` sold-out single-item purchase is live as of UOW-2596.
- `CM_BUY_ITEM` player action `0` partial-stack/non-closing purchase is live as of UOW-2597, including seller pack-count decrement.
- `CM_BUY_ITEM` player action `0` missing seller inventory item branch is live as of UOW-2598, including Java's kinah-transfer behavior.
- `CM_BUY_ITEM` private-store packet fanout sends Java delete type and cube updates as of UOW-2599 for covered sold-out/new-item branches.
- `CM_BUY_ITEM` multi-row private-store purchase cube counts use per-item snapshots as of UOW-2600.
- `CM_BUY_ITEM` seller-without-kinah fanout sends Java's zero-count kinah add/cube before kinah update as of UOW-2601.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
