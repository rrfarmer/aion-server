# Phase 6 Session 2014 Handoff - Break Weapons Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2014
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_BREAK_WEAPONS.readImpl`.
- Added C# `CmBreakWeapons` and registered opcode `207` for `InGame`.
- Added C# factory/parser coverage for D `npcObjId`, D `weaponObjId`, valid `InGame`, and invalid `Authed`.
- Added a documented no-op handler boundary in `GameServerConnection`; live armsfusion break behavior remains unported.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BREAK_WEAPONS_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesBreakWeaponsPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_BREAK_WEAPONS` parser golden passed with 1 test method.
- Focused C# break-weapons factory test passed with 1 test.
- Broad C# game-server suite passed with 5120 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 72 game-server tests.

## Known Gaps

- This unit proves only `npcObjId`/`weaponObjId` parsing, opcode registration, and source-reviewed deferred handler behavior.
- Java live behavior checks `player.isTargetingNpcWithFunction(npcObjId, DialogAction.DECOMPOUND_WEAPON)`, calls `ArmsfusionService.breakWeapons`, or audit-logs invalid use.
- C# does not yet perform live NPC function validation, audit logging, armsfusion break service dispatch, item mutation, persistence, inventory updates, or downstream packet sends for this route.
- Existing C# armsfusion planner coverage remains non-live and is not promoted to runtime parity by this unit.
- Encrypted frame capture, real-client behavior, and socket dispatch remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BREAK_WEAPONS_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBreakWeapons.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2014-Completion.md`
- `docs/Phase-6-Session-2014-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_BREAK_WEAPONS`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmBreakWeapons`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2014 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_BREAK_WEAPONS.readImpl` parser field order
  - `AionClientPacketFactory` opcode `207` registration state
  - `CM_BREAK_WEAPONS.runImpl` deferred armsfusion break service boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery with source-reviewed opcode `229` `CM_GF_WEBSHOP_TOKEN_REQUEST` as a compact empty-payload parser candidate. Java reads no payload and sends `SM_GF_WEBSHOP_TOKEN_RESPONSE("")`; decide whether to keep response behavior deferred or add a narrowly scoped server-packet response test.

Safe alternative candidates:

- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live break-weapons parity from UOW-2014; only parser/factory coverage is backed by objective evidence.
- UOW-2013 covered `CM_FUSION_WEAPONS`; UOW-2014 covered `CM_BREAK_WEAPONS` parser/factory coverage.
