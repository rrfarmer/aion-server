# Phase 6IS Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IR and covers Session 741.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionCastSpellTests|PlayerCastSpellEarlyExitServiceTests|GamePacketTests|StaticDataLoadingTests"`
  - Result: Passed, 120 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1330 tests.

## Recent Work Completed

### Session 741 - CM_CASTSPELL Runtime Skill Template Lookup

- Inspected Java `CM_CASTSPELL.runImpl`, `DataManager.SKILL_DATA.getSkillTemplate`, `SkillData.getSkillTemplate`, and `SkillTemplate.isPassive`.
- Added `SkillTemplateSummary.Activation` and `SkillTemplateSummary.IsPassive`.
- Updated static-data XML loading to preserve `skill_template` `activation`.
- Wired `GameServerConnection.HandleCastSpellAsync` to fall back to runtime static skill data when `GameServerCastSpellHandlerHooks.GetSkillTemplate` does not resolve a template.
- Added connection-level tests using loaded static data for active skill `539` and passive skill `40`.
- Full `SkillEngine` dispatch remains callback-only.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL.runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleCastSpellAsync` / `ResolveCastSpellSkillTemplate` | Client Packet Handler Seam | Partial | Regression Tested | Needs Verification | Cast-spell now falls back to loaded static skill data for template lookup when hooks do not provide a template. Full `PlayerController.useSkill`, target validation, effect execution, cooldown production, and combat fanout remain missing. |
| `com.aionemu.gameserver.dataholders.DataManager.SKILL_DATA` | `GameServerRuntimeContext.DataManager.StaticData.SkillTemplates` | Static Data Dependency | Partial | Regression Tested with loaded static data | Needs Verification | C# uses loaded static XML data when a runtime context is available. Java static singleton lifecycle, validation, reload behavior, reflection differences, and full skill-data APIs remain unverified. |
| `com.aionemu.gameserver.dataholders.SkillData.getSkillTemplate` | `Aion.GameServer.Dataholders.SkillTemplateTable.GetSkillTemplate` | Repository / Static Data Table | Partial | Regression Tested | Needs Verification | Lookup by skill id is wired for cast-spell early-exit decisions. Java collection behavior, duplicate handling, group/stack side effects, and all non-lookup APIs remain outside this unit. |
| `com.aionemu.gameserver.skillengine.model.SkillTemplate` | `Aion.GameServer.Dataholders.SkillTemplateSummary` plus `PlayerCastSpellSkillTemplate` projection | DTO / Skill Template Projection | Partial | Regression Tested | Needs Verification | C# projects only `SkillId` and passive state into cast-spell handling. Full Java template fields, properties, effects, target rules, motion, charge behavior, serialization, precision/rounding, and XML default semantics remain unported for this path. |
| `com.aionemu.gameserver.skillengine.model.SkillTemplate.isPassive` / `ActivationAttribute.PASSIVE` | `SkillTemplateSummary.Activation` / `SkillTemplateSummary.IsPassive` | DTO Helper / Enum Projection | Partial | Regression Tested | Needs Verification | C# preserves the XML `activation` string and treats `PASSIVE` as passive. Java enum parsing/default behavior and unsupported activation values such as toggle/provoked remain not fully modeled here. |

## Tests Added Or Updated

- `StaticDataLoadingTests.LoadStaticData_MergesAndIndexesJavaStaticData`
- `GameServerConnectionCastSpellTests.HandleCastSpellAsync_UsesRuntimeStaticSkillTemplateLookupWhenHookDoesNotResolveTemplate`
- `GameServerConnectionCastSpellTests.HandleCastSpellAsync_RuntimeStaticPassiveSkillTemplateExitsBeforeUseSkill`

Existing cast-spell planner and packet tests were rerun in the focused filter. The full GameServer suite was rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden data beyond existing static-data tests, live client socket order, full `SkillEngine`, reflection behavior, threading behavior, date/time behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 runtime static skill-template lookup slice plus activation/passive projection.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: full SkillEngine/player-controller execution, full skill-template projection, pet skill static lookup, Java runtime/golden data comparison, live client validation, reflection/enum parsing parity, and threading/date-time comparison.
- Estimated overall migration completion: 66%

## Remaining Risks

- Runtime static lookup requires `GameServerRuntimeContext.DataManager` to be loaded; tests cover loaded data, but production mixed-mode behavior still needs live validation.
- `PlayerCastSpellSkillTemplate` is still a narrow projection and does not expose Java skill target/effect/property/motion/charge data needed by real `useSkill`.
- Pet-order skill lookup still remains hook-based; Java `DataManager.PET_SKILL_DATA.isPetOrderSkill` is not wired to runtime static data in this unit.
- Hook-first fallback means custom hooks can still override static lookup; this is intentional for tests/future runtime seams, but differs from Java's direct singleton lookup and must be kept explicit.
- Static activation handling stores strings rather than Java enums; unsupported activation enum behavior and XML default behavior need future verification.

## Next Recommended Unit of Work

Wire Java `DataManager.PET_SKILL_DATA.isPetOrderSkill` into the cast-spell early-exit seam if C# has a pet-skill static table. If not, add the narrow pet-skill static data loader/table for order-skill ids and use it as the default `IsPetOrderSkill` fallback, keeping summon/pet state itself hook-based until player summon models are available.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IR-Completion.md`
   - this handoff
3. Inspect Java `CM_CASTSPELL.java`, `DataManager.PET_SKILL_DATA`, `PetSkillData.isPetOrderSkill`, static pet-skill XML, and C# static-data loading.
4. Inspect C# `GameServerConnection.ResolveCastSpellSkillTemplate`, `GameServerCastSpellHandlerHooks.IsPetOrderSkill`, `PlayerCastSpellEarlyExitService`, `StaticData`, `SkillTemplateTable`, and tests.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
