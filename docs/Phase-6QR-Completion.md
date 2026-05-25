# Phase 6QR Completion Handoff - ItemPurification Mutation Snapshot Preview

Date: May 25, 2026
Unit of Work: UOW-948
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-948] Preview item purification mutation snapshots`)

## Status

Phase 6 is still in progress. This unit adds a non-persistent ItemPurification mutation snapshot preview that turns a ready `ItemPurificationApplicationPlan` into post-mutation inventory snapshots and cube snapshot candidates without touching live player inventory, AP, persistence, or network send state.

The handler remains non-persistent and mostly non-mutating. It still does not invoke this preview from the live handler path, mutate inventory/AP, persist changes, call the send bridge, synthesize AP rank packets, or execute full Java `ItemFactory` / `ItemSocketService` side effects.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationMutationSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationMutationSnapshotServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QR-Completion.md`

## What Changed

- Added `ItemPurificationMutationSnapshotService.CreatePreview`.
- The preview accepts current inventory snapshots, a ready application plan, and caller-supplied cube expansion counters.
- It copies inventory DTOs before applying planned operations, so source `Player.InventoryItems` and AP remain unchanged.
- It applies Java-order ItemPurification operation previews:
  - material/base count updates
  - material/base deletes
  - AP spend as metadata-only
  - kinah preservation as the existing Java-quirk no-op
  - target item add from the already-planned target item
- It emits cube snapshots at packet operation indexes that match `ItemPurificationPacketInputSnapshotService` / `ItemPurificationPacketPlanService`.
- It reports missing or mismatched live inventory objects instead of assuming parity.
- Added tests proving generated snapshots feed the existing handler packet bridge into concrete packet objects.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Mutation snapshot preview | `ItemPurificationService.decreaseMaterials`, `upgradeItem`, `Storage` | new service/test files plus ItemPurification fixtures | Service / Tests | No | Medium | Shared ItemPurification fixtures and packet-index expectations require one writer. |
| B | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate from ItemPurification files; deferred. |
| C | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |
| D | Packet byte comparison gap audit | packet DTO/runtime observer work | packet tests/golden tooling | Read-only / Analysis | Yes | Low | Useful only after Java artifact format/tooling is available. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-948 implementation, tests, docs, commit | new mutation snapshot service/test files, progress/handoff docs | unrelated files | Code, tests, parity docs, commit |

