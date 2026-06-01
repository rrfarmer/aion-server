# Phase 6 Session 2020 Completion - Open Static Door Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2020
Status: Completed

## Work Discovery

- Re-read the latest Phase 6 handoff before choosing work.
- Compared Java and C# client packet factory registrations for opcode `23`.
- Inspected Java `CM_OPEN_STATICDOOR.readImpl` and `runImpl`.
- Searched the C# game-server port for existing static-door client packet coverage.
- Reviewed `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.

## What Changed

- Added Java golden test `CM_OPEN_STATICDOOR_ReadPayloadGoldenTest`.
- Added C# `CmOpenStaticDoor` parser for Java opcode `23`.
- Registered opcode `23` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving `doorId` parsing, valid `InGame`, and invalid `Authed`.
- Added a documented no-op `GameServerConnection` boundary for Java's `StaticDoorService.openStaticDoor` behavior.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_OPEN_STATICDOOR_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesOpenStaticDoorPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_OPEN_STATICDOOR` parser golden passed with 1 test method.
- Focused C# open-static-door factory test passed with 1 test.
- Broad C# game-server suite passed with 5126 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 79 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_OPEN_STATICDOOR.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for static-door service dispatch, door state mutation, packet fanout, persistence, encrypted frames, socket dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_OPEN_STATICDOOR_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmOpenStaticDoor.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2020-Completion.md`
- `docs/Phase-6-Session-2020-Handoff.md`

## Remaining Risks

- Java `CM_OPEN_STATICDOOR.runImpl` remains unported beyond a documented boundary.
- Live active-player lookup, `StaticDoorService.openStaticDoor`, static-door state mutation, packet fanout, socket dispatch, and real-client behavior remain unverified.

## Next Recommended Unit

- Continue packet-factory gap discovery from the missing registered-opcode list, preferring another compact parser with deferrable runtime behavior.

Safe alternatives:

- Inspect `CM_WINDSTREAM` as parser-only only if flight/quest/emotion side effects remain deferred.
- Inspect `CM_FIND_GROUP` only if a small action-specific parser vector is selected.
- Inspect Java `SM_UNWRAP_ITEM` writer parity as a server-packet-only unit.
- Inspect static-door service behavior read-only before any runtime wiring.
