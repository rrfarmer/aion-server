# Phase 6 Session 2015 Completion - GF Webshop Token Request/Response Golden

Date: 2026-06-01
Unit of Work: UOW-2015
Status: Completed

## Work Discovery

- Re-read the latest Phase 6 handoff before choosing work.
- Compared Java and C# client packet factory registrations around opcode `229`.
- Inspected Java `CM_GF_WEBSHOP_TOKEN_REQUEST.readImpl` and `runImpl`.
- Inspected Java `SM_GF_WEBSHOP_TOKEN_RESPONSE.writeImpl` and Java fixed-length `writeS(text, 32)` behavior.
- Searched the C# game-server port for existing GF webshop token coverage.
- Reviewed `GameClientPacketFactory`, `GameServerConnection`, server packet patterns, and packet factory tests.
- Source-reviewed Java opcode `232` `CM_CHALLENGE_LIST` as a safe next candidate.

## What Changed

- Added Java golden test `CM_GF_WEBSHOP_TOKEN_REQUEST_ReadPayloadGoldenTest`.
- Added Java golden test `SM_GF_WEBSHOP_TOKEN_RESPONSE_GoldenTest`.
- Added C# `CmGfWebshopTokenRequest` parser for Java opcode `229`.
- Registered opcode `229` in `GameClientPacketFactory` for `InGame`.
- Added C# packet factory coverage proving valid `InGame` and invalid `Authed`.
- Added C# `SmGfWebshopTokenResponse` server packet for Java opcode `274`.
- Added C# server-packet coverage for the Java-shaped empty fixed-length token payload.
- Wired `GameServerConnection` to send `SmGfWebshopTokenResponse(string.Empty)` for the request, matching Java's current TODO empty-token response.

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

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_GF_WEBSHOP_TOKEN_REQUEST.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_GF_WEBSHOP_TOKEN_RESPONSE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- Objective evidence is limited to Java parser golden coverage, Java server-packet golden coverage, C# parser/factory unit coverage, C# server-packet payload coverage, broad C# tests, and Maven reactor tests.
- No verified live parity is claimed for encrypted frame handling, real-client `-st` behavior, non-empty token variants, or future token source behavior.

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

## Remaining Risks

- Java currently returns an empty token with a TODO; C# mirrors that current behavior, but future non-empty token behavior is unverified.
- Live socket dispatch, encrypted frame capture, and real-client GF webshop behavior remain unverified.
- Non-empty fixed-length token truncation and padding are not covered by Java/C# tests in this unit.

## Next Recommended Unit

- Continue packet-factory discovery with opcode `232` `CM_CHALLENGE_LIST` as a source-reviewed compact parser candidate: Java reads UC `action`, D `taskOwner`, UC `ownerType`, D `playerId`, and D `dateSince`; live challenge task service, legion validation, and audit behavior should remain deferred unless separately scoped.

Safe alternatives:

- Inspect opcode `237` `CM_MEGAPHONE` parser coverage only if string/readD golden evidence is needed and item-use behavior remains deferred.
- Inspect another compact unregistered parser boundary with Java golden evidence.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
