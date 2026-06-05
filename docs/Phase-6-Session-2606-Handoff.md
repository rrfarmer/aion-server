# Phase 6 Session 2606 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2606: Execute NPC shop kinah buys live. See
[Phase-6-Session-2606-Completion.md](Phase-6-Session-2606-Completion.md).

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
- `60a17a2` - `[Phase 6][UOW-2603] Interleave private-store buyer fanout live`
- `b3a0d9c` - `[Phase 6][UOW-2604] Persist private-store purchases live`
- `4fe0644` - `[Phase 6][UOW-2605] Log private-store sales live`
- Current commit - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`

## Session Summary

- Java review confirmed `CM_BUY_ITEM` action `13` dispatches NPC buy-from-shop through
  `TradeService.performBuyFromShop`.
- Java review confirmed `TradeService.performBuyTransaction` spends kinah before adding bought items and uses
  `ItemAddType.BUY` / `ItemUpdateType.INC_ITEM_BUY`.
- C# previously built disabled action-13 plans but ordinary no-observer live `CM_BUY_ITEM` packets were dropped unless
  they were private-store packets.
- `GameServerConnection.HandleBuyItemAsync` now admits ordinary NPC action `13` packets to the live path.
- `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` now executes the first safe live slice: action `13`,
  `NORMAL` NPC trade list, kinah-only, stackable item purchase.
- C# now sends Java buy masks `0x1C` for bought item add/update packet paths.

## Files Changed In UOW-2606

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2606-Completion.md`
- `docs/Phase-6-Session-2606-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM`
- `com.aionemu.gameserver.services.TradeService#performBuyFromShop`
- `com.aionemu.gameserver.services.TradeService#performBuyTransaction`
- `com.aionemu.gameserver.model.trade.TradeList`
- `com.aionemu.gameserver.services.item.ItemPacketService`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 328/328.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler/planner/packet coverage. The filtered command built `Aion.GameServer`
and `Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM` action `13` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live handler | Partial | Unit Tested | Partial Parity | Normal kinah action 13 can execute live; AP/reward/required-item/denial branches remain incomplete. |
| `com.aionemu.gameserver.services.TradeService#performBuyFromShop` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers `NORMAL` kinah-only buy path; `ABYSS_KINAH`, `ABYSS`, `REWARD`, limited counters, and persistence remain missing. |
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Services.TradeBuyTransactionPlanService` + live handler application | Service/planner/live application | Partial | Unit Tested | Partial Parity | Existing planner supplies Java-derived costs and validation; live handler applies only the safe kinah/item subset. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType#BUY` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.Buy` | Packet mask | Complete | Unit Tested | Partial Parity | Numeric mask `0x1C` is pinned; full buy packet byte golden remains broader packet coverage. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType#INC_ITEM_BUY` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.IncreaseItemBuy` | Packet mask | Complete | Unit Tested | Partial Parity | Numeric mask `0x1C` is pinned; live stack-update branch is implemented but not separately covered this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopExecutesNormalKinahPurchaseWithoutObservers` | Unit/live handler | `CM_BUY_ITEM.runImpl` + `TradeService.performBuyTransaction` + `ItemPacketService` | Ordinary no-observer action 13 packet mutates kinah/item state and sends DEC_KINAH_BUY, BUY add, and cube update packets | Java source review + live C# handler assertion | Does not cover persistence, AP/token costs, limited counters, or denial packets. |
| `GamePacketTests` buy mask assertions | Unit/packet | `ItemPacketService.ItemAddType.BUY` and `ItemUpdateType.INC_ITEM_BUY` | Pins both C# buy packet masks to Java `0x1C` | Java source review + C# constant assertion | Not a full packet golden for the buy packets. |

## Known Gaps

- Action `13` live execution is limited to `TradeNpcType.NORMAL`, kinah-only, no AP/token requirements.
- No buy-from-shop inventory/kinah persistence is wired yet.
- Limited-item counters are not updated.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy paths remain non-live.
- Buy-from-shop denial packets remain disabled intents only.
- Existing buy-from-shop diagnostic outcome plans still report disabled side effects; they are now historical/planner
  evidence, not the live success executor's status.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Persistence Adapter First

- No generic existing inventory mutation persistence surface was available for arbitrary shop buys.
- Adding a new repository contract before proving live state/packet execution would have made this UOW larger and less
  directly client-visible.

### AP/Token Shops

- Java AP/reward shops require AP mutation, required-item consumption, denial packets, and limited counters.
- The selected first slice keeps one live branch small enough to validate directly.

## Next Recommended Runtime UOW

**UOW-2607 candidate: send the live not-enough-kinah denial packet for NPC action `13` normal shop buys.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: blocked NPC shop buys should send Java denial packets from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> if useKinah && !tradeList.calculateBuyListPrice -> SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync or adjacent action-13 denial helper using TradeBuyTransactionPlanStatus.BlockedNotEnoughKinah.
- Client-visible/state/persistence effect expected: live action 13 with insufficient kinah sends a real SM_SYSTEM_MESSAGE denial and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```

Suggested focused validation if this UOW includes the denial packet:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live packet side effect
changed; start focused and document any broad skip.

## Safe Runtime Candidates

- Live not-enough-kinah denial packet for action `13` normal shop buys.
- Live invalid-goods/full-inventory/limited-item denial packets for action `13`, one branch per UOW.
- Live action `13` existing-stack update branch coverage only if paired with a runtime fix discovered in packet/order/state.
- Buy-from-shop persistence only if an existing repository surface can be reused or a minimal Java-shaped inventory store
  contract is implemented and consumed by the live handler in the same UOW.
- `ABYSS_KINAH`, `ABYSS`, or `REWARD` buy execution only after AP/required-item mutation and packet effects are scoped
  from Java source.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` private-store purchase behavior has been progressively wired through UOW-2596 to
  UOW-2605, including packet order, persistence, and logging.
- `CM_BUY_ITEM` NPC action `13` normal kinah stackable buy executes live as of UOW-2606.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
