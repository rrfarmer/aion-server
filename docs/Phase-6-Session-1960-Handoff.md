# Phase 6 Session 1960 Handoff - Repurchase Packet Intent Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1960
Status: Completed

## What Changed

- Added disabled packet-intent diagnostics to `RepurchaseOutcomePlan`.
- Successful disabled repurchase outcomes now record packet intents for:
  - `DEC_KINAH_BUY` Kinah update from `Storage.tryDecreaseKinah`.
  - `ITEM_COLLECT` inventory add from `ItemService.addItem(player, repurchaseItem)` when a new cube item is added.
  - `INC_ITEM_COLLECT` inventory update from `ItemService.addItem(player, repurchaseItem)` when a stack is merged.
  - `STR_MSG_DICE_INVEN_ERROR` for inventory-full repurchase blocking.
- Added C# packet mask constants for Java `ItemAddType.REPURCHASE` and `ItemUpdateType.INC_ITEM_REPURCHASE`.
- Documented through tests/docs that Java `RepurchaseService.repurchaseFromShop` uses `ItemService.addItem(player, repurchaseItem)`, which uses the default collect add/update masks rather than the repurchase masks.
- This unit does not implement live packet dispatch, inventory/Kinah mutation, singleton repurchase map mutation, persistence changes, transaction behavior, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~SmInventory" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# repurchase/inventory slice passed with 84 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5008 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Known Gaps

- `RepurchasePacketIntentPlan` remains disabled and informational only.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java storage/item packet side effects inside `ItemPacketService.sendItemPacket`, `sendStorageUpdatePacket`, cube-size updates, warehouse branches, quest callbacks, and pet-feed unusual storage capture remain unmodeled.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Live BUY_AGAIN socket dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full clean Maven validation can be rerun if the prior login-server compile observation needs root-cause proof.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryAddItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemSideEffectOutcomePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1960-Completion.md`
- `docs/Phase-6-Session-1960-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.services.item.ItemService`
- `com.aionemu.gameserver.services.RepurchaseService`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem`
- `Aion.GameServer.Services.RepurchasePlanService`
- `Aion.GameServer.Services.RepurchaseOutcomePlanService`
- `Aion.GameServer.Services.RepurchasePacketIntentPlan`
- `Aion.GameServer.Tests.RepurchasePlanServiceTests`
- `Aion.GameServer.Tests.CmBuyItemSideEffectOutcomePlanServiceTests`

## Parity Table Updates

- Added Session 1960 rows to `PHASE-6-PROGRESS.md` for:
  - `ItemPacketService` repurchase-relevant update/add masks
  - `Storage.decreaseItemCount/add` packet calls
  - `RepurchaseService.repurchaseFromShop` packet/audit outcomes

## Next Recommended Unit of Work

- Next sequential task: inspect Java cube-size update emissions after repurchase item add/delete/update paths and add disabled cube-size packet intent diagnostics where Java `ItemPacketService` would send `SM_CUBE_UPDATE`, without enabling live dispatch.

Safe alternative candidates:

- Add a focused Java/C# diagnostic for BUY_AGAIN missing-template behavior before the packet can be composed.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run a clean Maven validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase work, inspect `ItemPacketService.sendStorageUpdatePacket`, `sendItemDeletePacket`, and Java `SM_CUBE_UPDATE.cubeSize`. Keep any cube-size packet-intent work disabled until live dispatch and inventory mutation boundaries are explicitly scoped and objectively validated.
- Avoid claiming Java `HashSet` iteration, storage packet, or live mutation parity; UOW-1960 only records disabled packet intents from source-reviewed Java paths.
