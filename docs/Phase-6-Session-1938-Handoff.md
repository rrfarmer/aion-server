# Phase 6 Session 1938 Handoff - Repurchase Socket Diagnostic Hydration

Date: 2026-06-01
Unit of Work: UOW-1938
Status: Completed

## What Changed

- Added disabled `CM_BUY_ITEM` action `2` socket-level diagnostic hydration in `GameServerConnection.HandleBuyItem`.
- The handler now derives repurchasable object IDs from the active player's `RepurchaseItems`, builds a Java-shaped repurchase read plan from the packet items/audit item, and carries a non-live `RepurchasePlan` into the existing repurchase dispatch descriptor.
- Added a socket-level regression test proving the hydrated plan includes read IDs, removed repurchase IDs, Kinah update, added-item diagnostics, no live side effects, and no sent packets.
- Kept all execution diagnostics non-live. No live socket dispatch, singleton repurchase state, inventory/Kinah mutation, repository write, Java golden output, or real-client validation was enabled.

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
- The handler path remains disabled diagnostics only.
- `CmBuyItemSideEffectOutcomePlanService` still reports repurchase as handler-not-outcome-eligible.
- Live `RepurchaseService` singleton state, BUY_AGAIN socket dispatch, repository transaction behavior, NPC function validation, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1938-Completion.md`
- `docs/Phase-6-Session-1938-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM`
- `com.aionemu.gameserver.model.trade.RepurchaseList`
- `com.aionemu.gameserver.services.RepurchaseService.repurchaseFromShop`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.CmBuyItemHandlerCompositionPlanService`
- `Aion.GameServer.Services.CmBuyItemRepurchaseReadPlanService`
- `Aion.GameServer.Services.RepurchasePlanService`

## Next Recommended Unit of Work

- Next sequential task: add disabled `CmBuyItemSideEffectOutcomePlanService` outcome composition for repurchase action `2`, preserving non-live behavior and proving the hydrated repurchase plan can be summarized at the side-effect boundary.

Safe alternative candidates:

- Add Java-runtime golden/source-capture coverage for `CM_BUY_ITEM` action `2` or `SM_REPURCHASE`.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full socket cases without enabling live mutation.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Continue source-only Java condition capture hardening if Java runtime golden capture is still not practical for the targeted behavior.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing the repurchase path, start from Java `CM_BUY_ITEM`, Java `RepurchaseService`, C# `GameServerConnection.HandleBuyItem`, C# `CmBuyItemSideEffectOutcomePlanService`, and `GameServerConnectionBuyItemTests`.
- Keep all side effects disabled until Java runtime/golden or real-client evidence supports live mutation and packet dispatch.
