# Phase 6 Session 1968 Handoff - Craft Delete Cube-Size Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1968
Status: Completed

## What Changed

- Added `CreateStartInventoryPacketPlan_DeleteCubeSizeSnapshotsExcludeKinah` to C# `CraftServiceTests`.
- The covered packet plan starts with Kinah plus three non-Kinah cube rows and deletes two non-Kinah stacks.
- C# evidence: the disabled craft packet plan emits:
  - `SM_DELETE_ITEM` for object `8051`, delete type `USE`
  - `SM_CUBE_UPDATE` with items count `2`
  - `SM_DELETE_ITEM` for object `8052`, delete type `USE`
  - `SM_CUBE_UPDATE` with items count `1`
- Java evidence reviewed: `Storage.delete` removes from item storage, `ItemPacketService.sendItemDeletePacket` sends delete then cube-size refresh, and Java Kinah is stored separately from `itemStorage`, so `Storage.size()` excludes Kinah.
- Refined `CraftStartInventoryPacketSendOperation.Disabled` so disabled send diagnostics distinguish:
  - `SM_DELETE_ITEM` from `ItemPacketService.sendItemDeletePacket`
  - `SM_CUBE_UPDATE.cubeSize` from `ItemPacketService.sendItemDeletePacket`
  - `SM_INVENTORY_UPDATE_ITEM` from `ItemPacketService.sendItemUpdatePacket`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# craft slice passed with 81 tests.
- First broad C# run timed out before a verdict; rerun with a longer timeout passed.
- Broad C# game-server suite passed with 5021 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 24 game-server tests.

## Known Gaps

- No new Java golden fixture was added for this exact craft delete packet sequence.
- Live craft-start mutation, inventory DB persistence, packet send, transaction boundaries, encrypted frame capture, and real-client validation remain unverified.
- The diagnostic refinement does not enable `SendPacketAsync`.
- Other cube delete callers outside craft may need their own source-reviewed tests.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1968-Completion.md`
- `docs/Phase-6-Session-1968-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.items.storage.Storage`
- `com.aionemu.gameserver.services.item.ItemPacketService`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## C# Artifacts Touched

- `Aion.GameServer.Services.CraftService`
- `Aion.GameServer.Services.CraftStartInventoryPacketSendAdapterPlanService`
- `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem`
- `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate`
- `Aion.GameServer.Tests.CraftServiceTests`

## Parity Table Updates

- Added Session 1968 rows to `PHASE-6-PROGRESS.md` for:
  - craft delete-path `SM_DELETE_ITEM` followed by `SM_CUBE_UPDATE`
  - Kinah-excluding cube size snapshots
  - disabled send-boundary diagnostic labels

## Next Recommended Unit of Work

- Next sequential task: inspect live `CM_PET` actionType 4 composition only if it can remain disabled and source-reviewed, focusing on packet/mutation boundary diagnostics rather than enabling pet autosell.

Safe alternative candidates:

- Audit signed Java `readH()` call sites where C# currently uses unsigned `PacketBuffer.ReadH()`.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live craft inventory parity from UOW-1968; the unit is disabled packet-plan evidence only.
- If continuing the recommended `CM_PET` slice, inspect Java `CM_PET` actionType 4 and the existing C# pet autosell diagnostics before deciding whether there is a small non-live boundary test to add.
