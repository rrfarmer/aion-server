# Phase 6 Session 1951 Handoff - SM_REPURCHASE Snapshot Adapter

Date: 2026-06-01
Unit of Work: UOW-1951
Status: Completed

## What Changed

- Added `RepurchasePacketSnapshotPlanService`, a disabled non-live adapter for Java `SM_REPURCHASE(Player, npcId)` snapshot composition.
- The adapter consumes supplied `RepurchaseSourceItem` facts, resolves `ItemTemplateSummary` values, and composes an existing `SmRepurchase` packet without querying singleton state or sending anything.
- Missing item templates block the snapshot plan with explicit missing template IDs.
- BUY_AGAIN dialog descriptors can now carry both the snapshot adapter plan and its composed `SmRepurchase` packet.
- This unit does not enable live singleton lookup, live dialog dispatch, encrypted frame validation, item persistence, or real-client validation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~RepurchasePacketSnapshotPlanServiceTests|FullyQualifiedName~NpcDialogServiceSelectPlanServiceTests|FullyQualifiedName~SmRepurchaseTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_REPURCHASE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused C# slice passed with 26 tests.
- Focused Java `SM_REPURCHASE_GoldenTest` passed with 3 test methods.
- Java/Maven reactor test run passed with 1 commons test and 22 game-server tests.
- Broad C# game-server suite passed with 4994 tests.

## Known Gaps

- The adapter uses supplied facts and does not query live singleton repurchase state.
- Java `RepurchaseService.addRepurchaseItems` stores a `HashSet`; live iteration order for the packet snapshot remains unverified.
- Live `DialogService` BUY_AGAIN dispatch, `PacketSendUtility.sendPacket`, live `CM_BUY_ITEM` repurchase execution, inventory/Kinah mutation, repository persistence, transaction behavior, encrypted frame capture, and real-client validation remain pending.
- Full `ItemInfoBlob` parity for advanced item state remains partial.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/RepurchasePacketSnapshotPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RepurchasePacketSnapshotPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1951-Completion.md`
- `docs/Phase-6-Session-1951-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.serverpackets.SM_REPURCHASE`
- `com.aionemu.gameserver.services.RepurchaseService`
- `com.aionemu.gameserver.services.DialogService`

## C# Artifacts Touched

- `Aion.GameServer.Services.RepurchasePacketSnapshotPlanService`
- `Aion.GameServer.Services.NpcDialogServiceSelectPlanService`
- `Aion.GameServer.Network.Aion.ServerPackets.SmRepurchase`
- `Aion.GameServer.Tests.RepurchasePacketSnapshotPlanServiceTests`
- `Aion.GameServer.Tests.NpcDialogServiceSelectPlanServiceTests`

## Next Recommended Unit of Work

- Next sequential task: inspect whether Java live `RepurchaseService` snapshot iteration order can be captured safely with a focused Java runtime test, then either document/order-proof the behavior or keep the C# adapter explicitly supplied-order only.

Safe alternative candidates:

- Add another narrow Java golden item-info vector only if the fixture remains simple, such as equipped-slot nonzero or one basic manastone socket.
- Inspect `PetService.activateAutoSell` and `SM_PET(AUTOSELL, activate)` runtime state wiring as a disabled activation planner.
- Harden private-store diagnostics for blocked/race/offline/cube-full cases without enabling live mutation.
- Inspect whether `CM_BUY_ITEM` amount signedness can be safely captured from Java `readUH()` versus C# unsigned reads.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- If investigating iteration order, inspect Java `RepurchaseService.addRepurchaseItems`, `SM_REPURCHASE.writeImpl`, and `HashSet` construction behavior; avoid assuming stable order from source review alone.
- If iteration-order proof gets brittle, switch to another narrow Java golden packet/vector or a disabled planner candidate.
