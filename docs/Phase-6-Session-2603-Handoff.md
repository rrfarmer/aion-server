# Phase 6 Session 2603 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2603: Interleave private-store buyer item fanout live. See
[Phase-6-Session-2603-Completion.md](Phase-6-Session-2603-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- `d15dc2f` - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`
- `8b85fa6` - `[Phase 6][UOW-2601] Send private-store seller kinah add live`
- `e5ab413` - `[Phase 6][UOW-2602] Order private-store seller messages live`
- Current commit - `[Phase 6][UOW-2603] Interleave private-store buyer fanout live`

## Session Summary

- Java review confirmed the private-store sale loop adds items to the buyer before sending the seller sale message.
- C# previously retained only aggregate buyer item packet lists, so live fanout could not preserve the Java per-bought-item
  cross-recipient order.
- `PrivateStorePurchasePlanService` now records buyer added/updated items per bought item.
- `GameServerConnection.TryExecutePrivateStorePurchaseAsync` now sends seller item packets, buyer item packets, then the
  seller sale message per applied bought item, with kinah packets still after item/message fanout.

## Files Changed In UOW-2603

- `dotnetConversion/src/Aion.GameServer/Services/PrivateStorePurchasePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2603-Completion.md`
- `docs/Phase-6-Session-2603-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.services.item.ItemService#addItem`
- `com.aionemu.gameserver.model.gameobjects.player.Storage#add`
- `com.aionemu.gameserver.services.item.ItemPacketService#sendItemPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService#sendStorageUpdatePacket`

## C# Artifacts Touched

- `Aion.GameServer.Services.PrivateStorePurchasePlanService`
- `Aion.GameServer.Services.PrivateStorePurchaseBuyerItemFanout`
- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
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
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Covered item/message/kinah ordering now follows the Java loop for sold-out and multi-row purchases. |
| `com.aionemu.gameserver.services.item.ItemService#addItem` | `Aion.GameServer.Services.PrivateStorePurchaseBuyerItemFanout` plus live handler fanout | Runtime inventory fanout | Partial | Unit Tested | Partial Parity | Per-bought-item buyer add/update groups drive live packets before seller messages; persistence and full storage semantics remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPacketService#sendItemPacket` / `sendStorageUpdatePacket` | `SmInventoryAddItem`, `SmInventoryUpdateItem`, `SmCubeUpdate` from `GameServerConnection` | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered buyer add/cube packets are interleaved before seller sale messages. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Sold-out purchase sends seller delete/cube, buyer add/cube, seller sale message, then kinah packets | Java source review + combined packet event assertion | Does not cover buyer existing-stack update. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Multi-row purchases repeat seller item, buyer item, seller message order per bought item | Java source review + combined packet event assertion | Covers one buyer added item per bought row. |

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Buyer existing-stack update fanout is supported by grouped metadata but lacks a focused live timeline branch.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- Insufficient-kinah denial, stale seller item counts, missing item templates, and mixed present/missing seller item
  behavior still need live branch coverage or fixes where discovery finds a runtime mismatch.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution
  surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Buyer Item Fanout As Aggregate Packets

- Java emits buyer item add/update before the seller sale message inside each bought-item loop.
- C# aggregate buyer fanout after seller messages was therefore not Java-equivalent for live private-store purchases.
- Grouping the buyer fanout by bought item was required before the live handler could preserve that runtime order.

## Next Recommended Runtime UOW

**UOW-2604 candidate: wire private-store purchase persistence for live inventory/kinah mutations using existing C#
database shape, if discovery finds a safe repository/service surface.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: live private-store purchase inventory, kinah, and store mutations should be persisted/restored instead of remaining memory-only.
- Java source method or runtime path: PrivateStoreService.sellStoreItem inventory/kinah mutations through Storage/Inventory persistence paths.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync and the existing inventory/character persistence repository or service discovered in dotnetConversion.
- Client-visible/state/persistence effect expected: bought/sold item and kinah changes survive the existing save/reload path or write through the existing database rows used by runtime inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it writes live handler mutations through the existing runtime persistence path.
```

Suggested focused validation if this UOW includes a runtime persistence fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~Inventory|FullyQualifiedName~Persistence" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live persistence writes
change if a runtime fix is required; start focused and document any broad skip.

## Safe Runtime Candidates

- Private-store purchase persistence for item/kinah/store mutations through an existing live repository/service surface.
- Mixed present/missing private-store purchase branch if discovery finds a live state or packet mismatch beyond UOW-2598.
- Private-store insufficient-kinah or stale seller-count denials only if they require a runtime send/state fix.
- Buyer inventory-full branch only if paired with a runtime fix; it was previously inspected as already wired.
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
- `CM_BUY_ITEM` seller sale messages precede seller kinah packets as of UOW-2602.
- `CM_BUY_ITEM` buyer item add/update packets precede the corresponding seller sale message as of UOW-2603.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
