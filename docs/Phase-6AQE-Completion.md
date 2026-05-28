# Phase 6AQE Completion - Selected ItemCharge Equipped Lookup

Date: 2026-05-28
Unit of Work: UOW-1611
Status: Complete after focused validation

## Scope

This unit aligned the selected-item `CM_CHARGE_ITEM` lookup path with Java source shape. Java reads selected object ids through `player.getInventory().getItemByObjId(...)` and passes found items to `ItemChargeService.chargeItems(...)`; C# had an extra `!IsEquipped` filter before charge planning. The filter was removed for selected conditioning so equipped selected rows can reach the same charge/payment flow.

This is not Java runtime verification. It is source-derived C# regression coverage.

## Completed Work

- Source-read Java `CM_CHARGE_ITEM.runImpl`, `ItemStorage.getItemByObjId`, and `ItemChargeService.chargeItems`.
- Removed the selected-item `!IsEquipped` guard from `GameServerConnection.HandleChargeItemAsync`.
- Added `HandleChargeItemAsync_SelectedEquippedItemCanBeChargedLikeJavaInventoryLookup`.
- Kept the remaining `Location == CubeStorageId` guard unchanged pending a deeper Java inventory/equipment lifecycle audit.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, risks, metrics, and next-unit guidance.

## Validation

Focused charge tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~GameServerConnectionChargeAllQuestionResponseTests"
```

Result: 98 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Selected ItemCharge equipped lookup | `GameServerConnection.cs`, charge handler tests | Low | Yes | One handler predicate plus one regression; source-derived from Java object-id lookup. |
| Nearby-refresh Java handler/XML extraction | nearby/quest extractor services/tests | Medium | No | Safe next candidate after fresh ownership map. |
| ItemPurification side-effect persistence analysis | ItemPurification planner/report files | Medium | No | Separate subsystem; avoid mixing with charge handler edits. |
| Java protection serializer implementation | Java serializer/observer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because implementation and test ownership overlapped in the charge-handler surface.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleChargeItemAsync_SelectedEquippedItemCanBeChargedLikeJavaInventoryLookup` | Added | Selected equipped AP-conditioning item is accepted, AP is spent, `IsEquipped` remains true, item charge reaches level 1, and charge/stat/complete packets are emitted. | Source-derived from Java `CM_CHARGE_ITEM`, `ItemStorage.getItemByObjId`, and `ItemChargeService.chargeItems`; no Java runtime artifact. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` | Client Packet Handler | Partial | Regression Tested in C# | Partial Parity | Selected-item lookup no longer rejects equipped items before charge planning. No Java runtime packet trace or encrypted socket comparison was run. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.getItemByObjId` | C# `Player.InventoryItems` object-id lookup in `HandleChargeItemAsync` | Inventory Lookup Dependency | Partial | Regression Tested in C# | Needs Verification | Java storage returns by object id without an explicit equipped guard. C# still requires `Location == CubeStorageId`; exact Java storage membership for every lifecycle state remains unverified. |
| `com.aionemu.gameserver.services.item.ItemChargeService.chargeItems` | `Aion.GameServer.Services.ItemChargeService.CreateChargePlan` consumed by `GameServerConnection.HandleChargeItemAsync` | Service / Charge Mutation | Partial | Regression Tested in C# | Partial Parity | Regression verifies an equipped selected AP-conditioning item can be charged. Java `ChargeInfo.updateChargePoints`, observer attachment, stats internals, threading, and full side-effect fanout remain partial. |
| `com.aionemu.gameserver.services.item.ItemChargeService.processAPPayment` | `Aion.GameServer.Services.ItemChargeService.CreateAbyssPointPaymentPlan` | Service / AP Payment Guard | Partial | Regression Tested in C# | Partial Parity | Existing AP guard is exercised by the new equipped selected-item regression. C# intentionally rejects payments above `int.MaxValue`; Java overflow behavior is not reproduced. |
| `com.aionemu.gameserver.model.gameobjects.Item` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model / Item State | Partial | Regression Tested in C# | Needs Verification | Regression preserves `IsEquipped=true` while updating charge on the copied item snapshot. Java live-object identity and concurrent inventory/equipment behavior remain unverified. |

## Remaining Risks

- Java runtime behavior for equipped/non-cube inventory lookup has not been executed.
- C# still requires `Location == CubeStorageId` for selected charge items.
- Full Java `ChargeInfo`, observer attachment, stat recalculation, packet bytes, and socket ordering remain unverified.
- Repository failure/rollback ordering for selected equipped charge mutations was not newly tested.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 selected ItemCharge lookup parity fix plus 1 focused regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime inventory/equipment lifecycle comparison, Java packet trace, encrypted socket comparison, full `ChargeInfo` observer/stat internals, repository failure/rollback comparison, broader non-cube storage lookup semantics.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Move to a fresh Phase 6 safe slice. Recommended candidates:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Nearby-refresh Java handler/XML quest-start extraction | nearby/quest extractor services/tests | Good isolated extractor/test work if scoped carefully. |
| ItemPurification side-effect persistence analysis | ItemPurification planner/report files | Keep separate from charge handler work. |
| ItemCharge storage-location audit | read-first Java `Inventory`/`Equipment` lifecycle and C# selected-charge lookup | Only continue ItemCharge if auditing the remaining `Location == CubeStorageId` assumption. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1611] Allow selected equipped item charge lookup
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQE-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
