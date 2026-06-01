# Phase 6 Session 1958 Handoff - BUY_AGAIN Repurchase Snapshot Diagnostic Threading

Date: 2026-06-01
Unit of Work: UOW-1958
Status: Completed

## What Changed

- Threaded the disabled `RepurchasePacketSnapshotPlan` through the non-live BUY_AGAIN dialog path.
- `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` now creates a snapshot diagnostic for Java `DialogService` BUY_AGAIN -> `SM_REPURCHASE(Player, npcId)` when item templates are available.
- `QuestDialogNpcTargetBranchInputAssemblyPlanService` and `NpcDialogControllerDispatchPlanService` now preserve the snapshot diagnostic into the final `NpcDialogServiceDescriptor`.
- Existing raw `SmRepurchase` packet fallback is preserved when the richer diagnostic cannot produce a packet.
- This unit does not implement live `PacketSendUtility.sendPacket`, singleton repurchase map lookup, Java `HashSet` bucket iteration order, inventory/Kinah mutation, persistence changes, transaction behavior, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~NpcDialogControllerDispatchPlanServiceTests|FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# dialog/repurchase slice passed with 67 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite first timed out at 240 seconds, then passed with 5007 tests when rerun with a longer timeout.
- Full Maven reactor passed commons, chat-server, and game-server tests, then failed compiling login-server at `login-server/src/com/aionemu/loginserver/service/PlayerTransferService.java:42` with `illegal start of expression`.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.

## Known Gaps

- BUY_AGAIN `SM_REPURCHASE` remains disabled and informational only.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Live BUY_AGAIN socket dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full Maven reactor validation is blocked by the login-server `PlayerTransferService.java:42` compile error.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogControllerDispatchPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1958-Completion.md`
- `docs/Phase-6-Session-1958-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.DialogService`
- `com.aionemu.gameserver.model.DialogAction`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_REPURCHASE`
- `com.aionemu.gameserver.services.RepurchaseService`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.QuestDialogNpcTargetBranchInputAssemblyPlanService`
- `Aion.GameServer.Services.NpcDialogControllerDispatchPlanService`
- `Aion.GameServer.Services.NpcDialogServiceSelectPlanService`
- `Aion.GameServer.Services.RepurchasePacketSnapshotPlanService`
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`

## Parity Table Updates

- Added Session 1958 rows to `PHASE-6-PROGRESS.md` for:
  - `DialogService.onDialogSelect` BUY_AGAIN diagnostic integration
  - `SM_REPURCHASE(Player, int)` packet snapshot diagnostic propagation
  - `RepurchaseService.getRepurchaseItems` supplied-state boundary for BUY_AGAIN

## Next Recommended Unit of Work

- Next sequential task: inspect Java `RepurchaseService.repurchaseFromShop` success mutation ordering and C# `RepurchaseOutcomePlanService`/`CmBuyItemSideEffectOutcomePlanService` to add a disabled success-bundle diagnostic for Kinah/item/state mutation ordering, without enabling live `CM_BUY_ITEM` action 2 execution.

Safe alternative candidates:

- Add a focused Java/C# diagnostic for BUY_AGAIN missing-template behavior before the packet can be composed.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Fix or isolate the login-server `PlayerTransferService.java:42` compile error so full Maven reactor validation can run again.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase work, inspect Java `RepurchaseService.repurchaseFromShop` and the C# disabled repurchase outcome path. Keep any mutation-order work disabled/diagnostic unless live state, persistence, packet sends, and transaction boundaries are explicitly scoped and objectively validated.
- Avoid claiming Java `HashSet` iteration parity; UOW-1958 only propagates supplied BUY_AGAIN snapshot facts.
