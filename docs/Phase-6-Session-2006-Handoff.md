# Phase 6 Session 2006 Handoff - View Player Details Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2006
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_VIEW_PLAYER_DETAILS.readImpl`.
- Added C# `CmViewPlayerDetails` and registered opcode `100` for `InGame`.
- Added C# factory/parser coverage for target object id parsing plus valid `InGame` and invalid `Authed` states.
- Added a documented no-op handler boundary in `GameServerConnection`; live known-list/privacy/detail-packet dispatch remains unported.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_VIEW_PLAYER_DETAILS_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesViewPlayerDetailsPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_VIEW_PLAYER_DETAILS` parser golden passed with 1 test method.
- Focused C# view-player-details factory test passed with 1 test.
- Broad C# game-server suite passed with 5112 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 64 game-server tests.

## Known Gaps

- This unit proves only target-object-id parser consumption, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior resolves the active player, finds the target player in known-list, checks `DeniedStatus.VIEW_DETAILS` and `AdminConfig.VIEW_PLAYER_DETAILS`, and sends either `SM_VIEW_PLAYER_DETAILS` or `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_WATCH`.
- C# does not yet perform target lookup, privacy/admin checks, equipment-detail packet serialization, rejection message sending, or downstream state effects for this route.
- Encrypted frame capture, real-client behavior, and socket dispatch remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_VIEW_PLAYER_DETAILS_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmViewPlayerDetails.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2006-Completion.md`
- `docs/Phase-6-Session-2006-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_VIEW_PLAYER_DETAILS`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmViewPlayerDetails`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2006 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_VIEW_PLAYER_DETAILS.readImpl` target object id parsing
  - `AionClientPacketFactory` opcode `100` registration state
  - `CM_VIEW_PLAYER_DETAILS.runImpl` deferred known-list/privacy/detail-packet boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery for another compact unregistered parser boundary with Java golden evidence. Source-review opcode `104` `CM_GAMEGUARD` or another small packet before selecting, especially because Java registers it for both `AUTHED` and `IN_GAME`.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live view-player-details parity from UOW-2006; only parser/factory coverage is backed by objective evidence.
- UOW-2005 covered `CM_HOUSE_TELEPORT_BACK`; UOW-2006 covered `CM_VIEW_PLAYER_DETAILS` parser/factory coverage.
