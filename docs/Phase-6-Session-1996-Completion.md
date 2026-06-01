# Phase 6 Session 1996 Completion - Pet Auto-Sell Parser Golden

Date: 2026-06-01
Unit of Work: UOW-1996
Status: Completed

## Work Discovery

- Re-read the required migration docs, latest completion/handoff, and progress file before choosing work.
- Inspected Java `CM_PET.readImpl` and `CM_PET.runImpl`.
- Inspected Java `PetService.activateAutoSell`.
- Inspected C# `CmPet`, `CmPetAutoSellActivationCompositionPlanService`, `PetAutoSellActivationPlanService`, and existing pet tests.

## What Changed

- Added Java golden test `CM_PET_AutoSellReadGoldenTest`.
- The Java test proves FOOD/actionType 4 parser field order: action id, action type, signed `activateSpecialFunction`, and two trailing D-word padding reads.
- Extended C# `CmPetTests` with a high-bit activation value to prove the disabled autosell composition treats any non-zero signed activation value as true, matching Java `activateSpecialFunction != 0`.
- Kept production behavior unchanged. No live pet mutation, audit logging, packet dispatch, persistence, threading, or real-client behavior was enabled.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_PET_AutoSellReadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmPetTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_PET` autosell parser golden test passed with 1 test method.
- Focused C# `CmPetTests` passed with 28 tests.
- Broad C# game-server suite passed with 5102 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 53 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPet.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`.
- Objective evidence is limited to Java parser golden coverage, C# parser/composition unit coverage, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_PET.runImpl`, `PetService.activateAutoSell`, `PetCommonData.setIsSelling`, audit logging, `SM_PET(AUTOSELL)` dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_AutoSellReadGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmPetTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1996-Completion.md`
- `docs/Phase-6-Session-1996-Handoff.md`

## Remaining Risks

- `CM_PET` actionType 4 remains disabled at the live side-effect boundary.
- Pet common-data selling state, audit logging, packet send ordering, persistence, threading, encrypted frame behavior, and real-client behavior are unverified.
- Other `CM_PET` branches still need branch-specific evidence where not already covered.

## Next Recommended Unit

- Choose another compact parser/factory/model boundary with Java golden evidence, or inspect another `CM_PET` sub-branch only if it can remain disabled and source-reviewed.

Safe alternatives:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.
