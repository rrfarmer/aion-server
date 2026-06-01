# Phase 6 Session 1977 Completion - CM_HOUSE_KICK Signed Padding

Date: 2026-06-01
Unit of Work: UOW-1977
Status: Completed

## What Changed

- Reviewed Java `CM_HOUSE_KICK.readImpl`, where an ignored padding field is consumed with signed `readH()`.
- Updated C# `CmHouseKick` to consume that padding with `PacketBuffer.ReadSignedH()`.
- Added C# parser/factory tests covering opcode `72` state gating and high-bit padding after the option byte.
- Added a Java golden test proving high-bit padding leaves `option` parsing intact and fully consumed.
- Kept this parser-focused. No live house visitor kick behavior was newly claimed.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseKickSignedPaddingTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_HOUSE_KICK_ReadSignedPaddingGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# house-kick padding parser/factory slice passed with 2 tests.
- Focused Java `CM_HOUSE_KICK_ReadSignedPaddingGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5043 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 34 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_KICK.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseKick.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_HOUSE_KICK.runImpl` or `HouseController.kickVisitors`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_KICK_ReadSignedPaddingGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmHouseKick.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmHouseKickSignedPaddingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1977-Completion.md`
- `docs/Phase-6-Session-1977-Handoff.md`

## Remaining Risks

- Live visitor kicking, active-house lookup parity, friend exclusion, audit logging, packet fanout, encrypted frame capture, and real-client validation remain unverified.
- Because Java discards the padding value, the evidence proves field alignment and full read consumption rather than an observable signed value.
- Other Java signed `readH()` call sites remain separate work.

## Next Recommended Unit

- Continue the signed Java `readH()` audit by inspecting another ignored-padding or parser-only call site, with `CM_MANASTONE`, `CM_QUESTION_RESPONSE`, `CM_PING`, `CM_TELEPORT_SELECT`, `CM_UI_SETTINGS`, and unported `CM_TOGGLE_SKILL_DEACTIVATE` as candidates.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
