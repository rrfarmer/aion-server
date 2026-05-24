# Phase 6IT Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IS and covers Session 742.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionCastSpellTests|PlayerCastSpellEarlyExitServiceTests|StaticDataLoadingTests"`
  - Result: Passed, 31 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1331 tests.

## Recent Work Completed

### Session 742 - CM_CASTSPELL Runtime Pet-Order Skill Lookup

- Inspected Java `PetSkillData`, `PetSkillTemplate`, pet skill XML, and the `CM_CASTSPELL` pet-order guard.
- Added C# `PetSkillTable` and `PetSkillSummary`.
- Extended static-data loading to parse `pet_skill` rows and expose `StaticData.PetSkills`.
- Wired `GameServerConnection.HandleCastSpellAsync` to fall back to runtime static `PetSkills.IsPetOrderSkill` when the injected hook does not match.
- Added loaded static-data tests for Java order skill `3835`, pet npc id `833288`, and pet skill id `22107`.
- Added a connection-level test proving order skill `3835` triggers `STR_SKILL_NOT_NEED_PET` without an injected `IsPetOrderSkill` hook.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleCastSpellAsync` / `IsCastSpellPetOrderSkill` | Client Packet Handler Seam | Partial | Regression Tested | Needs Verification | Cast-spell now falls back to runtime static pet-order skill lookup before template lookup. Player summon/pet state remains hook-based, and full pet command/skill remapping is not wired. |
| `com.aionemu.gameserver.dataholders.DataManager.PET_SKILL_DATA` | `GameServerRuntimeContext.DataManager.StaticData.PetSkills` | Static Data Dependency | Partial | Regression Tested with loaded static data | Needs Verification | C# exposes loaded pet-skill data through runtime context. Java static singleton lifecycle, JAXB `afterUnmarshal`, reload behavior, and full global DataManager parity remain unverified. |
| `com.aionemu.gameserver.dataholders.PetSkillData` | `Aion.GameServer.Dataholders.PetSkillTable` | Static Data Table | Partial | Regression Tested | Needs Verification | C# indexes by order skill and pet id, matching the Java lookup shapes needed here. Java's exact null/exception behavior for missing pet ids differs intentionally for now: C# returns `null`/`false` rather than throwing. |
| `com.aionemu.gameserver.model.templates.petskill.PetSkillTemplate` | `Aion.GameServer.Dataholders.PetSkillSummary` | DTO | Partial | Regression Tested | Needs Verification | C# captures `skill_id`, `pet_id`, and `order_skill`. JAXB default handling, transformed summon skills with missing `order_skill`, reflection/serialization behavior, and all pet model interactions remain unverified. |
| `com.aionemu.gameserver.dataholders.PetSkillData.isPetOrderSkill` | `PetSkillTable.IsPetOrderSkill` and `GameServerConnection.IsCastSpellPetOrderSkill` | Lookup Helper | Partial | Regression Tested | Needs Verification | Loaded data proves order skill `3835` is recognized and triggers the Java pet-required failure path. Live summon ownership, `getPetOrderSkill`, and `petHasSkill` callers remain mostly unwired. |

## Tests Added Or Updated

- `StaticDataLoadingTests.LoadStaticData_MergesAndIndexesJavaStaticData`
- `GameServerConnectionCastSpellTests.HandleCastSpellAsync_UsesRuntimeStaticPetOrderSkillLookupWhenHookDoesNotMatch`

Existing cast-spell planner tests were rerun in the focused filter. The full GameServer suite was rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated JAXB maps, live summon model behavior, reflection behavior, threading behavior, date/time behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 pet-skill static lookup slice plus cast-spell fallback.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live summon/pet state integration, pet order skill remapping/execution, full SkillEngine/player-controller execution, Java runtime/JAXB map comparison, live client validation, reflection/serialization parity, and threading/date-time comparison.
- Estimated overall migration completion: 66%

## Remaining Risks

- Player summon/pet state remains hook-based; C# has no live `player.getSummon() == null || !summon.isPet()` integration in this route.
- `GetPetOrderSkill` and `PetHasSkill` are modeled but not wired to live summon controllers or pet skill execution.
- C# intentionally returns nullable/false for missing pet lookups instead of Java's potential null-pointer behavior; this may need revision when callers require exact exception semantics.
- Static data tests validate loaded XML facts, not Java JAXB golden maps or live runtime behavior.
- Full `SkillEngine`, pet command dispatch, target/effect handling, and packet fanout remain missing.

## Next Recommended Unit of Work

Add a narrow represented summon/pet state to `Player` or `GameServerCastSpellHandlerHooks.HasPetSummon` fallback so `CM_CASTSPELL` can distinguish Java's `player.getSummon() == null || !player.getSummon().isPet()` without test-only hooks. Keep actual summon controller command execution and `PetSkillData.getPetOrderSkill` remapping as a later unit unless the needed summon model already exists.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IS-Completion.md`
   - this handoff
3. Inspect Java `CM_CASTSPELL.java`, `Player.getSummon`, summon `isPet`, `PetSkillData.getPetOrderSkill`, and C# player/summon state.
4. Inspect C# `Player`, `PetSkillTable`, `GameServerConnection.IsCastSpellPetOrderSkill`, `GameServerCastSpellHandlerHooks.HasPetSummon`, and cast-spell tests.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
