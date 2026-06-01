# Phase 6 Session 1943 Handoff - CM_BUY_ITEM Trade-List Action Set Read Capture

Date: 2026-06-01
Unit of Work: UOW-1943
Status: Completed

## What Changed

- Broadened Java runtime/source-capture coverage for `CM_BUY_ITEM.readImpl` trade-list actions.
- Java `CM_BUY_ITEM_ReadGuardGoldenTest` now verifies actions `1`, `13`, `14`, `15`, `16`, and `17` all create a non-audit `TradeList` with two entries in packet read order.
- C# `CmBuyItemTests` now uses a theory over the same action IDs and item/count values.
- Kept this as parser evidence only. No action `2` repurchase filtering, live `runImpl`, target validation, trade/private-store/pet mutation, socket dispatch, or real-client validation was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemSellToShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 2 test methods.
- Focused C# `CmBuyItemTests` passed with 14 tests.
- Wider C# buy-item slice passed with 83 tests.
- Java/Maven reactor test run passed with 1 commons test and 15 game-server tests.
- Broad C# game-server suite passed with 4977 tests.

## Known Gaps

- Java action `2` `RepurchaseList.addRepurchaseItem` filtering remains uncaptured in Java runtime tests.
- Java audit branches remain uncaptured in Java runtime tests.
- The Java test uses test-only `Unsafe.allocateInstance` and reflection. This is parser evidence only.
- Live `CM_BUY_ITEM.runImpl`, private-store/NPC/pet target validation, trade-template lookup, buy/sell transaction persistence, limited-item state, encrypted frame decoding, and real-client validation remain pending.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1943-Completion.md`
- `docs/Phase-6-Session-1943-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM`
- `com.aionemu.gameserver.model.trade.TradeList`
- `com.aionemu.gameserver.model.trade.TradeItem`
- `com.aionemu.gameserver.network.aion.AionClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmBuyItem`
- `Aion.GameServer.Tests.CmBuyItemTests`

## Next Recommended Unit of Work

- Next sequential task: inspect whether Java action `2` repurchase-list read behavior can be captured safely with a minimal player/repurchase singleton fixture.

Safe alternative candidates:

- Extend Java golden coverage to a non-empty `SM_REPURCHASE` item entry if a minimal Java `Item`/`ItemTemplate` fixture can be created safely.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full socket cases without enabling live mutation.
- Continue repurchase toward live singleton-state adapter boundaries without enabling live mutation.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing parser-capture work, inspect Java `RepurchaseList`, `RepurchaseService.canRepurchase`, `Player.getObjectId`, C# `CmBuyItemRepurchaseReadPlanService`, and `CmBuyItemTests`.
- If action `2` isolation is too invasive, switch to a disabled planner or diagnostic unit outside `CM_BUY_ITEM` parser coverage.
