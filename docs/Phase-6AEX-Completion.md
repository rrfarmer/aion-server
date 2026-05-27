# Phase 6AEX Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1318
Latest Commit: included in the UOW-1318 unit commit
Status: Pet row projection now hydrates the standalone `PetDopingBag`; live pet repository execution and runtime mutation remain disabled.

## What Changed

- Replaced the projection-only doping DTO in `PlayerPetRowProjection` with `PetDopingBag`.
- `ProjectDopingBag` now parses Java `dopings` CSV through `PetDopingBag.SetItem(parsedId, slot)`.
- Updated row projection tests for helper-backed food/drink/scroll views, load-time dirty behavior, null CSV behavior, and all-zero CSV expansion.
- Added `docs/Phase-6-BindPointTeleport-PetRowProjectionDopingBagWiring.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerPetRowProjection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerPetRowProjectionTests.cs`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerPetRowProjection|PetDopingBag"` passed 23 tests.

No live MySQL repository execution, live pet common-data model, dirty-save trigger, `PetService.useDoping`, Java runtime DB comparison, or socket dispatch was enabled.

## Migration Parity Table - UOW-1318

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerPetsDAO.getPlayerPets` | `Aion.GameServer.Data.PlayerPetRowProjection.ProjectDopingBag` | Repository Projection | Partial | Unit Tested | Partial Parity | Doping CSV now hydrates the standalone `PetDopingBag` by calling `SetItem(parsedId, slot)` like Java. SQL execution, Java outer catch/partial-list return, logging, live `DataManager` template lookup, and runtime DB comparison remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `Aion.GameServer.Services.ToyPet.PetDopingBag` via `PlayerPetLoadedProjection.DopingBag` | Model Helper / Projection Target | Complete for helper, Partial for integration | Unit Tested | Partial Parity | Row projection now exposes the live helper instead of the prior projection DTO. Non-zero DB values mark the bag dirty like Java `setItem`; all-zero CSV expands storage without dirtying. Live common-data ownership, persistence triggers, packet builders, and `PetService.useDoping` remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Data.PlayerPetLoadedProjection` | DTO / Model Projection | Partial | Unit Tested | Partial Parity | The loaded projection now carries `PetDopingBag?` for future common-data wiring. Full Java constructor template setup, synchronized runtime mutation ownership, volatile task fields, expirable callbacks, packet sends, and DAO list materialization remain unported. |
| `com.aionemu.gameserver.services.toypet.PetService.useDoping` | future C# live pet doping runtime | Service | Not Started | Manual Only | Needs Verification | Newly hydrated bag state is still non-live. Item lookup, slot validation, skill/cooldown use, inventory mutation, dirty save trigger, and `SM_PET.SPECIAL_FUNCTION` dispatch remain blocked. |

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProjectDopingBagParsesJavaCsvSlotOrder` | Unit | `PlayerPetsDAO.getPlayerPets` and `PetDopingBag.setItem` | CSV food/drink/scroll slots hydrate through `PetDopingBag`, preserve slot order, and mark dirty for non-zero loaded values. | Source-derived row projection assertion. | No live DAO execution or save trigger. |
| `ProjectDopingBagReturnsEmptyBagForNullCsvLikeJava` | Unit | `PlayerPetsDAO.getPlayerPets` null `dopings` guard | Null DB CSV returns an empty non-dirty bag. | Source-derived row projection assertion. | No live template lookup or common-data ownership. |
| `ProjectDopingBagZeroCsvExpandsWithoutDirtyLikeJavaSetItem` | Unit | `PetDopingBag.setItem(0, slot)` | All-zero CSV expands storage and scroll view without marking dirty. | Source-derived helper assertion through row projection. | No Java runtime DB comparison. |
| `ProjectSkipsDopingBagWhenTemplateDoesNotHaveDopingFunction` | Unit | `PlayerPetsDAO.getPlayerPets` null doping-function guard | Projection still omits doping state when the caller says the template lacks `DOPING`. | Source-derived row projection assertion. | Template lookup is supplied, not live `DataManager`. |
| `ProjectDopingBagRejectsMoreThanJavaMaxSlots` | Unit | `PetDopingBag.setItem` max-slot guard | More than eight CSV slots are rejected. | Source-derived guard assertion. | C# exception type differs from Java `IllegalArgumentException`. |
| `ProjectDopingBagRejectsMalformedCsvLikeJavaParseInt` | Unit | `Integer.parseInt` in `PlayerPetsDAO.getPlayerPets` | Malformed CSV throws during projection. | Source-derived guard assertion. | Full Java DAO outer catch/partial-list behavior is not modeled. |

## Remaining Risks

- Live `PlayerPetsDAO.getPlayerPets` execution is not implemented.
- Java outer catch logs and returns pets accumulated before a row failure; this helper projects one row and throws.
- Template lookup is supplied by `PlayerPetProjectionOptions` instead of Java `DataManager.PET_DATA`.
- The `PlayerPetLoadedProjection.DopingBag` API changed intentionally from a projection DTO to `PetDopingBag`; this is local to the non-live projection surface so far.
- Dirty flag behavior during DB load is now represented, but no live save trigger or dirty reset behavior exists.
- Java `getItems()` exposes the backing array while C# `GetItems()` returns a copy by intentional helper design.
- Concurrent Java/C# behavior has not been stress-tested.
- Timestamp binding/timezone behavior for database reads is still unverified.
- Java runtime DB comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 projection integration update and 6 focused projection/helper tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: live pet repository execution, Java template lookup, full common-data model, DAO outer-catch partial-list behavior, dirty-flag persistence trigger, live pet doping service, Java runtime DB comparison, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Audit and port the deterministic `PetFeedCalculator` reward/threshold behavior into a non-live helper.
- Why: Feed and doping row projection prerequisites now exist, but live feed runtime remains blocked by calculator/static-data behavior.
- Files: likely new C# helper/test files under `Services/ToyPet` and `Aion.GameServer.Tests`; keep live runtime and DI untouched.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `PetFeedCalculator` deterministic audit/port | New helper/test files plus Java read-only | Medium | Recommended next writer if scope stays non-live. |
| B | Pet DAO live execution readiness audit | Java/C# read-only, docs later | Low | Keep implementation separate from feed calculator. |
| C | Java pet vector generator retry | Tooling/docs read-only | Low | Only useful if Maven/Java runtime capture works. |
| D | `PetService.useDoping` dependency map | Java read-only | Medium | Analysis only; live mutation should not start yet. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Port/audit non-live `PetFeedCalculator` helper | New `Services/ToyPet` helper file, new focused tests | Shared docs until orchestrator update, live runtime, DI, repository execution |
| Agent B | Read-only `PetService.useDoping` dependency map | Read-only Java/C# inspection | All writes |

## Do Not Parallelize

- `PlayerPetRowProjection.cs` and `PetDopingBag.cs`: row projection and helper API are freshly connected; avoid concurrent edits until the next writer chooses a clear owner.
- Shared progress/handoff docs: orchestrator-owned only.
- Live pet repository/runtime mutation: still blocked by common-data ownership, template lookup, persistence, and socket surfaces.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Data/PlayerPetRowProjection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetDopingBag.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerPetRowProjectionTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetDopingBagTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetRowProjectionDopingBagWiring.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
