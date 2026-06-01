# Phase 6 Session 1962 Handoff - BUY_AGAIN Missing-Template Diagnostic

Date: 2026-06-01
Unit of Work: UOW-1962
Status: Completed

## What Changed

- Tightened disabled BUY_AGAIN repurchase packet diagnostics.
- `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` now lets an explicit `RepurchasePacketSnapshotPlan` win over the fallback packet helper.
- When static item templates are available and a supplied repurchase item lacks a template, the final dialog descriptor carries:
  - `RepurchasePacketSnapshotPlanStatus.BlockedMissingTemplate`
  - the missing item template ID
  - a null `SmRepurchase` packet
- This prevents the non-live fallback path from silently creating a best-effort empty/skipped-item packet that would hide the diagnostic.
- This unit does not implement live `SM_REPURCHASE` dispatch, Java singleton map queries, live packet serialization attempts, inventory/Kinah mutation, persistence, transaction behavior, encrypted frame capture, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|FullyQualifiedName~NpcDialogControllerDispatchPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest,CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# BUY_AGAIN/repurchase snapshot slice passed with 68 tests.
- Focused Java repurchase packet/parser tests passed with 11 test methods.
- Broad C# game-server suite passed with 5009 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 23 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Known Gaps

- BUY_AGAIN repurchase packet planning remains disabled and informational only.
- C# still has no live `RepurchaseService` singleton map equivalent.
- Java normally has template-backed `Item` instances; this unit does not prove exact Java runtime behavior for malformed missing-template item facts.
- Java `HashSet` bucket iteration order and returned set mutability are not emulated.
- Live BUY_AGAIN socket dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full clean Maven validation can be rerun if the prior login-server compile observation needs root-cause proof.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1962-Completion.md`
- `docs/Phase-6-Session-1962-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.DialogService`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_REPURCHASE`
- `com.aionemu.gameserver.services.RepurchaseService`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.RepurchasePacketSnapshotPlanService`
- `Aion.GameServer.Services.NpcDialogServiceSelectPlanService`
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`

## Parity Table Updates

- Added Session 1962 rows to `PHASE-6-PROGRESS.md` for:
  - `DialogService.onDialogSelect` BUY_AGAIN
  - `SM_REPURCHASE(Player, int)` write path
  - `RepurchaseService.getRepurchaseItems` state-source boundary

## Next Recommended Unit of Work

- Next sequential task: inspect `PetService.activateAutoSell` plus `SM_PET(PetAction.AUTOSELL, activate)` and add a disabled activation planner that records the Java runtime state/packet intent without enabling live pet merchant autosell.

Safe alternative candidates:

- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Inspect Java delete-path cube-size sends for another non-repurchase inventory diagnostic where `sendItemDeletePacket` is already represented by a C# planner.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Run a clean Maven validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing the recommended pet work, inspect Java `PetService.activateAutoSell`, Java `SM_PET` autosell constructor/write branch, C# pet merchant sell diagnostics, and any existing `SmPet` packet tests.
- Avoid claiming live BUY_AGAIN parity from UOW-1962; this unit only preserves non-live missing-template diagnostics through the dialog descriptor.
