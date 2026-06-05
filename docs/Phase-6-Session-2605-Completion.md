# Phase 6 Session 2605 Completion

## UOW

[Phase 6] UOW-2605: Log private-store sales live

## Status

Completed and validated with focused live handler and adjacent private-store coverage. Live `CM_BUY_ITEM`
private-store purchases now emit Java-shaped runtime log side effects for successful private-store sales and stale
seller stack abuse attempts.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: private-store sale and abuse audit logging now fire from live CM_BUY_ITEM instead of remaining planner metadata.
- Java source method or runtime path: PrivateStoreService.sellStoreItem writes the [PRIVATE STORE] sale exchange log and calls AuditLogger.log for negative price or stale seller stack abuse.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecutePrivateStorePurchaseAsync now logs blocked audit messages and successful applied sale rows through the live connection logger.
- Client-visible/state/persistence effect changed: live private-store success and audit branches now emit operational log side effects from the handler path.
- Why this is not preview-only/test-only/documentation-only: the live packet handler now performs real runtime logging during actual CM_BUY_ITEM execution.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` uses logger category `"EXCHANGE_LOG"` and writes
  `[PRIVATE STORE] > [Seller: ...] sold [Item: ...][Amount: ...] to [Buyer: ...] for [Price: ...]` after sending the
  seller sale message.
- `PrivateStoreService.sellStoreItem` calls `AuditLogger.log(buyer, "tried to buy item with negative kinah price from private store")`
  for negative/overflow price calculations.
- `PrivateStoreService.sellStoreItem` calls `AuditLogger.log(buyer, "tried to buy more than players private store item stack count")`
  when the seller's live inventory stack is smaller than the requested private-store count.
- `AuditLogger.log` writes to `"AUDIT_LOG"` when enabled and can notify online GMs or apply configured punishment.

## C# Changes

- Changed `GameServerConnection.TryExecutePrivateStorePurchaseAsync` to log `PrivateStorePurchasePlan.AuditMessage`
  from the live blocked purchase branch before returning.
- Changed `GameServerConnection.TryExecutePrivateStorePurchaseAsync` to emit a Java-shaped `[PRIVATE STORE]` information
  log for each applied bought item after the seller sale message is sent.
- Extended `GameServerConnectionBuyItemTests` with a capturing `ILogger` fixture so live handler tests can assert log
  side effects without depending on an external sink.
- Updated the live sold-out private-store purchase test to assert the successful sale log.
- Added a live stale-seller-count `CM_BUY_ITEM` test that asserts audit logging and no state, packet, or store mutation.

## Known Gaps

- C# logs through the connection `ILogger`; it does not yet create dedicated Java-equivalent `"EXCHANGE_LOG"` or
  `"AUDIT_LOG"` categories.
- C# audit logging does not notify online GMs and does not apply Java `PunishmentConfig` behavior.
- No external log sink or real server logging configuration was integration-tested.
- The negative-price/overflow audit branch now uses the same live logging hook when a plan exposes an audit message, but
  this UOW's live handler test covers the stale seller count branch only.
- Full Java `PrivateStore` object behavior remains unported.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store runtime logging.
- Specific behavior/contract: live success emits a Java-shaped private-store sale log per applied item; blocked stale seller-count abuse emits the buyer audit warning and stops before state/packet mutation.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStore" --no-restore -> 132/132 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live handler logging side effect changed.
- Broad .NET decision: skipped after focused live handler coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified CM_BUY_ITEM branches.
- Why this scope is sufficient: the passing tests dispatch encoded CM_BUY_ITEM packets and assert the new log side effects alongside existing state, persistence, and packet timeline assertions.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Live success now emits a Java-shaped private-store sale log and stale-count abuse emits an audit warning; full PrivateStore behavior remains incomplete. |
| `org.slf4j.Logger` category `"EXCHANGE_LOG"` private-store sale row | `ILogger.LogInformation` in `GameServerConnection` | Runtime log | Partial | Unit Tested | Partial Parity | Message shape matches Java sale record, but the logger category/sink is not Java-equivalent. |
| `com.aionemu.gameserver.utils.audit.AuditLogger#log` | `ILogger.LogWarning` in `GameServerConnection` | Runtime audit log | Partial | Unit Tested | Partial Parity | Stale seller stack branch logs the Java audit message; GM notification, config gating, and punishment remain unported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` exchange log | Live sold-out private-store purchase emits the Java-shaped `[PRIVATE STORE]` sale log while preserving packet/state/persistence assertions | Java source review + live C# handler assertion | Does not verify an external logger sink or Java logger category. |
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreStaleSellerCountLogsAuditAndStops` | Unit/live handler | `PrivateStoreService.sellStoreItem` + `AuditLogger.log` | Stale seller inventory count blocks the purchase, emits the Java audit message, and leaves buyer/seller state and packets untouched | Java source review + live C# handler assertion | Covers stale stack audit only, not negative-price audit. |

## Summary Metrics

- Focused UOW validation: 132 tests passed.
- Runtime progress: live private-store success and stale-count audit branches now emit operational log side effects.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 2.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: Java-equivalent audit category/sink, GM notification, punishment config, full Java `PrivateStore` object, real client/server log validation.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Runtime logs are only as durable as the configured C# logging sink.
- The log side effect is intentionally lightweight and does not yet model Java's audit notification or punishment behavior.
- Private-store logging coverage is focused on live `CM_BUY_ITEM`; other audit surfaces still need separate runtime discovery.

## Next Runtime Candidate

UOW-2606 candidate: wire the smallest live NPC buy-from-shop `CM_BUY_ITEM` transaction if discovery confirms the current
C# path is still planner-only or otherwise does not mutate live inventory/kinah and send purchase packets.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: NPC shop buy-from-list should execute from live CM_BUY_ITEM instead of stopping at plan/diagnostic output.
- Java source method or runtime path: TradeService.performBuyTransaction with ItemService.addItem and inventory kinah decrease.
- C# runtime artifact to wire or fix: GameServerConnection CM_BUY_ITEM action 13 branch plus TradeBuyTransactionPlanService and existing inventory/persistence packet helpers.
- Client-visible/state/persistence effect expected: active player kinah and inventory mutate, persistence is called where an existing surface exists, and item/kinah packets are sent from live code.
- Why this is not preview-only/test-only/documentation-only if feasible: it executes a live client packet purchase path with real inventory state and packet side effects.
```
