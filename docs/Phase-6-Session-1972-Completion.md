# Phase 6 Session 1972 Completion - CM_ATREIAN_PASSPORT Signed Count

Date: 2026-06-01
Unit of Work: UOW-1972
Status: Completed

## What Changed

- Reviewed Java `CM_ATREIAN_PASSPORT.readImpl`, where `count = readH()` is signed and `count == -1` is a sentinel that consumes complete passport pairs until bytes run out.
- Added parser-only C# `CmAtreianPassport`.
- Registered opcode `248` as `IN_GAME`, matching Java `AionClientPacketFactory`.
- Added Java golden and C# parser/factory tests for:
  - `count = 0xFFFF` -> signed `-1`
  - duplicate passport IDs grouped into timestamp sets
  - trailing incomplete data ignored for the sentinel path
  - positive count consuming only the declared number of entries
- Kept this parser-only. No Atreian passport reward execution or account persistence was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_ATREIAN_PASSPORT_ReadSignedCountGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# Atreian passport parser/factory slice passed with 3 tests.
- Focused Java `CM_ATREIAN_PASSPORT_ReadSignedCountGoldenTest` passed with 2 test methods.
- Broad C# game-server suite passed with 5032 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 29 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.Commons/Network/PacketBuffer.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_ATREIAN_PASSPORT.runImpl` or `AtreianPassportService.takeReward`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT_ReadSignedCountGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmAtreianPassport.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1972-Completion.md`
- `docs/Phase-6-Session-1972-Handoff.md`

## Remaining Risks

- Live Atreian passport reward execution, account passport mutation, database persistence, item reward delivery, audit logging, and response packets remain unported/unverified.
- Java invalid positive-count warning/logging behavior is not modeled in C#.
- Opcode `248` now parses in C#, but `GameServerConnection` still has no live reward handler.
- Other Java signed `readH()` call sites remain separate work.

## Next Recommended Unit

- Continue the signed Java `readH()` audit with `CM_MOVE_ITEM.slot` / opcode `156` if a parser-only C# packet and Java golden test can be added safely without enabling inventory move side effects.

Safe alternatives:

- Inspect `CM_LEGION` signed permission fields as parser-only coverage.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
