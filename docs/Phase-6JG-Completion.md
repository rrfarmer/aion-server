# Phase 6JG Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6JF and covers Session 755.

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

### Session 755 - Represented Known-Object Mercenary Metadata

- Re-inspected Java `Player.getSummonOrMercenary` mercenary lookup.
- Added `PlayerSummonKnownObject`.
- Added `PlayerSummonKnownNpcTemplateType.None` and `Mercenary`.
- Added `Player.SetSummonKnownObject(PlayerSummonKnownObject)` and `Player.TryGetSummonKnownObject`.
- Updated `Player.GetSummonOrMercenaryKind` to detect represented creator-owned known mercenaries using object id, creature kind, creator object id, and mercenary template type.
- Preserved kind-only known-object helpers for existing target validation.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.Player.getSummonOrMercenary` | `Aion.GameServer.Model.GameObjects.Player.GetSummonOrMercenaryKind` using `PlayerSummonKnownObject` | Player/Summon Lookup Projection | Partial | Regression Tested | Needs Verification | C# recognizes represented known-list mercenaries by object id, creature kind, creator id, and template type. Java returns live `Creature`/`Npc` references. |
| `com.aionemu.gameserver.world.knownlist.KnownList.getObject` | `Player.SetSummonKnownObject(PlayerSummonKnownObject)` / `TryGetSummonKnownObject` | Known-List Projection | Partial | Regression Tested | Needs Verification | C# stores metadata on `Player`, not a live known-list owner. Visibility lifecycle, synchronization, serialization, and live references remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.Npc.getCreatorId` | `PlayerSummonKnownObject.CreatorObjectId` | NPC Metadata Projection | Partial | Regression Tested | Needs Verification | Creator id is caller-seeded metadata, not a live `Npc` property. |
| `com.aionemu.gameserver.model.gameobjects.Npc.getNpcTemplateType` / `com.aionemu.gameserver.model.templates.npc.NpcTemplateType.MERCENARY` | `PlayerSummonKnownNpcTemplateType.Mercenary` | NPC Template Type Projection | Partial | Regression Tested | Needs Verification | Only the mercenary template value needed by this branch is represented. |
| `com.aionemu.gameserver.model.gameobjects.Creature` / `Npc` type check | `PlayerSummonKnownObject.Kind == Creature` plus template metadata | Type Projection | Partial | Regression Tested | Needs Verification | C# uses enum metadata instead of Java type hierarchy and `instanceof`. |

## Tests Added Or Updated

- `PlayerSummonCastSpellServiceTests.Handle_RecognizesCreatorOwnedKnownMercenaryMetadata`
  - Validates creator-owned represented known mercenary routes to `MercenaryReady`.
  - Validates wrong creator id returns `PetRequired` with actor kind `None`.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live known-list behavior, object identity, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented known-object mercenary lookup metadata slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `KnownList`, live `Npc`/`Creature` references, Java type hierarchy/reflection, spawn/template metadata loading, known-list lifecycle, synchronization/threading, serialization, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Known-list objects are metadata, not live Java objects.
- Java `instanceof Npc` and object equality are not represented beyond enum fields.
- Creator id and template type are caller-seeded.
- Known-list lifecycle, visibility updates, synchronization, serialization, Java runtime, and live-client behavior remain unverified.
- Controller execution, `SkillEngine`, release-on-success, packet fanout, and live audit/log sinks remain partial or missing.

## Next Recommended Unit of Work

Use the new `PlayerSummonKnownObject` metadata to reduce direct represented mercenary fields: derive represented mercenary npc/template id from known-object metadata where possible, or add represented object-id equality helpers that can later swap from ids to live `Creature` references. Keep full Java `KnownList`, `Npc`, template loading, and controller execution gaps explicit.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6JF-Completion.md`
   - this handoff
3. Inspect Java `Player.getSummonOrMercenary`, `CM_SUMMON_CASTSPELL.java`, `Npc.getObjectTemplate().getTemplateId`, `KnownList.getObject`, and summon target equality.
4. Inspect C# `Player`, `PlayerSummonKnownObject`, `PlayerSummonCastSpellService`, `PlayerSummonSkillExecutionService`, and summon cast tests.
5. Implement one narrow represented known-object or target-identity slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
