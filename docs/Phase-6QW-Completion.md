# Phase 6QW Completion Handoff - ItemPurification Persistence Plan

Date: May 25, 2026
Unit of Work: UOW-953
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-953] Document item purification persistence plan`)

## Status

Phase 6 is still in progress. This unit documents the ItemPurification persistence write set needed before automatic `CM_ITEM_PURIFICATION` live handler execution is safe.

No production C# behavior changed. Normal packet dispatch remains plan-only; the explicit opt-in live execution helper from UOW-952 remains the only live execution entry point.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `docs/ItemPurification-Persistence-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QW-Completion.md`

## What Changed

- Added a focused persistence plan for ItemPurification.
- Mapped Java mutation/persistence path:
  - `CM_ITEM_PURIFICATION.runImpl`
  - `ItemPurificationService.isPurificationAllowed`
  - `ItemPurificationService.decreaseMaterials`
  - `ItemPurificationService.upgradeItem`
  - `Storage` / `ItemStorage`
  - `InventoryDAO.store`
  - `AbyssRankDAO.storeAbyssRank`
- Mapped current C# surfaces:
  - `ItemPurificationLiveMutationService`
  - `ItemPurificationMutationSnapshotService`
  - `AbyssPointsService`
  - `IPlayerEnterWorldRepository` / `MySqlPlayerEnterWorldRepository`
- Proposed a narrow `SaveItemPurificationMutationAsync` repository contract instead of a broad generic inventory transaction.
- Documented the required write payload: material updates/deletes, base update/delete, target updates/adds, optional AP rank, and no kinah write while Java's negative-kinah no-op is preserved.
- Identified the main persistence blocker: target inserts currently persist inventory rows but not inherited `item_stones` rows for mana stones, fusion stones, godstones, or idian state.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Persistence plan analysis | `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `Storage`, `ItemStorage`, `InventoryDAO`, `AbyssRankDAO` | docs only | Analysis / Documentation | Yes | Low | Completed in this unit; no code behavior changed. |
| B | Narrow repository contract sketch | Same persistence artifacts | `PlayerEnterWorldRepository.cs`, repository tests | Code / Tests | No | Medium | Next likely unit; shared repository file should be exclusive. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate safe alternative. |
| D | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-953 docs, progress, handoff, commit | ItemPurification persistence plan doc, progress/handoff docs | runtime code files | Analysis docs, parity table, commit |

