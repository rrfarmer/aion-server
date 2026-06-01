# Phase 6 Session 1939 Completion - Repurchase Outcome Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1939
Status: Completed

## Work Discovery

- Resumed from Session 1938's handoff and inspected Java `CM_BUY_ITEM` action `2`, Java `RepurchaseService.repurchaseFromShop`, Java `SM_REPURCHASE`, C# `CmBuyItemSideEffectOutcomePlanService`, C# repurchase read/run/composition planners, and C# `RepurchasePlanService`.
- Confirmed the socket handler now hydrates a disabled repurchase execution plan, but the side-effect outcome layer still returned handler-not-outcome-eligible for selected repurchase plans.

## What Changed

- Added `RepurchaseOutcomePlanService`, a disabled final-outcome planner for reviewed Java `RepurchaseService.repurchaseFromShop` side effects.
- The outcome plan records non-live player inventory/Kinah persistence, singleton repurchase-set removal, packet intents, insufficient-Kinah audit logging, and a side-effect boundary.
- Wired `CmBuyItemSideEffectOutcomePlanService` to emit `RepurchaseOutcomeCreated` for selected repurchase handler plans.
- Updated socket and unit tests so `CM_BUY_ITEM` action `2` reaches the disabled side-effect outcome boundary with no live dispatch.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests.ProcessPacketAsync_CmBuyItemNpcRepurchaseHydratesDisabledExecutionPlanFromSnapshot|FullyQualifiedName~RepurchasePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused repurchase outcome/socket slice passed with 30 tests.
- Wider buy-item/repurchase slice passed with 86 tests.
- Broad C# game-server suite passed with 4972 tests.
- Java/Maven reactor test run passed with 1 commons test and 12 game-server tests.

## Known Gaps

- No Java runtime/golden comparison was captured for `CM_BUY_ITEM` action `2`, `SM_REPURCHASE`, audit output, item-add packets, Kinah updates, or repurchase state.
- The new outcome plan remains disabled diagnostics only and does not mutate live state.
- Live `CM_BUY_ITEM` repurchase execution, live `RepurchaseService` singleton state, BUY_AGAIN socket dispatch, repository transaction behavior, NPC function validation, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1939 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `RepurchaseService.repurchaseFromShop` final side-effect summary
  - `CM_BUY_ITEM.runImpl` action `2` side-effect outcome composition
  - `RepurchaseService.repurchaseFromShop` insufficient-Kinah audit outcome summary
- All rows remain non-runtime-verified. Verified runtime parity count remains 0 for this unit.
