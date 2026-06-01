# Phase 6 Session 2001 Completion - Disconnect Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2001
Status: Completed

## Work Discovery

- Re-read the required migration docs, latest progress log, and Session 2000 handoff/completion context before choosing work.
- Compared Java and C# client packet factory registrations.
- Inspected Java `CM_DISCONNECT.readImpl` and `runImpl`.
- Inspected C# quit/mayquit handling, `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.

## What Changed

- Added Java golden test `CM_DISCONNECT_ReadPayloadGoldenTest`.
- Added C# `CmDisconnect` parser for Java opcode `2`.
- Registered opcode `2` in `GameClientPacketFactory` for `Authed` and `InGame`.
- Added C# packet factory coverage proving the single byte is consumed for both valid states and rejected for `Connected`.
- Added a documented no-op `GameServerConnection` boundary matching Java `CM_DISCONNECT.runImpl`.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_DISCONNECT_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesDisconnectPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_DISCONNECT` parser golden passed with 1 test method.
- Focused C# disconnect factory test passed with 1 test.
- Broad C# game-server suite passed with 5108 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 59 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_DISCONNECT.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, source review of Java's no-op `runImpl`, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for encrypted frames, socket closure, network disconnect lifecycle, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_DISCONNECT_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDisconnect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2001-Completion.md`
- `docs/Phase-6-Session-2001-Handoff.md`

## Remaining Risks

- Socket closure behavior is outside Java `CM_DISCONNECT.runImpl` and remains unverified.
- This unit proves only parser field consumption, opcode registration states, and no-op packet-handler behavior.
- Encrypted client frames, dispatcher closure ordering, and real-client disconnect behavior remain unverified.

## Next Recommended Unit

- Inspect another compact unregistered packet boundary with Java golden evidence. `CM_CLOSE_DIALOG` opcode `53` and `CM_STOP_TRAINING` opcode `84` are plausible candidates, but source-review them before committing to parser work.

Safe alternatives:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.
