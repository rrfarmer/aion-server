# Phase 6 Session 2621 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2621: Execute `ABYSS` AP sell-to-shop action 1 live. See
[Phase-6-Session-2621-Completion.md](Phase-6-Session-2621-Completion.md).

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
- Current commit - `[Phase 6][UOW-2621] Execute abyss AP sell-to-shop live`

## Session Summary

- Java review confirmed `CM_BUY_ITEM.runImpl` action 1 routes `TradeNpcType.ABYSS` purchase templates to `TradeService.performSellForAPToShop`.
- Java `performSellForAPToShop` validates the purchase goods list, decreases inventory by object id, and awards AP through `AbyssPointsService.addAp`.
- C# live action 1 handling now executes ready `TradeSellForApToShopPlan` instances for exact-count item deletion.
- C# persists the updated abyss rank and sold item deletion through `SaveNpcShopApSellMutationAsync`.
- C# mutates live `Player.InventoryItems` and `Player.AbyssRank`, then sends delete/cube/AP-gain/rank packets.

## Files Changed In UOW-2621

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2621-Completion.md`
- `docs/Phase-6-Session-2621-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 116/116.

Java/Maven: not run. No narrow Java fixture was discovered for `TradeService.performSellForAPToShop`; Java behavior was verified by source review of `CM_BUY_ITEM` and `TradeService`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live handler/state/persistence boundary changed, but the focused command directly covered the edited AP sell dispatch, planner contract, service/repository handoff, and packet fanout while building affected projects.

Note: an initial focused run passed compilation but failed one existing assertion because the AP sell branch now sends live packets; the test was updated to assert the live persistence, state, and packet effects, and the final focused command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 1 now executes normal sell-to-shop and exact-count ABYSS AP sell-to-shop live. Repurchase action 2 remains disabled. |
| `com.aionemu.gameserver.services.TradeService#performSellForAPToShop` | `Aion.GameServer.Services.TradeSellForApToShopPlanService` plus `GameServerConnection.TryExecuteSellForApToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Exact-count AP item deletion executes live with AP gain, persistence, and packets. Partial-stack decrease is not yet modeled live. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService#addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` plus AP sell executor | Service/live state mutation | Partial | Unit Tested | Partial Parity | AP sell uses the existing C# add-AP plan and sends AP/rank packets. Rank side-effect breadth remains partial. |

## Known Gaps

- Live MySQL execution for AP sell-to-shop was not run.
- Real client validation was not run.
- Partial-stack AP sell is intentionally blocked by the live executor until seller item updates are modeled in `TradeSellForApToShopPlan`.
- Feature-disabled AP sell message remains disabled-output evidence only; this UOW covered the enabled exact-count success path.
- Java `performSellForAPToShop` continues after individual `decreaseByObjectId` failures; only the all-success exact-count path was executed live here.
- NPC repurchase action 2 remains disabled.

## Next Recommended Runtime UOW

**UOW-2622 candidate: execute NPC repurchase action 2 live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: `CM_BUY_ITEM` action 2 repurchase currently uses disabled `RepurchasePlan`/outcome records and does not restore items, decrease kinah, mutate repurchase state, persist inventory, or send live packets.
- Java source method or runtime path: CM_BUY_ITEM.runImpl action 2 -> RepurchaseService.repurchaseFromShop(Player, RepurchaseList), including canRepurchase, inventory.add, kinah decrease, removeRepurchaseItems, and inventory packet sends.
- C# runtime artifact to wire or fix: GameServerConnection action 2 repurchase executor, RepurchasePlanService, RepurchaseStatePlanService, PlayerEnterWorldService/repository persistence for kinah decrease and restored inventory rows.
- Client-visible/state/persistence effect expected: repurchasing an item decreases kinah, restores the repurchased item to the player inventory, removes it from repurchase state, persists inventory rows, and sends kinah/item/cube packets from live code.
- Why this is not preview-only/test-only/documentation-only if feasible: it executes a deferred client packet path with live inventory/kinah/repurchase mutation, persistence, and packet fanout.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java `RepurchaseService.repurchaseFromShop` fixture is discovered.

Broad-validation trigger: live handler/state/persistence boundary changes. Start focused on the repurchase handler and planner; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute NPC repurchase action 2 live.
- Model partial-stack AP sell item updates and then execute partial-stack AP sell live.
- Add live MySQL validation for NPC shop buy/sell persistence if an opt-in database fixture can be reused.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action 13 normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action 13 normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action 13 normal AP-cost buys execute live and persist as of UOW-2614.
- Successful `CM_BUY_ITEM` NPC action 13 normal limited-item buys mutate live limited counters as of UOW-2615.
- Limited-item counters reset from live scheduler callbacks as of UOW-2616.
- `CM_BUY_ITEM` NPC action 13 `ABYSS_KINAH` buys execute live and persist as of UOW-2617.
- `CM_BUY_ITEM` NPC action 13 `ABYSS` buys execute live without kinah as of UOW-2618.
- `CM_BUY_ITEM` NPC action 13 `REWARD` buys execute live without kinah as of UOW-2619.
- `CM_BUY_ITEM` NPC action 1 normal sell-to-shop executes live for covered whole-item sells as of UOW-2620.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop executes live for exact-count deletes as of UOW-2621.
- Denials for kinah, invalid goods, full inventory, limited item, AP, missing required items, negative AP, and normal not-sellable sell are live through UOW-2621.
- Buy/sell/repurchase planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
