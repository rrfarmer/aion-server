# Phase 6IA Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HZ and covers Session 723.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "WorldNpcDamageServiceTests|EquipmentObserverBurnWorkflowServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 58 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1300 tests.

## Recent Work Completed

### Session 723 - Production Bootstrap Wiring For Observer Burns

- Added a lazy item-template resolver to `WorldNpcSkillDamageService`.
- Updated `Program.cs` to construct `WorldNpcSkillDamageService` with lazy access to `GameServerRuntimeContext.DataManager.StaticData.ItemTemplates`.
- Wired production persistence delegates from `PlayerEnterWorldService.SaveIdianPolishBurnMutationAsync` and `SaveItemChargeBurnMutationAsync`.
- Preserved conservative pre-bootstrap behavior: missing static data skips observer burns instead of throwing or mutating with incomplete templates.
- Added `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_UsesLazyItemTemplatesForEquipmentObserverBurns`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.GameServer.main` / Java bootstrap static-data initialization | `Aion.GameServer.Program` registration for `WorldNpcSkillDamageService` plus `GameServerRuntimeContext.DataManager` lazy access | Bootstrap / DI Wiring | Partial | Regression Tested | Needs Verification | C# production registration now supplies post-bootstrap item-template access for observer burns. This is source-derived from Java's static `DataManager` availability after bootstrap, but no production host startup with this specific service resolution path, live static data, or live combat invocation was run. |
| `com.aionemu.gameserver.skillengine.effect.DamageEffect.applyEffect` | `Aion.GameServer.Services.WorldNpcSkillDamageService.ApplyDamageEffectAsync` | Skill Effect Caller | Partial | Regression Tested | Needs Verification | C# can now reach observer burns from represented ordinary skill damage using templates resolved lazily from production bootstrap state. Full Java `SkillEngine`, `AttackUtil`, result-list integration, and live packet order remain partial. |
| `com.aionemu.gameserver.skillengine.effect.AbstractOverTimeEffect.onPeriodicAction` / periodic damage effects | `WorldNpcSkillDamageService.ApplyDamageEffectAsync` through dot-attacked mappings | Skill Effect Caller / Dot Callback | Partial | Regression Tested | Needs Verification | Lazy template access also supports represented dot-attacked observer burns after static data loads. Actual scheduled effect lifecycle, Java `Effect` identity/order, and live dot dispatch remain unverified. |
| `com.aionemu.gameserver.dataholders.DataManager` static item-template access | `Aion.GameServer.Services.GameServerRuntimeContext.DataManager.StaticData.ItemTemplates` resolved by `WorldNpcSkillDamageService` | Static Data Dependency | Partial | Unit Tested with lazy resolver | Needs Verification | C# uses a delegate instead of Java static access because the port stores loaded static data in runtime context. This is an intentional C# DI difference. Pre-bootstrap null data skips observer burns; Java static-data availability assumptions need production startup validation. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` persistence-side item mutation helpers | `Aion.GameServer.Services.PlayerEnterWorldService.SaveIdianPolishBurnMutationAsync` / `SaveItemChargeBurnMutationAsync` delegates supplied from `Program.cs` | Persistence Dependency | Partial | Regression Tested through existing delegate capture tests | Needs Verification | Production registration now supplies the persistence delegates used by the skill-damage observer-burn workflow. Live DAO transaction/autocommit/rollback behavior, Java persistent-state flush cadence, and database comparison remain unverified. |
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `WorldNpcSkillDamageService` lazy-template observer burn path via `EquipmentObserverBurnWorkflowService` | Model Helper Dependency | Partial | Regression Tested | Needs Verification | New lazy-resolver test proves represented idian burn state mutation still occurs without direct constructor templates. Java synchronization, exhausted-idian packet-before-clear serialization, `RandomBonusEffect` stat refresh, and DAO delete timing remain unresolved. |
| `com.aionemu.gameserver.model.items.ChargeInfo.updateChargePoints` | `WorldNpcSkillDamageService` lazy-template observer burn path via `EquipmentObserverBurnWorkflowService` | Model Helper Dependency | Partial | Regression Tested | Needs Verification | New lazy-resolver test proves represented charge burn state mutation still occurs without direct constructor templates. Java synchronized mutation, `PersistentState.UPDATE_REQUIRED` batching, and live persistence timing remain unresolved. |

## Tests Added Or Updated

- `WorldNpcDamageServiceTests.ApplyDamageEffectAsync_UsesLazyItemTemplatesForEquipmentObserverBurns`
  - Validates a lazy item-template resolver is invoked once for represented ordinary attack observer burns, then idian and charge state are mutated through the existing workflow.
- Existing `WorldNpcDamageServiceTests`, `EquipmentObserverBurnWorkflowServiceTests`, and `PlayerEnterWorldServiceTests` matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, production host startup with live combat, Java-generated golden packets, packet ordering relative to `SM_ATTACK_STATUS`, threading behavior, live DAO behavior, reflection behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 production bootstrap wiring slice for represented skill-damage observer burns.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: production packet-send ordering, live combat caller invocation, full SkillEngine/AttackUtil integration, Java runtime/golden packet comparison, live DAO comparison, and synchronized/threading comparison.
- Estimated overall migration completion: 65%

## Remaining Risks

- `WorldNpcSkillDamageService` still returns observer-burn packets but does not send them through `GameServerConnection`; production packet ordering relative to `SM_ATTACK_STATUS` remains unresolved.
- Production bootstrap wiring is covered by compile/full-suite validation and unit-level lazy-resolver behavior, but not by a live server combat path.
- Pre-bootstrap missing static data intentionally skips observer burns; this is a C# lifecycle guard and not a proven Java behavior.
- Delegate persistence is wired from production registration, but live DAO transaction/autocommit/rollback behavior and Java flush cadence remain unverified.
- Java observer dispatch order, synchronization, exhausted-idian serialization, stat/effect refresh fanout, and live client behavior remain unverified.

## Next Recommended Unit of Work

Add a focused `WorldNpcSkillDamageResult` packet fanout/caller seam that proves observer-burn packets are sent after the `SM_ATTACK_STATUS` broadcast and in idian-before-charge order. If a safe production `GameServerConnection` skill-damage call path exists, wire that seam there; otherwise keep it as a caller-level service/test until the real skill/effect runtime has a stable home.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HZ-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
