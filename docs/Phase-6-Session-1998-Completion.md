# Phase 6 Session 1998 Completion - Summon Move Parser Golden

Date: 2026-06-01
Unit of Work: UOW-1998
Status: Completed

## Work Discovery

- Re-read the latest handoff and progress context after UOW-1997.
- Inspected Java `CM_SUMMON_MOVE.readImpl` and `runImpl`.
- Inspected Java and C# `MovementMask` constants and helper semantics.
- Inspected C# `CmMove`, summon attack/cast-spell packet parsers, packet factory registrations, `GameServerConnection`, and existing packet factory tests.

## What Changed

- Added Java golden test `CM_SUMMON_MOVE_ReadPayloadGoldenTest`.
- Added C# `CmSummonMove` parser for Java opcode `201`.
- Registered opcode `201` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage for the absolute/glide/vehicle branch and for Java's manual-position-without-absolute branch that reads no vector tail.
- Added a documented `GameServerConnection` no-op boundary for live summon movement.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_SUMMON_MOVE_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesSummonMove" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_SUMMON_MOVE` parser golden passed with 2 test methods.
- Focused C# `GamePacketTests.ClientPacketFactory_ParsesSummonMove*` coverage passed with 2 tests.
- Broad C# game-server suite passed with 5105 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 56 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SUMMON_MOVE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/controllers/movement/MovementMask.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmMove.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Controllers/Movement/MovementMask.cs`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for summon movement execution, summon/mercenary ownership lookup, movement controller mutation, world position updates, `SM_MOVE` fanout, encrypted frame handling, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_SUMMON_MOVE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmSummonMove.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1998-Completion.md`
- `docs/Phase-6-Session-1998-Handoff.md`

## Remaining Risks

- `CM_SUMMON_MOVE.runImpl` remains unported beyond a documented no-op boundary.
- Java's manual-position-without-absolute runtime early return and position semantics remain source-reviewed only.
- Live summon/mercenary state, abnormal-state gates, threading, movement fanout, encrypted client frames, and real-client behavior remain unverified.

## Next Recommended Unit

- Choose another compact parser/factory/model boundary with Java golden evidence and keep live mutation deferred unless the supporting subsystem is ready.

Safe alternatives:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.
