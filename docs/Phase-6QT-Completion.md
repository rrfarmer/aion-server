# Phase 6QT Completion Handoff - ItemPurification Live Mutation Adapter

Date: May 25, 2026
Unit of Work: UOW-950
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-950] Apply item purification live mutations`)

## Status

Phase 6 is still in progress. This unit adds the first live ItemPurification inventory/AP mutation adapter boundary.

The new service applies a ready `ItemPurificationApplicationPlan` to a `Player` by generating post-mutation snapshots, replacing `Player.InventoryItems`, and spending AP through `AbyssPointsService.AddAp`. It remains non-persistent and non-sending: repository writes, concrete packet dispatch, quest notifications, AP rank side-effect execution, and rollback/transaction behavior remain separate work.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveMutationService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationLiveMutationServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QT-Completion.md`

## What Changed

- Added `ItemPurificationLiveMutationService.Apply`.
- Added `ItemPurificationLiveMutationResult`.
- Added `ItemPurificationLiveMutationStatus`.
- The adapter:
  - rejects missing player/application inputs
  - rejects application plans that still need runtime inputs
  - generates snapshots through `ItemPurificationMutationSnapshotService`
  - stops without mutating inventory/AP if snapshots are not ready
  - replaces `Player.InventoryItems` with post-mutation snapshots when ready
  - spends AP through `AbyssPointsService.AddAp`
  - preserves the documented Java negative-kinah no-op quirk
- Added focused tests for success, generated-snapshot failure, and unready application plans.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Live mutation adapter boundary | `ItemPurificationService.decreaseMaterials`, `upgradeItem`, `AbyssPointsService.addAp`, `Storage` | new live mutation service/test files | Service / Tests | No | Medium | Live inventory/AP mutation should remain one coherent contract; no safe split inside this slice. |
| B | Handler result seam | `CM_ITEM_PURIFICATION`, `ItemPurificationService` | `GameServerConnection.cs`, handler tests | Integration Fix | No | Medium | Large shared handler file; should be exclusive. |
| C | Kinah charge-all partial drift | `ItemChargeService` charge-all Kinah path | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Test Creation | Yes | Medium | Separate from ItemPurification files; deferred. |
| D | Java ItemPurification runtime observer design | ItemPurification runtime packet path | docs only | Documentation / Analysis | Yes | Low | Useful when Java tooling exists; no runtime parity claim possible now. |

## File Ownership Map Used

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | UOW-950 implementation, tests, docs, commit | new live mutation service/test files, progress/handoff docs | unrelated files | Code, tests, parity docs, commit |

