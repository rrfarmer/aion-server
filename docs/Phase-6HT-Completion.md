# Phase 6HT Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HS and covers Session 716.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemChargeServiceTests|WorldNpcDamageServiceTests"`
  - Result: Passed, 37 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1284 tests.

## Recent Work Completed

### Session 716 - Equipped Charge Observer Burn Bridge

- Added `ItemChargeService.BurnEquippedChargePoints`, `ItemChargeBurnPlan`, and `ItemChargeObserverEvent`.
- The bridge scans equipped conditioned items, resolves main/fused-item `Improvement` data, applies Java attack/defend burn amounts, and returns immutable inventory updates plus charge-bar update flags.
- Preserved Java `ChargeInfo.attack` / `attacked` guard behavior:
  - attack/attacked observer callbacks burn only when `skillId == 0`,
  - dot-attacked observer callbacks can burn with a nonzero skill id.
- Added `ItemChargeServiceTests.BurnEquippedChargePoints_BurnsAllEquippedConditionedItemsForObserverEvents`.
- Added `ItemChargeServiceTests.BurnEquippedChargePoints_SkipsSkillAttackButAllowsDotAttacked`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.stats.listeners.ItemEquipmentListener.onItemEquipment` | `Aion.GameServer.Services.ItemChargeService.BurnEquippedChargePoints` | Equipment Listener / Observer Bridge | Partial | Unit Tested | Needs Verification | C# now has a bridge that behaves as if equipped conditioned items have `ChargeInfo` observers attached. Full equip-time observer registration, stat effect application/removal, summon stat updates, item-set recalculation, enchant/tempering behavior, reflection/JAXB stat-function loading, and live observer dispatch remain unported or outside this unit. |
| `com.aionemu.gameserver.model.items.ChargeInfo` | `Aion.GameServer.Services.ItemChargeService.BurnEquippedChargePoints` / `ItemChargeBurnPlan` / `ItemChargeUpdateResult` | Model Helper / Observer Boundary | Partial | Regression Tested | Needs Verification | C# now burns all equipped conditioned items for represented observer events and returns charge updates. Java's synchronized mutation, `PersistentState` changes, `World.getPlayer`, packet send, and actual `ActionObserver` lifecycle are still not modeled. |
| `com.aionemu.gameserver.model.items.ChargeInfo.attack` | `ItemChargeObserverEvent.Attack` consumed by `ItemChargeService.BurnEquippedChargePoints` | Observer Callback | Partial | Unit Tested | Needs Verification | C# preserves Java's `skillId == 0` guard and uses `BurnAttack` for outgoing ordinary attacks. The combat service currently emits attack observer notifications but the production caller has not yet persisted/sent these charge burns. |
| `com.aionemu.gameserver.model.items.ChargeInfo.attacked` | `ItemChargeObserverEvent.Attacked` consumed by `ItemChargeService.BurnEquippedChargePoints` | Observer Callback | Partial | Unit Tested | Needs Verification | C# preserves Java's `skillId == 0` guard and uses `BurnDefend` for incoming ordinary attacks. Actual player-vs-player/player-vs-NPC observer dispatch, packet order, and DB persistence remain unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo.dotattacked` | `ItemChargeObserverEvent.DotAttacked` consumed by `ItemChargeService.BurnEquippedChargePoints` | Observer Callback | Partial | Unit Tested | Needs Verification | C# allows dot-attacked burn even with nonzero skill id, matching Java source. Dot effect lifecycle, threading, and live effect observer sequencing are not implemented. |
| `com.aionemu.gameserver.model.gameobjects.Item.getImprovement` | `ItemChargeService.BurnEquippedChargePoints` via `ItemTemplateTable` main/fusion lookup | Data Accessor Dependency | Partial | Unit Tested | Needs Verification | C# resolves improvement from the main item or fusioned item for charge burn. Broader item object lifecycle, JAXB/reflection template loading, and exact fused-template state are not Java-runtime verified. |

## Tests Added Or Updated

- `ItemChargeServiceTests.BurnEquippedChargePoints_BurnsAllEquippedConditionedItemsForObserverEvents`
  - Validates all equipped conditioned items burn, non-conditioned/un-equipped/zero-charge items are skipped, fused-item improvement burn data is used, and charge-bar flags flow through the returned burn plan.
- `ItemChargeServiceTests.BurnEquippedChargePoints_SkipsSkillAttackButAllowsDotAttacked`
  - Validates Java's non-dot `skillId == 0` guard and dot-attacked exception.
- Existing `ItemChargeServiceTests` and `WorldNpcDamageServiceTests` matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden vectors, live observer dispatch, synchronized/threading behavior, persistent-state mutation output, packet ordering, serialization, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 equipped charge observer burn bridge slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: production combat/effect caller invocation, packet/persistence caller coverage, synchronized/threading comparison, Java runtime/golden comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The C# bridge is still not invoked from a production combat/effect caller; it prepares the next boundary but does not yet send `SM_INVENTORY_UPDATE_ITEM` or persist the charge updates.
- Java `ChargeInfo` mutates state under synchronization and sets persistent state on the item/equipment; C# returns immutable updates, so threading and persistence parity are unresolved.
- Actual `ItemEquipmentListener` responsibilities beyond charge observers are broad and remain partial: stats, set bonuses, idian effects, buff skills, enchant/tempering, summon stats, and observer registration/removal.
- Packet ordering for charge burn updates relative to attack status packets is unknown without live Java capture.
- No Java runtime, reflection/JAXB template, or live-client comparison was run.

## Next Recommended Unit of Work

Continue this charge observer path by adding the first production-style caller that consumes `ItemChargeBurnPlan`: persist changed items, send `SM_INVENTORY_UPDATE_ITEM` with update type `Charge` only for `ChargeBarChanged`, and preserve Java packet ordering relative to represented attack/dot observer notifications where a stable C# caller exists. If that caller is still too premature, move to the parallel idian burn caller using the same observer-event shape.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HS-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
