# Phase 6 Session 1944 Handoff - CM_BUY_ITEM Action 2 Repurchase Read Capture

Date: 2026-06-01
Unit of Work: UOW-1944
Status: Completed

## What Changed

- Added Java runtime/source-capture coverage for `CM_BUY_ITEM.readImpl` action `2`.
- The Java test seeds `RepurchaseService` with shell repurchase items for a shell player, reads payload ids `[101, 999, 102, 101]`, and confirms `RepurchaseList` stores `[101, 102]` in first-seen order with duplicates suppressed.
- The original `RepurchaseService` singleton map is restored after the test.
- Existing C# `CmBuyItemRepurchaseReadPlanServiceTests` already cover the same filtered-order shape with supplied repurchasable object ids.
- Kept this as parser/read-filter evidence only. No `runImpl`, NPC target validation, live repurchase execution, inventory/Kinah mutation, socket dispatch, or real-client validation was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemRepurchaseReadPlanServiceTests|FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemRepurchaseRunPlanServiceTests|FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java read-capture test passed with 3 test methods.
- Focused C# repurchase read planner tests passed with 9 tests.
- Wider C# repurchase/buy-item slice passed with 53 tests after a serial rerun.
- Java/Maven reactor test run passed with 1 commons test and 16 game-server tests.
- Broad C# game-server suite passed with 4977 tests.

## Known Gaps

- Java audit branches remain uncaptured in Java runtime tests.
- The Java test uses test-only `Unsafe.allocateInstance`, `Unsafe.objectFieldOffset`/`putInt`, and reflection. This is parser evidence only.
- Live `CM_BUY_ITEM.runImpl`, NPC target validation, `RepurchaseService.repurchaseFromShop`, inventory/Kinah mutation, item add behavior, encrypted frame decoding, and real-client validation remain pending.
- C# live player-bound repurchase singleton state remains unwired.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1944-Completion.md`
- `docs/Phase-6-Session-1944-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM`
- `com.aionemu.gameserver.model.trade.RepurchaseList`
- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.model.gameobjects.AionObject`
- `com.aionemu.gameserver.model.gameobjects.Item`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.CmBuyItemRepurchaseReadPlanService`
- `Aion.GameServer.Tests.CmBuyItemRepurchaseReadPlanServiceTests`

## Next Recommended Unit of Work

- Next sequential task: inspect whether a safe Java runtime capture can cover one `CM_BUY_ITEM` audit guard by disabling audit side effects or using a null-safe/static-state-safe setup.

Safe alternative candidates:

- Extend Java golden coverage to a non-empty `SM_REPURCHASE` item entry if a minimal Java `Item`/`ItemTemplate` fixture can be created safely.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full socket cases without enabling live mutation.
- Continue repurchase toward live singleton-state adapter boundaries without enabling live mutation.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing `CM_BUY_ITEM` parser-capture work, inspect Java `AuditLogger`, `AutoBan`, `GMService`, logging/punishment config defaults, and C# `CmBuyItemTests`.
- If audit capture is too invasive, switch to a disabled planner or diagnostic unit outside `CM_BUY_ITEM` parser coverage.
