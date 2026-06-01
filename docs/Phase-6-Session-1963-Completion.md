# Phase 6 Session 1963 Completion - Pet Autosell Activation Diagnostic

Date: 2026-06-01
Unit of Work: UOW-1963
Status: Completed

## Scope

- Inspected Java `PetService.activateAutoSell`, `CM_PET.runImpl` actionType 4, and `SM_PET(PetSpecialFunction, boolean)`.
- Inspected C# `CmPet`, `SmPet`, pet merchant sell diagnostics, and existing pet packet tests.
- Added a disabled pet autosell activation planner.

## What Changed

- Added `PetAutoSellActivationPlanService` in `TradeSellToShopPlanService.cs`.
- Added records/enums for disabled autosell activation diagnostics:
  - missing-pet early return
  - missing MERCHANT-function audit-only block when enabling
  - selling-state mutation intent
  - `SM_PET(AUTOSELL, activate)` packet intent
- Added focused tests for:
  - active and inactive packet intent bytes
  - enable-without-MERCHANT audit-only behavior
  - deactivate skipping the MERCHANT guard
  - missing-pet terminal behavior

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests|FullyQualifiedName~CmPetTests|FullyQualifiedName~GamePacketTests.SmPet|FullyQualifiedName~PetActionAndEmoteResolvers" --no-restore`
  - Passed with 79 tests.
  - Existing nullable/analyzer warnings were emitted in unrelated files.
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
  - Passed with 1 commons test and 23 game-server tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
  - First run timed out at about 184 seconds.
  - Rerun passed with 5014 tests when given a longer timeout.
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`
  - Passed the full Maven reactor in the current workspace.
  - This run did not force a clean login-server recompile.

## Parity Evidence

- Java source was reviewed for `PetService.activateAutoSell`.
- Java `CM_PET.runImpl` was reviewed for the pet-null return before `activateAutoSell`.
- Java `SM_PET` special-function autosell wire shape was compared to existing C# packet serializer behavior.
- C# tests serialize the planner's concrete `SmPet` packet intent and assert the Java-shaped autosell payload for both active and inactive values.

## Known Gaps

- The planner is disabled and informational only.
- No pet common-data selling flag is mutated.
- No `AuditLogger.log` call is made.
- No `SM_PET(AUTOSELL)` packet is sent.
- Exact Java `Pet.toString()` output in the audit message was not runtime-compared.
- Live `CM_PET` actionType 4 handler wiring, template/function lookup timing, persistence, threading, encrypted frame capture, and real-client validation remain pending.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetMerchantSellLiveExecutorFacadePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1963-Completion.md`
- `docs/Phase-6-Session-1963-Handoff.md`

## Next Recommended Unit of Work

- Inspect Java private-store sell/buy blocked branches and C# private-store diagnostics, then harden blocked/race/offline/cube-full diagnostics without enabling live mutation.
