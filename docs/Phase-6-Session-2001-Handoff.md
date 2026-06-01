# Phase 6 Session 2001 Handoff - Disconnect Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2001
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_DISCONNECT.readImpl`.
- Added C# `CmDisconnect` and registered opcode `2` for `Authed` and `InGame`.
- Added C# factory/parser coverage for the consumed flag byte and invalid `Connected` state.
- Added a documented no-op handler boundary in `GameServerConnection`, matching Java `CM_DISCONNECT.runImpl`.

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

## Known Gaps

- This unit proves only parser consumption, opcode registration, and source-reviewed no-op packet-handler behavior.
- Java socket closure behavior is outside `CM_DISCONNECT.runImpl`; C# live network lifecycle parity is not verified.
- Encrypted frame capture, real-client behavior, dispatcher closure ordering, and socket teardown remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_DISCONNECT_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDisconnect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2001-Completion.md`
- `docs/Phase-6-Session-2001-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DISCONNECT`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmDisconnect`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2001 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_DISCONNECT.readImpl` single-byte parser consumption
  - `AionClientPacketFactory` opcode `2` registration states
  - `CM_DISCONNECT.runImpl` no-op packet-handler boundary

## Next Recommended Unit of Work

- Next sequential task: inspect another compact unregistered packet boundary with Java golden evidence. `CM_CLOSE_DIALOG` opcode `53` or `CM_STOP_TRAINING` opcode `84` may be suitable, but source-review Java behavior before selecting the slice.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live network disconnect parity from UOW-2001; only parser/factory/no-op packet-handler behavior is backed by objective evidence.
- UOW-1998 covered `CM_SUMMON_MOVE`, UOW-1999 covered `CM_SUMMON_EMOTION`, UOW-2000 covered `CM_SUMMON_COMMAND`, and UOW-2001 covered `CM_DISCONNECT` parser/factory coverage.
