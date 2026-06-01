# Phase 6 Session 2018 Completion - Unwrap Item Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2018
Status: Completed

## Work Discovery

- Re-read required migration, orchestration, parity, progress, completion, and handoff documents before choosing work.
- Compared Java and C# client packet factory registrations around opcode `240`.
- Inspected Java `CM_UNWRAP_ITEM.readImpl` and `runImpl`.
- Inspected Java `SM_UNWRAP_ITEM.writeImpl` enough to document the deferred live-send boundary.
- Searched the C# game-server port for existing unwrap-item client packet coverage.
- Reviewed `GameClientPacketFactory`, `GameServerConnection`, and packet factory tests.
- Source-reviewed Java opcode `246` `CM_UPGRADE_ARCADE` as a safe next candidate after commented opcodes `241`-`243`.

## What Changed

- Added Java golden test `CM_UNWRAP_ITEM_ReadPayloadGoldenTest`.
- Added C# `CmUnwrapItem` parser for Java opcode `240`.
- Registered opcode `240` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving `objectId` parsing, valid `InGame`, and invalid `Authed`.
- Added a documented no-op `GameServerConnection` boundary for Java's live pack-count mutation, persistent-state update, `SM_UNWRAP_ITEM`, and `SM_INVENTORY_UPDATE_ITEM` behavior.

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

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_UNWRAP_ITEM.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_UNWRAP_ITEM.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, C# parser/factory unit coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for active-player lookup, inventory item lookup, positive pack-count guard, `SM_UNWRAP_ITEM` bytes, item pack-count mutation, persistent-state update, full inventory update serialization, encrypted frames, socket dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_UNWRAP_ITEM_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmUnwrapItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2018-Completion.md`
- `docs/Phase-6-Session-2018-Handoff.md`

## Remaining Risks

- Java `CM_UNWRAP_ITEM.runImpl` remains unported beyond a documented boundary.
- Live item lookup, positive pack-count branch behavior, `SM_UNWRAP_ITEM` count byte serialization, item pack-count negation, `PersistentState.UPDATE_REQUIRED`, `SM_INVENTORY_UPDATE_ITEM`, persistence, and real-client unwrap behavior remain unverified.
- This unit only preserves the parser and opcode-registration surface.

## Next Recommended Unit

- Continue packet-factory discovery with opcode `246` `CM_UPGRADE_ARCADE` as a source-reviewed parser candidate: Java reads C `action` and D `sessionId`, while live event config gating, `UpgradeArcadeService` action dispatch, and warning-log behavior should remain deferred unless separately scoped.

Safe alternatives:

- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect Java `SM_UNWRAP_ITEM`/inventory update writer parity as a server-packet-only unit if runtime mutation remains out of scope.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
