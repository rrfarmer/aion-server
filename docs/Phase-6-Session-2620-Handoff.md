# Phase 6 Session 2620 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2620: Execute normal NPC sell-to-shop action 1 live. See
[Phase-6-Session-2620-Completion.md](Phase-6-Session-2620-Completion.md).

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
- Current commit - `[Phase 6][UOW-2620] Execute normal NPC sell-to-shop live`

## Session Summary

- Java review confirmed `CM_BUY_ITEM.runImpl` action 1 calls `TradeService.performSellToShop` for non-ABYSS purchase templates.
- Java `TradeService.performSellToShop` deletes/decreases sold items, sets repurchase price, replaces repurchase state, then increases kinah with `ItemUpdateType.INC_KINAH_SELL`.
- C# live action 1 handling now reaches a normal sell-to-shop executor when diagnostic observers are absent.
- C# persists sold item updates/deletes and kinah update through a new `SaveNpcShopSellMutationAsync` repository method.
- C# mutates live `Player.InventoryItems`, replaces `Player.RepurchaseItems`, and sends item delete/cube/kinah update packets after persistence succeeds.

## Files Changed In UOW-2620

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2620-Completion.md`
- `docs/Phase-6-Session-2620-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 120/120.

Java/Maven: not run. No narrow Java fixture was discovered for `TradeService.performSellToShop`; Java behavior was verified by source review of `CM_BUY_ITEM`, `TradeService`, and `ItemPacketService`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live handler/state/persistence boundary changed, but the focused command directly covered the edited dispatch, planner, service handoff, repository test double contract, and packet update type while building affected projects.

Notes:
- An initial focused run failed at compile because `PlayerEnterWorldServiceTests.CapturingEnterWorldRepository` needed the new repository method; the test double was updated.
- A second focused run passed compilation but failed one existing assertion because the not-sellable branch now sends Java's live denial packet; the assertion was updated and the final focused command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 1 normal sell-to-shop now executes live for covered whole-item sell and not-sellable denial. ABYSS sell-for-AP remains disabled. |
| `com.aionemu.gameserver.services.TradeService#performSellToShop` | `Aion.GameServer.Services.TradeSellToShopPlanService` plus `GameServerConnection.TryExecuteSellToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Whole-item normal sell executes live with item deletion, kinah increase, repurchase snapshot replacement, persistence, and packets. Partial-stack and newly-created kinah rows are planned and wired but not live-handler tested in this UOW. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType#INC_KINAH_SELL` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.IncreaseKinahSell` | Packet update type | Complete | Unit Tested | Partial Parity | Numeric value `0x20` is ported and used by the live sell-to-shop kinah update packet; no Java golden packet was run in this UOW. |
| `com.aionemu.gameserver.services.RepurchaseService#addRepurchaseItems` | `Aion.GameServer.Model.GameObjects.Player.RepurchaseItems` plus sell live executor | Runtime state | Partial | Unit Tested | Partial Parity | Live sell replaces the player's in-memory repurchase snapshot. Java HashSet iteration order and full `SM_REPURCHASE` behavior remain partial. |

## Known Gaps

- Live MySQL execution for NPC sell-to-shop was not run.
- Real client validation was not run.
- Partial-stack sell and missing-kinah-row creation are wired through the same live executor but were not live-handler tested in this UOW.
- Not-sellable system-message item l10n text is not yet modeled; the live packet currently uses an empty string parameter.
- Java `RepurchaseService` is represented as a player snapshot, not a full singleton service with Java HashSet iteration behavior.
- `ABYSS` AP sell-to-shop action 1 remains disabled.

## Next Recommended Runtime UOW

**UOW-2621 candidate: execute `ABYSS` AP sell-to-shop action 1 live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: `CM_BUY_ITEM` action 1 with an `ABYSS` purchase template still creates a disabled `TradeSellForApToShopPlan`/outcome and does not mutate inventory or AP from live code.
- Java source method or runtime path: CM_BUY_ITEM.runImpl action 1 -> TradeService.performSellForAPToShop(Player, TradeList, TradeListTemplate), including CustomConfig.SELLING_APITEMS_ENABLED, PlayerRestrictions.canTrade, goods-list validation, inventory.decreaseByObjectId, and AbyssPointsService.addAp.
- C# runtime artifact to wire or fix: GameServerConnection action 1 AP sell executor, TradeSellForApToShopPlanService, PlayerEnterWorldService/repository persistence for sold item updates/deletes and abyss-rank update.
- Client-visible/state/persistence effect expected: selling an AP item to an ABYSS purchase NPC removes or decreases the sold item, increases player AP/rank state, persists inventory/AP state, sends inventory and AP/rank packets from live code.
- Why this is not preview-only/test-only/documentation-only if feasible: it executes the remaining Java action 1 shop branch with live inventory/AP mutation, persistence, and packet fanout.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java `TradeService.performSellForAPToShop` fixture is discovered.

Broad-validation trigger: live handler/state/persistence boundary changes. Start focused on the AP sell handler and planner; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute `ABYSS` AP sell-to-shop action 1 live.
- Add live-handler coverage for normal partial-stack sell and missing-kinah-row creation if the next AP sell slice reveals shared executor risk.
- Execute NPC repurchase action 2 live after repurchase state and persistence boundaries are re-reviewed against Java.

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
- Denials for kinah, invalid goods, full inventory, limited item, AP, missing required items, negative AP, and normal not-sellable sell are live through UOW-2620.
- Buy/sell planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
