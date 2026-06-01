# Phase 6 Session 2012 Completion - Auto Group Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2012
Status: Completed

## Work Discovery

- Re-read the latest Phase 6 handoff before choosing work.
- Compared Java and C# client packet factory registrations around opcode `200`.
- Inspected Java `CM_AUTO_GROUP.readImpl` and `runImpl`.
- Searched the C# game-server port for existing auto-group client packet coverage.
- Reviewed `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.
- Source-reviewed Java opcode `206` `CM_FUSION_WEAPONS` as a safe next candidate.

## What Changed

- Added Java golden test `CM_AUTO_GROUP_ReadPayloadGoldenTest`.
- Added C# `CmAutoGroup` parser for Java opcode `200`.
- Registered opcode `200` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving `instanceMaskId`, `windowId`, and `entryRequestId` parsing, valid `InGame`, and invalid `Authed`.
- Added a documented no-op `GameServerConnection` boundary for Java's live auto-group config, window action dispatch, entry-request lookup, auto-group service calls, and periodic instance request behavior.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_AUTO_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesAutoGroupPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_AUTO_GROUP` parser golden passed with 1 test method.
- Focused C# auto-group factory test passed with 1 test.
- Broad C# game-server suite initially timed out before reporting at the shorter tool timeout, then passed with 5118 tests when rerun with a longer timeout.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 70 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for auto-group config denial messaging, window action dispatch, entry-request lookup, auto-group registration/enter/cancel behavior, periodic instance request handling, encrypted frames, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmAutoGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2012-Completion.md`
- `docs/Phase-6-Session-2012-Handoff.md`

## Remaining Risks

- Java `CM_AUTO_GROUP.runImpl` remains unported beyond a documented boundary.
- Live auto-group config checks, service calls, periodic instance requests, unknown-window behavior, and Dredgion failure branch behavior remain unverified.
- Encrypted client frames, dispatcher behavior, and real-client auto-group behavior remain unverified.

## Next Recommended Unit

- Continue packet-factory discovery with opcode `206` `CM_FUSION_WEAPONS` as a source-reviewed compact parser candidate: Java reads D `npcObjId`, D `mainWeaponObjId`, and D `fuseWeaponObjId`, and live armsfusion/NPC-function/audit behavior should remain deferred unless separately scoped.

Safe alternatives:

- Inspect opcode `207` `CM_BREAK_WEAPONS` as the paired armsfusion parser boundary.
- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
