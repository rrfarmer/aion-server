# Phase 6 Session 1986 Completion - CM_HOUSE_SCRIPT Parser Boundary

Date: 2026-06-01
Unit of Work: UOW-1986
Status: Completed

## What Changed

- Reviewed Java `CM_HOUSE_SCRIPT.readImpl`, where address, script id, total size, compressed size, uncompressed size, and compressed script bytes are read.
- Added C# `CmHouseScript` with Java-shaped parser fields and the Java compressed-script-size guard.
- Registered C# opcode `30` as `IN_GAME` only, matching Java `AionClientPacketFactory`.
- Added a parser-only runtime boundary in `GameServerConnection` documenting that live house-script mutation and broadcast behavior is still unported.
- Added C# parser/factory tests and a Java golden test covering valid compressed script reads and oversized compressed-size early return behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_HOUSE_SCRIPT_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# house-script parser/factory slice passed with 3 tests.
- Focused Java `CM_HOUSE_SCRIPT_ReadPayloadGoldenTest` passed with 2 test methods.
- Broad C# game-server suite passed with 5062 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 44 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_SCRIPT.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_HOUSE_SCRIPTS.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_HOUSE_SCRIPT.runImpl`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_SCRIPT_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseScript.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmHouseScriptTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1986-Completion.md`
- `docs/Phase-6-Session-1986-Handoff.md`

## Remaining Risks

- Java active-house ownership validation, `PlayerScripts` mutation, overflow system message behavior, `SM_HOUSE_SCRIPTS` broadcast/serialization, encrypted frame capture, and real-client validation remain unverified.
- The C# port currently parses and registers the packet only; it intentionally does not execute live house-script behavior.
- Other unported client-packet parser and runtime boundaries remain separate work.

## Next Recommended Unit

- Continue Work Discovery for unported compact client packets. `CM_VERSION_CHECK` is the next named candidate only if it can be safely scoped, because Java's response path builds dynamic `SM_VERSION_CHECK` data from config/time/chat/event-theme state.

Safe alternatives:

- Inspect another compact unported parser/factory boundary with Java golden evidence.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