No write sub-agents were spawned because progress/handoff docs must be serialized.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionItemPurificationTests|ItemPurificationLiveExecutionServiceTests|ItemPurificationLiveMutationServiceTests|ItemPurificationMutationSnapshotServiceTests|ItemPurificationPacketInputSnapshotServiceTests|ItemPurificationPacketPlanServiceTests|AbyssPointsServiceTests"
```

Result: passed, 48 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1644 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationLiveExecutionAsync` plus persistence plan doc | Client Handler / Persistence Boundary | Partial | Regression Tested in C# | Needs Verification | Handler opt-in live execution exists, but automatic packet dispatch remains plan-only. This unit documents the persistence contract needed before production live execution can be enabled. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationLiveMutationService` plus planned `IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` | Service / Material/Base/AP Mutation | Partial | Regression Tested in C# | Partial Parity | Inventory/AP live mutation is implemented in memory. Persistence still needs material update/delete, base update/delete, AP rank write, rollback/commit behavior, and no kinah write for Java's negative-kinah no-op. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationLiveMutationService` / `ItemPurificationInheritanceService` plus planned repository insert/update | Service / Target Add Persistence | Partial | Regression Tested in C# | Needs Verification | Target item snapshots are produced and applied in memory. Repository insert currently does not persist inherited socket/godstone/fusion/idian rows for new purification targets, so live DB parity is blocked. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` and `Aion.GameServer.Data.PlayerEnterWorldRepository` | Storage / Persistent-State Source | Partial | Manual Source Review + Regression Tested in C# | Needs Verification | Java persistent-state flags, deleted item queue, quest remove/get callbacks, packet sends inside storage, and `decreaseKinah(amount > 0)` semantics were source-reviewed. C# still uses snapshot replacement and lacks storage persistent-state modeling. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage` | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` | Storage Collection | Partial | Manual Source Review | Needs Verification | Java uses `ConcurrentHashMap` storage with limit checks and object-id/item-id lookups. C# list snapshots are deterministic in tests but threading, collection ordering, and capacity behavior are unverified. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository` private inventory helpers plus planned repository method | Repository / Inventory Persistence | Partial | Manual Source Review | Needs Verification | C# has reusable insert/delete/count helpers and action-specific transaction methods. Missing method: `SaveItemPurificationMutationAsync`. Missing behavior: inserted target item-stone/godstone/fusion/idian persistence for inherited target rows. |
| `com.aionemu.gameserver.dao.AbyssRankDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveAbyssRankAsync` and `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Repository / AP Rank Persistence | Partial | Manual Source Review | Needs Verification | C# has rank upsert helper and AP plan metadata. Java persistent-state insert/update distinction, last-update timing, rank position fields, and side-effect execution remain unverified for ItemPurification. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService` plus planned rank persistence call | AP Spend Boundary | Partial | Regression Tested in C# | Partial Parity | AP spend updates in-memory rank and returns packets/side-effect metadata. Repository persistence and AP side-effect execution are not wired into live ItemPurification. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| No new tests in UOW-953 | Docs-only analysis | Java `CM_ITEM_PURIFICATION`, `ItemPurificationService`, `Storage`, `ItemStorage`, `InventoryDAO`, and `AbyssRankDAO` source review | Existing focused ItemPurification and AP regressions were rerun to confirm no regression while documenting the persistence plan. | C# regression suite still passes for the current non-persistent seams. | Does not validate DB writes, Java runtime DB state, packet bytes, quest callbacks, or AP side-effect execution. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and does not invoke live execution or persistence.
- `SaveItemPurificationMutationAsync` is not implemented yet.
- C# target insert persistence does not yet write inherited `item_stones` rows for mana stones, fusion stones, godstones, or idian state.
- C# storage still lacks Java `PersistentState`, `deletedItems`, quest item callbacks, packet sends from storage methods, and `ConcurrentHashMap`/locking behavior.
- C# should probably commit the whole purification write set atomically, which is an intentional safety difference from Java's category-level `InventoryDAO` commits and must be documented when implemented.
- AP rank persistence, rank-limit equipment checks, abyss skill updates, Legion contribution, Siege callbacks, and AP packet fanout remain incomplete.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 0 new runtime artifacts; 1 persistence plan document added for ItemPurification
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, ItemPurification repository transaction implementation, inherited item-stone persistence for target inserts, automatic live handler invocation, and quest/AP side-effect execution
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Implement the narrow ItemPurification repository contract/payload plumbing from `docs/ItemPurification-Persistence-Plan.md`.

Suggested shape:
- Add `IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync`.
- Add the `EmptyPlayerEnterWorldRepository` stub/recording fields needed for focused handler/service tests.
- Add the MySQL repository method using existing helper methods but keep it uncalled by automatic packet dispatch.
- Start with a focused test that proves the payload maps material update/delete, base delete, target add, and AP rank update from the existing mutation preview/AP plan.
- Defer inherited `item_stones` insertion for target items to a separate unit unless the repository helper is tiny and directly testable.

Do not combine with:
- automatic live packet sending from `HandleInfrastructurePacketAsync`
- quest item get/remove callbacks
- AP rank side-effect execution
- inherited socket/godstone/fusion/idian row persistence for new target items
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Repository contract/payload plumbing | `PlayerEnterWorldRepository.cs`, focused tests | Medium | Shared repository file; do sequentially. |
| B | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| C | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |
| D | Target item-stone insert plan | docs or repository helper tests | Medium | Needed before full target DB parity. |

## Do Not Parallelize

- Multiple agents editing `PlayerEnterWorldRepository.cs`.
- Multiple agents editing ItemPurification handler/service tests.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Read `docs/ItemPurification-Persistence-Plan.md`.
3. Confirm branch status and latest commit.
4. Run Parallel Work Discovery before selecting the next write unit.
5. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
6. If still tooling-blocked, implement repository contract/payload plumbing or take the isolated Kinah charge-all regression.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.

