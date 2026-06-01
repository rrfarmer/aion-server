# Phase 6 Session 1973 Completion - CM_MOVE_ITEM Signed Slot

Date: 2026-06-01
Unit of Work: UOW-1973
Status: Completed

## What Changed

- Reviewed Java `CM_MOVE_ITEM.readImpl`, where `slot = readH()` is a signed `short`.
- Added parser-only C# `CmMoveItem`.
- Registered opcode `156` as `IN_GAME`, matching Java `AionClientPacketFactory`.
- Added an explicit parser-only no-op boundary in `GameServerConnection` for Java `CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem`.
- Added Java golden and C# parser/factory tests for `slot = 0xFFFF` reading as signed `-1`.
- Kept this parser-only. No inventory or warehouse movement side effects were enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmMoveItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_MOVE_ITEM_ReadSignedSlotGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# move-item parser/factory slice passed with 2 tests.
- Focused Java `CM_MOVE_ITEM_ReadSignedSlotGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5034 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 30 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_MOVE_ITEM.runImpl` or `ItemMoveService.moveItem`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM_ReadSignedSlotGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmMoveItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmMoveItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1973-Completion.md`
- `docs/Phase-6-Session-1973-Handoff.md`

## Remaining Risks

- Live `ItemMoveService.moveItem` behavior, storage mutation, database persistence, packet sends, transaction behavior, encrypted frame capture, and real-client validation remain unported/unverified.
- Opcode `156` now parses in C#, but `GameServerConnection` intentionally keeps move execution as a no-op.
- Other Java signed `readH()` call sites remain separate work.

## Next Recommended Unit

- Continue the signed Java `readH()` audit with `CM_LEGION` permission fields if a parser-only C# packet and Java golden test can be added safely without enabling legion mutation side effects.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
