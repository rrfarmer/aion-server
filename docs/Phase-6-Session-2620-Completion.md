# Phase 6 Session 2620 Completion

## UOW

[Phase 6] UOW-2620: Execute normal NPC sell-to-shop action 1 live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `CM_BUY_ITEM` action 1 normal sell-to-shop previously created disabled outcome records without mutating inventory, kinah, repurchase state, persistence, or packets.
- Java source/runtime path: CM_BUY_ITEM.runImpl action 1 -> TradeService.performSellToShop(Player, TradeList, TradeListTemplate, sellModifier), including item delete/decrease, RepurchaseService.addRepurchaseItems, and inventory.increaseKinah(... INC_KINAH_SELL).
- C# runtime artifact wired: GameServerConnection.HandleBuyItemAsync now dispatches normal sell-to-shop plans to a live executor; PlayerEnterWorldService and repository persist sold-item and kinah inventory rows.
- Client-visible/state/persistence effect: selling a normal item to an NPC removes or decreases the sold item, increases kinah, replaces the live player repurchase snapshot, persists inventory rows, and sends delete/cube/update packets from live code.
- Why this is runtime progress: this UOW executes a deferred client packet path with live player inventory mutation, repurchase state mutation, database-shaped persistence, and server packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`
  - `runImpl` action 1 NPC sell branch.
- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performSellToShop(Player, TradeList, TradeListTemplate)`
  - `performSellToShop(Player, TradeList, TradeListTemplate, int)`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `ItemUpdateType.INC_KINAH_SELL`.

## C# Changes

- `GameServerConnection.HandleBuyItemAsync`
  - Treats NPC action 1 as a live packet path even when diagnostic observers are absent.
  - Dispatches normal sell-to-shop plans after private-store execution and before buy-from-shop execution.
- `GameServerConnection.TryExecuteSellToShopAsync`
  - Applies Java normal sell-to-shop side effects for ready `TradeSellToShopPlan` instances.
  - Sends Java not-sellable system message for `BlockedNotSellable`.
  - Mutates `Player.InventoryItems` and replaces `Player.RepurchaseItems` after persistence succeeds.
  - Sends `SmDeleteItem`, `SmCubeUpdate`, and `SmInventoryUpdateItem` with Java `INC_KINAH_SELL` semantics.
- `PlayerEnterWorldService` / `PlayerEnterWorldRepository`
  - Added `SaveNpcShopSellMutationAsync` to persist sold item updates/deletes and the kinah increase inside one transaction.
- `SmInventoryUpdateItem`
  - Added `IncreaseKinahSell = 0x20` from Java `ItemUpdateType.INC_KINAH_SELL`.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcSellActionExecutesNormalSellToShopLive` | Unit/live handler | `CM_BUY_ITEM.runImpl` action 1 and `TradeService.performSellToShop` | Live NPC action 1 whole-item sell persists sold item deletion and kinah update, mutates player inventory, replaces repurchase items, and sends delete/cube/kinah update packets. | Java source review + focused C# live handler assertions. | Uses fake repository capture; no live MySQL or real client validation. |
| `ProcessPacketAsync_CmBuyItemNpcSellActionBlocksDisabledNormalSellPlanWhenTemplateMaskIsNotSellable` | Unit/live handler denial | `TradeService.performSellToShop -> !item.isSellable()` | Not-sellable normal sell now sends the Java system message from live code instead of remaining silent. | Java source review + focused C# packet assertion. | Packet parameter uses the currently available empty-name fallback; item l10n text is not yet modeled. |
| Existing `TradeSellToShopPlanServiceTests` | Unit/planner consumed live | `TradeService.performSellToShop` | Confirms sell reward, whole-item delete, partial-stack plan, purchase-template validation, sell-limit count, and blocked cases used by the live executor. | Java source review + focused C# assertions. | Planner-only evidence counts here only because the plan is now consumed by live handler code. |

## Validation Decision

```text
- Changed surface: live handler/state/persistence boundary plus inventory packet update type.
- Specific behavior/contract: normal NPC action 1 sell-to-shop deletes a sold item, increases kinah with Java INC_KINAH_SELL, replaces repurchase state, persists inventory rows, and sends live packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for `TradeService.performSellToShop`.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/planner/service coverage because the command built the affected project and directly covered the edited dispatch, persistence handoff, planner contract, and packet update type.
- Why this scope is sufficient: the focused tests exercise the new live action 1 branch, sold-item delete persistence capture, kinah persistence capture, player inventory mutation, repurchase snapshot replacement, not-sellable live denial, and Java sell packet update type constant.
```

Result: passed, 120/120.

Notes:
- An initial focused run failed at compile because `PlayerEnterWorldServiceTests.CapturingEnterWorldRepository` needed the new repository method; the test double was updated.
- A second focused run passed compilation but failed one existing assertion because the not-sellable branch now sends Java's live denial packet; the assertion was updated and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 1 normal sell-to-shop now executes live for covered whole-item sell and not-sellable denial. ABYSS sell-for-AP remains disabled. |
| `com.aionemu.gameserver.services.TradeService#performSellToShop` | `Aion.GameServer.Services.TradeSellToShopPlanService` plus `GameServerConnection.TryExecuteSellToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Whole-item normal sell executes live with item deletion, kinah increase, repurchase snapshot replacement, persistence, and packets. Partial-stack and newly-created kinah rows are planned and wired but not live-handler tested in this UOW. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType#INC_KINAH_SELL` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.IncreaseKinahSell` | Packet update type | Complete | Unit Tested | Partial Parity | Numeric value `0x20` is ported and used by the live sell-to-shop kinah update packet; no Java golden packet was run in this UOW. |
| `com.aionemu.gameserver.services.RepurchaseService#addRepurchaseItems` | `Aion.GameServer.Model.GameObjects.Player.RepurchaseItems` plus sell live executor | Runtime state | Partial | Unit Tested | Partial Parity | Live sell replaces the player's in-memory repurchase snapshot. Java HashSet iteration order and full `SM_REPURCHASE` behavior remain partial. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 6.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live MySQL execution for NPC sell-to-shop was not run.
- Real client validation was not run.
- Partial-stack sell and missing-kinah-row creation are wired through the same live executor but were not live-handler tested in this UOW.
- Not-sellable system-message item l10n text is not yet modeled; the live packet currently uses an empty string parameter.
- Java `RepurchaseService` is represented as a player snapshot, not a full singleton service with Java HashSet iteration behavior.
- `ABYSS` AP sell-to-shop action 1 remains disabled.
