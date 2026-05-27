# Phase 6AJV Completion - Normal Decompose Success Ordering

Date: 2026-05-27
Unit of Work: UOW-1446
Status: Complete after validation.

## Scope

Align scheduled normal decompose completion ordering with Java `DecomposeAction.run` after UOW-1445 added reward add-plus-cube fanout. Java normal decompose consumes the source, sends success, adds rewards, then broadcasts the final success animation. Java selectable decompose uses `CM_SELECT_DECOMPOSABLE` and intentionally keeps a different message/source/show/reward order.

## Completed Work

- Reviewed Java `DecomposeAction.run`: `postValidate` consumes the source through `Storage.decreaseByObjectId`, then Java sends `STR_DECOMPOSE_ITEM_SUCCEED`, calls `ItemService.addItem` for rewards, and finally broadcasts success `SM_ITEM_USAGE_ANIMATION`.
- Reviewed Java `CM_SELECT_DECOMPOSABLE`: selectable decompose sends `STR_UNCOMPRESS_COMPRESSED_ITEM_SUCCEEDED`, consumes the source, sends `SM_SECONDARY_SHOW_DECOMPOSABLE`, then calls `ItemService.addItem`.
- Updated `CompleteDecomposeUseItemAsync` so normal decompose sends source mutation packets before the success message, reward packets before final success animation, and keeps selectable decompose unchanged.
- Updated normal decompose, source-delete, restricted reward, reward-merge, and encrypted normal decompose tests to assert the Java-derived order.
- Ran a read-only parallel toy-pet/kisk audit and closed the subagent. That audit found a separate actionable mismatch for the next unit: Java toy-pet spawn broadcasts success before source consume/world spawn, while C# currently persists/world-adds before success on normal success paths.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_Decompose|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleSelectDecomposableAsync_|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.CaptureSelectableDecomposeObservationJson|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.CompareSelectableDecomposeJavaArtifacts|FullyQualifiedName~DecomposeServiceTests"`.
- Result: passed 30 tests.

