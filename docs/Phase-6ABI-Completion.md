# Phase 6ABI Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1225
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, teleport side-effect metadata, live-adapter readiness docs, concrete failure system-message helpers, non-live handler composition, runtime owner/control/fanout bridges, metadata-only action `1` scheduling, callback-side cooldown/action `3` fanout for supplied Kinah-success metadata, a scheduled Kinah live-boundary audit, packet-level `DEC_KINAH_FLY` readiness, a Kinah mutation owner design audit, and a non-live scheduled Kinah mutation planner. Full `GameServerConnection` dispatch, live Kinah mutation, inventory persistence, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1225 added `BindPointTeleportScheduledKinahMutationPlanService`, a non-live planner for the future scheduled `tryDecreaseKinah(price, DEC_KINAH_FLY)` callback boundary. It models missing/insufficient Kinah, exact payment to zero, positive decrement, non-positive no-mutation success, unrelated inventory preservation, and `SmInventoryUpdateItem.DecreaseKinahFly` packet intent metadata without mutating live player inventory.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahMutationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledKinahMutationPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KinahMutationOwner-Design.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveBoundary-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ABI-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportScheduledKinahMutationPlanServiceTests" --nologo` passed 7 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 99 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1225

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahMutationPlanService.CreatePlan` | Storage / Mutation Planner | Partial | Unit Tested | Needs Verification | Non-live planner models missing/insufficient, exact-to-zero, positive decrement, and non-positive no-mutation success. It does not persist, send packets, or lock live inventory. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService` | Storage / Count Mutation Planner | Partial | Unit Tested | Needs Verification | C# records updated Kinah metadata and keeps zero-count Kinah item. Java runtime storage comparison was not run. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` | `SmInventoryUpdateItem.DecreaseKinahFly` carried by mutation plan | Enum / Packet Mask | Complete | Unit Tested | Needs Verification | Planner carries packet-intent update type `0x4B`; no live packet send. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | future send of `SmInventoryUpdateItem` from mutation plan metadata | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Packet mask can be serialized, but this planner only carries intent metadata. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future composition of mutation plan into callback metadata/execution bridge | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Mutation facts are not yet composed into `BindPointTeleportScheduledCallbackPlanService` or runtime callback execution. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_MissingKinahSendsFeeAndDoesNotPrepareMutation` | Missing Kinah records fee failure and no packet intent. | Source-derived only. |
| `CreatePlan_InsufficientKinahSendsFeeAndPreservesSnapshot` | Insufficient Kinah records failure and preserves count. | Source-derived only. |
| `CreatePlan_ExactKinahSucceedsAndKeepsZeroCountKinahItem` | Exact payment creates zero-count Kinah update intent. | Source-derived only. |
| `CreatePlan_PositiveDecrementPreparesKinahUpdateAndPacketIntent` | Positive payment records updated Kinah and packet mask `0x4B`. | Source-derived only. |
| `CreatePlan_NonPositivePriceContinuesWithoutMutationLikeJava` | Non-positive amount succeeds without mutation/packet intent. | Source-derived only. |
| `CreatePlan_PreservesUnrelatedInventoryAndCopiesKinahMetadata` | Unrelated items are preserved, Kinah metadata is copied, and player inventory is not mutated. | C# safety guard plus source-derived behavior. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live mutation planner plus 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 3 grouped categories: live owner/lock, persistence/send policy, and callback composition
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Planner is non-live and is not yet composed into scheduled callback metadata/execution.
- Live persistence, packet send ordering, and rollback policy remain blocked.
- No shared C# inventory owner/lock exists yet.
- Java runtime storage or packet comparison was not executed.
- Runtime fanout still uses C# visible-player registry approximation instead of Java persistent known-list membership.
- Final movement remains metadata-only.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `BindPointTeleportScheduledKinahMutationPlanService` into `BindPointTeleportScheduledCallbackPlanService` metadata.
- Scope:
  - Let callback plans carry updated Kinah item and `DecreaseKinahFly` packet-intent facts.
  - Preserve existing callback ordering: Kinah failure stops before cooldown/fanout/movement; success continues to cooldown/fanout/movement metadata.
  - Keep all behavior non-live: no repository writes, no packet sends, no dispatch, no movement.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Compose mutation planner into scheduled callback metadata | `BindPointTeleportScheduledCallbackPlanService.cs`, tests | Medium | Best next step; avoid runtime execution and persistence. |
| B | Known-list parity audit for C# visible registry approximation | new doc only | Low/Medium | Helps unblock future live fanout claims. |
| C | Static hotspot fact assembly audit | new doc or isolated adapter/test | Medium | Avoid shared static-data loaders unless owned exclusively. |
| D | Persistence-boundary repository contract plan | new doc or repository interface draft | Medium | Do after mutation metadata composition. |

### Do Not Parallelize

- Live Kinah mutation and `GameServerConnection` dispatch.
- Kinah mutation and final movement.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahMutationPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledKinahMutationPlanServiceTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledCallbackPlanServiceTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- Latest completed commits:
  - `c4b8e63a1 [Phase 6][UOW-1224] Add bind point teleport Kinah mutation owner design`
  - next commit should be `[Phase 6][UOW-1225] Add bind point teleport scheduled Kinah mutation plan`

Keep live bind-point behavior disabled until live inventory mutation/persistence, final movement packet ordering, and persistent known-list behavior each have focused parity slices.