No write sub-agents were spawned because the unit touched one coherent ItemPurification fixture boundary.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationMutationSnapshotServiceTests|ItemPurificationPacketInputSnapshotServiceTests|GameServerConnectionItemPurificationTests|ItemPurificationPacketPlanServiceTests"
```

Result: passed, 32 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1636 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationMutationSnapshotService` | Service / Mutation Preview | Partial | Regression Tested in C# | Partial Parity | C# now previews material/base update/delete effects on copied inventory snapshots and preserves Java operation order. It does not mutate live inventory, persist changes, execute Java `Storage`, or perform rollback/runtime side effects. Missing live inventory objects are explicit diagnostics. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationMutationSnapshotService` plus `ItemPurificationInheritanceService` | Service / Target Add Preview | Partial | Regression Tested in C# | Partial Parity | Preview adds the already-planned target item snapshot and produces add-time cube metadata. Full Java `ItemFactory`, `ItemSocketService`, inventory add persistence, quest notification, and live storage add remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Storage` | `Aion.GameServer.Services.ItemPurificationMutationSnapshotService` and `ItemPurificationPacketInputSnapshotService` | Storage / Snapshot Adapter | Partial | Regression Tested in C# | Needs Verification | C# models post-storage item list and cube-size snapshots with supplied expansion counters. Java `Storage.decreaseByItemId`, `decreaseByObjectId`, `add`, lock/threading semantics, and exact cube expansion state are source-reviewed only, not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` plus `ItemPurificationHandlerPacketBridgeService` and `ItemPurificationMutationSnapshotService` | Client Handler / Adapter | Partial | Regression Tested in C# | Partial Parity | A ready handler/application plan can now be paired with generated post-mutation snapshots to build concrete packet plans. The live handler still does not invoke mutation preview, mutate, persist, send, or synthesize AP rank packets. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.ItemPurificationApplicationPlanService` and `ItemPurificationMutationSnapshotService` | AP Spend Boundary | Partial | Regression Tested in C# | Needs Verification | AP spend remains metadata-only in preview and bridge. Java rank side effects, AP packet fanout, and persistence are still deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM`, `SM_DELETE_ITEM`, `SM_CUBE_UPDATE`, `SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`, `SmDeleteItem`, `SmCubeUpdate`, `SmInventoryAddItem` | Packet DTOs | Partial | Regression Tested in C# | Needs Verification | Preview-generated snapshots feed concrete C# packet objects in Java-like order. Packet bytes and Java runtime captures remain unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePreview_ProducesPostMutationSnapshotsWithoutMutatingInventory` | Regression | Java `ItemPurificationService.decreaseMaterials` then `upgradeItem` source review | Validates material count update, base delete, target add, cube snapshot indexes/counts, kinah preservation, and no live inventory/AP mutation. | Deterministic C# regression for Java operation order and post-storage snapshot shape. | Does not execute Java runtime, persist, or compare packet bytes. |
| `CreatePreview_FeedsHandlerPacketBridgeConcretePlan` | Regression | Java ItemPurification packet fanout source review | Validates generated snapshots feed the existing handler bridge into concrete success, update, delete, cube, add, and cube packets while AP/Kinah remain metadata. | Deterministic C# bridge regression using generated snapshots rather than hand-authored snapshots. | Does not send packets or compare Java packet bytes. |
| `CreatePreview_ReportsMissingCurrentInventoryItem` | Regression | Java storage decrease behavior source review | Validates missing current material object is reported explicitly while the partial preview remains inspectable. | Deterministic C# diagnostic regression for stale/missing live storage state. | Java's exact partial-mutation failure behavior is not runtime-captured. |
| `CreatePreview_RejectsApplicationPlanThatStillNeedsRuntimeInputs` | Regression | Java target item requires runtime object id/random bonus before add | Validates unready application plans do not produce snapshots. | Deterministic C# guard regression. | Runtime allocation and random selection are covered by prior UOWs but not revalidated against Java runtime. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `HandleItemPurificationAsync` still does not call the mutation preview, mutate inventory/AP, persist changes, call the send bridge, synthesize AP rank packets, or execute full Java `ItemFactory` / `ItemSocketService` side effects.
- The preview copies C# DTOs and shares socket/godstone/idian reference payloads; this is acceptable for non-mutating snapshots but must be revisited before live mutation.
- Cube snapshots require caller-supplied expansion counters; exact Java `Storage` expansion state and lock/threading behavior remain unverified.
- AP rank side effects, quest notifications, persistence transactions, rollback behavior, and packet byte parity remain incomplete.
- Java missing-base null behavior and negative-Kinah deduction quirk remain documented but not runtime-verified.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 ItemPurification mutation snapshot preview
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, live inventory/AP persistence, live handler mutation/send invocation, full socket/godstone/fusion side-effect parity, and packet-byte/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Wire the ready mutation snapshot preview into a non-persistent handler bridge path.

Suggested shape:
- Add or extend a narrow result object that composes:
  - ready `ItemPurificationHandlerPlan`
  - `ItemPurificationMutationSnapshotPlan`
  - `ItemPurificationHandlerPacketBridgeResult`
- Feed `Player.InventoryItems`, the ready application plan, item templates, and storage expansion counters into the preview and bridge.
- Keep the seam non-persistent and non-sending.
- Add a handler-level or service-level regression proving the live handler plan can be converted into a concrete packet plan using generated snapshots, while live inventory/AP remain unchanged.

Do not combine with:
- repository transaction persistence
- live inventory/AP mutation
- AP rank side-effect packets
- quest notifications
- live packet sending from the handler
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-persistent handler bridge integration for generated mutation snapshots | likely ItemPurification bridge/service/test files | Medium | Single writer if it touches shared handler fixtures. |
| B | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| C | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |
| D | Packet byte comparison gap audit | read-only packet tests/golden tooling | Low | Useful if Java artifact format becomes available. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionItemPurificationTests.cs`.
- Multiple agents changing ItemPurification workflow/application/packet services in the same unit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose non-persistent handler bridge integration for generated mutation snapshots or isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
