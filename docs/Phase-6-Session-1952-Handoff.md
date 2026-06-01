# Phase 6 Session 1952 Handoff - SM_REPURCHASE Snapshot Ordering Evidence

Date: 2026-06-01
Unit of Work: UOW-1952
Status: Completed

## What Changed

- Added Java runtime coverage for `SM_REPURCHASE(Player, npcId)` item row ordering.
- The Java test proves packet item rows are serialized in the same order as the current `RepurchaseService.getRepurchaseItems(playerId)` set iteration.
- Added C# coverage proving `RepurchasePacketSnapshotPlanService` preserves supplied snapshot order.
- This unit does not implement Java `HashSet` ordering in C# and does not enable live singleton state, socket sends, persistence, or real-client validation.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_REPURCHASE_GoldenTest` passed with 4 test methods.
- Focused C# repurchase snapshot/packet slice passed with 8 tests.
- Java/Maven reactor test run passed with 1 commons test and 23 game-server tests.
- Broad C# game-server suite passed with 4995 tests.

## Known Gaps

- Java packet ordering follows service set iteration, but that is not the same as insertion order.
- C# adapter remains supplied-order only and does not model Java live `HashSet` iteration or singleton map replacement.
- Live `DialogService` BUY_AGAIN dispatch, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_REPURCHASE_GoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePacketSnapshotPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1952-Completion.md`
- `docs/Phase-6-Session-1952-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.serverpackets.SM_REPURCHASE`
- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.model.gameobjects.AionObject`

## C# Artifacts Touched

- `Aion.GameServer.Services.RepurchasePacketSnapshotPlanService`
- `Aion.GameServer.Network.Aion.ServerPackets.SmRepurchase`
- `Aion.GameServer.Tests.RepurchasePacketSnapshotPlanServiceTests`

## Next Recommended Unit of Work

- Next sequential task: inspect whether a small disabled live-state adapter can represent Java `RepurchaseService` map replacement/remove lifecycle (`addRepurchaseItems`, `removeRepurchaseItems`, `getRepurchaseItems`) without enabling socket dispatch or mutation.

Safe alternative candidates:

- Add another narrow Java golden item-info vector only if the fixture remains simple, such as equipped-slot nonzero or one basic manastone socket.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If continuing repurchase state work, inspect Java `RepurchaseService` singleton map lifecycle and the existing C# `RepurchaseDiagnosticSnapshotPlanService` / `RepurchasePacketSnapshotPlanService`.
- Avoid claiming insertion-order parity for repurchase packets; the evidence only proves service-set iteration is what `SM_REPURCHASE` writes.
