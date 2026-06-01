# Phase 6 Session 2018 Handoff - Unwrap Item Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2018
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_UNWRAP_ITEM.readImpl`.
- Added C# `CmUnwrapItem` and registered opcode `240` for `InGame`.
- Added C# factory/parser coverage for D `objectId`, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live unwrap item behavior remains unported.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_UNWRAP_ITEM_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesUnwrapItemPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_UNWRAP_ITEM` parser golden passed with 1 test method.
- Focused C# unwrap-item factory test passed with 1 test.
- Broad C# game-server suite passed with 5124 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 77 game-server tests.

## Known Gaps

- This unit proves only `objectId` parsing, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior gets the active player, retrieves the cube item by object ID, checks `item.getPackCount() > 0`, sends `SM_UNWRAP_ITEM(objectId, packCount)`, multiplies pack count by `-1`, marks the item `UPDATE_REQUIRED`, and sends `SM_INVENTORY_UPDATE_ITEM`.
- C# does not yet perform live inventory lookup, pack-count guard/mutation, persistent-state update, unwrap/inventory packet sends, DB persistence, encrypted frame handling, or real-client validation for this route.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_UNWRAP_ITEM_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmUnwrapItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2018-Completion.md`
- `docs/Phase-6-Session-2018-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_UNWRAP_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_UNWRAP_ITEM` (source-reviewed only)
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmUnwrapItem`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2018 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_UNWRAP_ITEM.readImpl` parser field order
  - `AionClientPacketFactory` opcode `240` registration state
  - `CM_UNWRAP_ITEM.runImpl` deferred unwrap mutation/send boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery with source-reviewed opcode `246` `CM_UPGRADE_ARCADE` as a parser candidate. Java reads C `action` and D `sessionId`; live behavior checks `EventsConfig.ENABLE_EVENT_ARCADE`, dispatches action `0`-`5` to `UpgradeArcadeService`, and warning-logs unknown actions, so runtime wiring should remain deferred unless separately scoped.

Safe alternative candidates:

- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect Java `SM_UNWRAP_ITEM`/inventory update writer parity as a server-packet-only unit if runtime mutation remains out of scope.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live unwrap parity from UOW-2018; only parser/factory coverage is backed by objective evidence.
- UOW-2017 covered `CM_MEGAPHONE`; UOW-2018 covered `CM_UNWRAP_ITEM` parser/factory coverage.
