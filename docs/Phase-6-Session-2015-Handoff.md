# Phase 6 Session 2015 Handoff - GF Webshop Token Request/Response Golden

Date: 2026-06-01
Unit of Work: UOW-2015
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_GF_WEBSHOP_TOKEN_REQUEST.readImpl`.
- Added Java golden server-packet coverage for `SM_GF_WEBSHOP_TOKEN_RESPONSE.writeImpl`.
- Added C# `CmGfWebshopTokenRequest` and registered opcode `229` for `InGame`.
- Added C# factory/parser coverage for valid `InGame` and invalid `Authed`.
- Added C# `SmGfWebshopTokenResponse` opcode `274` and payload coverage for Java's empty fixed-length token.
- Wired `GameServerConnection` to send `SmGfWebshopTokenResponse(string.Empty)`, matching Java's current `SM_GF_WEBSHOP_TOKEN_RESPONSE("")` behavior.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_GF_WEBSHOP_TOKEN_REQUEST_ReadPayloadGoldenTest,SM_GF_WEBSHOP_TOKEN_RESPONSE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesGfWebshopTokenRequestPacket" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java GF webshop request/response golden tests passed with 2 test methods.
- Focused C# GF webshop request factory test passed with 1 test.
- Focused C# server-packet payload test passed with 1 test.
- Broad C# game-server suite passed with 5121 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 74 game-server tests.

## Known Gaps

- This unit proves only empty request parsing, opcode registration, empty-token response serialization, and source-reviewed handler wiring.
- Java currently sends `SM_GF_WEBSHOP_TOKEN_RESPONSE("")`; C# mirrors that current TODO behavior.
- C# does not yet prove live encrypted socket dispatch or real-client `-st` behavior for this route.
- Non-empty token truncation/padding behavior is source-reviewed from Java fixed-string logic but not tested here.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_GF_WEBSHOP_TOKEN_REQUEST_ReadPayloadGoldenTest.java`
- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_GF_WEBSHOP_TOKEN_RESPONSE_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmGfWebshopTokenRequest.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmGfWebshopTokenResponse.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2015-Completion.md`
- `docs/Phase-6-Session-2015-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_GF_WEBSHOP_TOKEN_REQUEST`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_GF_WEBSHOP_TOKEN_RESPONSE`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmGfWebshopTokenRequest`
- `Aion.GameServer.Network.Aion.ServerPackets.SmGfWebshopTokenResponse`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2015 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_GF_WEBSHOP_TOKEN_REQUEST.readImpl` empty parser
  - `AionClientPacketFactory` opcode `229` registration state
  - `SM_GF_WEBSHOP_TOKEN_RESPONSE.writeImpl` empty fixed-length token payload
  - `CM_GF_WEBSHOP_TOKEN_REQUEST.runImpl` empty response handler boundary

## Next Recommended Unit of Work

- Next sequential task: continue packet-factory discovery with source-reviewed opcode `232` `CM_CHALLENGE_LIST` as a compact parser candidate. Java reads UC `action`, D `taskOwner`, UC `ownerType`, D `playerId`, and D `dateSince`; live behavior validates legion ownership for `ownerType == 1`, audit-logs invalid legion requests, and calls `ChallengeTaskService.showTaskList`, so runtime wiring should remain deferred unless separately scoped.

Safe alternative candidates:

- Inspect opcode `237` `CM_MEGAPHONE` parser coverage only if string/readD golden evidence is needed and item-use behavior remains deferred.
- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim real-client GF webshop token parity from UOW-2015; only parser/factory/empty response serialization coverage is backed by objective evidence.
- UOW-2014 covered `CM_BREAK_WEAPONS`; UOW-2015 covered `CM_GF_WEBSHOP_TOKEN_REQUEST` and `SM_GF_WEBSHOP_TOKEN_RESPONSE` empty-token coverage.
