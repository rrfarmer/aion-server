# Phase 6 Session 2605 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2605: Log private-store sales live. See
[Phase-6-Session-2605-Completion.md](Phase-6-Session-2605-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- `d15dc2f` - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`
- `8b85fa6` - `[Phase 6][UOW-2601] Send private-store seller kinah add live`
- `e5ab413` - `[Phase 6][UOW-2602] Order private-store seller messages live`
- `60a17a2` - `[Phase 6][UOW-2603] Interleave private-store buyer fanout live`
- `b3a0d9c` - `[Phase 6][UOW-2604] Persist private-store purchases live`
- Current commit - `[Phase 6][UOW-2605] Log private-store sales live`

## Session Summary

- Java review confirmed `PrivateStoreService.sellStoreItem` writes a successful sale line through `"EXCHANGE_LOG"`.
- Java review confirmed `PrivateStoreService.sellStoreItem` calls `AuditLogger.log` for negative price and stale seller
  stack abuse branches.
- C# private-store purchase plans already carried Java audit messages, but the live handler did not write them.
- `GameServerConnection.TryExecutePrivateStorePurchaseAsync` now logs blocked audit messages before returning from the
  live handler.
- `GameServerConnection.TryExecutePrivateStorePurchaseAsync` now logs a Java-shaped successful sale line for each applied
  bought item after the seller sale message is sent.
- Focused live handler tests now capture and assert both the successful sale log and stale seller stack audit warning.

## Files Changed In UOW-2605

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2605-Completion.md`
- `docs/Phase-6-Session-2605-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.utils.audit.AuditLogger#log`
- `org.slf4j.Logger` category `"EXCHANGE_LOG"`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests.CapturingLogger`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStore" --no-restore
```

Result: passed, 132/132.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler coverage. The filtered command built `Aion.GameServer` and
`Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` branches.

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

## Known Gaps

- C# logs through the connection `ILogger`; it does not yet create dedicated Java-equivalent `"EXCHANGE_LOG"` or
  `"AUDIT_LOG"` categories.
- C# audit logging does not notify online GMs and does not apply Java `PunishmentConfig` behavior.
- No external log sink or real server logging configuration was integration-tested.
- The negative-price/overflow audit branch now uses the same live logging hook when a plan exposes an audit message, but
  UOW-2605's live handler test covers the stale seller count branch only.
- Full Java `PrivateStore` object remains unported.
- Buyer existing-stack update fanout is supported by grouped metadata and persistence payloads but lacks a focused live
  timeline branch.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- NPC buy-from-shop `CM_BUY_ITEM` action 13 still needs fresh runtime discovery; earlier tests in this file still expose
  disabled/planner outcomes for trade-list paths.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution
  surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Dedicated Audit/Exchange Logger Categories

- Java has separate `"AUDIT_LOG"` and `"EXCHANGE_LOG"` categories, but adding a new logging abstraction/category-only
  surface without changing live handler behavior would be scaffolding.
- The selected UOW instead used the existing live connection logger so real runtime log side effects now fire.

### Negative-Price Audit Test Only

- The negative-price audit message is now logged by the same live `AuditMessage` hook when a plan reaches that branch.
- Adding only another focused test without a runtime behavior change would not pass the Runtime Progress Gate.

## Next Recommended Runtime UOW

**UOW-2606 candidate: wire the smallest live NPC buy-from-shop `CM_BUY_ITEM` transaction if discovery confirms the
current C# path is still planner-only or otherwise does not mutate live inventory/kinah and send purchase packets.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: NPC shop buy-from-list should execute from live CM_BUY_ITEM instead of stopping at plan/diagnostic output.
- Java source method or runtime path: TradeService.performBuyTransaction with ItemService.addItem and inventory kinah decrease.
- C# runtime artifact to wire or fix: GameServerConnection CM_BUY_ITEM action 13 branch plus TradeBuyTransactionPlanService and existing inventory/persistence packet helpers.
- Client-visible/state/persistence effect expected: active player kinah and inventory mutate, persistence is called where an existing surface exists, and item/kinah packets are sent from live code.
- Why this is not preview-only/test-only/documentation-only if feasible: it executes a live client packet purchase path with real inventory state and packet side effects.
```

Suggested focused validation if this UOW includes a runtime NPC shop buy fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~TradeBuyTransactionPlanServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: live inventory/packet
side effects are expected; start focused and document any broad skip.

## Safe Runtime Candidates

- NPC shop buy-from-list `CM_BUY_ITEM` action 13 live execution, only if discovery confirms the handler is still
  planner-only or missing runtime state/packet/persistence effects.
- Private-store buyer existing-stack update timeline only if discovery finds a live packet/state/persistence mismatch,
  not as branch coverage alone.
- Private-store negative-price or insufficient-kinah denial only if discovery finds a runtime logging/packet/state
  mismatch beyond existing live hooks.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.
- Java XML/static-data loading only when the data is immediately used by live code.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` sold-out single-item purchase is live as of UOW-2596.
- `CM_BUY_ITEM` player action `0` partial-stack/non-closing purchase is live as of UOW-2597, including seller pack-count decrement.
- `CM_BUY_ITEM` player action `0` missing seller inventory item branch is live as of UOW-2598, including Java's kinah-transfer behavior.
- `CM_BUY_ITEM` private-store packet fanout sends Java delete type and cube updates as of UOW-2599 for covered sold-out/new-item branches.
- `CM_BUY_ITEM` multi-row private-store purchase cube counts use per-item snapshots as of UOW-2600.
- `CM_BUY_ITEM` seller-without-kinah fanout sends Java's zero-count kinah add/cube before kinah update as of UOW-2601.
- `CM_BUY_ITEM` seller sale messages precede seller kinah packets as of UOW-2602.
- `CM_BUY_ITEM` buyer item add/update packets precede the corresponding seller sale message as of UOW-2603.
- `CM_BUY_ITEM` private-store purchase inventory/kinah mutations call the existing player inventory persistence surface as of UOW-2604.
- `CM_BUY_ITEM` private-store sale and stale-stack audit log side effects fire from the live handler as of UOW-2605.
- Existing disabled planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
