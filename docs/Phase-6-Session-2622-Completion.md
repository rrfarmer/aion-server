# Phase 6 Session 2622 Completion

## UOW

[Phase 6] UOW-2622: Execute NPC repurchase action 2 live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_BUY_ITEM action 2 repurchase previously stopped at disabled plan/outcome evidence and did not restore items, decrease Kinah, mutate repurchase state, persist rows, or send live packets.
- Java source/runtime path: CM_BUY_ITEM.runImpl action 2 -> RepurchaseService.repurchaseFromShop(Player, RepurchaseList), including PlayerRestrictions.canTrade, inventory-full message, repurchase item lookup, tryDecreaseKinah, ItemService.addItem, and removal from the repurchase set.
- C# runtime artifact wired: GameServerConnection now admits action 2 NPC packets into live handling and executes RepurchasePlan through PlayerEnterWorldService/repository persistence.
- Client-visible/state/persistence effect: successful repurchase decreases Kinah, restores the item to inventory, removes the item from Player.RepurchaseItems, persists Kinah/restored item rows, and sends Kinah/item/cube packets.
- Why this is runtime progress: this UOW executes a deferred client packet path with live inventory, Kinah, runtime repurchase-state mutation, persistence, and packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`
  - `readImpl` action 2 builds `RepurchaseList`.
  - `runImpl` action 2 calls `RepurchaseService.getInstance().repurchaseFromShop(player, repurchaseList)` when the target NPC can buy.
- `game-server/src/com/aionemu/gameserver/services/RepurchaseService.java`
  - `repurchaseFromShop` applies the can-trade guard, inventory-full packet, Kinah decrease, `ItemService.addItem`, and repurchase set removal.

## C# Changes

- `GameServerConnection.HandleBuyItemAsync`
  - Allows NPC action 2 through the live handler guard.
  - Dispatches ready repurchase plans through `TryExecuteRepurchaseAsync`.
- `GameServerConnection.TryExecuteRepurchaseAsync`
  - Sends plan messages such as the Java inventory-full message.
  - Persists before live mutation.
  - Applies Kinah, restored item updates/adds, and repurchase-state removal to `Player`.
  - Sends `SmInventoryUpdateItem` Kinah decrease, `SmInventoryAddItem`/`SmInventoryUpdateItem` item restoration packets, and `SmCubeUpdate` for new cube items.
- `PlayerEnterWorldService` / `PlayerEnterWorldRepository`
  - Added `SaveNpcShopRepurchaseMutationAsync` to persist Kinah row updates plus restored item row updates/inserts using existing database shape.
- `GameServerConnectionBuyItemTests`
  - Converted the action 2 test from disabled-output-only evidence into live persistence/state/packet evidence.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcRepurchaseExecutesLiveFromSnapshot` | Unit/live handler | `CM_BUY_ITEM.runImpl` action 2 and `RepurchaseService.repurchaseFromShop` | Live repurchase decreases Kinah, persists the restored item, mutates inventory and repurchase state, and sends Kinah/add/cube packets. | Java source review plus focused C# live handler assertions. | Uses fake repository capture; no live MySQL or real client validation. |
| Existing `RepurchasePlanServiceTests` | Unit/planner consumed live | `RepurchaseService.repurchaseFromShop` | Confirms can-trade, inventory-full, missing item, insufficient Kinah, stack merge, and add-failure planning used by the live executor. | Java source review plus focused C# assertions. | Planner-only evidence counts here only because the plan is consumed by live handler code. |

## Validation Decision

```text
- Changed surface: live handler/state/persistence boundary for NPC shop repurchase.
- Specific behavior/contract: NPC action 2 successful repurchase decreases Kinah, restores item inventory rows, removes repurchase runtime state, and sends live Kinah/item/cube packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for `RepurchaseService.repurchaseFromShop`.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/planner/service coverage because the command built the affected project and directly covered the edited dispatch, persistence handoff, state mutation, and packet fanout.
- Why this scope is sufficient: the focused tests exercise the action 2 handler, Java-derived planner contract, repository capture, player state mutation, and packet sequence.
```

Result: passed, 125/125.

Note: an initial focused run built product code but failed test compilation because `Assert.NotNull` returned void in this xUnit version. The test assertion was corrected and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 2 repurchase now executes live for the covered success path. Pet sell action 17 and broader real-client validation remain gaps. |
| `com.aionemu.gameserver.services.RepurchaseService#repurchaseFromShop` | `Aion.GameServer.Services.RepurchasePlanService` plus `GameServerConnection.TryExecuteRepurchaseAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Successful restoration, Kinah decrease, runtime repurchase removal, and item/cube packet fanout execute live. Audit logging for insufficient Kinah and live MySQL are not validated. |
| `com.aionemu.gameserver.services.item.ItemService#addItem` | `Aion.GameServer.Services.InventoryAddService.CreateAddItemPlan` plus repurchase executor | Service/live inventory mutation | Partial | Unit Tested | Partial Parity | Repurchase consumes existing add-plan behavior for clone/merge restoration. Broader ItemService side effects remain partial. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live MySQL execution for repurchase was not run.
- Real client validation was not run.
- Java audit logging for insufficient-Kinah repurchase remains modeled but not emitted through live C# audit infrastructure.
- C# still reports disabled side-effect outcome records to existing observers for diagnostics; live side effects are executed independently by the handler.
- Broader `ItemService.addItem` side effects remain partial outside the restored inventory row and packet path covered here.
