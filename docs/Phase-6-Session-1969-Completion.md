# Phase 6 Session 1969 Completion - CM_PET Autosell Activation Composition

Date: 2026-06-01
Unit of Work: UOW-1969
Status: Completed

## What Changed

- Reviewed Java `CM_PET.readImpl` and `CM_PET.runImpl` for FOOD actionType `4`.
- Reviewed Java `PetService.activateAutoSell` and the existing C# disabled autosell activation planner.
- Added `CmPetAutoSellActivationCompositionPlanService`, a non-live bridge from parsed `CmPet` FOOD/actionType `4` packets into `PetAutoSellActivationPlanService`.
- Added tests proving parsed `activateSpecialFunction` values map to disabled target selling-state intent and that missing-pet/non-autosell branches stop before live side effects.
- This unit remains non-live. No `CM_PET` live handler, pet common-data mutation, audit logging, packet dispatch, persistence, encrypted frame capture, or real-client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmPetTests|FullyQualifiedName~PetMerchantSellLiveExecutorFacadePlanServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# pet slice passed with 39 tests.
- Broad C# game-server suite passed with 5026 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 24 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmPet.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`.
- Objective evidence is limited to source review, C# unit tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_PET` runtime dispatch, audit logging, pet state mutation, persistence, or socket output.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/TradeSellToShopPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmPetTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1969-Completion.md`
- `docs/Phase-6-Session-1969-Handoff.md`

## Remaining Risks

- Live `CM_PET` actionType `4` handler wiring remains absent/unverified.
- Existing disabled audit text still approximates Java's `"tried to enable auto-sell on non-selling " + pet`; exact Java `Pet.toString()` was not runtime-compared.
- Full Maven reactor validation was not rerun in this unit; only the game-server reactor was run.
- Other `CM_PET` branches remain separate work.

## Next Recommended Unit

- Audit signed Java `readH()` call sites where C# currently uses unsigned `PacketBuffer.ReadH()`, then choose one high-bit field with a deterministic Java/C# parser test.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
