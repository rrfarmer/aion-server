# Phase 6IU Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IT and covers Session 743.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionCastSpellTests|PlayerCastSpellEarlyExitServiceTests"`
  - Result: Passed, 18 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1332 tests.

## Recent Work Completed

### Session 743 - CM_CASTSPELL Represented Pet Summon Guard

- Inspected Java `Player.getSummon`, `Player.setSummon`, `Summon.isPet`, and the `CM_CASTSPELL` pet-order guard.
- Added represented player pet-summon state:
  - `Player.HasPetSummon`
- Wired `GameServerConnection.HandleCastSpellAsync` so `GameServerCastSpellHandlerHooks.HasPetSummon` remains the first seam, with `Player.HasPetSummon` as the default fallback.
- Added a connection-level regression proving static pet-order skill `3835` reaches the use-skill seam when represented player pet-summon state exists.
- Full summon object/controller behavior remains missing and documented.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleCastSpellAsync` / `HasCastSpellPetSummon` | Client Packet Handler Seam | Partial | Regression Tested | Needs Verification | Pet-order skill guard can now use represented player pet-summon state instead of only injected hooks. Full pet command execution and skill remapping remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummon` / `setSummon` | `Aion.GameServer.Model.GameObjects.Player.HasPetSummon` | Player State | Partial | Regression Tested through connection seam | Needs Verification | C# represents only whether the current summon is a pet. It does not model a `Summon` object, summon object id, lifecycle, master relation, controller, stats, persistence, or threading behavior. |
| `com.aionemu.gameserver.model.gameobjects.Summon.isPet` | `Player.HasPetSummon` fallback consumed by `HasCastSpellPetSummon` | Summon State Projection | Partial | Regression Tested | Needs Verification | Java checks `NpcTemplateType.SUMMON_PET`; C# stores a boolean projection. Template-type lookup and live summon/NPC template behavior remain unsupported. |
| `com.aionemu.gameserver.dataholders.PetSkillData.isPetOrderSkill` plus summon guard | `PetSkillTable.IsPetOrderSkill` plus `Player.HasPetSummon` | Guard Composition | Partial | Regression Tested | Needs Verification | Test validates static order skill `3835` proceeds past pet-required rejection when represented pet summon state exists. Java `getPetOrderSkill` remapping and summon controller validation are still not wired. |

## Tests Added Or Updated

- `GameServerConnectionCastSpellTests.HandleCastSpellAsync_PlayerPetSummonAllowsRuntimeStaticPetOrderSkillPastPetGuard`

Existing cast-spell planner tests were rerun in the focused filter. The full GameServer suite was rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, live summon object behavior, Java-generated golden packet/data comparison, reflection behavior, threading behavior, date/time behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 4
- Total artifacts ported or partially modeled in this handoff window: 1 represented pet-summon guard slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked/not-started artifacts: live summon model/lifecycle, NPC template type integration, pet order skill remapping/execution, full SkillEngine/player-controller execution, Java runtime/live-client validation, and threading/date-time comparison.
- Estimated overall migration completion: 66%

## Remaining Risks

- `Player.HasPetSummon` is a boolean projection and can drift from future live summon lifecycle unless real summon models update it.
- `PetSkillData.getPetOrderSkill` and `SummonController` command execution remain unwired, so pet-order skills are only allowed past the early guard.
- Full Java summon lifecycle, object id, NPC template type, stats, movement/controller behavior, despawn behavior, and persistence remain missing.
- The hook-first fallback still allows tests/future runtime code to override represented state, which differs from Java's direct `Player.getSummon()` check but keeps seams explicit until full models land.

## Next Recommended Unit of Work

Add the next narrow pet-order execution bridge: use `PetSkillTable.GetPetOrderSkill(orderSkill, petNpcId)` once represented player pet summon npc id exists, or add `Player.PetSummonNpcId` as a projection next to `HasPetSummon` and test Java's order-skill-to-pet-skill remapping without invoking full `SummonController`.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IT-Completion.md`
   - this handoff
3. Inspect Java `CM_CASTSPELL.java`, `PetSkillData.getPetOrderSkill`, `SummonController`, `Player.getSummon`, and C# player/summon projections.
4. Inspect C# `Player.HasPetSummon`, `PetSkillTable`, `GameServerConnection.HasCastSpellPetSummon`, `GameServerCastSpellHandlerHooks`, and cast-spell tests.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
