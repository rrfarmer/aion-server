# Phase 6 Session 2004 Handoff - Instance Leave Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2004
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_INSTANCE_LEAVE.readImpl`.
- Added C# `CmInstanceLeave` and registered opcode `46` for `InGame`.
- Added C# factory/parser coverage for valid `InGame` and invalid `Authed` states.
- Added a documented no-op handler boundary in `GameServerConnection`; live instance-handler leave dispatch remains unported.

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

## Known Gaps

- This unit proves only empty parser consumption, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior resolves the active player, checks `player.isInInstance()`, then delegates to the current world-map instance handler's `leaveInstance(player)`.
- C# does not yet perform active-player instance checks, world-map instance resolution, instance-handler dispatch, leave-instance state mutation, or downstream packet/state effects for this route.
- Encrypted frame capture, real-client behavior, and socket dispatch remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_INSTANCE_LEAVE_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmInstanceLeave.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2004-Completion.md`
- `docs/Phase-6-Session-2004-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_INSTANCE_LEAVE`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmInstanceLeave`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2004 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_INSTANCE_LEAVE.readImpl` empty payload behavior
  - `AionClientPacketFactory` opcode `46` registration state
  - `CM_INSTANCE_LEAVE.runImpl` deferred live instance-handler boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery for another compact unregistered parser boundary with Java golden evidence. `CM_HOUSE_TELEPORT_BACK` opcode `95` is an empty-payload candidate, but live behavior uses battle-return teleport state, so source-review and keep any first slice parser/factory-only unless dependencies are ready.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live instance-leave parity from UOW-2004; only parser/factory coverage is backed by objective evidence.
- UOW-2001 covered `CM_DISCONNECT`, UOW-2002 covered `CM_STOP_TRAINING`, UOW-2003 covered `CM_CLOSE_DIALOG`, and UOW-2004 covered `CM_INSTANCE_LEAVE` parser/factory coverage.
