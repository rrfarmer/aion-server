# Phase 6 Session 1986 Handoff - CM_HOUSE_SCRIPT Parser Boundary

Date: 2026-06-01
Unit of Work: UOW-1986
Status: Completed

## What Changed

- Added C# `CmHouseScript` for Java opcode `30`.
- Registered opcode `30` as `IN_GAME` only in `GameClientPacketFactory`.
- Matched Java `CM_HOUSE_SCRIPT.readImpl` field order: address, script id, total size, compressed size, uncompressed size, and compressed script content bytes.
- Matched Java's oversized compressed-size early return branch using the same constant formula as `SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE`.
- Added C# parser/factory coverage and Java golden coverage for valid compressed payloads and oversized compressed-size payloads.
- Added an explicit C# runtime boundary documenting unported live active-house ownership checks, script mutation, overflow messaging, and `SM_HOUSE_SCRIPTS` broadcast behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_HOUSE_SCRIPT_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# house-script parser/factory slice passed with 3 tests.
- Focused Java house-script golden test passed with 2 test methods.
- Broad C# game-server suite passed with 5062 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 44 game-server tests.

## Known Gaps

- This unit proves only `CM_HOUSE_SCRIPT` parser field handling, compressed-size guard behavior, and opcode state gating for focused vectors.
- Java runtime active-house lookup, ownership validation, `PlayerScripts` mutation, overflow system messages, `SM_HOUSE_SCRIPTS` serialization/broadcast, encrypted frame capture, and real-client validation remain unverified.
- Other unported client packet parsers and live housing script surfaces still need separate audits.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_SCRIPT_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseScript.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmHouseScriptTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1986-Completion.md`
- `docs/Phase-6-Session-1986-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_SCRIPT`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_SCRIPT.runImpl`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.commons.network.packet.BaseClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmHouseScript`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.Commons.Network.PacketBuffer`
- `Aion.GameServer.Network.Aion.GameServerPacket`
- `Aion.GameServer.Tests.CmHouseScriptTests`

## Parity Table Updates

- Added Session 1986 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_HOUSE_SCRIPT.readImpl`
  - oversized compressed-size guard
  - `SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE` constant use
  - opcode `30` factory registration
  - `CM_HOUSE_SCRIPT.runImpl` live handler boundary

## Next Recommended Unit of Work

- Next sequential task: continue Work Discovery for unported compact client packets. Inspect `CM_VERSION_CHECK` only if it can be kept evidence-backed and safely scoped, because Java's response path builds dynamic `SM_VERSION_CHECK` data from config/time/chat/event-theme state.

Safe alternative candidates:

- Inspect another compact unported parser/factory boundary with Java golden evidence.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live house-script parity from UOW-1986; this is parser/factory/guard evidence only.
- Treat `CM_VERSION_CHECK` cautiously: Java `runImpl` sends dynamic server configuration, time, chat-server, and event-theme data through `SM_VERSION_CHECK`, so a parser-only unit may not be enough unless the scope is explicit.
