# Phase 6 Session 1961 Handoff - Repurchase Cube-Size Packet Intent Diagnostic

Date: 2026-06-01
Unit of Work: UOW-1961
Status: Completed

## What Changed

- Added disabled cube-size packet-intent diagnostics to `RepurchaseOutcomePlan`.
- Successful disabled repurchase outcomes now record:
  - `DEC_KINAH_BUY` Kinah update from `Storage.tryDecreaseKinah`.
  - `ITEM_COLLECT` inventory add from `ItemService.addItem(player, repurchaseItem)` when a new cube item is added.
  - `SM_CUBE_UPDATE` opcode marker after that new cube item add, matching Java `ItemPacketService.sendStorageUpdatePacket`.
  - `INC_ITEM_COLLECT` inventory update from `ItemService.addItem(player, repurchaseItem)` when a stack is merged, without a cube-size update.
  - `STR_MSG_DICE_INVEN_ERROR` for inventory-full repurchase blocking.
- This unit does not implement live packet dispatch, concrete cube-size payload snapshots, inventory/Kinah mutation, singleton repurchase map mutation, persistence changes, transaction behavior, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~SmInventory|FullyQualifiedName~SmCube" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# repurchase/inventory/cube slice passed with 167 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5008 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Known Gaps

- `RepurchasePacketIntentPlan` remains disabled and informational only.
- The cube-size intent records an opcode marker only; it does not construct `SM_CUBE_UPDATE.cubeSize` with post-mutation item counts or expand fields.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java storage/item packet side effects outside this narrow success-add path remain unmodeled, including delete-path cube-size updates, quest callbacks, warehouse branches, pet-feed unusual storage capture, persistent-state flags, and deleted-item queues.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Live BUY_AGAIN socket dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full clean Maven validation can be rerun if the prior login-server compile observation needs root-cause proof.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1961-Completion.md`
- `docs/Phase-6-Session-1961-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.services.RepurchaseService`

## C# Artifacts Touched

- `Aion.GameServer.Services.RepurchasePlanService`
- `Aion.GameServer.Services.RepurchaseOutcomePlanService`
- `Aion.GameServer.Services.RepurchasePacketIntentPlan`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- `Aion.GameServer.Tests.RepurchasePlanServiceTests`

## Parity Table Updates

- Added Session 1961 rows to `PHASE-6-PROGRESS.md` for:
  - `ItemPacketService.sendStorageUpdatePacket`
  - `ItemPacketService.sendItemUpdatePacket`
  - `SM_CUBE_UPDATE.cubeSize`
  - `RepurchaseService.repurchaseFromShop` success packet outcomes

## Next Recommended Unit of Work

- Next sequential task: add a focused Java/C# diagnostic for BUY_AGAIN missing-template behavior before `SM_REPURCHASE` can be composed, keeping the fallback packet path disabled and documenting whether Java would throw or skip from the constructor/template lookup path.

Safe alternative candidates:

- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Inspect Java delete-path cube-size sends for another non-repurchase inventory diagnostic where `sendItemDeletePacket` is already represented by a C# planner.
- Run a clean Maven validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase work, inspect Java `SM_REPURCHASE(Player, npcId)` construction and the C# `RepurchasePacketSnapshotPlanService` missing-template path. Keep the path disabled until live dialog dispatch and singleton map access are explicitly scoped and objectively validated.
- Avoid claiming concrete `SM_CUBE_UPDATE` payload parity from UOW-1961; this unit records only source-reviewed disabled intent metadata.
