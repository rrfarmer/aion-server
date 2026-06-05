# Phase 6 Session 2614 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2614: Spend NPC shop abyss points live. See
[Phase-6-Session-2614-Completion.md](Phase-6-Session-2614-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- `1daf029` - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`
- `c015455` - `[Phase 6][UOW-2613] Consume NPC shop required items live`
- Current commit - `[Phase 6][UOW-2614] Spend NPC shop abyss points live`

## Session Summary

- Java review confirmed `TradeService.performBuyTransaction` calls `AbyssPointsService.addAp(player, -requiredAp)` before kinah, required-item, and reward item mutations.
- Java `AbyssPointsService.addAp(Player, int)` sends AP spend/gain system message, sends `SM_ABYSS_RANK` when AP changes, and invokes rank-change side effects if rank changes.
- C# live normal NPC shop buys now plan required AP spends, persist the updated abyss rank in the NPC-shop transaction, apply `player.AbyssRank`, and send AP packets before inventory packets.
- Existing kinah, required-item, and bought-item success behavior remains in the same live handler path.

## Files Changed In UOW-2614

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2614-Completion.md`
- `docs/Phase-6-Session-2614-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~AbyssPointsServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 123/123.

Java/Maven: not run. No narrow Java fixture was discovered for this runtime path; Java behavior was verified by source review of `TradeService`, `AbyssPointsService`, and `AbyssRank`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live side-effect/persistence boundary changed, but the focused command directly covered the edited dispatch path and adjacent AP/persistence contracts while building affected projects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Normal action 13 buy success now handles kinah-only, required-item, and AP-cost branches. Limited counters and non-normal shop types remain incomplete. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService#addAp(Player,int)` | `Aion.GameServer.Services.AbyssPointsService` plus buy-handler invocation | Service/state/packet path | Partial | Unit Tested | Partial Parity | AP spend packet order is covered for non-rank-change shop spend; rank-change broadcast/equipment/skill side effects are delegated to existing helper and remain a risk area. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank#addAp` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank.AddAp` | Model/state mutation | Partial | Unit Tested | Partial Parity | Existing service tests cover AP spend/gain/cap; this UOW confirms the live buy path consumes the plan. |
| `com.aionemu.gameserver.dao.AbyssRankDAO#storeAbyssRank` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` | Repository | Partial | Compile Covered/Unit Captured | Needs Verification | Optional abyss rank is saved in the NPC-shop buy transaction; no DB integration test was run. |

## Known Gaps

- Limited-item counter mutation/persistence is still not wired.
- Live NPC shop execution remains scoped to `TradeNpcType.NORMAL`, action `13`.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Rank-change AP spend side effects in this handler were not directly exercised.
- SQL execution for AP + inventory NPC-shop buy persistence was not validated against a live database fixture.
- Real client validation was not run.

## Next Recommended Runtime UOW

**UOW-2615 candidate: persist successful NPC shop limited-item counters live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: successful normal NPC shop buys for limited items currently deny blocked purchases, but successful purchases do not mutate limited-item counters.
- Java source method or runtime path: TradeService.performBuyTransaction success branch -> LimitedItemTradeService.getInstance().buyItem(...) after ItemService.addItem succeeds.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync, limited-item runtime state/repository service if present, and the existing transaction/persistence boundary for successful buy mutations.
- Client-visible/state/persistence effect expected: successful action 13 normal shop buy updates the player/global limited-item counter so later buys see the consumed quantity and blocked-limit denials reflect live state.
- Why this is not preview-only/test-only/documentation-only if feasible: it mutates/persists live shop-limit runtime state from the client packet path and changes subsequent client-visible buy eligibility.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~NpcDialogLimitedItemFactAdapterServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java `LimitedItemTradeService` fixture is discovered.

Broad-validation trigger: live state/persistence boundary likely changes. Start focused on the buy handler and limited-item service; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Persist successful NPC shop limited-item counter updates live after scoping Java `LimitedItemTradeService`.
- Extend live buy execution to `ABYSS_KINAH` after Java pricing/AP/kinah behavior is scoped.
- Extend live buy execution to `ABYSS` or `REWARD` only after Java branch-specific AP/required-item/reward packet effects are scoped.
- Add DB-backed validation for NPC shop AP/required-item buy persistence if an existing opt-in database fixture can be reused as part of a runtime persistence UOW.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action `13` normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action `13` normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action `13` normal AP-cost buys execute live and persist as of UOW-2614.
- Denials for kinah, invalid goods, full inventory, limited item, AP, missing required items, and negative AP are live through UOW-2611.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
