# Phase 6IW Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6IV and covers Session 745.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|PlayerPetOrderSkillServiceTests|GamePacketTests"`
  - Result: Passed, 98 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1340 tests.

## Recent Work Completed

### Session 745 - Represented Summon Cast Order Consumption

- Inspected Java `CM_SUMMON_CASTSPELL.runImpl`, `Summon.addSkillOrder`, `Summon.retrieveNextSkillOrder`, `SkillOrder`, and `SummonController.useSkill(SkillOrder)`.
- Added `Player.RetrieveNextPetSkillOrder`, matching Java's poll-style order consumption for the represented queue.
- Added `PlayerSummonCastSpellService`:
  - rejects missing or mismatched represented pet summon ids;
  - consumes the next represented pet order;
  - requires target object id match;
  - records skill id/level mismatch while still selecting the queued order, matching Java's warning-then-use behavior.
- Kept full known-list target lookup, mercenary support, `SummonController.useSkill`, `SkillEngine`, release-on-success, and audit/logging outside this unit.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_CASTSPELL.runImpl` | `Aion.GameServer.Services.PlayerSummonCastSpellService` plus `CmSummonCastSpell` parser | Client Packet Handler / Service | Partial | Regression Tested | Needs Verification | C# models the represented pet summon order-consumption path. It is not yet wired into `GameServerConnection` and does not perform live known-list target resolution, mercenary handling, packet sends, audit logging, or summon-controller execution. |
| `com.aionemu.gameserver.model.gameobjects.Summon.retrieveNextSkillOrder` | `Player.RetrieveNextPetSkillOrder` | Summon Queue Projection | Partial | Regression Tested | Needs Verification | Poll behavior is represented, but the queue lives on `Player` and is not Java's thread-safe `ConcurrentLinkedQueue` on `Summon`. |
| `com.aionemu.gameserver.model.gameobjects.Summon.addSkillOrder` / `SkillOrder` | `Player.AddPetSkillOrder` / `PlayerPetSkillOrder` | Summon Skill Order Projection | Partial | Regression Tested | Needs Verification | Add and retrieve now exist for represented orders. Target equality is object-id based and does not model Java `Creature` identity. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummonOrMercenary` | `Player.HasPetSummon` / `Player.PetSummonObjectId` checks | Player/Summon Lookup Projection | Partial | Regression Tested | Needs Verification | Only the represented pet branch exists. Java's summon-or-mercenary lookup, non-pet summon rejection, and NPC template checks remain unsupported. |
| `CM_SUMMON_CASTSPELL` skill mismatch warning | `PlayerSummonCastSpellResult.SkillMismatch` | Logging/Audit Projection | Partial | Regression Tested | Needs Verification | C# records mismatch and continues with the queued order. It does not emit Java-equivalent warning logs. |
| `com.aionemu.gameserver.controllers.SummonController.useSkill(SkillOrder)` | Not yet implemented; represented by `ExecutedOrder` result | Controller / Skill Execution | Not Started | No Tests | Needs Verification | Java validates `petHasSkill`, creates and uses a `SkillEngine` skill, transfers hate, and may release the summon. |
| `com.aionemu.gameserver.dataholders.PetSkillData.petHasSkill` | `PetSkillTable.PetHasSkill`, not consumed here | Static Data Lookup | Partial | Regression Tested in static data only | Needs Verification | Lookup exists from earlier work but is not wired into summon execution yet. |

## Tests Added Or Updated

- `PlayerSummonCastSpellServiceTests.Handle_ConsumesMatchingQueuedPetOrderForRepresentedSummon`
- `PlayerSummonCastSpellServiceTests.Handle_UsesQueuedOrderWhenClientSkillDiffersAndMarksMismatch`
- `PlayerSummonCastSpellServiceTests.Handle_ConsumesQueuedOrderWithoutExecutionWhenTargetDoesNotMatch`
- `PlayerSummonCastSpellServiceTests.Handle_RequiresRepresentedPetSummonAndQueuedOrder`

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated packet goldens, live known-list target resolution, live summon object behavior, Java logging output, reflection behavior, threading behavior, date/time behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented summon-cast order-consumption service.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: GameServerConnection handler wiring, live known-list target resolution, live Summon model/queue ownership, mercenary branch, `SummonController.useSkill`, SkillEngine execution/release, and Java runtime/live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The service is not yet invoked by the socket packet handler.
- Target resolution is represented by object id only.
- Queue behavior is not thread-safe and is not attached to a real `Summon`.
- Mercenary handling, `PetSkillData.petHasSkill`, summon-controller execution, release behavior, and audit/logging are still missing.
- No Java runtime or live-client validation was performed.

## Next Recommended Unit of Work

Wire the represented `PlayerSummonCastSpellService` into `GameServerConnection` for `CmSummonCastSpell` packets with a conservative packet-send seam for `STR_SKILL_NOT_NEED_PET`, or add the next execution-side bridge for `SummonController.useSkill(SkillOrder)` that validates `PetSkillTable.PetHasSkill` and records the would-be `SkillEngine` invocation. Keep live known-list, mercenary, release, and audit/logging gaps explicit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6IV-Completion.md`
   - this handoff
3. Inspect Java `CM_SUMMON_CASTSPELL.java`, `SummonController.useSkill(SkillOrder)`, and `PetSkillData.petHasSkill`.
4. Inspect C# `PlayerSummonCastSpellService`, `PlayerPetSkillOrder`, `PlayerPetOrderSkillService`, `CmSummonCastSpell`, and connection packet dispatch.
5. Implement one narrow parser or caller unit with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
