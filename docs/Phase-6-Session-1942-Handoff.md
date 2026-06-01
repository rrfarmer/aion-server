# Phase 6 Session 1942 Handoff - CM_BUY_ITEM Action 13 Trade-List Read Capture

Date: 2026-06-01
Unit of Work: UOW-1942
Status: Completed

## What Changed

- Added Java runtime/source-capture coverage for `CM_BUY_ITEM.readImpl` action `13`.
- The Java test captures the parser boundary where seller `7001`, action `13`, amount `2`, and item/count pairs `(100000001, 1)` plus `(100000002, 5)` create a non-audit `TradeList` in read order.
- Updated C# `CmBuyItemTests.ReadFrom_ReadsSellerActionAmountAndItemsLikeJava` to assert the same action `13` field and item sequence.
- Kept this as parser evidence only. No live `runImpl`, NPC target validation, `TradeService.performBuyFromShop`, inventory/Kinah/AP mutation, socket dispatch, or real-client validation was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemBuyFromShopCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 2 game-server tests.
- Focused C# `CmBuyItemTests` passed with 9 tests.
- Wider C# buy-item slice passed with 67 tests.
- Java/Maven reactor test run passed with 1 commons test and 15 game-server tests.
- Broad C# game-server suite passed with 4972 tests.

## Known Gaps

- Java audit branches remain uncaptured in Java runtime tests.
- Java action `2` `RepurchaseList.addRepurchaseItem` filtering remains outside this parser branch.
- The Java test uses test-only `Unsafe.allocateInstance` and reflection to avoid full connection construction and private field exposure. This is parser evidence only.
- Live `CM_BUY_ITEM.runImpl`, private-store seller state, repurchase/NPC/pet branch execution, encrypted frame decoding, and real-client validation remain pending.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1942-Completion.md`
- `docs/Phase-6-Session-1942-Handoff.md`

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

- Add Java-runtime/source-capture coverage for another non-audit `CM_BUY_ITEM` trade-list action such as sell-to-shop action `1` or pet sell action `17`.
- Extend Java golden coverage to a non-empty `SM_REPURCHASE` item entry if a minimal Java `Item`/`ItemTemplate` fixture can be created safely.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full socket cases without enabling live mutation.
- Continue repurchase toward live singleton-state adapter boundaries without enabling live mutation.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing parser-capture work, inspect Java `CM_BUY_ITEM.readImpl`, `RepurchaseList`, `RepurchaseService.canRepurchase`, C# `CmBuyItemTests`, and the previous non-live repurchase read planner.
- If action `2` isolation is too invasive, choose another non-audit trade-list action read boundary or switch to a disabled planner unit.
