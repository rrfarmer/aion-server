# Phase 6 Session 2602 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2602: Send private-store seller messages before kinah packets. See
[Phase-6-Session-2602-Completion.md](Phase-6-Session-2602-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- `d15dc2f` - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`
- `8b85fa6` - `[Phase 6][UOW-2601] Send private-store seller kinah add live`
- Current commit - `[Phase 6][UOW-2602] Order private-store seller messages live`

## Session Summary

- Java review confirmed seller private-store sale messages are sent inside the per-bought-item loop before the final
  buyer/seller kinah transfer.
- C# previously sent seller kinah packets before seller sale messages.
- Live C# private-store execution now sends seller item delete/update packets and the corresponding seller message per
  applied bought item before seller kinah fanout.

## Files Changed In UOW-2602

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2602-Completion.md`
- `docs/Phase-6-Session-2602-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
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
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Direct seller item/message/kinah order now matches covered Java order; buyer item fanout is still not globally interleaved before seller messages. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` private-store sale messages | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` private-store sale messages | Packet/fanout | Partial | Unit Tested | Partial Parity | Covered only for sale message placement relative to seller item and kinah packets. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Sold-out seller message is sent before seller kinah add/update packets | Java source review + live C# handler assertion | Does not prove global buyer/seller interleaving. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreMultiAddUsesPerItemCubeSnapshots` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Multi-row seller messages are interleaved with seller delete/cube rows before seller kinah packets | Java source review + live C# handler assertion | Buyer add packets still use a separate sink and are not globally interleaved before messages. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStorePartialStackKeepsStoreOpenAndDecrementsPackCount` | Unit/live handler | `PrivateStoreService.sellStoreItem` | Partial-stack seller message is sent after seller item update and before seller kinah packets | Java source review + live C# handler assertion | Does not cover existing seller kinah normal sale. |

## Known Gaps

- Inventory/kinah/store persistence is still not wired for private-store purchases.
- Full cross-recipient packet interleaving still differs from Java: Java sends buyer item add/update before each seller
  sale message; C# still sends seller item/message fanout before buyer item fanout.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- Insufficient-kinah denial, stale seller item counts, missing item templates, and mixed present/missing seller item
  behavior still need live branch coverage or fixes.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution
  surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Seller Kinah Before Sale Message

- Java sends `STR_MSG_PERSONAL_SHOP_SELL_ITEM` or `STR_MSG_PERSONAL_SHOP_SELL_ITEM_MULTI` inside the per-item sale loop.
- Java sends buyer and seller kinah updates only after that loop.
- C# seller-kinah-before-message order was therefore not Java-equivalent for live private-store purchases.

## Next Recommended Runtime UOW

**UOW-2603 candidate: preserve per-bought-item buyer item fanout before seller messages from live `CM_BUY_ITEM`.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: buyer item add/update packets should be emitted before each seller sale message from the same bought-item loop point Java uses.
- Java source method or runtime path: PrivateStoreService.sellStoreItem -> decreaseItemFromPlayer -> ItemService.addItem(buyer, item, count) -> PacketSendUtility.sendPacket(seller, STR_MSG_PERSONAL_SHOP_SELL_ITEM[_MULTI]).
- C# runtime artifact to wire or fix: PrivateStorePurchasePlanService per-bought-item buyer packet grouping and GameServerConnection.TryExecutePrivateStorePurchaseAsync buyer/seller fanout order.
- Client-visible/state effect expected: live CM_BUY_ITEM emits seller item packet(s), buyer item add/update packet(s), then seller sale message for each applied bought item, while keeping final kinah packets after all item/message fanout.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes real server packets emitted by live CM_BUY_ITEM and may add runtime plan metadata only to drive that live fanout.
```

Suggested focused validation if this UOW includes a runtime fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Behavior to prove: a live private-store purchase emits buyer item add/update packets before the corresponding seller
sale message for each applied bought item; final buyer/seller kinah packets remain after item/message fanout.

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet output changes
if a runtime fix is required; start focused and document any broad skip.

## Safe Runtime Candidates

- Per-bought-item buyer item fanout before seller sale messages from live private-store `CM_BUY_ITEM`.
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
- `CM_BUY_ITEM` seller sale messages now precede seller kinah packets as of UOW-2602.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
