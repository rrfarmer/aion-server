# Phase 6 Session 1996 Handoff - Pet Auto-Sell Parser Golden

Date: 2026-06-01
Unit of Work: UOW-1996
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_PET.readImpl` FOOD/actionType 4 autosell payloads.
- Extended C# `CmPetTests` to cover a high-bit signed `activateSpecialFunction` value through the disabled autosell composition planner.
- Strengthened the boundary between Java `activateSpecialFunction != 0` and C# `ParsedActivationFlag`.
- No production pet behavior, live packet dispatch, audit logging, persistence, threading, or real-client validation was enabled.

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

## Known Gaps

- This unit proves only autosell parser field-order/padding and disabled-composition activation truth behavior.
- Live `CM_PET.runImpl` actionType 4 remains incomplete.
- Live `PetService.activateAutoSell`, `PetCommonData.setIsSelling`, audit logger effects, and `SM_PET(AUTOSELL)` send ordering remain disabled and unverified.
- Encrypted frame capture and real-client pet autosell behavior remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_PET_AutoSellReadGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmPetTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1996-Completion.md`
- `docs/Phase-6-Session-1996-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_PET`
- `com.aionemu.gameserver.services.toypet.PetService`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmPet`
- `Aion.GameServer.Services.CmPetAutoSellActivationCompositionPlanService`
- `Aion.GameServer.Services.PetAutoSellActivationPlanService`
- `Aion.GameServer.Tests.CmPetTests`

## Parity Table Updates

- Added Session 1996 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_PET.readImpl` FOOD/actionType 4 parser branch
  - `CM_PET.runImpl` FOOD/actionType 4 disabled composition route
  - `PetService.activateAutoSell` disabled service plan boundary

## Next Recommended Unit of Work

- Next sequential task: choose another compact parser/factory/model boundary with Java golden evidence, or inspect another `CM_PET` sub-branch only if it can remain disabled and source-reviewed.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live pet autosell parity from UOW-1996; only parser and disabled-composition evidence is covered.
- `CM_PET` actionType 3 autoloot already has separate Java/C# evidence; actionType 4 now has matching parser/composition input evidence but no live side effects.
