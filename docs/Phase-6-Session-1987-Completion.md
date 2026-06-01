# Phase 6 Session 1987 Completion - CM_VERSION_CHECK Parser Boundary

Date: 2026-06-01
Unit of Work: UOW-1987
Status: Completed

## What Changed

- Reviewed Java `CM_VERSION_CHECK.readImpl`, where two unsigned version fields, three integer Windows fields, and one lite-info byte are read.
- Reviewed Java `SM_VERSION_CHECK.writeImpl` enough to document why the live response is out of this unit's scope.
- Added C# `CmVersionCheck` with Java-shaped parser fields.
- Registered C# opcode `0` as `CONNECTED` only, matching Java `AionClientPacketFactory`.
- Added a parser-only runtime boundary in `GameServerConnection` documenting that live `SM_VERSION_CHECK` response generation is still unported.
- Added C# parser/factory tests and a Java golden test covering unsigned version reads and full payload consumption.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmVersionCheckTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_VERSION_CHECK_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# version-check parser/factory slice passed with 2 tests.
- Focused Java `CM_VERSION_CHECK_ReadPayloadGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5064 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 45 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_VERSION_CHECK.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_VERSION_CHECK.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_VERSION_CHECK.runImpl` or `SM_VERSION_CHECK.writeImpl`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_VERSION_CHECK_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmVersionCheck.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmVersionCheckTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1987-Completion.md`
- `docs/Phase-6-Session-1987-Handoff.md`

## Remaining Risks

- Java `SM_VERSION_CHECK` response bytes, dynamic server config, start/current time, chat-server IP/port, ratio limitation, passport state, event theme, encrypted frame capture, and real-client validation remain unverified.
- The C# port currently parses and registers the packet only; it intentionally does not execute live version-check response behavior.
- Other unported connected-state and handshake surfaces remain separate work.

## Next Recommended Unit

- Continue Work Discovery for the smallest remaining evidence-backed parser/factory or deterministic planner boundary. Avoid live `SM_VERSION_CHECK` writer parity unless deterministic Java byte vectors can be produced for both failure and success branches.

Safe alternatives:

- Inspect another compact unported parser/factory boundary with Java golden evidence.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
