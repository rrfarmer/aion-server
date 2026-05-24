# Phase 6JH Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6JG and covers Session 756.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 29 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1353 tests.

## Recent Work Completed

### Session 756 - Represented Mercenary Template Id Resolution

- Re-inspected Java `CM_SUMMON_CASTSPELL.runImpl` mercenary pet-skill guard.
- Extended `PlayerSummonKnownObject` with `NpcTemplateId`.
- Added `Player.GetSummonOrMercenaryNpcId(objectId)`.
- Updated `PlayerSummonSkillExecutionService.ValidateMercenaryExecution` to resolve represented mercenary actor kind and npc/template id through `Player`.
- Updated service and connection tests to seed Java-shaped known-object mercenary metadata instead of relying only on direct represented mercenary fields.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_CASTSPELL.runImpl` mercenary pet-skill guard | `PlayerSummonSkillExecutionService.ValidateMercenaryExecution` using `Player.GetSummonOrMercenaryNpcId` | Client Packet Handler / Service | Partial | Regression Tested | Needs Verification | C# derives represented mercenary npc/template id from known-object metadata before `PetSkillTable.PetHasSkill`. Java uses a live `Creature` template id. |
| `com.aionemu.gameserver.model.gameobjects.Npc.getObjectTemplate().getTemplateId` | `PlayerSummonKnownObject.NpcTemplateId` / `Player.GetSummonOrMercenaryNpcId` | NPC Template Id Projection | Partial | Regression Tested | Needs Verification | Template id is metadata, not a live `NpcTemplate`. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummonOrMercenary` | `Player.GetSummonOrMercenaryKind` / `GetSummonOrMercenaryNpcId` | Player/Summon Lookup Projection | Partial | Regression Tested | Needs Verification | C# resolves represented pet summon, known-list mercenary metadata, and legacy fallback. Java returns live `Creature` references. |
| `com.aionemu.gameserver.dataholders.PetSkillData.petHasSkill` | `PetSkillTable.PetHasSkill` consumed by `ValidateMercenaryExecution` | Static Data Lookup | Partial | Regression Tested with loaded static data | Needs Verification | Tests validate represented template id `833288` accepts `22107` and rejects `9999`; Java singleton/golden behavior remains unverified. |
| `com.aionemu.gameserver.world.knownlist.KnownList.getObject` | `PlayerSummonKnownObject` metadata feeding execution validation | Known-List Projection | Partial | Regression Tested | Needs Verification | Metadata remains player-owned, not live known-list state. |

## Tests Added Or Updated

- `PlayerSummonCastSpellServiceTests.Handle_RecognizesCreatorOwnedKnownMercenaryMetadata`
  - Validates known mercenary metadata carries npc/template id `833288`.
  - Validates wrong creator id yields no mercenary npc id.
- `PlayerSummonSkillExecutionServiceTests.ValidateMercenaryExecution_PlansControllerUseAndAuditsInvalidSkill`
  - Validates valid/invalid mercenary pet-skill guard behavior from known-object metadata.
- `GameServerConnectionCastSpellTests.HandleSummonCastSpellAsync_ValidRepresentedMercenarySkillPlansControllerUse`
  - Validates connection-level valid-skill planning using known-object metadata.
- `GameServerConnectionCastSpellTests.HandleSummonCastSpellAsync_InvalidRepresentedMercenarySkillRecordsAuditProjection`
  - Validates invalid-skill audit projection using known-object metadata.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live `NpcTemplate` behavior, object identity, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented mercenary npc/template-id resolution slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `Npc`/`NpcTemplate` objects, XML template object behavior, live `KnownList`, object identity/equality, controller execution, audit/log sinks, Java runtime comparison, live-client validation, and threading/serialization verification.
- Estimated overall migration completion: 66%

## Remaining Risks

- NPC template id is caller-seeded metadata.
- Known-list objects are not live Java objects and do not model visibility/lifecycle/synchronization.
- Direct represented mercenary fields still exist as a fallback.
- Controller execution, target mutation, `SkillEngine`, packet fanout, audit/log sinks, Java runtime, and live-client behavior remain unverified.
- Reflection, threading, serialization, precision/rounding, and date/time behavior remain unverified for the broader path.

## Next Recommended Unit of Work

Continue replacing legacy direct represented mercenary fields by carrying enough known-object metadata for connection-level target assignment and audit output, or add a small target-reference abstraction around object ids so future live `Creature` references can replace id equality cleanly. Keep live `Npc`, `Creature`, template loading, and controller execution gaps explicit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6JG-Completion.md`
   - this handoff
3. Inspect Java `CM_SUMMON_CASTSPELL.java`, `Player.getSummonOrMercenary`, `Npc.getObjectTemplate().getTemplateId`, `KnownList.getObject`, and target equality.
4. Inspect C# `Player`, `PlayerSummonKnownObject`, `PlayerSummonCastSpellService`, `PlayerSummonSkillExecutionService`, and summon cast tests.
5. Implement one narrow represented known-object or target-reference slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
