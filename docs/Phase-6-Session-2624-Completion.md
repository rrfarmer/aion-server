# Phase 6 Session 2624 Completion

## UOW

[Phase 6] UOW-2624: Execute pet merchant sell-to-shop live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_BUY_ITEM action 17 pet merchant sell-to-shop was classified in C# but did not execute live inventory, Kinah, repurchase, persistence, or packet side effects.
- Java source/runtime path: CM_BUY_ITEM.runImpl pet branch -> PetFunctionType.MERCHANT -> TradeService.performSellToShop(player, tradeList, null, pf.getRatePrice()).
- C# runtime artifact wired: GameServerConnection now recognizes pet world objects, resolves pet merchant rate facts, creates a TradeSellToShopPlan with purchaseTemplate null and the pet sell modifier, and allows the live sell executor to run for action 17 pet targets.
- Client-visible/state/persistence effect: selling to a merchant pet deletes/decreases the sold item, increases Kinah, updates runtime repurchase state, persists item/Kinah row changes, and sends delete/cube/Kinah packets from live code.
- Why this is runtime progress: this UOW wires a previously deferred client packet branch into live mutation, persistence, and packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`
  - `readImpl` accepts action `17` into `TradeList`.
  - `runImpl` checks `target instanceof Pet`, requires `PetFunctionType.MERCHANT`, and calls `TradeService.performSellToShop(player, tradeList, null, pf.getRatePrice())`.
- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performSellToShop` with `purchaseTemplate == null` checks sellability, applies `PricesService.getSellReward(price, sellModifier)`, deletes/decreases inventory, adds repurchase items, and increases Kinah.

## C# Changes

- `IWorldPetObject`
  - Added the minimal visible-object contract needed for Java's pet merchant branch: object id, merchant function presence, and merchant sell modifier.
- `CmBuyItemKnownListTargetFactAdapterService`
  - Classifies `IWorldPetObject` targets as `CmBuyItemRunTargetKind.Pet`.
- `GameServerConnection`
  - Admits action `17` pet sell packets into buy-item handling.
  - Resolves pet merchant facts and builds a normal sell-to-shop plan with the pet sell modifier.
  - Allows the existing live sell-to-shop executor to run for pet action `17`, reusing the same inventory/Kinah/persistence/packet implementation as Java's shared `TradeService.performSellToShop`.
- Focused tests now prove pet target classification and live packet execution for the whole-item sell success path.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePlan_PetTargetClassifiesPetWhenKnown` | Unit/target fact | `CM_BUY_ITEM.runImpl -> target instanceof Pet` | Known-list target facts can classify pet visible objects before handler branch selection. | Java source review plus C# target adapter assertion. | Uses a test pet object; production pet spawn registration remains a gap. |
| `ProcessPacketAsync_CmBuyItemPetMerchantSellActionExecutesSellToShopLive` | Unit/live handler | `CM_BUY_ITEM.runImpl` action 17 and `TradeService.performSellToShop(..., pf.getRatePrice())` | Action 17 merchant pet sell deletes the item, increases Kinah with the pet rate, updates repurchase state, persists sell rows, and sends delete/cube/Kinah packets. | Focused C# live handler assertions over runtime state, repository capture, and packets. | Uses fake repository capture and a test pet world object; no live MySQL or real client validation. |

## Validation Decision

```text
- Changed surface: live handler/state/persistence/packet boundary for pet merchant sell-to-shop.
- Specific behavior/contract: CM_BUY_ITEM action 17 pet merchant sell uses the pet merchant sell modifier, mutates inventory/Kinah/repurchase state, persists sell rows, and sends live sell packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemKnownListTargetFactAdapterServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for CM_BUY_ITEM action 17 pet merchant sell.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered target classification, the shared sell planner contract, live handler mutation, persistence handoff, and packet fanout.
- Why this scope is sufficient: the focused tests exercise the Java-derived pet branch and the reused live sell-to-shop executor that performs the runtime side effects.
```

Result: passed, 66/66.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Pet action 17 merchant sell now executes live when the target world object exposes merchant facts. Production pet world registration and real-client validation remain gaps. |
| `com.aionemu.gameserver.model.gameobjects.Pet` | `Aion.GameServer.Model.GameObjects.IWorldPetObject` | Runtime visible object contract | Partial | Unit Tested | Partial Parity | Minimal merchant facts are represented for buy-item branch selection. Broader Java Pet state/functions are not ported by this interface. |
| `com.aionemu.gameserver.services.TradeService#performSellToShop` | `Aion.GameServer.Services.TradeSellToShopPlanService` plus `GameServerConnection.TryExecuteSellToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Shared sell executor is now consumed by NPC action 1 and pet action 17. Pet auto-sell and real pet spawn integration remain partial. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Production pet spawn/known-list registration has not been proven to create `IWorldPetObject` instances with merchant facts.
- Live MySQL execution for pet merchant sell persistence was not run.
- Real client validation was not run.
- Partial-stack pet merchant sell is covered by the reused sell planner/executor but was not separately asserted in this UOW.
- Java audit logging for sell-abuse cases remains modeled but not emitted through live C# audit infrastructure.
