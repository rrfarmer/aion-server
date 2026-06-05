# Phase 6 Session 2622 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2622: Execute NPC repurchase action 2 live. See
[Phase-6-Session-2622-Completion.md](Phase-6-Session-2622-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- `1daf029` - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`
- `c015455` - `[Phase 6][UOW-2613] Consume NPC shop required items live`
- `40ea371` - `[Phase 6][UOW-2614] Spend NPC shop abyss points live`
- `b848bbb` - `[Phase 6][UOW-2615] Mutate NPC shop limited counters live`
- `867b01a` - `[Phase 6][UOW-2616] Schedule NPC shop limited resets live`
- `3507068` - `[Phase 6][UOW-2617] Execute NPC shop abyss kinah buys live`
- `36b6746` - `[Phase 6][UOW-2618] Execute NPC shop abyss buys live`
- `a56bdbb` - `[Phase 6][UOW-2619] Execute NPC shop reward buys live`
- `816199d` - `[Phase 6][UOW-2620] Execute normal NPC sell-to-shop live`
- `46397e7` - `[Phase 6][UOW-2621] Execute abyss AP sell-to-shop live`
- Current commit - `[Phase 6][UOW-2622] Execute NPC shop repurchase live`

## Session Summary

- Java review confirmed `CM_BUY_ITEM.runImpl` action 2 routes NPC `canBuy()` repurchase requests to `RepurchaseService.repurchaseFromShop`.
- Java `repurchaseFromShop` checks can-trade, sends the inventory-full message, decreases Kinah, restores the repurchased item through `ItemService.addItem`, and removes the item from the runtime repurchase set.
- C# live buy-item handling now admits NPC action 2 packets and executes ready `RepurchasePlan` instances.
- C# persists Kinah decrease and restored item updates/inserts through `SaveNpcShopRepurchaseMutationAsync`.
- C# mutates `Player.InventoryItems` and `Player.RepurchaseItems`, then sends Kinah decrease plus item/cube packets.

## Files Changed In UOW-2622

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2622-Completion.md`
- `docs/Phase-6-Session-2622-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 125/125.

Java/Maven: not run. No narrow Java fixture was discovered for `RepurchaseService.repurchaseFromShop`; Java behavior was verified by source review of `CM_BUY_ITEM` and `RepurchaseService`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live handler/state/persistence boundary changed, but the focused command directly covered the edited repurchase dispatch, planner contract, service/repository handoff, and packet fanout while building affected projects.

Note: an initial focused run built product code but failed test compilation because `Assert.NotNull` returned void in this xUnit version. The test assertion was corrected and the final focused command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 2 repurchase now executes live for the covered success path. Pet sell action 17 and broader real-client validation remain gaps. |
| `com.aionemu.gameserver.services.RepurchaseService#repurchaseFromShop` | `Aion.GameServer.Services.RepurchasePlanService` plus `GameServerConnection.TryExecuteRepurchaseAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Successful restoration, Kinah decrease, runtime repurchase removal, and item/cube packet fanout execute live. Audit logging for insufficient Kinah and live MySQL are not validated. |
| `com.aionemu.gameserver.services.item.ItemService#addItem` | `Aion.GameServer.Services.InventoryAddService.CreateAddItemPlan` plus repurchase executor | Service/live inventory mutation | Partial | Unit Tested | Partial Parity | Repurchase consumes existing add-plan behavior for clone/merge restoration. Broader ItemService side effects remain partial. |

## Known Gaps

- Live MySQL execution for repurchase was not run.
- Real client validation was not run.
- Java audit logging for insufficient-Kinah repurchase remains modeled but not emitted through live C# audit infrastructure.
- C# still reports disabled side-effect outcome records to existing observers for diagnostics; live side effects are executed independently by the handler.
- Broader `ItemService.addItem` side effects remain partial outside the restored inventory row and packet path covered here.
- Partial-stack AP sell remains blocked from UOW-2621.

## Next Recommended Runtime UOW

**UOW-2623 candidate: execute partial-stack AP sell-to-shop live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: `CM_BUY_ITEM` action 1 `ABYSS` sell-to-shop currently executes exact-count AP item deletes live but intentionally returns for partial-stack item counts because item count updates are not modeled in the live executor.
- Java source method or runtime path: CM_BUY_ITEM.runImpl action 1 ABYSS branch -> TradeService.performSellForAPToShop(Player, TradeList, TradeListTemplate), especially inventory.decreaseByObjectId for counts smaller than the seller stack.
- C# runtime artifact to wire or fix: TradeSellForApToShopPlanService and GameServerConnection.TryExecuteSellForApToShopAsync must carry and apply seller item count updates, then persist them through SaveNpcShopApSellMutationAsync and send SmInventoryUpdateItem decrease packets.
- Client-visible/state/persistence effect expected: selling part of an AP item stack decreases the live inventory count, awards AP, persists the updated item and abyss rank, and sends item-update/AP/rank packets without deleting the stack.
- Why this is not preview-only/test-only/documentation-only if feasible: it extends an already-live client packet path with additional inventory mutation, persistence, and packet fanout for a Java runtime branch.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java `TradeService.performSellForAPToShop` fixture is discovered.

Broad-validation trigger: live handler/state/persistence boundary changes. Start focused on the AP sell handler and planner; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute partial-stack AP sell-to-shop live.
- Add live MySQL validation for NPC shop buy/sell/repurchase persistence if an opt-in database fixture can be reused.
- Execute pet merchant action 17 sell-to-shop live once pet merchant runtime facts are available.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action 13 normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action 13 normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action 13 normal AP-cost buys execute live and persist as of UOW-2614.
- Successful `CM_BUY_ITEM` NPC action 13 normal limited-item buys mutate live limited counters as of UOW-2615.
- Limited-item counters reset from live scheduler callbacks as of UOW-2616.
- `CM_BUY_ITEM` NPC action 13 `ABYSS_KINAH` buys execute live and persist as of UOW-2617.
- `CM_BUY_ITEM` NPC action 13 `ABYSS` buys execute live without Kinah as of UOW-2618.
- `CM_BUY_ITEM` NPC action 13 `REWARD` buys execute live without Kinah as of UOW-2619.
- `CM_BUY_ITEM` NPC action 1 normal sell-to-shop executes live for covered whole-item sells as of UOW-2620.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop executes live for exact-count deletes as of UOW-2621.
- `CM_BUY_ITEM` NPC action 2 repurchase executes live for the covered success path as of UOW-2622.
- Denials for Kinah, invalid goods, full inventory, limited item, AP, missing required items, negative AP, normal not-sellable sell, and repurchase inventory-full plan messages are covered through UOW-2622.
- Buy/sell/repurchase planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
