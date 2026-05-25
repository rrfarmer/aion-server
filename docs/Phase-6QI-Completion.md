# Phase 6QI Completion Handoff - Live ItemCharge Kinah Guard Consolidation

Date: May 25, 2026
Unit of Work: UOW-939
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-939] Consolidate live ItemCharge Kinah guard`)

## Status

Phase 6 is still in progress. This unit adds a pure Kinah payment guard for ItemCharge and wires it into the live selected-item and charge-all C# handlers.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemChargeService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemChargeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QI-Completion.md`

## What Changed

- Added `ItemChargeService.CreateKinahPaymentPlan`.
- Added `ItemChargeKinahPaymentPlan` and `ItemChargeKinahPaymentStatus`.
- The Kinah guard returns:
  - `Ready` with a copied Kinah row update when payment is affordable
  - `NoPaymentRequired`
  - `NoKinahItem`
  - `InsufficientKinah`
- Rewired selected-item and charge-all live Kinah payment checks in `GameServerConnection` to use the shared guard.
- Added fixture static-data support for a charge-way-1 conditioning item and Kinah item template.
- Added service-level and live-handler insufficient-Kinah regressions.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Pure Kinah payment guard | `ItemChargeService.processKinahPayment`, `Storage.tryDecreaseKinah` | `ItemChargeService.cs`, service tests | Service/Test | Sequential writer | Low | Completed; compact pure planner. |
| B | Selected-item Kinah live guard | `CM_CHARGE_ITEM`, `processKinahPayment` | `GameServerConnection.cs`, live handler tests | Handler/Test | Sequential writer | Medium | Completed; same handler ownership as charge-all. |
| C | Charge-all Kinah live guard | `startChargingEquippedItems`, `processKinahPayment` | `GameServerConnection.cs`, live handler tests | Handler/Test | Sequential writer | Medium | Completed after selected-item because both edit the same handler file. |
| D | Charge-all pending item drift | `startChargingEquippedItems`, `chargeItems` | read-only Java/C# charge-all files | Analysis/Next | Yes | Medium | Deferred; it is the next ItemCharge parity risk after payment guards. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionInventoryExpansionUseItemTests|ItemChargeServiceTests"
```

Result: passed, 63 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1618 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemChargeService.processKinahPayment` | `Aion.GameServer.Services.ItemChargeService.CreateKinahPaymentPlan` | Service / Kinah Payment Guard | Partial | Regression Tested in C# | Partial Parity | C# now has a pure Kinah affordability guard returning a copied Kinah row update. Full Java `Storage.tryDecreaseKinah` side effects are not executed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync` | Client Handler | Partial | Regression Tested in C# | Partial Parity | Selected-item Kinah payment now uses the shared guard before persistence and packet fanout. Java runtime packet comparison remains unavailable. |
| `com.aionemu.gameserver.services.item.ItemChargeService.startChargingEquippedItems` | `Aion.GameServer.Network.Aion.GameServerConnection.StartChargingEquippedItemsAsync` and `HandleChargeAllQuestionResponseAsync` | Service / Request Flow | Partial | Regression Tested in C# | Partial Parity | Charge-all accept response now uses the shared Kinah guard before repository mutation or packet fanout. Pending item drift remains open. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.ItemChargeService.CreateKinahPaymentPlan` plus `GameServerConnection` persistence boundary | Storage / Currency Mutation Boundary | Partial | Regression Tested in C# | Needs Verification | C# represents Kinah as an inventory item copy and persists through charge mutation repositories; Java storage locking and dirty-state behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet DTO | Partial | Existing Regression Tested in C# | Needs Verification | Success-path tests cover Kinah update packet type/order; this unit adds negative-path no-packet checks. Byte-level Java packet comparison remains blocked. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateKinahPaymentPlan_RejectsMissingOrInsufficientKinahBeforeMutation` | Regression | Java `ItemChargeService.processKinahPayment` / `Storage.tryDecreaseKinah` source review | Missing and insufficient Kinah fail without producing a Kinah row update or mutating the source item. | Deterministic C# service regression for Java affordability failure. | Does not execute Java storage internals. |
| `CreateKinahPaymentPlan_CreatesKinahUpdateWhenAffordable` | Regression | Java `Storage.tryDecreaseKinah` source review | Affordable Kinah payment returns a copied Kinah row with reduced count and leaves the original row unchanged. | Deterministic C# service regression for planned mutation shape. | Repository transaction and packet fanout remain live-handler responsibilities. |
| `CreateKinahPaymentPlan_SkipsZeroPayment` | Regression | Java charge payment source review | Zero/negative payment creates no Kinah update and succeeds as a no-op. | Deterministic C# service regression. | Java runtime no-op path not captured. |
| `HandleChargeItemAsync_KinahPaymentRejectsInsufficientKinahWithoutSideEffects` | Regression | Java `CM_CHARGE_ITEM.runImpl` -> `ItemChargeService.chargeItems` -> `processKinahPayment` source review | Selected-item insufficient Kinah leaves item charge and Kinah unchanged, skips repository persistence, and sends no Kinah, charge, stats, success, or all-complete packets. | Deterministic C# live-handler regression for Java Kinah failure path. | No Java runtime packet trace. |
| `HandleQuestionResponseAsync_ChargeAllKinahPaymentRejectsInsufficientKinahWithoutSideEffects` | Regression | Java `ItemChargeService.startChargingEquippedItems.acceptRequest` source review | Charge-all insufficient Kinah consumes the response request, leaves item charge and Kinah unchanged, skips repository persistence, and sends no Kinah, charge, stats, success, or all-complete packets. | Deterministic C# live-handler regression for Java accept-request Kinah failure path. | No Java runtime response-order artifact. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `Storage.tryDecreaseKinah` full Java behavior, including storage locking, dirty-state, item packet helper integration, and exact failure messaging, is not runtime-verified.
- Charge-all request creation snapshots planned charges before accept; Java keeps live `Item` references and recalculates charge eligibility on accept.
- Broader inventory repository transaction semantics remain outside this unit.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 2 live ItemCharge Kinah caller guard integrations plus 1 pure Kinah payment guard
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including Java runtime artifact generation, full Java storage mutation semantics, charge-all pending inventory drift, and broader repository transaction parity
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Audit and harden charge-all pending item drift: compare Java's captured live `Item` references and `chargeItems(... requirePayment=false)` accept-time recalculation against C# `PendingChargeAllRequest` snapshots, then add focused regressions for an item already charged or missing between question and accept without overhauling repository transactions.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java charge-all drift audit | read-only `ItemChargeService.java` and `CM_CHARGE_ITEM.java` | Low | Confirm accept-time recalculation and payment behavior. |
| B | C# charge-all drift audit | read-only `GameServerConnection.cs`, pending request model/tests | Low | Identify whether missing/already-charged items mutate incorrectly. |
| C | Focused drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs`, maybe `GameServerConnection.cs` | Medium | Do after audits because handler behavior may need a small recalculation helper. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Charge-all drift hardening with broad repository transaction rewrites.
- ItemCharge payment work with unrelated AP rank/legion/siege side effects.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, audit and harden the charge-all pending item drift boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
