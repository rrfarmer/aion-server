# Phase 6 Session 2608 Completion

## UOW

[Phase 6] UOW-2608: Send NPC shop invalid-goods denial live

## Status

Completed and validated with focused live handler, transaction planner, and packet coverage. Live `CM_BUY_ITEM` action
`13` for normal NPC shop buys now sends Java's invalid-goods `SM_MESSAGE` text when the requested item is not sold by
the NPC, and leaves inventory state unchanged.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: invalid NPC shop goods now send Java denial text from live CM_BUY_ITEM instead of only composing a disabled planner result.
- Java source method or runtime path: TradeService.performBuyTransaction -> validateBuyItems false -> PacketSendUtility.sendMessage(player, "Some items are not allowed to be sold from this NPC.") and return false.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedInvalidBuyItem and SmMessage.
- Client-visible/state/persistence effect changed: live action 13 with an item not sold by the NPC sends a real golden-yellow SM_MESSAGE and does not mutate player inventory.
- Why this is not preview-only/test-only/documentation-only: this sends a real server packet from the live client packet path.
```

## Java Source Reviewed

- `TradeService.performBuyTransaction` calls `validateBuyItems` before kinah/AP/free-slot/limit checks.
- `TradeService.validateBuyItems` rejects trade items with `count < 1` or item IDs absent from the NPC goods-list union.
- On invalid goods, Java sends `PacketSendUtility.sendMessage(player, "Some items are not allowed to be sold from this NPC.")`.
- `PacketSendUtility.sendMessage(Player, String)` constructs `new SM_MESSAGE(0, null, msg, ChatType.GOLDEN_YELLOW)`.

## C# Changes

- Changed `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` so `BlockedInvalidBuyItem` sends the Java
  invalid-goods text via `SmMessage` before any success-only mutation branch.
- Kept the invalid-goods branch before the insufficient-kinah branch to mirror Java validation order.
- Added live no-observer action `13` coverage for invalid goods that asserts no mutation and the golden-yellow message
  payload.

## Known Gaps

- Other buy-from-shop denial branches remain incomplete: AP/required-item shortages, negative AP audit, full inventory,
  and limited-item blocks.
- Successful NPC shop buys still do not persist kinah/item mutations through a repository surface.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 invalid-goods denial packet.
- Specific behavior/contract: invalid NPC goods send Java's golden-yellow SM_MESSAGE text, emit no inventory mutation, and keep the no-observer live path active.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore -> 330/330 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet side effect changed.
- Broad .NET decision: skipped after focused live handler/planner/packet coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing no-observer test dispatches an encoded CM_BUY_ITEM packet through the ordinary live path and asserts the Java text packet payload plus unchanged inventory state; existing packet tests cover SmMessage golden-yellow shape.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success, invalid-goods denial, and insufficient-kinah denial; AP/token, full-inventory, limited-item, persistence, and other branches remain incomplete. |
| `com.aionemu.gameserver.services.TradeService#validateBuyItems` | `Aion.GameServer.Services.TradeBuyTransactionPlanService.CreatePlan` consumed by live handler | Service/planner/live application | Partial | Unit Tested | Partial Parity | Planner identifies invalid count or goods-list mismatch; live handler now sends the Java text denial for that status. |
| `com.aionemu.gameserver.utils.PacketSendUtility#sendMessage(Player, String)` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage` | Packet helper | Partial | Unit Tested | Partial Parity | C# uses golden-yellow `SM_MESSAGE` for the invalid-goods denial; broader `sendMessage` usages remain individually scoped. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopInvalidGoodsSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` invalid-goods branch + `PacketSendUtility.sendMessage` | Ordinary no-observer action 13 packet sends the exact Java text through golden-yellow `SM_MESSAGE` and leaves kinah unchanged | Java source review + live C# handler assertion | Does not cover invalid count separately or real client display. |

## Summary Metrics

- Focused UOW validation: 330 tests passed.
- Runtime progress: live NPC action `13` normal shop buy now sends the invalid-goods denial message.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: other buy denial packets, buy persistence, AP/token shop purchases, limited-item counters, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The live branch depends on the C# planner's allowed-goods result matching Java goods-list union behavior.
- Invalid count uses the same planner status but was not separately covered by the live handler test.
- The text packet is validated against reviewed Java source and existing C# `SmMessage` packet tests, not a new Java byte
  golden.
- Existing nullable/analyzer warnings appeared during focused validation; they are pre-existing and outside this UOW's
  changed lines.

## Next Runtime Candidate

UOW-2609 candidate: send the live full-inventory denial packet for NPC action `13` normal shop buys.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: full-inventory NPC shop buys should send Java denial from live CM_BUY_ITEM instead of only disabled send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> if freeSlots < tradeList.size() -> SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY and return false.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync using TradeBuyTransactionPlanStatus.BlockedInventoryFull and SmSystemMessage.FullInventory().
- Client-visible/state/persistence effect expected: live action 13 with insufficient free cube slots sends a real SM_SYSTEM_MESSAGE and does not mutate inventory.
- Why this is not preview-only/test-only/documentation-only if feasible: it sends a real server packet from the live client packet path.
```
