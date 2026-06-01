# Phase 6 Session 2019 Completion - Upgrade Arcade Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2019
Status: Completed

## Work Discovery

- Re-read the latest Phase 6 completion and handoff before choosing work.
- Compared Java and C# client packet factory registrations around opcode `246`.
- Inspected Java `CM_UPGRADE_ARCADE.readImpl` and `runImpl`.
- Searched the C# game-server port for existing upgrade-arcade client packet coverage and reviewed the existing arcade frenzy planner.
- Reviewed `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.
- Ran a broader packet-factory gap pass and source-reviewed Java `CM_OPEN_STATICDOOR` as a safe next candidate.

## What Changed

- Added Java golden test `CM_UPGRADE_ARCADE_ReadPayloadGoldenTest`.
- Added C# `CmUpgradeArcade` parser for Java opcode `246`.
- Registered opcode `246` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving `action` and `sessionId` parsing, valid `InGame`, and invalid `Authed`.
- Added a documented no-op `GameServerConnection` boundary for Java's event config gate and `UpgradeArcadeService` dispatch behavior.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_UPGRADE_ARCADE_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesUpgradeArcadePacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_UPGRADE_ARCADE` parser golden passed with 1 test method.
- Focused C# upgrade-arcade factory test passed with 1 test.
- Broad C# game-server suite passed with 5125 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 78 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_UPGRADE_ARCADE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for event gating, arcade action dispatch, warning logs, arcade packets, persistence, encrypted frames, socket dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_UPGRADE_ARCADE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmUpgradeArcade.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2019-Completion.md`
- `docs/Phase-6-Session-2019-Handoff.md`

## Remaining Risks

- Java `CM_UPGRADE_ARCADE.runImpl` remains unported beyond a documented boundary.
- Live `EventsConfig.ENABLE_EVENT_ARCADE` gating, active-player assumptions, `UpgradeArcadeService` action behavior, warning logs, real-client arcade flow, and socket behavior remain unverified.

## Next Recommended Unit

- Continue packet-factory discovery with opcode `23` `CM_OPEN_STATICDOOR` as a source-reviewed parser candidate: Java reads D `doorId`, while live `StaticDoorService.openStaticDoor` should remain deferred unless separately scoped.

Safe alternatives:

- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect Java `SM_UNWRAP_ITEM` writer parity as a server-packet-only unit.
- Inspect `CM_WINDSTREAM` parser only if flight/quest/emotion side effects remain deferred.
- Inspect `CM_FIND_GROUP` only if a small action-specific parser vector is selected.
