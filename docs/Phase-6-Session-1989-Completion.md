# Phase 6 Session 1989 Completion - SM_VERSION_CHECK Incompatible-Version Branch

Date: 2026-06-01
Unit of Work: UOW-1989
Status: Completed

## What Changed

- Reviewed Java `SM_VERSION_CHECK.writeImpl` and selected its deterministic incompatible-client branch.
- Added C# `SmVersionCheck` opcode `0` with Java `InternalVersion = 207`.
- Ported the Java branch that writes answer id `1` when the client version does not equal 207.
- Wired `CM_VERSION_CHECK` handling to send this deterministic response for incompatible versions only.
- Kept the success branch as an explicit runtime boundary because Java success serialization depends on dynamic config/time/chat-server/ratio/passport/event-theme state.
- Added C# packet tests and a Java golden test for the incompatible-version payload.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmVersionCheckTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_VERSION_CHECK_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# `SmVersionCheck` slice passed with 2 tests.
- Focused Java `SM_VERSION_CHECK_GoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5075 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 47 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_VERSION_CHECK.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_VERSION_CHECK.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerPacket.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# packet tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for the success `SM_VERSION_CHECK` payload.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_VERSION_CHECK_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmVersionCheck.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmVersionCheckTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1989-Completion.md`
- `docs/Phase-6-Session-1989-Handoff.md`

## Remaining Risks

- Java success response bytes, dynamic server config, start/current time, chat-server IP/port, ratio limitation, passport state, event theme, encrypted frame capture, and real-client validation remain unverified.
- C# success serialization throws an explicit `NotSupportedException` until that branch is ported with evidence.

## Next Recommended Unit

- Continue Work Discovery for a deterministic `SM_VERSION_CHECK` success sub-slice only if Java bytes can be produced by controlling dynamic dependencies, or choose another compact planner/parser/model boundary with Java golden evidence.

Safe alternatives:

- Inspect another compact unported parser/factory or enum/model dependency with Java golden evidence.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
