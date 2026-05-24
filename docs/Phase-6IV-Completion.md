# Phase 6IV Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IU and covers Session 744.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerPetOrderSkillServiceTests|GamePacketTests"`
  - Result: Passed, 94 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1336 tests.

## Recent Work Completed

### Session 744 - Pet Order Ultra-Skill Remap Bridge

- Inspected Java `PetOrderUseUltraSkillEffect`, `SM_SUMMON_USESKILL`, `SummonController.useSkill(SkillOrder)`, `CM_SUMMON_CASTSPELL`, and `PetSkillData.getPetOrderSkill`.
- Confirmed Java order-skill remapping happens when the order skill effect applies, not directly inside `CM_CASTSPELL`.
- Added represented C# pet summon identity to `Player`:
  - `HasPetSummon`
  - `PetSummonObjectId`
  - `PetSummonNpcId`
- Added represented `PlayerPetSkillOrder` queue for Java `Summon.addSkillOrder`.
- Added `SmSummonUseSkill` packet with opcode `162` and Java payload shape.
- Added `PlayerPetOrderSkillService` to map order skill plus pet npc id to the pet skill, resolve skill level, apply the Java hate threshold, queue a represented order, and create the summon-use-skill packet.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.PetOrderUseUltraSkillEffect` | `Aion.GameServer.Services.PlayerPetOrderSkillService` | Effect Bridge / Service | Partial | Regression Tested | Needs Verification | C# models the order-skill-to-pet-skill lookup, skill-template level lookup, hate threshold, represented order queue, and `SM_SUMMON_USESKILL` packet creation. Full effect engine invocation, `Effect` object behavior, JAXB/reflection attributes, and target object references remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_USESKILL` | `Aion.GameServer.Network.Aion.ServerPackets.SmSummonUseSkill` | Server Packet | Complete | Regression Tested | Needs Verification | Payload shape matches Java source, but no Java golden bytes, encrypted live frames, or client rendering were validated. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummon` / `Summon.getObjectId` / `Summon.getNpcId` | `Player.HasPetSummon`, `PetSummonObjectId`, `PetSummonNpcId` | Player/Summon Projection | Partial | Regression Tested through service | Needs Verification | Represents only current pet summon identity. No live `Summon`, NPC template, lifecycle, object visibility, stats, controller, or threading behavior exists yet. |
| `com.aionemu.gameserver.model.gameobjects.Summon.addSkillOrder` / `com.aionemu.gameserver.model.summons.SkillOrder` | `Player.AddPetSkillOrder` / `PlayerPetSkillOrder` | Summon Skill Order Projection | Partial | Regression Tested | Needs Verification | C# queues represented skill id, level, target object id, hate, and release flag on `Player`. It does not yet validate target object equality or consume orders through `CM_SUMMON_CASTSPELL`. |
| `com.aionemu.gameserver.dataholders.PetSkillData.getPetOrderSkill` | `PetSkillTable.GetPetOrderSkill` consumed by `PlayerPetOrderSkillService` | Static Data Lookup | Partial | Regression Tested with loaded static data | Needs Verification | Loaded XML maps order skill `3835` and pet npc `833288` to pet skill `22107`. Missing mapping behavior intentionally remains nullable instead of Java's possible null-pointer behavior. |
| `com.aionemu.gameserver.dataholders.DataManager.SKILL_DATA.getSkillTemplate` / `SkillTemplate.getLvl` | `SkillTemplateTable.GetSkillTemplate` / `SkillTemplateSummary.Level` consumed by `PlayerPetOrderSkillService` | Static Data Lookup / DTO Projection | Partial | Regression Tested with loaded static data | Needs Verification | C# uses loaded skill template `22107` to derive level `1`. Full Java skill-template/effect runtime and XML enum/default behavior remain unverified. |
| `com.aionemu.gameserver.controllers.SummonController.useSkill(SkillOrder)` | Not yet implemented; represented by queued `PlayerPetSkillOrder` only | Controller / Skill Execution | Not Started | No Tests | Needs Verification | Newly documented dependency. Java validates `petHasSkill`, creates `SkillEngine` skill, sets hate, executes it, and releases the summon when requested. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_CASTSPELL` | Existing parser `Aion.GameServer.Network.Aion.ClientPackets.CmSummonCastSpell`; no handler bridge yet | Client Packet Handler | Partial | No new handler tests | Needs Verification | C# parses the packet from earlier work, but target resolution, queued order consumption, mismatch logging, mercenary handling, and summon-controller execution remain unimplemented. |

## Tests Added Or Updated

- `PlayerPetOrderSkillServiceTests.ApplyUltraSkillOrder_MapsOrderSkillQueuesSummonOrderAndCreatesUseSkillPacket`
- `PlayerPetOrderSkillServiceTests.ApplyUltraSkillOrder_UsesJavaHateThreshold`
- `PlayerPetOrderSkillServiceTests.ApplyUltraSkillOrder_RequiresRepresentedSummonAndTemplateInputs`
- `GamePacketTests` existing packet-shape coverage now includes `SmSummonUseSkill`

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated packet/data goldens, live effect-engine execution, live summon order consumption, reflection/JAXB behavior, threading behavior, date/time behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 8
- Total artifacts ported or partially modeled in this handoff window: 1 represented pet-order ultra-skill bridge plus `SM_SUMMON_USESKILL` packet.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked/not-started artifacts: full SkillEngine/effect invocation, live Summon model/order queue, `CM_SUMMON_CASTSPELL` handler, `SummonController.useSkill`, Java runtime/golden comparison, live-client validation, and reflection/threading/date-time comparison.
- Estimated overall migration completion: 66%

## Remaining Risks

- The new bridge is not invoked by a full C# `SkillEngine` effect pipeline yet.
- The represented queue lives on `Player`, not a true Java-equivalent `Summon`.
- `CM_SUMMON_CASTSPELL` still lacks handler parity for consuming queued orders and invoking summon-controller execution.
- `SummonController.useSkill(SkillOrder)` remains unported.
- Static data and packet assertions are C# tests derived from Java source/XML, not Java runtime/golden comparisons.

## Next Recommended Unit of Work

Continue the summon order path by adding a narrow `CM_SUMMON_CASTSPELL` handler/service that consumes represented `PlayerPetSkillOrder` entries for a represented pet summon object id and target object id, validates skill id/level against the queued order, and records the would-be summon-controller execution. Keep target known-list lookup, mercenary skill handling, live `SummonController.useSkill`, `SkillEngine`, release-on-success, and audit logging documented if they remain unsupported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IU-Completion.md`
   - this handoff
3. Inspect Java `CM_SUMMON_CASTSPELL.java`, `SummonController.useSkill(SkillOrder)`, `Summon.retrieveNextSkillOrder`, and `PetSkillData.petHasSkill`.
4. Inspect C# `PlayerPetSkillOrder`, `Player.PetSkillOrders`, `PlayerPetOrderSkillService`, `CmSummonCastSpell`, and summon packet tests.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
