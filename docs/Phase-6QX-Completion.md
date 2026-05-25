# Phase 6QX Completion Handoff - ItemPurification Persistence Payload Plumbing

Date: May 25, 2026
Unit of Work: UOW-954
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-954] Add item purification persistence payload`)

## Status

Phase 6 is still in progress. This unit adds the narrow ItemPurification persistence payload mapper and repository contract, but does not enable automatic live `CM_ITEM_PURIFICATION` execution.

The normal packet dispatch path remains plan-only. The UOW-952 opt-in live execution helper still mutates/sends only when explicitly invoked by tests or future controlled callers.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistencePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPersistencePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/ItemPurification-Persistence-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QX-Completion.md`

## What Changed

- Added `ItemPurificationPersistencePlanService`.
- Added `ItemPurificationPersistencePlan` and status enum.
- Added `IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync`.
- Added recording fields/method to `EmptyPlayerEnterWorldRepository` and the test-only `CapturingEnterWorldRepository`.
- Added MySQL repository implementation using existing helpers:
  - material count update
  - material delete
  - base item count update/delete
  - target item count update/add
  - optional abyss rank save
- Added focused tests proving ready live mutation output maps to persistence payloads and unready inputs produce no writes.
- Updated the persistence plan document to note that the contract/payload mapper now exists while automatic invocation and inherited target `item_stones` persistence remain open.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Repository contract/payload plumbing | `ItemPurificationService`, `InventoryDAO`, `AbyssRankDAO`, `Storage` | repository, new service, tests | Code / Tests | No | Medium | Completed in this unit; shared repository file required sequential ownership. |
| B | Target item-stone insert persistence | `InventoryDAO`, `ItemStoneListDAO`, `ItemPurificationService.upgradeItem` | repository helper/tests | Code / Tests | No | Medium | Needed before full target DB parity. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate safe alternative. |
| D | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationPersistencePlanServiceTests|ItemPurificationLiveMutationServiceTests|ItemPurificationMutationSnapshotServiceTests|GameServerConnectionItemPurificationTests|AbyssPointsServiceTests"
```

Result: passed, 29 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1646 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPersistencePlanService` and `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` | Service / Persistence Payload | Partial | Unit Tested | Partial Parity | C# now maps ready material/base/AP mutation results into repository payloads. Missing Java behaviors remain: storage persistent-state flags, deleted item queue, quest callbacks, packet sends inside storage, and automatic live handler persistence. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationPersistencePlanService` and `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` | Service / Target Add Persistence | Partial | Unit Tested | Needs Verification | C# maps target add rows and repository can insert inventory rows. Target item-stone/godstone/fusion/idian row insertion for inherited target items is still not implemented, so DB parity is blocked. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` | Repository / Inventory Persistence | Partial | Build/Regression Tested | Needs Verification | C# method writes material updates/deletes, base update/delete, and target updates/adds in one transaction using existing helpers. This is intentionally more atomic than Java category-level commits and has not been DB integration tested. |
| `com.aionemu.gameserver.dao.AbyssRankDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` via `SaveAbyssRankAsync` | Repository / AP Rank Persistence | Partial | Build/Regression Tested | Needs Verification | C# method can save an updated AP rank when supplied. Java persistent-state insert/update distinction, `last_update` timing, rank-position semantics, and AP side-effect execution remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Services.ItemPurificationPersistencePlanService` / `Player.InventoryItems` snapshots | Storage / Dirty Write Projection | Partial | Unit Tested | Needs Verification | C# projects from post-mutation snapshots rather than Java `PersistentState`. Kinah no-write is preserved for Java's negative `decreaseKinah` call. Threading, collection order, storage capacity, and quest notifications remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage` | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` | Storage Collection | Partial | Manual Source Review | Needs Verification | No new collection behavior was ported. C# mapper depends on deterministic snapshot object ids; Java `ConcurrentHashMap` ordering and slot/limit behavior are still unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationLiveExecutionAsync` plus persistence contract | Client Handler / Persistence Boundary | Partial | Regression Tested in C# | Needs Verification | Repository contract exists but handler automatic dispatch remains plan-only and does not call it. Runtime packet/DB ordering is still unverified. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService` plus persistence payload `AbyssRank` | AP Spend Boundary | Partial | Unit Tested | Partial Parity | AP plan updated rank now flows into the persistence payload. AP rank packets, rank-limit equipment checks, abyss skill updates, Legion contribution, Siege callbacks, and Java runtime comparison remain deferred. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePersistencePlan_MapsMaterialBaseTargetAndAbyssRankWrites` | Unit | Java `ItemPurificationService.decreaseMaterials`, `upgradeItem`, `Storage`, `InventoryDAO`, and `AbyssRankDAO` source review | Validates material count update, base delete, target add, AP rank, and kinah no-write payload mapping from ready live mutation output. | Deterministic C# unit coverage for repository payload shape. | Does not execute real DB writes, target item-stone insert rows, quest callbacks, packet bytes, or Java runtime comparison. |
| `CreatePersistencePlan_RejectsUnreadyMutationPreview` | Unit | Java storage mutation requires valid live storage state before persistence | Validates unready application/mutation inputs do not produce persistence writes. | Deterministic C# guard coverage. | Does not model Java partial material decrement behavior under mid-loop failure. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and does not invoke live execution or persistence.
- `SaveItemPurificationMutationAsync` is implemented but not called by live handler execution.
- C# target insert persistence still does not write inherited `item_stones` rows for mana stones, fusion stones, godstones, or idian state.
- C# repository method uses one transaction for the whole write set; this is an intentional safety difference from Java `InventoryDAO` category commits and still needs DB integration verification.
- C# storage still lacks Java `PersistentState`, `deletedItems`, quest item callbacks, packet sends from storage methods, and `ConcurrentHashMap`/locking behavior.
- AP rank side-effect execution remains metadata only for ItemPurification.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 2 narrow C# persistence artifacts (`ItemPurificationPersistencePlanService`, repository contract/method)
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, target inherited item-stone persistence, automatic live handler persistence invocation, quest callbacks, and AP side-effect execution
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Implement target inherited `item_stones` persistence for newly inserted ItemPurification targets, or add a narrow persistence caller seam that invokes `SaveItemPurificationMutationAsync` only from explicit opt-in live execution.

Suggested shape for target item-stone persistence:
- Read Java `ItemStoneListDAO` and current C# item-stone helper methods.
- Add an insert helper that persists mana stones, fusion stones, godstone, and idian state for newly inserted target items.
- Add focused tests around generated SQL/helper behavior if the repository test surface supports it; otherwise document the blocker and keep runtime persistence disabled.

Suggested shape for opt-in persistence caller seam:
- Compose `ItemPurificationLiveExecutionService`, `ItemPurificationPersistencePlanService`, and repository save behind a new explicit helper/service.
- Preserve current success-send-before-mutation ordering analysis.
- Do not wire the normal packet dispatcher yet.

Do not combine with:
- automatic live packet sending from `HandleInfrastructurePacketAsync`
- quest item get/remove callbacks
- AP rank side-effect execution
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Target inherited item-stone insert persistence | `PlayerEnterWorldRepository.cs`, tests | Medium | Shared repository file; do sequentially. |
| B | Explicit opt-in persistence caller seam | new service + tests, maybe handler helper | Medium | Avoid automatic dispatch. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| D | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

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
6. If still tooling-blocked, choose target inherited item-stone persistence, an opt-in persistence caller seam, or the isolated Kinah charge-all regression.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
