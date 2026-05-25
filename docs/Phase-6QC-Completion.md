# Phase 6QC Completion Handoff - ItemPurification Target Add Packet Bridge

Date: May 25, 2026
Unit of Work: UOW-933
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-933] Bridge ItemPurification target add packet`)

## Status

Phase 6 is still in progress. This unit adds a narrow concrete `SmInventoryAddItem` bridge for ItemPurification target-add packet-plan operations while keeping cube-size/AP packet fanout and live mutation as metadata-only or future work.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QC-Completion.md`

## What Changed

- Reused caller-provided `ItemPurificationInventoryPacketInput` snapshots for target-add packet construction.
- `ItemPurificationPacketPlanService` now attaches `SmInventoryAddItem.CreateItemCollect` to `AddTargetItem` operations only when the supplied snapshot matches the planned object id, item id, count, and template id, and the target object id is already allocated.
- Added focused regression coverage that decodes the concrete add packet payload and validates Java `ITEM_COLLECT` mask `0x19`, one item entry, target object/template ids, first-available slot sentinel `0xffff`, and metadata-only trailing cube-size operation.
- Success system-message, inventory-update, delete, and target-add packets can now be concrete in the dry-run packet plan.
- Cube-size, AP, and Kinah no-packet operations remain metadata-only.
- Kept object-id allocation, target `Storage.add`, live sends, AP mutation, repository persistence, cube counts, quest hook execution, and connection wiring out of scope.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Target-add packet bridge | `ItemPurificationService.upgradeItem`, `Storage.add`, `ItemPacketService.sendStorageUpdatePacket`, `SM_INVENTORY_ADD_ITEM` | `ItemPurificationPacketPlanService.cs`, focused tests | Packet Planner Bridge | Write sequential | Medium | Shared service/test files; completed by orchestrator. |
| B | Java target-add behavior check | same Java target-add packet path plus `SM_CUBE_UPDATE` | read-only | Java Analysis | Yes | Low-Medium | Completed by read-only explorer; confirmed `ITEM_COLLECT` mask `0x19`, add-then-cube ordering, and quest hook after packets. |
| C | C# add DTO API check | `SmInventoryAddItem`, `InventoryItem`, `ItemTemplateSummary`, existing packet tests | read-only | C# Analysis | Yes | Low | Completed by read-only explorer; confirmed caller-provided target snapshot/template is enough for packet construction and highlighted slot parity. |
| D | Live-send adapter | packet plan concrete packets only | connection/service/test files | Integration Fix | Later | Medium | Needs careful boundary so metadata-only operations are skipped and live mutation remains out of scope. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter ItemPurificationPacketPlanServiceTests
```

Result: passed, 7 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1595 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `Aion.GameServer.Services.ItemPurificationPacketPlanService.CreatePacketPlan` | Packet Planner / Inventory Add Bridge | Partial | Regression Tested in C# | Partial Parity | Target-add operations can now carry a concrete `SmInventoryAddItem.CreateItemCollect` when caller supplies a post-creation target item/template snapshot. Java source reviewed: purification target add uses `ITEM_COLLECT` mask `0x19`. Live send and Java runtime byte comparison remain unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` via `ItemPurificationPacketOperation.ConcretePacket` | Server Packet DTO / Inventory Add | Partial | Regression Tested in C# planner | Partial Parity | Focused test decodes add type, entry count, object id, template id, slot sentinel, and cloth flag. Full `SmInventoryInfo` blob parity, Java `PARTIAL_WITH_SLOT` remap for explicit slots, and runtime golden bytes remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `ItemPurificationApplicationOperationType.AddTargetItem` plus packet-plan snapshot validation | Storage Add Boundary Projection | Partial | Regression Tested in C# planner | Partial Parity | C# creates only the packet projection from caller-provided snapshots. It does not insert the target item, set location/persistent state, compute cube counts, or call `QuestEngine.onItemGet`. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `ItemPurificationPacketPlanService` | ItemPurification Target Add Fanout Projection | Partial | Regression Tested in C# planner | Partial Parity | Packet plan now covers concrete success, update, delete, and target-add packets when supplied snapshots are available. Target object-id allocation, random bonus reroll, inherited snapshot completeness, cube update, quest hook, and persistence remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `ItemPurificationPacketOperationType.CubeSizeUpdate` | Packet Intent / Cube Size | Partial | Regression Tested in C# planner | Needs Verification | C# preserves add-then-cube ordering but keeps cube payload metadata-only because Java computes cube fields from live player storage/expansion state at send time. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePacketPlan_AttachesConcreteTargetAddPacketWhenRuntimeItemInputProvided` | Regression | Java `ItemPurificationService.upgradeItem`, `Storage.add`, `ItemPacketService.sendStorageUpdatePacket`, and `SM_INVENTORY_ADD_ITEM` source review | Validates target-add operation carries concrete `SmInventoryAddItem`, serializes add type `0x19`, one entry, target object/template ids, first-available slot sentinel, and leaves trailing cube-size metadata-only. | Deterministic C# payload regression grounded in Java add packet fields and ordering. | No Java runtime golden file; full item-info blob and cube-size payload remain unmodeled. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Concrete target-add packets rely on caller-provided post-creation snapshots; live object-id allocation and `Storage.add` do not yet produce or send those snapshots.
- `ItemPurificationInheritanceService` currently creates target snapshot data without a live storage slot assignment; callers must provide Java first-available slot sentinel `-1`/`0xffff` for add-packet parity until the live storage bridge owns slot semantics.
- `SmInventoryAddItem` payload parity depends on complete `SmInventoryInfo` blob coverage and complete inherited target item snapshots, including enchant, sockets, fusion, godstone, tune/random bonus, tempering, soulbound/amplified/buff skill, creator/color/expiration/charge/idian data.
- Java `PARTIAL_WITH_SLOT` remap for explicit slots is documented but not modeled in this bridge because purification target adds should use first-available slot before live storage placement.
- Cube-size and AP packet operations remain metadata-only.
- Live inventory mutation, target add persistent-state changes, quest item-get hooks, AP/rank side effects, dirty-state persistence, `ItemStoneListDAO.save`, Kinah parity decision, packet byte order, and rollback/error behavior remain unimplemented.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 narrow ItemPurification target-add packet bridge slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification packet emission, cube/AP concrete fanout, live object-id allocation/storage mutation, repository dirty-state persistence, and AP/rank side effects
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a narrow live-send adapter boundary for `ItemPurificationPacketPlanService` that can emit only the concrete packets already present in the plan while explicitly skipping metadata-only operations, or choose ItemCharge AP spend hardening if live-send wiring risks crossing into mutation/persistence too soon.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Analyze live-send adapter boundary | read-only `GameServerConnection`, packet plan service/tests | Medium | Confirm where a concrete-packet-only emitter can live without mutating inventory/AP state. |
| B | ItemCharge AP hardening fallback | ItemCharge service/test files | Medium | Independent AP caller work if live-send adapter crosses too many boundaries. |
| C | Cube update prerequisites audit | read-only Java/C# cube update and storage models | Medium | Map exact inputs needed before `SM_CUBE_UPDATE` can become concrete. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Inspect concrete-packet live-send boundary for packet plan | read-only C# connection/services/tests | edits, docs | Safe adapter recommendation or blocker report. |
| Agent B | Inspect cube update prerequisite data | read-only Java/C# cube/storage files | edits, docs | Input map for future cube bridge. |
| Orchestrator | Implement only if adapter stays concrete-packet-only | likely packet plan service/tests or connection tests, docs | live inventory/AP mutation, object-id allocation, repository persistence | Code, tests, docs, commit. |

## Do Not Parallelize

- Live `GameServerConnection.cs` handler edits with storage mutation or AP mutation work.
- Concrete cube/AP packet emission with object-id allocation/factory work.
- Kinah behavior changes without explicit parity decision.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, add the concrete-packet live-send adapter only if it does not mutate inventory/AP state, or choose another AP/item caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
