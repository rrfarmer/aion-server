# Phase 6 Session 2612 Completion

## UOW

[Phase 6] UOW-2612: Persist NPC shop kinah buys live

## Status

Completed and validated with focused live handler and player-enter-world service coverage. Live `CM_BUY_ITEM` action
`13` for successful normal NPC shop kinah buys now persists the updated kinah row plus bought item updates/additions
through `PlayerEnterWorldService` before swapping in-memory inventory state or sending success packets.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: successful normal NPC shop kinah buys mutated live in-memory inventory/kinah and sent packets, but did not persist item/kinah state.
- Java source method or runtime path: TradeService.performBuyTransaction -> inventory.tryDecreaseKinah(...) and ItemService.addItem(...) with InventoryDAO persistence of dirty item rows.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync now calls PlayerEnterWorldService.SaveNpcShopBuyMutationAsync, backed by IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository.
- Client-visible/state/persistence effect changed: successful action 13 normal kinah shop buys save the kinah count and bought item rows using the existing inventory table shape before sending success packets.
- Why this is not preview-only/test-only/documentation-only: this persists live inventory state from the live client packet path.
```

## Java Source Reviewed

- `TradeService.performBuyTransaction` subtracts AP/kinah/required items before adding bought items.
- For normal kinah buys in this C# slice, Java calls `inventory.tryDecreaseKinah(tradeListPrice)` and then
  `ItemService.addItem(player, tradeItem.getItemId(), tradeItem.getCount(), true, new ItemUpdatePredicate(ItemAddType.BUY, ItemUpdateType.INC_ITEM_BUY))`.
- `ItemService.addItem` mutates `Storage` by increasing stackable rows or adding new item rows; Java inventory DAO
  persistence is the source for C# inventory-table writes.

## C# Changes

- Added `IPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` for updated bought stack rows, added item rows, and
  the updated kinah row.
- Implemented `MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` using the existing `inventory` table helpers
  in one transaction.
- Added `PlayerEnterWorldService.SaveNpcShopBuyMutationAsync`.
- Changed `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` so successful normal shop buys persist before
  replacing `player.InventoryItems` and before success packets are sent.
- Added live handler coverage for successful persistence capture and persistence-failure no-mutation/no-packet behavior.

## Known Gaps

- The new MySQL method is compile-covered and live-boundary-tested through the service fake, but no DB integration test
  executed the SQL this UOW.
- Successful AP/required-item NPC shop purchases remain disabled by the success-only live guard.
- Required-item/AP mutations and limited-item counter updates remain disabled.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only successful purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 successful kinah buy persistence boundary plus repository/service interface.
- Specific behavior/contract: successful normal NPC shop buy persists updated kinah, updated bought stacks, and newly added item rows before packets; persistence failure leaves in-memory inventory unchanged and sends no success packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore -> 97/97 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: persistence boundary and live state side effect changed.
- Broad .NET decision: skipped after focused live handler/service coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and covered the modified dispatch boundary plus interface implementations.
- Why this scope is sufficient: the passing live handler tests dispatch encoded CM_BUY_ITEM packets, assert the repository capture receives Java-shaped kinah/item rows, and assert repository failure prevents mutation/packets. SQL execution remains a documented residual risk because no repository DB fixture exists for this path.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Normal kinah success now mutates, sends packets, and calls persistence; AP/required-item successes, audit logging, limited counters, and non-normal shop types remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemService#addItem` | `Aion.GameServer.Services.InventoryAddService` plus `PlayerEnterWorldService.SaveNpcShopBuyMutationAsync` | Service/persistence path | Partial | Unit Tested | Partial Parity | Live normal shop path persists stack updates and added rows after C# add-plan application; Java overflow/remaining-count edge cases beyond this scoped buy path still need verification. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` | Repository | Partial | Unit Tested/Compile Covered | Needs Verification | Uses existing inventory table update/insert helpers in one transaction; no DB integration test executed this specific SQL path. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopPersistsSuccessfulNormalKinahPurchaseBeforePackets` | Unit/live handler | `TradeService.performBuyTransaction` success branch and `ItemService.addItem` stack/new-row behavior | Ordinary action 13 packet persists kinah, one updated stack, and one added item before sending success packets | Java source review + live C# handler assertion | Uses fake repository capture; does not execute MySQL. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopPersistenceFailureStopsMutationAndPackets` | Unit/live handler | Java persistence side effects must be committed before client-visible success is trusted | Repository failure leaves player inventory unchanged and sends no success packets | C# live handler assertion tied to Java transaction intent | Java failure behavior for DB write errors was not runtime-compared. |

## Summary Metrics

- Focused UOW validation: 97 tests passed.
- Runtime progress: live NPC action `13` normal kinah shop buy now persists inventory/kinah mutations through the
  existing player repository boundary.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: SQL integration evidence for this path, successful AP/required-item mutation, AP/token shop
  purchases, limited-item counter updates, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- SQL execution for `SaveNpcShopBuyMutationAsync` is not covered by a live database fixture.
- The C# handler persists before mutating memory/sending packets; Java mutates storage inline and relies on dirty-item
  persistence. This ordering is conservative for C# failure safety but still needs real DB/client validation.
- Successful limited-item counter mutation/persistence remains missing.
- AP and required-item success branches still return before live mutation.

## Next Runtime Candidate

UOW-2613 candidate: execute successful normal NPC shop required-item consumption for kinah-only trade lists with
required item metadata, if scoped without AP mutation.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: successful normal NPC shop buys with required item costs are planned but blocked by the live success guard because RequiredItems.Count != 0.
- Java source method or runtime path: TradeService.performBuyTransaction -> tradeList.getRequiredItems() loop -> player.getInventory().decreaseByItemId(...) before ItemService.addItem.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync plus PlayerEnterWorldService/Repository persistence for required-item decreases/deletes and bought item rows.
- Client-visible/state/persistence effect expected: successful action 13 normal shop buy consumes required item stacks, persists those changes with kinah/bought item rows, sends inventory update/delete/add packets, and does not run AP mutation.
- Why this is not preview-only/test-only/documentation-only if feasible: it mutates and persists live inventory state from the client packet path.
```
