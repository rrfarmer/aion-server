# Phase 6 Session 2005 Handoff - House Teleport Back Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2005
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_HOUSE_TELEPORT_BACK.readImpl`.
- Added C# `CmHouseTeleportBack` and registered opcode `95` for `InGame`.
- Added C# factory/parser coverage for valid `InGame` and invalid `Authed` states.
- Added a documented no-op handler boundary in `GameServerConnection`; live battle-return teleport dispatch remains unported.

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

## Known Gaps

- This unit proves only empty parser consumption, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior resolves the active player, reads battle-return coords/map, teleports to the stored map and coordinates with `TeleportAnimation.FADE_OUT_BEAM`, and clears battle-return coords.
- C# does not yet perform battle-return state checks, teleport dispatch, teleport animation behavior, battle-return state clearing, or downstream packet/state effects for this route.
- Encrypted frame capture, real-client behavior, and socket dispatch remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_TELEPORT_BACK_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseTeleportBack.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2005-Completion.md`
- `docs/Phase-6-Session-2005-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_TELEPORT_BACK`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmHouseTeleportBack`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2005 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_HOUSE_TELEPORT_BACK.readImpl` empty payload behavior
  - `AionClientPacketFactory` opcode `95` registration state
  - `CM_HOUSE_TELEPORT_BACK.runImpl` deferred battle-return teleport boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery for another compact unregistered parser boundary with Java golden evidence. Source-review opcode `100` `CM_VIEW_PLAYER_DETAILS` or another small read-only packet before selecting.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live house-teleport-back parity from UOW-2005; only parser/factory coverage is backed by objective evidence.
- UOW-2004 covered `CM_INSTANCE_LEAVE`; UOW-2005 covered `CM_HOUSE_TELEPORT_BACK` parser/factory coverage.
