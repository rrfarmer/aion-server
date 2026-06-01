# Phase 6 Session 2012 Handoff - Auto Group Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2012
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_AUTO_GROUP.readImpl`.
- Added C# `CmAutoGroup` and registered opcode `200` for `InGame`.
- Added C# factory/parser coverage for D `instanceMaskId`, C `windowId`, C `entryRequestId`, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live auto-group service behavior remains unported.

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

## Known Gaps

- This unit proves only `instanceMaskId`/`windowId`/`entryRequestId` parsing, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior checks `AutoGroupConfig.AUTO_GROUP_ENABLE`, sends the disabled auto-group text message, dispatches window IDs `100` through `105`, resolves `EntryRequestType.getTypeById`, calls `AutoGroupService.startLooking`, `cancelRegistration`, `pressEnter`, `cancelEnter`, and delegates icon requests to `PeriodicInstanceManager.handleRequest`.
- C# does not yet perform config checks, disabled-message sending, entry-request lookup, auto-group service dispatch, periodic instance request dispatch, unknown-window live no-op behavior, or Dredgion failure branch behavior for this route.
- Encrypted frame capture, real-client behavior, and socket dispatch remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_AUTO_GROUP_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmAutoGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2012-Completion.md`
- `docs/Phase-6-Session-2012-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_AUTO_GROUP`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmAutoGroup`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2012 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_AUTO_GROUP.readImpl` parser field order
  - `AionClientPacketFactory` opcode `200` registration state
  - `CM_AUTO_GROUP.runImpl` deferred auto-group service boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery with source-reviewed opcode `206` `CM_FUSION_WEAPONS` as a compact parser candidate. Java reads D `npcObjId`, D `mainWeaponObjId`, and D `fuseWeaponObjId`; live behavior checks `DialogAction.COMPOUND_WEAPON`, calls `ArmsfusionService.fusionWeapons`, or audit-logs invalid use, so runtime wiring should remain deferred unless separately scoped.

Safe alternative candidates:

- Inspect opcode `207` `CM_BREAK_WEAPONS` as the paired armsfusion parser boundary: Java reads D `npcObjId` and D `weaponObjId`.
- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live auto-group parity from UOW-2012; only parser/factory coverage is backed by objective evidence.
- UOW-2011 covered `CM_ABYSS_RANKING_PLAYERS`; UOW-2012 covered `CM_AUTO_GROUP` parser/factory coverage.
