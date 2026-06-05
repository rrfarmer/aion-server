# Phase 6 Session 2619 Completion

## UOW

[Phase 6] UOW-2619: Execute NPC shop `REWARD` buys live without kinah.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: `REWARD` buy transactions were planned with useKinah=false but blocked by the live handler's NPC type classifier.
- Java source/runtime path: TradeService.performBuyFromShop -> TradeNpcType.REWARD -> performBuyTransaction(..., false), which skips kinah price/decrease but still validates AP/required items, inventory, limited items, then adds bought items and updates limited counters.
- C# runtime artifact wired: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync now accepts `REWARD` through the live action 13 buy handler.
- Client-visible/state/persistence effect: action 13 `REWARD` NPC buys consume required reward/token items and/or AP without requiring kinah, persist live inventory/AP mutations, send item/AP packets, add bought items, and keep limited-counter behavior available.
- Why this is runtime progress: this UOW removes the remaining live packet-handler shop-type guard for Java buy-from-shop types and executes a real no-kinah client buy path with state mutation, persistence, and packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performBuyFromShop`
  - `performBuyTransaction`
- `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`
  - `TradeNpcType.REWARD`

## C# Changes

- `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
  - Added `REWARD` to the live action 13 buy-from-shop type classifier.
  - Reuses the existing no-kinah transaction path introduced for Java `useKinah=false` shop types.
- `GameServerConnectionBuyItemTests`
  - Added live handler coverage for a `REWARD` token purchase that consumes a full required-token stack, requires no kinah template or kinah row, persists the token deletion and bought item, and sends the live item packets.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcRewardBuyFromShopConsumesTokenWithoutKinahLive` | Unit/live handler | `TradeService.performBuyFromShop` `REWARD` branch and `performBuyTransaction(..., false)` | Live action 13 `REWARD` buy consumes a reward token full stack, persists deletion without a kinah row, mutates player inventory, adds the bought item, and sends delete/cube/add/cube packets. | Java source review + focused C# live handler assertions. | Uses fake repository capture; no live MySQL or real client validation. |
| Existing `TradeBuyTransactionPlanServiceTests.CreatePlan_SkipsKinahCalculationWhenJavaUseKinahIsFalse` | Unit/planner consumed live | Java `performBuyTransaction(..., false)` | Confirms the transaction plan skips kinah cost calculation when Java useKinah is false. | Java source review + focused C# assertions. | Planner itself is not runtime progress except when consumed by this UOW's live handler. |

## Validation Decision

```text
- Changed surface: live handler shop-type classifier and no-kinah reward/token buy execution.
- Specific behavior/contract: `REWARD` action 13 buys execute live with Java useKinah=false, consume reward tokens without requiring kinah, persist no kinah item, and send no kinah packet.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for `TradeService` `REWARD` no-kinah buy behavior.
- Broad-validation trigger: live handler/state/persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/planner coverage because the changed runtime path was isolated to action 13 reward buy execution and the filtered command built the affected project/dependencies.
- Why this scope is sufficient: the focused tests cover the edited dispatch classifier, Java useKinah=false handling, full-stack reward token deletion, no-kinah persistence handoff, absence of kinah packets, item add, and cube update fanout.
```

Result: passed, 57/57.

Note: an initial run of the same focused command failed at compile because the new test referenced a nonexistent `SmDeleteItem.DeleteType` member. The assertion was corrected to use the existing delete-packet payload helper, and the final focused command passed.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyFromShop` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | `NORMAL`, `ABYSS_KINAH`, `ABYSS`, and `REWARD` action 13 buy branches now execute live for the covered behavior slices. |
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Services.TradeBuyTransactionPlanService` plus live handler consumption | Service/live transaction | Partial | Unit Tested | Partial Parity | `REWARD` uses Java `useKinah=false`; required reward-token deletion and bought-item add execute through live handler state mutation, persistence, and packets. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeNpcType#REWARD` | `Aion.GameServer.Dataholders.TradeListTemplateSummary.NpcType` and buy handler classifier | Static data/handler routing | Partial | Unit Tested | Partial Parity | `REWARD` now routes to live no-kinah buy execution. Full XML/default coverage remains outside this UOW. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Live MySQL execution for this path was not run.
- Real client validation was not run.
- Rank-change AP spend side effects reuse existing helper behavior but were not specifically exercised in this reward-token UOW.
- Action 13 buy-from-shop Java shop-type routing is live for `NORMAL`, `ABYSS_KINAH`, `ABYSS`, and `REWARD` covered slices, but broader real data/client validation remains partial.
- NPC sell-to-shop action 1 remains disabled and does not yet mutate live inventory, kinah, or repurchase state.