## Migration Parity Table - UOW-1446

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.DecomposeAction` | `GameServerConnection.HandleDecomposeUseItemAsync` / `CompleteDecomposeUseItemAsync` | Item Action / Connection Handler | Partial | Regression Tested | Partial Parity | Scheduled normal decompose packet order now follows reviewed Java source consume, success message, reward fanout, final success animation. Persistence remains transaction-first in C# before visible packets. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `GameServerConnection.ApplySourceItemMutationAsync` | Storage Source Mutation | Partial | Regression Tested | Partial Parity | Normal decompose source update/delete packets now precede success message. Delete branch still sends cube update from C# projection; Java byte shape remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `GameServerConnection.SendDecomposeRewardItemsAsync` | Service / Reward Packet Fanout | Partial | Regression Tested | Partial Parity | Reward add/update packets now precede final success animation for normal decompose. Added rewards still include UOW-1445 trailing cube update; merges remain update-only. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.DecomposeItemSucceed` | Packet | Partial | Regression Tested | Partial Parity | Success message now appears after source mutation for normal decompose. Selectable success message ordering intentionally remains Java `CM_SELECT_DECOMPOSABLE` shaped. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet | Partial | Regression Tested | Partial Parity | Final normal decompose success animation now follows reward fanout. Start/failure/cancel paths were not changed. Runtime animation broadcast fanout remains unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `GameServerConnection.HandleSelectDecomposableAsync` | Client Packet Handler | Partial | Regression Tested | Needs Verification | Reviewed as a dependency and left unchanged because Java sends system message before source consume on selectable path. Java runtime artifacts for selectable remain unavailable. |
| `com.aionemu.gameserver.model.templates.item.actions.ToyPetSpawnAction` | `GameServerConnection.HandleToyPetSpawnUseItemAsync` / `CompleteToyPetSpawnUseItemAsync` | Item Action / Kisk Spawn | Partial | Manual Only | Needs Verification | Read-only sidecar discovered Java broadcasts success before source consume/world spawn; C# currently world-adds/persists before success on normal success paths and suppresses success on persistence failure. Not changed in this unit. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine` / `com.aionemu.gameserver.world.World` | `Aion.GameServer.World.GameWorld` / `PlayerKiskRuntimeState` | World Spawn / Runtime Registry | Partial | Manual Only | Needs Verification | Newly discovered toy-pet dependency. C# uses lightweight `WorldNpc` plus runtime state rather than Java `Kisk` object/controller construction. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_DecomposeCompletesAndAddsReward` | Regression / connection packet order | `DecomposeAction.run`, `Storage.decreaseByObjectId`, `ItemService.addItem` | Source update precedes success message; reward add/cube precede final success animation. | Deterministic C# packet assertions against reviewed Java order. | No Java runtime bytes. |
| `HandleUseItemAsync_DecomposeDeletesLastSourceAndAddsReward` | Regression / connection packet order | Same Java delayed action path | Source delete/cube precede success message; reward add/cube precede final success animation. | Deterministic C# packet assertions against reviewed Java order. | Java delete/cube bytes remain unverified. |
| `HandleUseItemAsync_DecomposeAddsRestrictedRewardWithCleanupSealFlag` | Regression / connection packet order and blob metadata | Same Java action/storage path | Restricted source update, success message, restricted reward add/cube, final success animation. | C# packet parsing against reviewed Java order. | Cleanup/seal blob has no runtime Java byte comparison. |
| `HandleUseItemAsync_DecomposeMergesRestrictedRewardWithCleanupSealFlag` | Regression / connection packet order | `Storage.increaseItemCount` | Merge reward update precedes final success animation and remains update-only. | Deterministic C# packet assertions from reviewed Java merge branch. | No runtime bytes. |
| `RunAsync_EncryptedUseItemFrameSchedulesAndCompletesDecompose` / `RunAsync_EncryptedUseItemFrameDeletesLastSourceAndAddsReward` | Encrypted frame regression | `CM_USE_ITEM` plus normal `DecomposeAction` completion | Encrypted normal decompose follows the same post-key packet order for source-update and source-delete cases. | C# encrypted frame dispatch assertions. | No Java runtime encrypted frames. |
| `HandleSelectDecomposableAsync_*`, `RunAsync_EncryptedSelectDecomposableFrameDispatchesSelection`, selectable capture/compare tests, `DecomposeServiceTests` | Existing regression slice | `CM_SELECT_DECOMPOSABLE` and decompose planning | Selectable path remained stable while normal path changed. | 30-test focused suite passed. | Java selectable artifact files remain unavailable. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# normal decompose still persists transaction-first before sending visible packets, unlike Java's immediate runtime mutation; rollback/failure behavior may differ under persistence failure.
- Selectable decompose was source-reviewed but still lacks generated Java artifact comparison.
- Toy-pet/kisk spawn ordering has a confirmed actionable mismatch and should be the next narrow unit.
- Exact `SM_ITEM_USAGE_ANIMATION`, inventory packet, and cube snapshot bytes remain C# deterministic but not Java-runtime compared.
- Scheduled real-client item-use ordering remains unvalidated across decompose, assembly, extraction, XP extraction, composition, AP extraction, and toy-pet spawn.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 normal decompose packet-order branch changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, selectable runtime artifacts, toy-pet/kisk ordering fix, real-client scheduled ordering
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: address toy-pet/kisk spawn ordering.
- Why: The read-only sidecar found Java broadcasts the toy-pet success end animation before source consume/world spawn, while C# currently performs world insertion and persistence first on normal success paths.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
  - `game-server/src/com/aionemu/gameserver/utils/VisibleObjectSpawner.java`
  - `game-server/src/com/aionemu/gameserver/spawnengine/SpawnEngine.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/services/kisk/KiskService.java`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKiskSpawnService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFlightZoneFanoutTests.cs`
  - docs/progress/handoff docs

## Suggested Acceptance Criteria

- Add or update a focused test around `CompleteToyPetSpawnUseItemAsync` persistence-failure and success packet order.
- Decide whether to reorder C# success animation to match Java `ToyPetSpawnAction`, or document an explicit intentional rollback difference.
- Keep no spawn/registry/visibility behavior on failed consume/persist explicit.
- Preserve Java breadcrumbs for `ToyPetSpawnAction`, `VisibleObjectSpawner.spawnKisk`, `SpawnEngine.bringIntoWorld`, `World.spawn`, and `KiskService.regKisk`.
- Do not claim verified parity without Java runtime artifact bytes or deterministic source-backed evidence.

## Follow-Up Sequential Tasks

- Generate or compare Java runtime artifacts for normal/selectable decompose when Java 25/Maven tooling is available.
- Real-client validate scheduled item-use ordering for decompose, assembly, XP extraction, composition, extraction, AP extraction, and toy-pet spawn once readiness work begins.
- Continue broader extraction RNG/drop and enchant-service parity after packet ordering gaps are closed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Toy-pet/kisk ordering implementation | `GameServerConnection.cs`, `GameServerConnectionFlightZoneFanoutTests.cs` | Medium | Keep one owner because success ordering and rollback behavior share handler/tests. |
| B | Extraction RNG/drop audit | Java/C# enchant service sources | Medium | Read-only sidecar only; do not mix with toy-pet edits. |
| C | Java artifact tooling note | docs/source read-only | Low | Useful if tooling paths changed, but do not claim runtime verification unless artifacts are generated. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` packet-order changes.
- Multiple edits to `GameServerConnectionFlightZoneFanoutTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1446] Align decompose success order`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/DecomposeAction.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SELECT_DECOMPOSABLE.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
- C# files changed in UOW-1446:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJV-Completion.md`
