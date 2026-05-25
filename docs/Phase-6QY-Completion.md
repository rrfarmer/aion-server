# Phase 6QY Completion Handoff - ItemPurification Target Item-Stone Persistence

Date: May 25, 2026
Unit of Work: UOW-955
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-955] Persist inserted item stones`)

## Status

Phase 6 is still in progress. This unit removes the repository blocker where newly inserted ItemPurification target rows could carry inherited socket/godstone/fusion/idian state in memory but not persist the matching `item_stones` rows.

The normal `CM_ITEM_PURIFICATION` packet dispatch path remains plan-only. The explicit opt-in live execution helper still does not call repository persistence.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryItemStonePersistenceTests.cs`
- `docs/ItemPurification-Persistence-Plan.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QY-Completion.md`

## What Changed

- Updated `InsertInventoryItemAsync` to persist attached `item_stones` rows after inserting a new inventory row.
- Added `BuildItemStonePersistenceRows` and `ItemStonePersistenceRow` for deterministic row mapping.
- Preserved Java `ItemStone.ItemStoneType` ordinal categories:
  - `0` manastone
  - `1` godstone
  - `2` fusionstone
  - `3` idianstone
- Added focused tests for:
  - mana stones
  - godstone proc count
  - fusion stones
  - idian polish number/charge
  - no-stone items
- Updated the ItemPurification persistence plan to mark inserted target `item_stones` row mapping as implemented with unit-only evidence.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Target inherited item-stone persistence | `ItemStoneListDAO`, `InventoryDAO`, `ItemPurificationService.upgradeItem` | `PlayerEnterWorldRepository.cs`, focused tests/docs | Repository Port | No | Medium | Completed in this unit; shared repository file required exclusive ownership. |
| B | Explicit opt-in persistence caller seam | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | new service/tests, maybe handler helper | Integration Fix | Not with A | Medium | Recommended next now that target stone row persistence exists. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Independent safe alternative. |
| D | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-955 repository helper, tests, docs, commit | `PlayerEnterWorldRepository.cs`, `PlayerEnterWorldRepositoryItemStonePersistenceTests.cs`, progress/handoff/persistence docs | unrelated runtime files | Code, focused tests, parity docs, commit |

No sub-agents were spawned because the selected implementation owned a shared repository file.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerEnterWorldRepositoryItemStonePersistenceTests|ItemPurificationPersistencePlanServiceTests|ItemPurificationInheritanceServiceTests|ItemPurificationLiveMutationServiceTests"
```

Result: passed, 13 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1648 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.ItemStoneListDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.BuildItemStonePersistenceRows` and inserted-item stone persistence | Repository / Item-Stone Persistence | Partial | Unit Tested | Partial Parity | C# now maps and inserts mana stone, godstone, fusion stone, and idian child rows for newly inserted inventory items. Java persistent-state add/update/delete batching, overload cleanup during load, and category-level commit behavior remain unverified. |
| `com.aionemu.gameserver.model.items.ItemStone` | `Aion.GameServer.Data.ItemStonePersistenceRow` / `Aion.GameServer.Model.GameObjects.ItemStoneSocket` | Model / Persistence Row | Partial | Unit Tested | Partial Parity | C# row mapper preserves Java `ItemStoneType` ordinal categories and slot/proc/polish fields for inserts. Java `PersistentState` transitions, `StatOwner`, equality/hash behavior, and mutable slot updates are not modeled. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.InsertInventoryItemAsync` | Repository / Inventory Insert | Partial | Unit Tested + Regression Tested | Needs Verification | Insert helper now persists attached `item_stones` after the inventory row. This affects new item inserts beyond ItemPurification too, but only row mapping is unit-tested; live DB integration and rollback behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemPurificationMutationAsync` plus inserted-item stone persistence | Service / Target Add Persistence | Partial | Unit Tested | Partial Parity | Newly inserted purification target snapshots can now carry inherited mana stones, fusion stones, godstone, and idian state into persistence rows. Automatic handler persistence, quest get callbacks, and Java runtime DB comparison remain missing. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationPersistencePlanService` / repository save path | Service / Material/Base/AP Mutation | Partial | Regression Tested in C# | Partial Parity | No new material/AP behavior changed. Persistence write set remains available but not invoked from automatic handler dispatch. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationLiveExecutionAsync` plus repository persistence pieces | Client Handler / Persistence Boundary | Partial | Regression Tested in C# | Needs Verification | Automatic packet dispatch remains plan-only and does not call live execution or repository persistence. Send/DB failure ordering is still unimplemented. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BuildItemStonePersistenceRows_MapsAllInheritedTargetStoneCategories` | Unit | Java `ItemStoneListDAO.addItemStones` and `ItemStone.ItemStoneType` source review | Validates mana stone category `0`, godstone category `1` with proc count, fusion stone category `2`, and idian category `3` with polish fields. | Deterministic C# unit coverage for Java category/column mapping. | Does not execute MySQL inserts, Java load cleanup rules, persistent-state transitions, or runtime comparison. |
| `BuildItemStonePersistenceRows_ReturnsEmptyRowsForPlainItem` | Unit | Java `ItemStoneListDAO.save` skips blank stone sets | Validates plain inserted items produce no child stone rows. | Deterministic C# guard coverage. | Does not prove DB integration behavior. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and does not invoke live execution or persistence.
- `SaveItemPurificationMutationAsync` and inserted target-stone persistence are implemented but not called by live handler execution.
- Live DB integration for new target `item_stones` rows is not tested; rollback/transaction behavior remains unverified.
- Java `ItemStoneListDAO.load` overload cleanup for invalid manastone/fusion slots is not modeled in C# loading.
- Java `ItemStone` persistent-state transitions and slot mutation behavior are not modeled.
- C# repository method still uses one transaction for the whole write set, an intentional safety difference from Java category-level commits.
- Quest item get/remove callbacks and AP side-effect execution remain metadata or missing behavior.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 inserted-item `item_stones` persistence row mapper/helper
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, automatic live handler persistence invocation, live DB integration for item-stone inserts, quest callbacks, and AP side-effect execution
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add an explicit opt-in persistence caller seam that composes live execution, persistence payload generation, and `SaveItemPurificationMutationAsync` while keeping normal packet dispatch disabled.

Suggested shape:
- Create a service/helper that takes player, handler plan, current inventory/cube expansion inputs, repository, and connection registry override.
- Execute the existing opt-in live execution path, derive `ItemPurificationPersistencePlan`, then call `SaveItemPurificationMutationAsync`.
- Add fake repository tests proving persistence payload is passed only for ready execution and not for bridge/mutation failures.
- Document send-before-mutation-before-persistence ordering and the remaining risk that Java storage sends packets during mutations rather than after repository save.

Do not combine with:
- automatic live packet sending from `HandleInfrastructurePacketAsync`
- quest item get/remove callbacks
- AP rank side-effect execution
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Explicit opt-in persistence caller seam | new service + tests, maybe handler helper | Medium | Avoid automatic dispatch. |
| B | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| C | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |
| D | ItemStone load cleanup analysis | docs/read-only or repository load tests | Low/Medium | Java deletes overload/invalid stones during load; separate from insert persistence. |

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
6. If still tooling-blocked, choose explicit opt-in persistence caller seam, isolated Kinah charge-all regression, or Java ItemStone load cleanup analysis.
7. Run focused and full tests for any C# code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.

