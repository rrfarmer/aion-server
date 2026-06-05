# Phase 6 Session 2600 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2600: Snapshot private-store cube updates per bought item. See
[Phase-6-Session-2600-Completion.md](Phase-6-Session-2600-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- Current commit - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`

## Session Summary

- Java review confirmed `PrivateStoreService.sellStoreItem` performs seller item decrease and buyer item add inside the
  per-bought-item loop.
- C# private-store purchase execution previously applied all seller deletes and buyer adds before packet fanout, so
  multi-row purchases reused final cube counts for every `SmCubeUpdate`.
- Live C# purchase execution now uses projected cube count snapshots for seller deletes and buyer adds.

## Files Changed In UOW-2600

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2600-Completion.md`
- `docs/Phase-6-Session-2600-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.model.items.storage.Storage#decreaseItemCount`
- `com.aionemu.gameserver.model.items.storage.Storage#add`
- `com.aionemu.gameserver.services.item.ItemPacketService#sendItemDeletePacket`
- `com.aionemu.gameserver.services.item.ItemPacketService#sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE#cubeSize`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
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
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Multi-row purchase cube count snapshots now follow Java per-bought-item fanout for covered seller delete and buyer add branches; global cross-recipient packet interleaving and persistence remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage#decreaseItemCount` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` seller deleted-item fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | Exhausted seller cube rows now send stepped delete cube counts for multi-delete purchases. |
| `com.aionemu.gameserver.model.items.storage.Storage#add` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` buyer added-item fanout | Packet/fanout | Partial | Unit Tested | Partial Parity | New buyer cube rows now send stepped add cube counts for multi-add purchases. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Packet | Partial | Unit Tested | Partial Parity | Snapshot helper was reused to model Java fanout-point inventory size; broader storage families are outside this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `PrivateStoreService.sellStoreItem`, `Storage.decreaseItemCount`, `Storage.add`, `ItemPacketService` | Two-row private-store purchase sends seller cube counts `1, 0` and buyer cube counts `1, 2` from live `CM_BUY_ITEM` | Java source review + live C# handler assertion | Does not verify real client behavior or global cross-recipient send interleaving. |

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Seller-without-kinah fanout likely differs from Java: Java creates a zero-count kinah item through `Storage.add`, which
  sends add/cube before the kinah increase update; C# currently sends only the update.
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

### Final-State Cube Snapshot For Multi-Row Private Store Purchase

- Java sends cube updates from inside the per-bought-item loop after each seller delete and buyer add.
- C# had been applying all item mutations before sending the packet fanout, so final-state cube snapshots were not
  Java-equivalent for multi-row purchases.

## Next Recommended Runtime UOW

**UOW-2601 candidate: fix seller-without-kinah private-store purchase fanout from live `CM_BUY_ITEM` action `0`.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: private-store seller kinah gain when the seller has no existing kinah item should emit Java's kinah add/cube packet pair before the kinah increase update.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> seller.getInventory().increaseKinah(price) -> Storage.increaseKinah -> Storage.add(ItemFactory.newItem(KINAH, 0)) -> ItemPacketService.sendStorageUpdatePacket -> Storage.increaseItemCount -> ItemPacketService.sendItemPacket.
- C# runtime artifact to wire or fix: PrivateStorePurchasePlanService seller kinah plan metadata and GameServerConnection.TryExecutePrivateStorePurchaseAsync seller kinah packet fanout.
- Client-visible/state effect expected: seller receives the Java-equivalent kinah add/cube packets before SmInventoryUpdateItem IncreaseKinahCollect when no kinah row existed before the sale.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real server packets emitted by live CM_BUY_ITEM and mutates/uses live seller inventory state.
```

Suggested focused validation if this UOW includes a runtime fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Behavior to prove: a seller who begins with no kinah row receives Java-equivalent add/cube before seller kinah update
from live `CM_BUY_ITEM`; a seller with an existing kinah row should keep the current update-only fanout.

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet output changes
if a runtime fix is required; start focused and document any broad skip.

## Safe Runtime Candidates

- Seller-without-kinah private-store fanout from live `CM_BUY_ITEM`.
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
- `CM_BUY_ITEM` multi-row private-store purchase cube counts now use per-item snapshots as of UOW-2600.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
