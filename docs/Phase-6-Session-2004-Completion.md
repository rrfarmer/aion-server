# Phase 6 Session 2004 Completion - Instance Leave Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2004
Status: Completed

## Work Discovery

- Re-read the required migration docs and latest Session 2003 handoff before choosing work.
- Confirmed the worktree was clean.
- Compared Java and C# client packet factory registrations.
- Inspected Java `CM_INSTANCE_LEAVE.readImpl` and `runImpl`.
- Inspected C# empty-payload packet patterns, `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.

## What Changed

- Added Java golden test `CM_INSTANCE_LEAVE_ReadPayloadGoldenTest`.
- Added C# `CmInstanceLeave` parser for Java opcode `46`.
- Registered opcode `46` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving the packet is accepted in `InGame` and rejected in `Authed`.
- Added a documented no-op `GameServerConnection` boundary for Java's live instance-handler dispatch.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_INSTANCE_LEAVE_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesInstanceLeavePacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_INSTANCE_LEAVE` parser golden passed with 1 test method.
- Focused C# instance-leave factory test passed with 1 test.
- Broad C# game-server suite passed with 5110 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 62 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_INSTANCE_LEAVE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for active-player lookup, `isInInstance()` guarding, world-map instance resolution, instance-handler dispatch, leave-instance state changes, encrypted frames, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_INSTANCE_LEAVE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmInstanceLeave.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2004-Completion.md`
- `docs/Phase-6-Session-2004-Handoff.md`

## Remaining Risks

- Java `CM_INSTANCE_LEAVE.runImpl` remains unported beyond a documented boundary.
- Live instance membership checks and `leaveInstance(player)` side effects remain unverified.
- Encrypted client frames, dispatcher behavior, and real-client instance-leave behavior remain unverified.

## Next Recommended Unit

- Continue packet-factory discovery for another compact unregistered parser boundary with Java golden evidence. `CM_HOUSE_TELEPORT_BACK` opcode `95` is an empty-payload candidate, but source-review the live teleport/battle-return behavior before selecting it.

Safe alternatives:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.
