# Phase 6QF Completion Handoff - ItemPurification Packet Input Snapshot Assembler

Date: May 25, 2026
Unit of Work: UOW-936
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-936] Add ItemPurification packet input snapshot assembler`)

## Status

Phase 6 is still in progress. This unit adds a pure snapshot assembler that bridges already-mutated item/cube snapshots into the packet-plan inputs introduced in previous ItemPurification units. It does not mutate inventory, allocate object ids, spend AP, persist state, trigger quests, send packets, or wire `GameServerConnection`.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketInputSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketInputSnapshotServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QF-Completion.md`

## What Changed

- Added `ItemPurificationPacketInputSnapshotService`.
- The service consumes an `ItemPurificationApplicationPlan`, an already-mutated inventory snapshot, `ItemTemplateTable`, and packet-indexed `ItemPurificationCubeSnapshot` values.
- It produces:
  - inventory packet inputs keyed by object id
  - cube packet inputs keyed by packet-plan operation index
  - diagnostic lists for missing templates, missing/mismatched item snapshots, missing cube snapshots, and invalid cube snapshots
- It rejects missing, empty, and not-ready application plans.
- It validates post-mutation item object id, item id, and count before creating inventory packet inputs.
- `CreateCubeSnapshot` projects Java `Storage.size()` semantics by excluding C# kinah inventory rows and non-cube locations.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Pure ItemPurification packet input assembler | `ItemPacketService`, `SM_CUBE_UPDATE`, `Storage.size` | new service + new tests | Service/Test | Sequential writer | Medium | Completed by orchestrator; new files only. |
| B | Assembler API/keying analysis | same Java/C# planner/storage files | read-only | Java/C# Analysis | Yes | Medium | Completed by read-only explorer; confirmed new service/tests scope. |
| C | ItemCharge AP hardening fallback | Java/C# item charge AP code | read-only | Analysis | Yes | Medium | Completed by read-only explorer; recommended as next smaller AP unit. |
| D | Live handler audit | `CM_ITEM_PURIFICATION`, `GameServerConnection` | read-only | Analysis | Yes | High | Deferred; live handler remains blocked by mutation/AP/persistence scope. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationPacketInputSnapshotServiceTests|ItemPurificationPacketPlanServiceTests"
```

Result: passed, 19 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1607 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.ItemPurificationPacketInputSnapshotService` | Runtime Packet Input Projection | Partial | Regression Tested in C# | Partial Parity | Assembler projects already-mutated stack item snapshots into inventory update packet inputs, requiring matching object id, item id, and post-mutation count. It does not execute Java `Storage.decreaseItemCount`. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `ItemPurificationPacketInputSnapshotService` | Runtime Packet Input Projection | Partial | Regression Tested in C# | Partial Parity | Assembler projects target-add snapshots into inventory add packet inputs only when the post-mutation target item exists and matches the planned object/item/count. It does not allocate object ids, call `Storage.add`, or execute quest hooks. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `ItemPurificationPacketInputSnapshotService` plus `ItemPurificationCubeSnapshot` | Cube Snapshot Projection | Partial | Regression Tested in C# | Partial Parity | Assembler accepts packet-indexed cube snapshots and emits guarded cube packet inputs. It preserves order-sensitive keying but depends on caller-supplied per-fanout snapshots for exact Java counts. |
| `com.aionemu.gameserver.model.items.storage.Storage.size` | `ItemPurificationPacketInputSnapshotService.CreateCubeSnapshot` | Storage Count Projection | Partial | Regression Tested in C# | Needs Verification | Helper counts non-kinah C# cube items at location `0`, matching Java's separate kinah storage assumption. Broader live `ItemStorage.size()` parity and special inventory behavior still need verification. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage` | `ItemPurificationPacketInputSnapshotService` | Storage Snapshot Consumer | Not Started for live storage mutation | Regression Tested Consumer | Needs Verification | Service consumes caller-provided post-mutation snapshots only. It does not mutate the C# inventory collection, maintain deleted-item queues, or set persistent state. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `ItemPurificationPacketInputSnapshotService` | Purification Runtime Boundary Projection | Partial | Regression Tested in C# | Partial Parity | Projection follows the operation order produced from Java decrease/upgrade flow but still lacks live material/base mutation, AP spend, Kinah decision, persistence, rollback, and Java runtime packet capture. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateInputs_ProjectsPostMutationSnapshotsIntoPacketPlanInputs` | Regression | Java `ItemPacketService`, `SM_CUBE_UPDATE`, and C# packet-plan source review | Validates post-mutation inventory and packet-indexed cube snapshots produce concrete update/delete/cube/add/cube packets through the existing packet plan. | Deterministic C# regression grounded in Java fanout order and packet input requirements. | Does not execute live mutation or compare Java runtime packets. |
| `CreateInputs_ReportsMissingSnapshotsWithoutSynthesizingPackets` | Regression | Java storage fanout source review | Validates missing target item and cube snapshots are reported instead of synthesized from stale state. | Deterministic safety regression. | Does not cover every missing snapshot combination. |
| `CreateInputs_ReportsMismatchedInventorySnapshotCounts` | Regression | Java packets serialize already-mutated `Item` source review | Validates stale item counts are rejected before packet planning. | Deterministic C# regression for post-mutation count guard. | No Java runtime artifact. |
| `CreateInputs_ReportsInvalidCubeSnapshots` | Regression | Java `SM_CUBE_UPDATE.writeImpl` source review | Validates expansion fields outside Java `writeC` byte range are rejected. | Deterministic C# guard regression. | Does not validate negative item count separately. |
| `CreateCubeSnapshot_UsesJavaStorageSizeSemanticsForKinah` | Regression | Java `Storage.size` / `ItemStorage.size` source review | Validates C# helper excludes kinah and non-cube locations from the projected cube item count. | Deterministic C# regression grounded in Java kinah storage separation. | Broader special-cube/live storage behavior remains unverified. |
| `CreateInputs_ReportsMissingTemplatesBeforeMissingInventorySnapshots` | Regression | Java packet constructors require item templates | Validates missing item templates are reported and prioritized. | Deterministic C# regression. | Does not compare Java exception behavior. |
| `CreateInputs_RejectsApplicationPlanThatStillNeedsRuntimeInputs` | Regression | C# runtime blocker review plus Java object-id allocation requirement | Validates assembler refuses not-ready application plans such as target object id `0`. | Deterministic safety regression. | Live object-id allocation remains unimplemented. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Packet-index coupling remains intentional and must be kept aligned with `ItemPurificationPacketPlanService`.
- The assembler consumes caller-provided snapshots; it does not prove live mutation, storage count capture, object-id allocation, AP spend, Kinah behavior, persistence, quest hooks, or rollback parity.
- `CreateCubeSnapshot` approximates Java kinah separation for C# snapshots but broader `Storage.size()` parity for special cube items and live inventory objects still needs verification.
- ItemCharge AP spend hardening remains a smaller independent fallback and should be considered next if live ItemPurification boundaries become too broad.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 narrow ItemPurification packet-input snapshot assembler slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification mutation/emission, live object-id allocation/storage mutation, AP concrete fanout, repository persistence, and quest/rollback side effects
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Prefer ItemCharge AP spend hardening as the next isolated AP caller convergence unit: add pure/service-level insufficient-AP guards or tests so charge planning cannot rely on `AbyssPointsService` clamping as a spend boundary.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | ItemCharge AP spend hardening | `ItemChargeService.cs`, `ItemChargeServiceTests.cs` | Medium | Recommended next UOW; keep live handler edits out unless tests prove a gap. |
| B | ItemCharge live insufficiency audit | read-only `GameServerConnection` charge sites | Low | Verify existing live checks still cover selected and charge-all paths. |
| C | ItemPurification live mutation audit | read-only purification handler/planners | High | Analysis only; do not wire live execution yet. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Inspect Java/C# ItemCharge AP spend behavior | read-only charge/AP files | edits, docs | Exact small code/test recommendation. |
| Agent B | Audit existing charge handler tests for insufficient AP | read-only charge test files | edits, docs | Test gap report. |
| Orchestrator | Add pure ItemCharge AP guard if bounded | `ItemChargeService.cs`, `ItemChargeServiceTests.cs`, docs | live handler, unrelated AP callers | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live ItemPurification handler wiring.
- ItemPurification mutation, AP, persistence, quest hooks, and rollback in the same unit.
- ItemCharge AP hardening with broad `AbyssPointsService` rank-side-effect changes.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, prefer the bounded ItemCharge AP spend hardening unit.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
