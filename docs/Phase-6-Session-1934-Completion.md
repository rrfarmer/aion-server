# Phase 6 Session 1934 Completion - BUY_AGAIN Repurchase Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1934
Status: Completed

## Work Discovery

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and the latest handoff.
- Inspected Java `CM_BUY_ITEM`, `DialogService`, `DialogAction`, `SM_REPURCHASE`, and `RepurchaseService`.
- Inspected C# dialog-select planners, `GameServerConnection`, `SmRepurchase`, and existing repurchase planners/tests.
- Source-truth correction: Java `BUY_AGAIN` is dialog action `70`, not `CM_BUY_ITEM` action `18`; Java `CM_BUY_ITEM` repurchase remains action `2`.

## What Changed

- Added `CmDialogSelect.BuyAgain = 70`.
- Routed BUY_AGAIN through the existing disabled dialog-select diagnostic path.
- Threaded optional `SmRepurchase` snapshots through dialog branch/controller/service composition plans.
- Added `Player.RepurchaseItems` as a diagnostic snapshot surface for future repurchase-state hydration.
- Added socket regression coverage showing BUY_AGAIN records a disabled repurchase packet descriptor without sending packets.
- Added a guard test confirming `CM_BUY_ITEM` action `18` remains the Java unknown-action branch, not repurchase.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~NpcDialogControllerDispatchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~SmRepurchaseTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused dialog/repurchase slice passed with 78 tests.
- First broad attempt timed out without a result and was not counted as evidence.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4964 tests.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until compatible Java and Maven are available.
- BUY_AGAIN is still disabled diagnostics only; no live `SM_REPURCHASE` packet dispatch or singleton repurchase lookup is enabled.
- `Player.RepurchaseItems` is not live Java `RepurchaseService` state.
- Live sell-to-shop repurchase source writes, `CM_BUY_ITEM` action `2`, inventory/Kinah mutation, packets, repository writes, and transaction behavior remain disabled.

## Parity Table Updates

- Added Session 1934 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `DialogAction.BUY_AGAIN = 70` / `DialogService` BUY_AGAIN routing
  - `SM_REPURCHASE(Player, npcId)` packet snapshot payload
  - `RepurchaseService.getRepurchaseItems` diagnostic state surface
  - Java `CM_BUY_ITEM` action `18` unsupported regression guard
- All rows remain non-runtime-verified.
- Verified runtime parity count remains 0 for this unit.
