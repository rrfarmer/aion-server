# Phase 6 Session 1938 Completion - Repurchase Socket Diagnostic Hydration

Date: 2026-06-01
Unit of Work: UOW-1938
Status: Completed

## Work Discovery

- Re-read the required orchestration/parity documents earlier in this orchestration pass, then resumed from Session 1937's completion and handoff.
- Inspected Java `CM_BUY_ITEM` action `2`, Java `RepurchaseList`, Java `RepurchaseService.repurchaseFromShop`, C# `GameServerConnection.HandleBuyItem`, C# handler composition, C# repurchase read/run/composition planners, and C# `RepurchasePlanService`.
- Confirmed the C# socket handler selected the disabled repurchase composition path but did not populate repurchasable object IDs or an execution diagnostic plan from the active player snapshot.

## What Changed

- Added disabled `CM_BUY_ITEM` action `2` socket diagnostic hydration in `GameServerConnection.HandleBuyItem`.
- The handler now resolves eligible repurchase object IDs from `Player.RepurchaseItems`, creates the read plan from packet items plus audit item, and builds a non-live `RepurchasePlan` from player inventory, item templates, and the diagnostic object-id provider.
- Added a socket-level regression test proving the handler carries a hydrated action `2` execution payload while `ShouldDispatchLiveSideEffects` remains false and no packets are sent.
- Kept the unit disabled/non-live. No live singleton repurchase state, inventory/Kinah mutation, repository write, BUY_AGAIN packet send, or real-client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests.ProcessPacketAsync_CmBuyItemNpcRepurchaseHydratesDisabledExecutionPlanFromSnapshot|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused socket/repurchase slice passed with 40 tests.
- Wider buy-item/repurchase slice passed with 84 tests.
- Broad C# game-server suite passed with 4970 tests.
- Java/Maven reactor test run passed with 1 commons test and 12 game-server tests.

## Known Gaps

- No Java runtime/golden comparison was captured for `CM_BUY_ITEM` action `2`, `SM_REPURCHASE`, audit output, item-add packets, Kinah updates, or repurchase state.
- The socket hydration remains disabled diagnostics only and does not mutate live state.
- Repurchase is still not summarized by `CmBuyItemSideEffectOutcomePlanService`; it currently remains handler-not-outcome-eligible.
- Live `CM_BUY_ITEM` repurchase execution, live `RepurchaseService` singleton state, BUY_AGAIN socket dispatch, repository transaction behavior, NPC function validation, and real-client validation remain pending.

## Parity Table Updates

- Added Session 1938 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `CM_BUY_ITEM.runImpl` action `2` disabled socket diagnostic hydration
  - `RepurchaseList.addRepurchaseItem` snapshot-derived eligible object IDs
  - `RepurchaseService.repurchaseFromShop` execution inputs carried to `RepurchasePlanService`
- All rows remain non-runtime-verified. Verified runtime parity count remains 0 for this unit.
