# Phase 6 Session 1903 Completion - Pet Merchant Disabled Executor Facade

Date: 2026-05-31
Unit of Work: UOW-1903
Status: Completed

## Work Discovery

- Re-read required Phase 6 orchestration, parity, progress, completion, and handoff context before selecting work.
- Inspected Java `CM_BUY_ITEM.runImpl` pet branch and Java `TradeService.performSellToShop`.
- Inspected C# `CmBuyItemHandlerCompositionPlanService`, `TradeSellToShopPlanService`, and the newly added private-store disabled executor facade pattern.
- Confirmed the next safe unit was a disabled pet merchant facade, not live pet merchant execution.

## What Changed

- Added `PetMerchantSellLiveExecutorFacadePlanService.CreateDisabledPlan`.
- Added pet merchant facade status, operation kind/status, operation, and plan records.
- The facade consumes a `CmBuyItemHandlerCompositionPlan` selected for pet-target action `17`.
- For ready pet sell-to-shop plans, it records Java side-effect boundaries without dispatch:
  - seller inventory delete/decrease
  - repurchase item addition
  - Kinah increase
- Added terminal statuses for missing handler plan, non-pet-merchant handler plan, missing sell modifier, missing sell-to-shop plan, and blocked sell-to-shop plan.
- Added `PetMerchantSellLiveExecutorFacadePlanServiceTests`.
- Kept this work disabled and non-live. No inventory mutation, Kinah mutation, repurchase mutation, packet send, repository write, Java runtime capture, or real client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~PrivateStoreLiveExecutorFacadePlanServiceTests|FullyQualifiedName~TradeSellToShopPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~TradeSellForApToShopPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused pet merchant facade slice passed with 29 tests.
- Related sell/private-store/CM_BUY_ITEM slice passed with 64 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4871 tests.
- Test builds emitted existing nullable/analyzer warnings in unrelated files.

## Known Gaps

- No Java runtime/golden comparison was captured.
- Local Java execution remains blocked until JDK 25-compatible Java and Maven are available.
- The facade is disabled and non-live by design.
- Live pet object/template lookup, merchant function metadata, seller inventory mutation, repurchase state mutation, Kinah mutation, packet sends, persistence, transaction boundaries, real known-list facts, and real client behavior remain unwired.

## Parity Table Updates

- Added Session 1903 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl` pet merchant action `17` live side-effect boundary facade
  - `TradeService.performSellToShop(..., pf.getRatePrice())` pet modifier executor boundary
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly not Java-runtime verified.
- Verified runtime parity count remains 0 for this unit.

## Next Recommended Unit of Work

- Next sequential task: investigate replacing the current world-object-only `CM_BUY_ITEM` diagnostic target classification with a per-player known-list membership service or explicit known-list fact adapter.

Safe alternative candidates:

- Connect `TradeSellForApToShopPlanService` as an optional non-live payload inside `CmBuyItemSellToShopCompositionPlanService` for ABYSS purchase-template dispatch if not already covered in the active branch ordering.
- Add disabled persistence/send adapter plans for private-store purchase outputs before any live execution attempt.
- Add disabled persistence/send adapter plans for pet merchant sell outputs before any live execution attempt.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime remains unavailable.
