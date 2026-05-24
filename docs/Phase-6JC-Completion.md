# Phase 6JC Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6JB and covers Session 751.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 26 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1350 tests.

## Recent Work Completed

### Session 751 - Represented Summon-Or-Mercenary Actor Routing

- Re-inspected Java `Player.getSummonOrMercenary` and `CM_SUMMON_CASTSPELL.runImpl`.
- Added `PlayerSummonOrMercenaryKind` with `None`, `PetSummon`, `NonPetSummon`, and `Mercenary`.
- Added represented `Player.GetSummonOrMercenaryKind` plus `RepresentedSummonOrMercenaryObjectId` / `RepresentedSummonOrMercenaryKind`.
- Updated `PlayerSummonCastSpellResult` to carry actor kind metadata.
- Added `PlayerSummonCastSpellStatus.MercenaryUnsupported` so represented mercenary packets do not send `STR_SKILL_NOT_NEED_PET`.
- Kept real mercenary target resolution, `setTarget`, `PetSkillData.petHasSkill`, controller `useSkill(skillId, skillLvl)`, and invalid-skill audit out of scope.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummonOrMercenary` | `Aion.GameServer.Model.GameObjects.Player.GetSummonOrMercenaryKind` plus represented fields | Player/Summon Lookup Projection | Partial | Regression Tested | Needs Verification | C# distinguishes missing, pet summon, non-pet summon, and mercenary outcomes by enum. Java returns live `Creature` references from owned summon or creator-owned known-list mercenary NPC lookup. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_CASTSPELL.runImpl` summon-or-mercenary precheck | `Aion.GameServer.Services.PlayerSummonCastSpellService` actor-kind routing | Client Packet Handler / Service | Partial | Regression Tested | Needs Verification | Missing and non-pet summon outcomes map to `PetRequired`; pet summon continues through represented queued-order path; mercenary returns `MercenaryUnsupported`. |
| `com.aionemu.gameserver.model.gameobjects.Summon.isPet` | `PlayerSummonOrMercenaryKind.PetSummon` / `NonPetSummon` | Summon Type Projection | Partial | Regression Tested | Needs Verification | Pet vs non-pet is represented as metadata. No live summon object, controller, template, threading, or despawn lifecycle. |
| `com.aionemu.gameserver.model.gameobjects.Npc` mercenary branch | `PlayerSummonOrMercenaryKind.Mercenary` / `PlayerSummonCastSpellStatus.MercenaryUnsupported` | Mercenary Projection | Partial | Regression Tested | Needs Verification | C# can identify represented mercenary and avoid pet-required packet. It does not validate creator id, template type, target assignment, skill ownership, or controller execution. |
| `com.aionemu.gameserver.model.templates.npc.NpcTemplateType.MERCENARY` | represented enum outcome only | Template Type Dependency | Not Started | Regression Tested as represented outcome only | Needs Verification | No C# template lookup exists in this path. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_NOT_NEED_PET` | `GameServerConnection.HandleSummonCastSpellAsync` pet-required send branch | Packet Side Effect | Partial | Regression Tested | Needs Verification | Missing/non-pet summon style rejection remains packeted; represented mercenary no longer sends pet-required. No encrypted/live-client comparison was run. |

## Tests Added Or Updated

- `PlayerSummonCastSpellServiceTests.Handle_RequiresRepresentedPetSummonAndQueuedOrder`
  - Validates actor kind for missing/wrong summon and pet summon no-order outcomes.
- `PlayerSummonCastSpellServiceTests.Handle_DistinguishesRepresentedNonPetSummonAndMercenary`
  - Validates non-pet represented summon returns `PetRequired` and preserves queued orders.
  - Validates represented mercenary returns `MercenaryUnsupported`.
- `GameServerConnectionCastSpellTests.HandleSummonCastSpellAsync_RepresentedMercenaryDoesNotSendPetRequiredPacket`
  - Validates represented mercenary stops without sending `STR_SKILL_NOT_NEED_PET`.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live known-list NPC lookup, object identity, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 represented summon-or-mercenary actor-kind projection.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: live `Creature`/`Summon`/`Npc` models, known-list NPC creator lookup, NPC template type lookup, mercenary target assignment, mercenary pet-skill guard, mercenary controller execution, invalid mercenary audit, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Actor kind is represented metadata, not a live Java `Creature`.
- Mercenary handling still stops before target resolution, target assignment, pet-skill validation, controller execution, and invalid-skill audit.
- Non-pet summon rejection depends on seeded metadata rather than live `Summon.isPet()`.
- Known-list NPC creator id and `NpcTemplateType.MERCENARY` checks are not implemented.
- Threading, serialization, reflection, precision/rounding, date/time, Java runtime, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue the represented mercenary branch of `CM_SUMMON_CASTSPELL`: reuse or extend represented known-list target validation for mercenary, carry a `SetTarget` / `UseSkill` action plan for valid mercenary pet skills, and add invalid mercenary skill audit metadata. Keep live `Npc`, creator id, `NpcTemplateType.MERCENARY`, and controller execution gaps explicit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6JB-Completion.md`
   - this handoff
3. Inspect Java `CM_SUMMON_CASTSPELL.java`, `Player.getSummonOrMercenary`, `PetSkillData.petHasSkill`, and the mercenary branch.
4. Inspect C# `PlayerSummonCastSpellService`, `PlayerSummonOrMercenaryKind`, `PlayerSummonCastSpellResult`, and connection-level summon cast tests.
5. Implement one narrow represented mercenary slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
