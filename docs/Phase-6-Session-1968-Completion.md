# Phase 6 Session 1968 Completion - Craft Delete Cube-Size Diagnostics

Date: 2026-06-01
Unit of Work: UOW-1968
Status: Completed

## What Changed

- Reviewed Java craft consumption delete flow through `Storage.decreaseByItemId`, `Storage.delete`, `ItemPacketService.sendItemPacket`, `ItemPacketService.sendItemDeletePacket`, and `SM_CUBE_UPDATE.cubeSize`.
- Added C# craft packet-plan coverage for deleted cube stacks when Kinah is also present in the flattened C# inventory.
- Confirmed the C# craft delete packet plan emits `SM_DELETE_ITEM(USE)` followed by `SM_CUBE_UPDATE` snapshots whose item counts exclude Kinah and decrement once per deleted non-Kinah stack.
- Refined disabled craft packet-send diagnostics so delete, cube-size refresh, and inventory-update packets name their specific Java `ItemPacketService` send boundary.
- This unit remains non-live. No craft inventory mutation, persistence, socket send, transaction, encrypted frame capture, or real-client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# craft slice passed with 81 tests.
- First broad C# run timed out at the command limit before producing a pass/fail result; rerun with a longer timeout passed.
- Broad C# game-server suite passed with 5021 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 24 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDeleteItem.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`.
- Objective evidence is limited to source review, C# unit tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for craft-start mutation, DB persistence, packet dispatch, or client-observed behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1968-Completion.md`
- `docs/Phase-6-Session-1968-Handoff.md`

## Remaining Risks

- Java `Storage.size()` Kinah exclusion was source-reviewed but not covered with a new Java golden packet fixture in this unit.
- Other delete-path cube-size callers outside craft were not exhaustively audited.
- Live craft inventory persistence and send ordering remain disabled/unverified.
- Full Maven reactor validation was not rerun in this unit; only the game-server reactor was run.

## Next Recommended Unit

- Inspect live `CM_PET` actionType 4 composition only if it can remain disabled and source-reviewed, focusing on packet/mutation boundary diagnostics rather than enabling pet autosell.

Safe alternatives:

- Audit signed Java `readH()` call sites where C# currently uses unsigned `PacketBuffer.ReadH()`.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