No write sub-agents were spawned because the unit adds one coherent mutation adapter boundary.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationLiveMutationServiceTests|ItemPurificationMutationSnapshotServiceTests|GameServerConnectionItemPurificationTests|ItemPurificationPacketInputSnapshotServiceTests|ItemPurificationPacketPlanServiceTests|AbyssPointsServiceTests"
```

Result: passed, 45 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1641 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPurificationService.decreaseMaterials` | `Aion.GameServer.Services.ItemPurificationLiveMutationService` plus `ItemPurificationMutationSnapshotService` | Service / Live Mutation Adapter | Partial | Regression Tested in C# | Partial Parity | C# now applies generated material/base inventory mutations to `Player.InventoryItems` and spends AP after snapshot validation. Java `Storage` persistent states, deleted item queue, quest remove callbacks, packet fanout, transaction behavior, and rollback remain unimplemented/unverified. |
| `com.aionemu.gameserver.services.item.ItemPurificationService.upgradeItem` | `Aion.GameServer.Services.ItemPurificationLiveMutationService` plus `ItemPurificationInheritanceService` | Service / Target Add Mutation | Partial | Regression Tested in C# | Partial Parity | Target item snapshots are now added to live player inventory by the adapter. Full Java `ItemFactory`, `ItemSocketService`, storage add semantics, quest get callbacks, persistence, and live packet sending remain incomplete. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService` via `ItemPurificationLiveMutationService` | AP Spend Boundary | Partial | Regression Tested in C# | Partial Parity | The adapter spends AP using the existing AP service and returns AP plan metadata. Java AP packet fanout, rank-limit item checks, abyss skill updates, Legion/Siege side effects, and persistence remain deferred. |
| `com.aionemu.gameserver.model.gameobjects.player.Storage` | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` mutated by `ItemPurificationLiveMutationService` | Storage / Inventory Mutation | Partial | Regression Tested in C# | Needs Verification | C# replaces immutable inventory snapshots rather than mutating Java `Storage`/`ItemStorage`. Collection ordering, storage locks, persistent state flags, deleted item queue, and exact `Storage.add` capacity behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ITEM_PURIFICATION` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleItemPurificationAsync` plus live mutation adapter seam | Client Handler / Adapter | Partial | Regression Tested in C# | Partial Parity | The live mutation adapter is available for ready handler plans but the handler does not yet invoke it automatically. Live send, persistence, transaction, and runtime error behavior remain incomplete. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.ItemPurificationPacketSendAdapter` and mutation adapter metadata | Packet Send Boundary | Partial | Regression Tested in C# | Needs Verification | Mutation and packet-send seams remain separate. This unit intentionally does not send concrete packets or compare Java packet bytes. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_MutatesInventoryAndSpendsApWithoutKinahMutation` | Regression | Java `ItemPurificationService.decreaseMaterials` / `upgradeItem` and `AbyssPointsService.addAp` source review | Validates material count update, base removal, target add, AP spend, cube snapshot metadata, kinah no-op, and target inherited stats after applying the adapter. | Deterministic C# regression for the first live inventory/AP mutation boundary. | Does not persist, send packets, fire quest notifications, or compare Java runtime output. |
| `Apply_DoesNotMutateWhenGeneratedSnapshotsAreNotReady` | Regression | Java storage decrease failure source review | Validates stale/missing current inventory stops before inventory/AP mutation and reports missing object ids. | Deterministic C# guard regression for stale live state. | Java partial mutation behavior after earlier material decrements is not runtime-captured. |
| `Apply_RejectsUnreadyApplicationWithoutMutation` | Regression | Java target creation requires runtime-ready inputs before storage add | Validates application plans needing target id/random bonus/base-delete verification do not mutate player inventory/AP. | Deterministic C# guard regression. | Runtime allocation/random selection already covered by prior UOWs, not Java runtime-compared here. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `HandleItemPurificationAsync` still does not invoke the live mutation adapter automatically, persist changes, call the send bridge, synthesize/send AP rank packets, fire quest notifications, or execute repository transactions.
- C# replaces `Player.InventoryItems` with copied snapshots rather than mutating Java `Item` objects in `Storage`; persistent state flags, deleted item queues, and storage locking/threading behavior remain unverified.
- Kinah remains intentionally preserved because the Java source calls `decreaseKinah(-necessaryKinah)`, which is a no-op under `Storage.decreaseKinah`'s `amount > 0` guard; runtime verification is still missing.
- AP side-effect metadata is returned but not sent/executed for rank-limit equipment, abyss skill updates, Legion contribution, or Siege callbacks.
- Current snapshot copying shares socket/godstone/idian payload references and must be revisited before persistence writes.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 ItemPurification live mutation adapter boundary
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories, including Java runtime artifact generation, persistence/transaction writes, live handler mutation/send invocation, quest/AP side-effect execution, and packet-byte/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Continue ItemPurification toward live execution by connecting this live mutation adapter to the handler/packet bridge path through an explicit non-persistent result seam or a test-only service composition.

Suggested shape:
- Option A: add a handler result object/method that returns `ItemPurificationHandlerPlan`, `ItemPurificationLiveMutationResult`, and optional concrete packet bridge result for ready plans without sending/persisting.
- Option B: add a service that composes live mutation plus concrete packet send adapter in tests only, while still avoiding repository persistence.
- Keep cube expansion inputs explicit until a real C# storage expansion model is available.

Do not combine with:
- repository transaction persistence
- automatic live packet sending from `HandleItemPurificationAsync`
- AP rank side-effect execution
- quest notifications
- Java runtime byte capture

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Handler result seam for live mutation output | likely `GameServerConnection.cs`, handler tests | Medium | Single writer because handler file is shared and large. |
| B | Test-only live mutation + concrete packet send composition | ItemPurification service/test files | Medium | Keep non-persistent and avoid handler auto-send. |
| C | Kinah charge-all partial-drift regression | `GameServerConnectionInventoryExpansionUseItemTests.cs` | Medium | Separate from ItemPurification files; safe alternative. |
| D | Java ItemPurification runtime observer design | docs only | Low | Do not claim runtime parity until tooling exists. |

## Do Not Parallelize

- Multiple agents editing `GameServerConnection.cs`.
- Multiple agents editing `GameServerConnectionItemPurificationTests.cs`.
- Multiple agents changing ItemPurification workflow/application/packet/mutation services in the same unit.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose a handler result seam, test-only live mutation + send composition, or isolated Kinah charge-all partial-drift regression.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
