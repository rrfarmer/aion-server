# Phase 6 Session 2005 Completion - House Teleport Back Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2005
Status: Completed

## Work Discovery

- Re-read the latest Session 2004 handoff before choosing work.
- Compared Java and C# client packet factory registrations.
- Inspected Java `CM_HOUSE_TELEPORT_BACK.readImpl` and `runImpl`.
- Inspected C# teleport packet boundaries, `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.

## What Changed

- Added Java golden test `CM_HOUSE_TELEPORT_BACK_ReadPayloadGoldenTest`.
- Added C# `CmHouseTeleportBack` parser for Java opcode `95`.
- Registered opcode `95` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving the packet is accepted in `InGame` and rejected in `Authed`.
- Added a documented no-op `GameServerConnection` boundary for Java's live battle-return teleport dispatch.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_HOUSE_TELEPORT_BACK_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesHouseTeleportBackPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_HOUSE_TELEPORT_BACK` parser golden passed with 1 test method.
- Focused C# house-teleport-back factory test passed with 1 test.
- Broad C# game-server suite passed with 5111 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 63 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_TELEPORT_BACK.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for battle-return coordinate/map checks, teleport dispatch, battle-return state clearing, encrypted frames, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_TELEPORT_BACK_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseTeleportBack.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2005-Completion.md`
- `docs/Phase-6-Session-2005-Handoff.md`

## Remaining Risks

- Java `CM_HOUSE_TELEPORT_BACK.runImpl` remains unported beyond a documented boundary.
- Live battle-return state checks, `TeleportService.teleportTo`, teleport animation selection, and state clearing remain unverified.
- Encrypted client frames, dispatcher behavior, and real-client house-teleport-back behavior remain unverified.

## Next Recommended Unit

- Continue packet-factory discovery for another compact unregistered parser boundary with Java golden evidence. Source-review opcode `100` `CM_VIEW_PLAYER_DETAILS` or another small read-only packet before selecting.

Safe alternatives:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.
