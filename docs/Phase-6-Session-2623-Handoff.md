# Phase 6 Session 2623 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2623: Execute partial-stack AP sell-to-shop live. See
[Phase-6-Session-2623-Completion.md](Phase-6-Session-2623-Completion.md).

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
- `46397e7` - `[Phase 6][UOW-2621] Execute abyss AP sell-to-shop live`
- `0eeb30d` - `[Phase 6][UOW-2622] Execute NPC shop repurchase live`
- Current commit - `[Phase 6][UOW-2623] Execute partial AP sell-to-shop live`

## Session Summary

- Java review confirmed `TradeService.performSellForAPToShop` relies on `Storage.decreaseByObjectId`, which decreases partial stacks and awards AP only when the decrease succeeds.
- C# AP sell planning now carries seller item updates for partial-stack decreases and skips oversized sell counts before AP rewards.
- C# live `CM_BUY_ITEM` action 1 ABYSS execution now accepts smaller-than-stack sell counts, mutates the player's inventory count, persists the updated item row with abyss rank, and sends an inventory decrease packet followed by AP/rank packets.
- Focused live handler coverage proves the new runtime path using the existing repository capture.

## Files Changed In UOW-2623

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/TradeSellForApToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemHandlerCompositionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSellToShopCompositionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeSellForApToShopPlanServiceTests.cs`
- `docs/Phase-6-Session-2623-Completion.md`
- `docs/Phase-6-Session-2623-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 119/119.

Java/Maven: not run. No narrow Java fixture was discovered for `TradeService.performSellForAPToShop` partial-stack behavior; Java behavior was verified by source review of `CM_BUY_ITEM`, `TradeService`, and `Storage`.

Broad .NET: skipped after focused coverage. Broad trigger existed because a live handler/state/persistence boundary changed, but the focused command directly covered the edited AP sell dispatch, planner contract, service/repository handoff, and packet fanout while building affected projects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBuyItemAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | NPC action 1 ABYSS AP sell now executes exact-count deletes and partial-stack decreases for the covered success paths. |
| `com.aionemu.gameserver.services.TradeService#performSellForAPToShop` | `Aion.GameServer.Services.TradeSellForApToShopPlanService` plus `GameServerConnection.TryExecuteSellForApToShopAsync` | Service/live transaction | Partial | Unit Tested | Partial Parity | Partial-stack item updates, AP reward, persistence, and packets execute live. |
| `com.aionemu.gameserver.model.items.storage.Storage#decreaseByObjectId` | AP sell inventory mutation in `GameServerConnection` | Runtime inventory mutation | Partial | Unit Tested | Partial Parity | Covered only through AP sell-to-shop. Broader storage semantics remain partial. |

## Known Gaps

- Live MySQL execution for AP sell partial-stack persistence was not run.
- Real client validation was not run.
- Java audit logging for AP sell trade-abuse cases remains modeled but not emitted through live C# audit infrastructure.
- C# still reports disabled side-effect outcome records to existing observers for diagnostics; live side effects are executed independently by the handler.
- Pet merchant action 17 sell-to-shop remains deferred.

## Next Recommended Runtime UOW

**UOW-2624 candidate: execute pet merchant action 17 sell-to-shop live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_BUY_ITEM action 17 pet merchant sell-to-shop remains deferred while NPC action 1 sell paths now execute live.
- Java source method or runtime path: CM_BUY_ITEM.runImpl action 17 pet branch -> TradeService.performSellToShop(player, tradeList).
- C# runtime artifact to wire or fix: GameServerConnection.HandleBuyItemAsync pet target/action 17 classification and execution should reuse the existing sell-to-shop plan/executor if pet merchant facts prove Java-equivalent permission.
- Client-visible/state/persistence effect expected: selling to a pet merchant deletes/decreases inventory, increases Kinah, persists item/Kinah rows, and sends sell result packets from the live packet path.
- Why this is not preview-only/test-only/documentation-only if feasible: it executes a currently deferred client packet branch with inventory/Kinah mutation, persistence, and packet fanout.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java pet merchant fixture is discovered.

Broad-validation trigger: live handler/state/persistence boundary changes. Start focused on pet action 17 sell-to-shop handler and existing sell/persistence services; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Execute pet merchant action 17 sell-to-shop live once pet merchant runtime facts are confirmed.
- Add live MySQL validation for NPC shop buy/sell/repurchase/AP sell persistence if an opt-in database fixture can be reused.
- Execute additional `Storage.decreaseByObjectId` live paths when a concrete packet/service branch consumes them.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action 13 normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action 13 normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action 13 normal AP-cost buys execute live and persist as of UOW-2614.
- Successful `CM_BUY_ITEM` NPC action 13 normal limited-item buys mutate live limited counters as of UOW-2615.
- Limited-item counters reset from live scheduler callbacks as of UOW-2616.
- `CM_BUY_ITEM` NPC action 13 `ABYSS_KINAH` buys execute live and persist as of UOW-2617.
- `CM_BUY_ITEM` NPC action 13 `ABYSS` buys execute live without Kinah as of UOW-2618.
- `CM_BUY_ITEM` NPC action 13 `REWARD` buys execute live without Kinah as of UOW-2619.
- `CM_BUY_ITEM` NPC action 1 normal sell-to-shop executes live for covered whole-item sells as of UOW-2620.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop executes live for exact-count deletes as of UOW-2621.
- `CM_BUY_ITEM` NPC action 2 repurchase executes live for the covered success path as of UOW-2622.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop partial-stack decreases execute live as of UOW-2623.
- Buy/sell/repurchase planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
