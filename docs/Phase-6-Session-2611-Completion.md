# Phase 6 Session 2611 Completion

## UOW

[Phase 6] UOW-2611: Send NPC shop abyss-point denial live

## Status

Completed and validated with focused live handler, transaction planner, and packet coverage. Live `CM_BUY_ITEM` action
`13` for normal NPC shop buys now sends Java `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT` (`1300927`) when AP
shortage, missing required-item, or negative required-AP audit blocks the requested purchase. The live denial path
returns before inventory, kinah, required-item, or AP mutation.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: AP/required-item/negative-AP NPC shop buy failures now send the Java denial packet from live CM_BUY_ITEM instead of only composing disabled planner send intents.
- Java source method or runtime path: TradeService.performBuyTransaction -> !tradeList.calculateAbyssRewardBuyList or tradeList.getRequiredAp() < 0 -> SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT and return false.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync consumes TradeBuyTransactionPlanStatus.BlockedNotEnoughAbyssPoints, BlockedNotEnoughRequiredItems, and AuditNegativeRequiredAp with SmSystemMessage.MsgNotEnoughAbyssPoints().
- Client-visible/state/persistence effect changed: live action 13 blocked by AP, required-item, or negative required-AP sends a real SM_SYSTEM_MESSAGE denial and does not mutate inventory/AP.
- Why this is not preview-only/test-only/documentation-only: this sends a real server packet from the live client packet path.
```

## Java Source Reviewed

- `TradeService.performBuyTransaction` calls `tradeList.calculateAbyssRewardBuyList(player, apSellModifier)` after the
  kinah check and sends `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT()` when it returns `false`.
- `TradeService.performBuyTransaction` audits `tradeList.getRequiredAp() < 0`, sends the same not-enough-AP packet, and
  returns `false`.
- `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT` is message id `1300927`.

## C# Changes

- Added `SmSystemMessage.MsgNotEnoughAbyssPoints()` for Java message id `1300927`.
- Changed `GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` so AP shortage, missing required item, and negative
  required-AP audit statuses send the Java not-enough-AP denial from live action `13`.
- Added live no-observer tests proving AP shortage, required-item shortage, and negative required-AP requests send
  `1300927` without mutating inventory/AP.
- Added a packet helper assertion in `GamePacketTests`.

## Known Gaps

- Successful AP/required-item NPC shop purchases remain disabled by the success-only live guard.
- Negative required-AP audit logging remains planner metadata only; the live packet denial is wired.
- Successful NPC shop buys still do not persist kinah/item mutations through a repository surface.
- Successful limited-item counter updates are not live or persisted.
- Live NPC shop execution remains limited to `TradeNpcType.NORMAL`, action `13`, kinah-only successful purchases.
- `ABYSS_KINAH`, `ABYSS`, and `REWARD` buy branches remain non-live.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM action 13 AP/required-item/negative required-AP denial packet plus system-message helper.
- Specific behavior/contract: AP shortage, missing required item, and negative required AP send STR_MSG_NOT_ENOUGH_ABYSSPOINT (1300927), emit no inventory/AP mutation, and keep the no-observer live path active.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore -> 335/335 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet side effect changed.
- Broad .NET decision: skipped after focused live handler/planner/packet coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing no-observer tests dispatch encoded CM_BUY_ITEM packets through the ordinary live path and assert the Java not-enough-AP message plus unchanged inventory/AP state; GamePacketTests pins the message id.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.TradeService#performBuyTransaction` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecuteBuyFromShopPurchaseAsync` | Service path in handler | Partial | Unit Tested | Partial Parity | Covers normal kinah success and invalid-goods, insufficient-kinah, AP/required-item/negative-AP, full-inventory, and limited-item denials; AP/token successes, persistence, audit logging, and some branches remain incomplete. |
| `com.aionemu.gameserver.model.trade.TradeList#calculateAbyssRewardBuyList` | `Aion.GameServer.Services.TradeBuyTransactionPlanService.CreatePlan` | Service/planner | Partial | Unit Tested | Partial Parity | Planner already detects AP and required-item shortages; live handler now consumes those blocked statuses for the packet effect. Successful AP/required-item mutation remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_NOT_ENOUGH_ABYSSPOINT` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.MsgNotEnoughAbyssPoints` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300927` is pinned; no full Java packet byte golden was run this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopNotEnoughAbyssPointsSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` AP shortage branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_ABYSSPOINT` and leaves kinah/AP unchanged when AP is insufficient | Java source review + live C# handler assertion | Does not execute successful AP-cost purchase. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopMissingRequiredItemSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` required-item shortage branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_ABYSSPOINT` and leaves inventory unchanged when required items are missing | Java source review + live C# handler assertion | Does not decrease required items on success. |
| `ProcessPacketAsync_CmBuyItemNpcBuyFromShopNegativeRequiredApSendsLiveDenialWithoutMutation` | Unit/live handler | `TradeService.performBuyTransaction` negative required-AP audit branch | Ordinary no-observer action 13 packet sends `STR_MSG_NOT_ENOUGH_ABYSSPOINT` and leaves kinah/AP unchanged for negative AP metadata | Java source review + live C# handler assertion | Live audit logging remains disabled. |
| `GamePacketTests` system-message assertion | Unit/packet | `SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_ABYSSPOINT` | Pins C# helper to Java message id `1300927` | Java source review + C# packet assertion | Not a full byte-for-byte Java golden. |

## Summary Metrics

- Focused UOW validation: 335 tests passed.
- Runtime progress: live NPC action `13` normal shop buy now sends the not-enough-AP denial packet for AP/required-item
  and negative required-AP failures.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: buy persistence, successful AP/required-item mutation, AP/token shop purchases, limited-item counter
  updates, real client validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Required-item availability currently comes from the C# fact adapter; successful required-item decrease is not live.
- Negative required AP sends the Java denial packet but does not yet write the Java audit log.
- The packet id is pinned from Java source review and C# packet tests, not a new Java byte-for-byte golden.
- Existing nullable/analyzer warnings appeared during focused validation; they are pre-existing and outside this UOW's
  changed lines.

## Next Runtime Candidate

UOW-2612 candidate: persist successful normal NPC shop kinah buys through an existing C# repository boundary if a
minimal save surface can be reused safely.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: successful normal NPC shop kinah buys currently mutate live in-memory inventory/kinah and send packets, but item/kinah state is not persisted.
- Java source method or runtime path: TradeService.performBuyTransaction -> player.getInventory().decreaseKinah(...) and ItemService.addItem(...) with Java inventory persistence side effects.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecuteBuyFromShopPurchaseAsync plus the existing player inventory persistence/repository surface.
- Client-visible/state/persistence effect expected: successful action 13 normal kinah shop buys persist bought item rows and kinah decrease using the existing database shape.
- Why this is not preview-only/test-only/documentation-only if feasible: it persists/restores live inventory state rather than only reporting planner readiness.
```
