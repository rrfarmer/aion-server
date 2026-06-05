# Phase 6 Session 2619 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2619: Execute NPC shop `REWARD` buys live without kinah. See
[Phase-6-Session-2619-Completion.md](Phase-6-Session-2619-Completion.md).

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
- Current commit - `[Phase 6][UOW-2619] Execute NPC shop reward buys live`

## Session Summary

- Java review confirmed `TradeService.performBuyFromShop` routes `TradeNpcType.REWARD` to `performBuyTransaction(..., false)`.
- Java `performBuyTransaction(..., false)` skips the kinah price/decrease branch but still performs AP/required-item checks, inventory checks, item add, and limited-item updates.
- C# live action 13 buy handling now accepts `REWARD`.
- A focused live handler test verifies a `REWARD` token purchase succeeds with no kinah template, no kinah inventory row, no kinah persistence row, required-token full-stack deletion, bought-item add, and item/cube packets.

## Files Changed In UOW-2619

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2619-Completion.md`
- `docs/Phase-6-Session-2619-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests" --no-restore
```

Result: passed, 57/57.

Java/Maven: not run. No narrow Java fixture was discovered for `TradeService` `REWARD` no-kinah buy behavior; Java behavior was verified by source review of `TradeService` and `TradeListTemplate`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live handler/state/persistence boundary changed, but the focused command directly covered the edited dispatch classifier, no-kinah reward-token side effects, and adjacent planner behavior while building affected projects.

Note: an initial run of the same focused command failed at compile because the new test referenced a nonexistent `SmDeleteItem.DeleteType` member. The assertion was corrected to use the existing delete-packet payload helper, and the final focused command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyFromShop` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | `NORMAL`, `ABYSS_KINAH`, `ABYSS`, and `REWARD` action 13 buy branches now execute live for the covered behavior slices. |
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Services.TradeBuyTransactionPlanService` plus live handler consumption | Service/live transaction | Partial | Unit Tested | Partial Parity | `REWARD` uses Java `useKinah=false`; required reward-token deletion and bought-item add execute through live handler state mutation, persistence, and packets. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeNpcType#REWARD` | `Aion.GameServer.Dataholders.TradeListTemplateSummary.NpcType` and buy handler classifier | Static data/handler routing | Partial | Unit Tested | Partial Parity | `REWARD` now routes to live no-kinah buy execution. Full XML/default coverage remains outside this UOW. |

## Known Gaps

- Live MySQL execution for this path was not run.
- Real client validation was not run.
- Rank-change AP spend side effects reuse existing helper behavior but were not specifically exercised in this reward-token UOW.
- Action 13 buy-from-shop Java shop-type routing is live for `NORMAL`, `ABYSS_KINAH`, `ABYSS`, and `REWARD` covered slices, but broader real data/client validation remains partial.
- NPC sell-to-shop action 1 remains disabled and does not yet mutate live inventory, kinah, or repurchase state.

## Next Recommended Runtime UOW

**UOW-2620 candidate: execute normal NPC sell-to-shop action 1 live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: `CM_BUY_ITEM` action 1 normal sell-to-shop currently builds a sell-to-shop plan/outcome but does not execute live inventory, kinah, packet, or repurchase side effects.
- Java source method or runtime path: TradeService.performSellToShop(Player, TradeList, TradeListTemplate, sellModifier), including item sellability/list validation, item delete/decrease, RepurchaseService.addRepurchaseItems, inventory.increaseKinah(... INC_KINAH_SELL), and inventory update packets.
- C# runtime artifact to wire or fix: GameServerConnection action 1 sell-to-shop executor, TradeSellToShopPlanService, player inventory mutation/persistence handoff, and repurchase runtime state if already represented.
- Client-visible/state/persistence effect expected: selling a normal item to an NPC removes or decreases the sold item, adds kinah, persists inventory changes, records repurchase data if supported, and sends delete/update/kinah packets from live code.
- Why this is not preview-only/test-only/documentation-only if feasible: it executes a deferred client packet path with live inventory/kinah mutation, persistence, and packet fanout.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java `TradeService` sell-to-shop fixture is discovered.

Broad-validation trigger: live handler/state/persistence boundary changes. Start focused on the sell handler and transaction planner; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute normal NPC sell-to-shop action 1 live.
- Execute `ABYSS` AP sell-to-shop live after the normal sell path is live.
- Add DB-backed validation for NPC shop buy/sell persistence if an existing opt-in database fixture can be reused as part of a runtime persistence UOW.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action 13 normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action 13 normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action 13 normal AP-cost buys execute live and persist as of UOW-2614.
- Successful `CM_BUY_ITEM` NPC action 13 normal limited-item buys mutate live limited counters as of UOW-2615.
- Limited-item counters reset from live scheduler callbacks as of UOW-2616.
- `CM_BUY_ITEM` NPC action 13 `ABYSS_KINAH` buys execute live and persist as of UOW-2617.
- `CM_BUY_ITEM` NPC action 13 `ABYSS` buys execute live without kinah as of UOW-2618.
- `CM_BUY_ITEM` NPC action 13 `REWARD` buys execute live without kinah as of UOW-2619.
- Denials for kinah, invalid goods, full inventory, limited item, AP, missing required items, and negative AP are live through UOW-2611.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
