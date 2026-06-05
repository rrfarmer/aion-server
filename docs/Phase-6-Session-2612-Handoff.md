# Phase 6 Session 2612 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2612: Persist NPC shop kinah buys live. See
[Phase-6-Session-2612-Completion.md](Phase-6-Session-2612-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- Current commit - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`

## Session Summary

- Java review confirmed normal buy success subtracts kinah and then calls `ItemService.addItem` for each bought item.
- C# live action `13` normal shop buy now calls `PlayerEnterWorldService.SaveNpcShopBuyMutationAsync` before swapping
  in-memory inventory or sending success packets.
- `MySqlPlayerEnterWorldRepository.SaveNpcShopBuyMutationAsync` saves kinah count, updated bought stacks, and added item
  rows in one transaction using existing inventory table helpers.
- Repository failure now stops live success mutation/packets for this path.

## Files Changed In UOW-2612

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2612-Completion.md`
- `docs/Phase-6-Session-2612-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.TradeService#performBuyTransaction`
- `com.aionemu.gameserver.services.item.ItemService#addItem`
- `com.aionemu.gameserver.dao.InventoryDAO`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync`
- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 97/97.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler/service coverage. The filtered command built `Aion.GameServer` and
`Aion.GameServer.Tests` and covered the modified dispatch boundary plus repository interface implementations.

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

## Known Gaps

- SQL execution for `SaveNpcShopBuyMutationAsync` is not covered by a live database fixture.
- Successful AP/required-item NPC shop purchases remain disabled by the success-only live guard.
- Required-item/AP mutations and limited-item counter updates remain disabled.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only successful purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Next Recommended Runtime UOW

**UOW-2613 candidate: execute successful normal NPC shop required-item consumption for kinah-only trade lists.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: successful normal NPC shop buys with required item costs are planned but blocked by the live success guard because RequiredItems.Count != 0.
- Java source method or runtime path: TradeService.performBuyTransaction -> tradeList.getRequiredItems() loop -> player.getInventory().decreaseByItemId(...) before ItemService.addItem.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync plus PlayerEnterWorldService/Repository persistence for required-item decreases/deletes and bought item rows.
- Client-visible/state/persistence effect expected: successful action 13 normal shop buy consumes required item stacks, persists those changes with kinah/bought item rows, sends inventory update/delete/add packets, and does not run AP mutation.
- Why this is not preview-only/test-only/documentation-only if feasible: it mutates and persists live inventory state from the client packet path.
```

Suggested focused validation if this UOW wires required-item consumption:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live inventory mutation
and persistence boundary changed; start focused and document any broad skip or escalation after focused evidence.

## Safe Runtime Candidates

- Execute required-item consumption for normal NPC shop buys without AP mutation.
- Add DB integration coverage for NPC shop buy persistence if an existing opt-in database fixture can be reused inside
  the same runtime UOW.
- Execute successful AP-cost buy branches only after AP mutation, AP packet ordering, and abyss-rank persistence are
  scoped from Java.
- Successful limited-item counter updates only if live mutation and persistence are scoped from Java
  `LimitedItemTradeService`.
- `ABYSS_KINAH`, `ABYSS`, or `REWARD` buy execution only after AP/required-item mutation and packet effects are scoped
  from Java source.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action `13` normal kinah stackable buy executes live as of UOW-2606.
- `CM_BUY_ITEM` NPC action `13` normal denial packets are live through UOW-2611.
- `CM_BUY_ITEM` NPC action `13` successful normal kinah buy persistence is live as of UOW-2612.
- Buy-from-shop planner/outcome services are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
