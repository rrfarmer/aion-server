# Phase 6 Session 2614 Completion

## UOW

[Phase 6] UOW-2614: Spend NPC shop abyss points live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful normal NPC shop buys with RequiredAbyssPoints > 0 no longer stop at the live success guard.
- Java source/runtime path: TradeService.performBuyTransaction -> AbyssPointsService.addAp(player, -tradeList.getRequiredAp()) before kinah/required-item consumption and ItemService.addItem.
- C# runtime artifact wired: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync, AbyssPointsService.CreateAddApPlan, PlayerEnterWorldService.SaveNpcShopBuyMutationAsync, and MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync.
- Client-visible/state/persistence effect: action 13 normal shop success subtracts AP, persists the updated abyss-rank row with the inventory buy transaction, sends the AP spend system message and SM_ABYSS_RANK before inventory packets, then mutates live player inventory.
- Why this is runtime progress: this UOW changes live client packet handling, player abyss-rank state, database persistence, and packet fanout; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/TradeService.java`
  - `performBuyTransaction`
- `game-server/src/com/aionemu/gameserver/services/abyss/AbyssPointsService.java`
  - `addAp(Player, int)`
  - `onRankChanged`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/AbyssRank.java`
  - `addAp`

## C# Changes

- `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
  - Removed the successful-buy guard for non-zero required AP.
  - Plans AP spend with `AbyssPointsService.CreateAddApPlan` before persistence.
  - Persists the planned `PlayerAbyssRank` together with kinah, required-item, and bought-item mutations.
  - Applies `player.AbyssRank` after persistence succeeds.
  - Sends AP player packets before kinah/item packets, matching Java call order.
  - Calls existing rank-change side effects when static data is available.
- `PlayerEnterWorldService.SaveNpcShopBuyMutationAsync`
  - Accepts an optional `PlayerAbyssRank`.
- `IPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync`
  - Accepts an optional `PlayerAbyssRank`.
- `MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync`
  - Saves the abyss-rank row in the same transaction as NPC-shop buy inventory mutations.
- `EmptyPlayerEnterWorldRepository.NpcShopBuyPersistenceCapture`
  - Captures the optional abyss rank for live handler tests.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopSpendsAbyssPointsBeforeInventoryPackets` | Unit/live handler | `TradeService.performBuyTransaction` plus `AbyssPointsService.addAp(Player, int)` | Live action 13 normal shop AP-cost buy persists AP/kinah/reward rows, mutates player AP/inventory, and sends AP spend message + `SM_ABYSS_RANK` before inventory packets. | Java source review + focused C# live handler assertions. | Uses fake repository capture; no live MySQL execution or real client validation. |
| Existing NPC shop buy persistence tests | Unit/live handler | `TradeService.performBuyTransaction` inventory branches | Updated through the widened persistence API. | Focused C# compile/test coverage. | No DB integration. |
| `PlayerEnterWorldServiceTests` fake repository | Unit/service boundary | `AbyssRankDAO.storeAbyssRank` as part of dirty rank persistence | Updated through the widened repository interface. | Focused C# compile/test coverage. | No DB integration. |

## Validation Decision

```text
- Changed surface: live handler, player abyss-rank state, packet fanout, and persistence boundary.
- Specific behavior/contract: AP-cost normal NPC shop buy spends AP through the live action 13 handler, persists the updated abyss rank atomically with inventory rows, and sends AP packets before inventory packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~AbyssPointsServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for TradeService/AbyssPointsService NPC-shop buy success.
- Broad-validation trigger: live side-effect and persistence boundary changed.
- Broad .NET decision: skipped after focused live handler/AP service/persistence coverage because the changed path was isolated to NPC-shop action 13 and the filtered command built the affected project/dependencies.
- Why this scope is sufficient: the focused tests cover the edited live dispatch path, existing AP service packet contract, transaction planner AP-cost calculation, and repository interface implementations.
```

Result: passed, 123/123.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Normal action 13 buys now handle kinah, required-item, and AP-cost success branches; limited counters and non-normal shop types remain incomplete. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService#addAp(Player,int)` | `Aion.GameServer.Services.AbyssPointsService` and buy-handler invocation | Service/state/packet path | Partial | Unit Tested | Partial Parity | AP spend message and `SM_ABYSS_RANK` fanout are covered for a non-rank-change spend. Rank-change broadcast/equipment/skill side effects reuse existing helper but were not specifically exercised by this UOW. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank#addAp` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank.AddAp` | Model/state mutation | Partial | Unit Tested | Partial Parity | Existing AP service tests cover spend/gain/cap behavior; DB-backed persistence for this buy path still needs integration evidence. |
| `com.aionemu.gameserver.dao.AbyssRankDAO#storeAbyssRank` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` | Repository | Partial | Compile Covered/Unit Captured | Needs Verification | NPC-shop buy persistence now calls `SaveAbyssRankAsync` in the transaction; no live MySQL test executed. |

## Known Gaps

- Limited-item counter mutation/persistence is still not live.
- Live NPC shop execution remains scoped to `TradeNpcType.NORMAL`, action `13`.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Rank-change AP spend side effects rely on existing helper coverage; this UOW only tested a non-rank-change spend.
- MySQL execution for the widened NPC-shop buy persistence path was not validated against a live database fixture.
- Real client validation was not run.
