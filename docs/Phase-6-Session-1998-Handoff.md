# Phase 6 Session 1998 Handoff - Summon Move Parser Golden

Date: 2026-06-01
Unit of Work: UOW-1998
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_SUMMON_MOVE.readImpl`.
- Added C# `CmSummonMove` and registered opcode `201` for `InGame`.
- Added C# factory/parser coverage for absolute/glide/vehicle payloads and the Java manual-position-without-absolute branch that consumes no vector coordinates.
- Added a documented no-op handler boundary in `GameServerConnection`; live summon movement remains unported.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_SUMMON_MOVE_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesSummonMove" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_SUMMON_MOVE` parser golden passed with 2 test methods.
- Focused C# summon-move factory tests passed with 2 tests.
- Broad C# game-server suite passed with 5105 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 56 game-server tests.

## Known Gaps

- This unit proves only parser field consumption and opcode registration.
- C# does not execute Java `CM_SUMMON_MOVE.runImpl` live behavior: summon/mercenary lookup, spawned check, abnormal-state guards, movement-controller mutation, world position update, last-move update, or `SM_MOVE` broadcast.
- Encrypted frame capture, real-client behavior, threading, and live movement state parity remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_SUMMON_MOVE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmSummonMove.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1998-Completion.md`
- `docs/Phase-6-Session-1998-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_MOVE`
- `com.aionemu.gameserver.controllers.movement.MovementMask`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmSummonMove`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 1998 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_SUMMON_MOVE.readImpl` parser field order
  - `AionClientPacketFactory` opcode `201` registration
  - `CM_SUMMON_MOVE.runImpl` no-op live handler boundary

## Next Recommended Unit of Work

- Next sequential task: choose another compact parser/factory/model boundary with Java golden evidence and no broad live mutation.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live summon movement parity from UOW-1998; only parser/factory parity is backed by objective evidence.
- UOW-1996 and UOW-1997 covered compact pet parser branches; UOW-1998 covers summon movement parser/factory coverage.
