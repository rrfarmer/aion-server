# Phase 6 Session 1987 Handoff - CM_VERSION_CHECK Parser Boundary

Date: 2026-06-01
Unit of Work: UOW-1987
Status: Completed

## What Changed

- Added C# `CmVersionCheck` for Java opcode `0`.
- Registered opcode `0` as `CONNECTED` only in `GameClientPacketFactory`.
- Matched Java `CM_VERSION_CHECK.readImpl` field order: unsigned Aion client version, unsigned NPC script interface version, Windows encoding, Windows version, Windows sub-version, and lite-info byte.
- Added C# parser/factory coverage and Java golden coverage for high-bit unsigned version values and full payload consumption.
- Added an explicit C# runtime boundary documenting unported live `SM_VERSION_CHECK` response generation.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmVersionCheckTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_VERSION_CHECK_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# version-check parser/factory slice passed with 2 tests.
- Focused Java version-check golden test passed with 1 test method.
- Broad C# game-server suite passed with 5064 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 45 game-server tests.

## Known Gaps

- This unit proves only `CM_VERSION_CHECK` parser field handling and opcode state gating for focused vectors.
- Java runtime `SM_VERSION_CHECK` failure/success payloads, dynamic config/time/chat-server/ratio/passport/event-theme data, encrypted frame capture, and real-client validation remain unverified.
- Other unported connected-state packet and handshake surfaces still need separate audits.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_VERSION_CHECK_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmVersionCheck.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmVersionCheckTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1987-Completion.md`
- `docs/Phase-6-Session-1987-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_VERSION_CHECK`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_VERSION_CHECK.runImpl`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_VERSION_CHECK`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`
- `com.aionemu.commons.network.packet.BaseClientPacket`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmVersionCheck`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.Commons.Network.PacketBuffer`
- `Aion.GameServer.Tests.CmVersionCheckTests`

## Parity Table Updates

- Added Session 1987 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_VERSION_CHECK.readImpl`
  - opcode `0` factory registration
  - `CM_VERSION_CHECK.runImpl` live handler boundary
  - `SM_VERSION_CHECK.writeImpl` unported writer boundary

## Next Recommended Unit of Work

- Next sequential task: continue Work Discovery for the smallest remaining evidence-backed parser/factory or deterministic planner boundary. Prefer a compact unported packet over live `SM_VERSION_CHECK` unless a deterministic Java byte vector can be produced for both failure and success branches.

Safe alternative candidates:

- Inspect another compact unported parser/factory boundary with Java golden evidence.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live version-check parity from UOW-1987; this is parser/factory evidence only.
- Treat `SM_VERSION_CHECK` as a larger server-packet/runtime unit because Java writes dynamic config, server-time, ratio, chat-server, passport, and event-theme fields.
