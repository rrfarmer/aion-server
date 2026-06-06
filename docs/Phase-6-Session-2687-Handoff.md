# Phase 6 Session 2687 Handoff

## Completed UOW

[Phase 6] UOW-2687: Preserve CM_CHARGE_ITEM selected item order.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_CHARGE_ITEM multi-item charging now processes selected item object ids in client packet order, matching Java's itemObjectIds list.
- Java source/runtime path: CM_CHARGE_ITEM.runImpl reads itemObjectIds into an ArrayList, resolves each inventory item in that order, then ItemChargeService.chargeItems iterates that ordered collection.
- C# runtime artifact wired: GameServerConnection.HandleChargeItemAsync.
- Client-visible/state/persistence effect: when AP/Kinah only covers part of a multi-item charge request, the item selected first in the client packet is charged first, mutating the matching inventory item/AP and emitting packets for that item.
- Why this is runtime progress: it changes live inventory/AP mutation and packet emission order from a client packet handler; it is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2687] Preserve charge item packet order`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2687-Completion.md`
- `docs/Phase-6-Session-2687-Handoff.md`

## Validation

Focused C# command attempted first:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --logger "console;verbosity=minimal"
```

Result:

- Timed out after 124 seconds before producing test results.
- This class-level filter was treated as too broad for the current focused validation policy.

Focused C# command used:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleChargeItemAsync_ProcessesSelectedItemsInPacketOrderLikeJava|FullyQualifiedName~HandleChargeItemAsync_ApPaymentSendsAbyssPointsPlannerPackets|FullyQualifiedName~HandleChargeItemAsync_ApPaymentRejectsInsufficientAbyssPointsWithoutSideEffects" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this packet-order branch in this checkout.

## Conservative Parity Status

- Live C# selected-item charge processing now follows Java packet-list order.
- The covered limited-AP scenario now charges the first packet-selected item, not the first matching inventory item.
- Complete `CM_CHARGE_ITEM` or `ItemChargeService` parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CHARGE_ITEM.runImpl` | `GameServerConnection.HandleChargeItemAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Selected item order now follows packet order. Complete charge-item parity is not claimed. |
| `ItemChargeService.chargeItems` | `ItemChargeService.CreateChargePlan` plus `GameServerConnection.HandleChargeItemAsync` loop | Service / live item mutation | Partial | Unit Tested indirectly | Partial Parity | Per-item iteration order is covered through the live handler. Other Java charge branches and duplicate-id behavior remain partially verified. |

## Known Gaps / Watchouts

- Do not claim full charge-item parity.
- Duplicate item object ids in one request were not separately tested.
- Mixed charge-way completion message ordering remains dependent on hash-set behavior and was not changed in this UOW.
- Broad class-level validation timed out; use narrower filters for this test file unless a broad trigger is documented.
- Java/Maven runtime comparison was not run.

## Next Recommended Runtime UOW

Recommended candidate: inspect another live/deferred inventory or item-use path for a concrete Java/C# runtime mismatch where existing C# inventory services can support a small mutation/packet fix.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: a real inventory/item-use client packet mutates inventory/player state, persists through existing repository shape, or emits a Java-equivalent packet currently missing or ordered incorrectly.
- Java source/runtime path: choose after source review confirms a concrete Java side effect, such as CM_USE_ITEM, CM_MOVE_ITEM, CM_SPLIT_ITEM, CM_DELETE_ITEM, CM_EQUIP_ITEM, or an item service method.
- C# runtime artifact likely involved: GameServerConnection packet handler plus the smallest existing inventory/item service and focused connection test.
- Client-visible/state/persistence effect expected: inventory item count/location/charge/equipment/AP/Kinah or emitted packet order changes from the current C# behavior toward Java.
- Why this is runtime progress: proceed only if implementation changes live runtime state, persistence, or packet emission; skip planner, readiness, metadata, or test-only work.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "<replace-with-edited-live-handler-test-name>|<one-or-two-adjacent-existing-test-names>" --logger "console;verbosity=minimal"
```

Prefer naming individual tests in `GameServerConnectionInventoryExpansionUseItemTests` if that file is touched; the full class-level filter timed out in this session.

## Other Safe Runtime Candidates

- Inspect `CM_CHARGE_ITEM` duplicate item-id behavior or mixed charge-way complete-message ordering only if discovery confirms a concrete live packet/state mismatch.
- Revisit Passport request-map iteration only if a concrete live packet/state/persistence ordering gap is confirmed.
- Move to a quest/AI/zone/command/dynamic-handler UOW only when Java source contains a non-empty runtime effect and C# has enough infrastructure to execute it.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview, readiness, metadata, and test-only recommendations unless the user explicitly requests them or they directly unblock a same-session runtime behavior change.
