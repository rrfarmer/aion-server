# Phase 6 Session 2613 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2613: Consume NPC shop required items live. See
[Phase-6-Session-2613-Completion.md](Phase-6-Session-2613-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- `1daf029` - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`
- Current commit - `[Phase 6][UOW-2613] Consume NPC shop required items live`

## Session Summary

- Java review confirmed `TradeService.performBuyTransaction` decreases kinah, then consumes `tradeList.getRequiredItems()` with `Storage.decreaseByItemId`, then calls `ItemService.addItem`.
- Java `Storage.decreaseItemCount` sends `DEC_ITEM_USE` updates for reduced stacks and `ItemPacketService.sendItemDeletePacket` with delete type `USE` plus cube refresh when non-kinah stacks hit zero.
- C# live normal NPC shop buys now execute required-item consumption before reward add planning.
- Required-item updates/deletes are persisted atomically with kinah and bought item mutations.
- Success packet fanout now includes required-item decrease/delete packets before bought item add/update packets.

## Files Changed In UOW-2613

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2613-Completion.md`
- `docs/Phase-6-Session-2613-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 114/114.

Java/Maven: not run. No narrow Java fixture was discovered; Java behavior was verified by source review of `TradeService`, `Storage`, and `ItemPacketService`.

Broad .NET: skipped after focused live handler/planner/service coverage.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `TradeService.performBuyTransaction` | `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | Normal action 13 buys now handle kinah-only and required-item success paths; AP-cost success, limited counters, and non-normal shop types remain incomplete. |
| `Storage.decreaseByItemId`/`decreaseItemCount` | NPC-shop required item consumption in `GameServerConnection` | Inventory mutation | Partial | Unit Tested | Partial Parity | This path consumes unequipped cube stacks, updates/deletes in working inventory, and sends Java-shaped packets. |
| `ItemPacketService.sendItemPacket/sendItemDeletePacket` | `SmInventoryUpdateItem`, `SmDeleteItem`, `SmCubeUpdate` | Packet fanout | Partial | Unit Tested | Partial Parity | Required-item stack reduction sends `DEC_ITEM_USE`; exact-stack deletion sends `USE` delete plus cube refresh. |
| `InventoryDAO.store` | `MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` | Persistence | Partial | Compile Covered/Unit Captured | Needs DB Verification | Required-item updates/deletes share the NPC-shop buy transaction with kinah and bought rows; no DB integration test was run. |

## Known Gaps

- Successful AP-cost NPC shop buys remain disabled by `RequiredAbyssPoints != 0`.
- Limited-item counter mutation/persistence is still not live.
- Live NPC shop execution remains scoped to `TradeNpcType.NORMAL`, action `13`.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- MySQL execution for the widened NPC-shop buy persistence path needs a database-backed validation UOW if an existing fixture is available.
- Real client validation was not run.

## Next Recommended Runtime UOW

**UOW-2614 candidate: execute successful AP-cost normal NPC shop buys live.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: successful normal NPC shop buys with RequiredAbyssPoints > 0 are planned but still blocked by the live success guard.
- Java source method or runtime path: TradeService.performBuyTransaction -> AbyssPointsService.addAp(player, -tradeList.getRequiredAp()) before kinah/required-item consumption and ItemService.addItem.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync, AbyssPointsService/player abyss-rank mutation, AP persistence through PlayerEnterWorldService/MySqlPlayerEnterWorldRepository, and AP packet fanout if required by existing C# parity conventions.
- Client-visible/state/persistence effect expected: successful action 13 normal shop buy subtracts AP, persists restored abyss-rank/AP state with inventory mutations, sends the real AP/inventory success packets, and still handles kinah/required-item/reward rows atomically.
- Why this is not preview-only/test-only/documentation-only if feasible: it mutates live abyss-rank/player state, persists runtime state, and sends success packets from the client packet path.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~AbyssPointsServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

## Safe Runtime Candidates

- Execute successful AP-cost normal NPC shop buys live, including AP mutation/persistence/packets.
- Execute limited-item counter decrement/increment persistence for successful NPC shop buys after scoping Java `LimitedItemTradeService`.
- Add DB-backed validation for NPC shop buy persistence if an existing opt-in database fixture can be reused as part of a runtime persistence UOW.
- Extend live buy execution to `ABYSS_KINAH` after Java pricing/AP/kinah behavior is scoped.
- Extend live buy execution to `ABYSS` or `REWARD` only after AP/required-item and reward-branch packet effects are scoped from Java.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action `13` normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action `13` normal required-item buys execute live and persist as of UOW-2613.
- Denials for kinah, invalid goods, full inventory, limited item, AP, missing required items, and negative AP are live through UOW-2611.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
