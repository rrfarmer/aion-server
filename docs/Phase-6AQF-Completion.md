# Phase 6AQF Completion - ItemPurification AP Persistence Guard

Date: 2026-05-28
Unit of Work: UOW-1612
Status: Complete after focused validation

## Scope

This unit hardened the staged ItemPurification persistence payload. Java spends AP inline through `AbyssPointsService.addAp` during purification before dirty state is persisted. C# models that as an `AbyssPointsAddPlan`; if an application plan says AP must be spent, the persistence plan should not become ready unless the AP mutation produced an updated rank snapshot.

This is a fail-closed C# staging guard. It does not execute Java runtime, repository SQL, or packet bytes.

## Completed Work

- Added `ItemPurificationPersistencePlanStatus.MissingAbyssRankMutation`.
- Updated `ItemPurificationPersistencePlanService.CreatePersistencePlan` to reject AP-spend plans when `AbyssPointsAddPlan.UpdatedRank` is missing.
- Added `CreatePersistencePlan_RejectsMissingAbyssRankMutationWhenApplicationSpendsAp`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused ItemPurification tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ItemPurificationPersistencePlanServiceTests|FullyQualifiedName~ItemPurificationPersistentLiveExecutionServiceTests|FullyQualifiedName~ItemPurificationLiveExecutionServiceTests|FullyQualifiedName~ItemPurificationLiveMutationServiceTests|FullyQualifiedName~ItemPurificationApplicationPlanServiceTests"
```

Result: 19 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| ItemPurification AP persistence guard | `ItemPurificationPersistencePlanService.cs`, persistence tests | Low | Yes | Isolated guard for AP rank payload completeness. |
| Nearby-refresh Java handler/XML extraction | nearby/quest extractor services/tests | Medium | No | Good next candidate; requires real-data count audit. |
| ItemCharge storage-location audit | read-only Java/C# charge files | Low | No | Useful after UOW-1611 but should be read-first. |
| Java protection serializer implementation | Java serializer/observer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because the service and test are tightly coupled.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePersistencePlan_RejectsMissingAbyssRankMutationWhenApplicationSpendsAp` | Added | AP-spend persistence payloads fail with `MissingAbyssRankMutation` when no updated rank is supplied, and emit no item/rank payload. | Source-derived from Java AP spend ordering; no Java runtime artifact. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPersistencePlanService.CreatePersistencePlan` | Service / Persistence Payload Planner | Partial | Unit Tested | Partial Parity | Persistence plan now refuses AP-spend payloads when no rank mutation result exists. Java mutates AP inline before dirty-state persistence; C# remains staged. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService` / `AbyssPointsAddPlan` consumed by `ItemPurificationPersistencePlanService` | Service / AP Mutation Dependency | Partial | Unit Tested through ItemPurification | Needs Verification | Guard requires `UpdatedRank` for AP spend plans. Full AP side effects and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.dao.InventoryDAO` and rank persistence side effects | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` payload inputs | Repository Boundary | Partial | Existing Regression Tested + New Unit Tested Guard | Needs Verification | Repository payload cannot be ready without rank data when AP was spent. SQL execution, rollback ordering, and Java dirty-state persistence timing remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player.AbyssRank` | Model / Runtime State | Partial | Unit Tested | Needs Verification | Player rank mutation is represented by copied `PlayerAbyssRank` snapshots. Java live object identity/threading and downstream observer behavior remain unverified. |

## Remaining Risks

- C# ItemPurification persistence remains transaction-oriented while Java mutates storage/AP live and later persists dirty state.
- Full AP side effects beyond current rank/player packet seams remain partial.
- Repository SQL execution, rollback ordering, and Java dirty-state timing are not runtime-compared.
- Quest notifications and nearby quest refresh remain opt-in/no-op seams for ItemPurification.
- Serialization, threading, reflection, date/time behavior, and packet byte comparison remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 1 ItemPurification AP persistence guard plus 1 focused unit regression.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows.
- Total blocked artifacts: Java runtime AP/persistence comparison, repository SQL/rollback validation, dirty-state timing comparison, full AP side effects, quest notification dispatch, nearby-refresh dispatch, packet byte comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Move to a fresh safe slice:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Nearby-refresh Java handler/XML quest-start extraction | nearby/quest extractor services/tests | Best next isolated extractor/test candidate. |
| ItemCharge storage-location audit | read-only Java `Inventory`/`Equipment` lifecycle and C# selected-charge lookup | Continue from UOW-1611 only as a read-first audit. |
| ItemPurification repository payload/rollback guard | ItemPurification persistence/execution tests | Safe only if kept isolated; do not enable automatic dispatch. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1612] Require purification AP rank persistence payload
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistencePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPersistencePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQF-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
