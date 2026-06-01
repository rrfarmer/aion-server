# Phase 6 Session 1989 Handoff - SM_VERSION_CHECK Incompatible-Version Branch

Date: 2026-06-01
Unit of Work: UOW-1989
Status: Completed

## What Changed

- Added C# `SmVersionCheck` for Java server opcode `0`.
- Added Java `InternalVersion = 207` constant in C#.
- Ported Java `SM_VERSION_CHECK.writeImpl` incompatible-client branch: non-207 versions serialize answer id `1` and return.
- Wired `CM_VERSION_CHECK` handling to send this deterministic incompatible-version response.
- Kept the success branch as an explicit unported boundary because it needs dynamic config, time, chat-server, ratio, passport, and event-theme state.
- Added C# packet coverage and Java golden coverage for the incompatible-version payload.

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

## Known Gaps

- This unit proves only the incompatible-client `SM_VERSION_CHECK` branch.
- Java success `SM_VERSION_CHECK` serialization, dynamic config/time/chat-server/ratio/passport/event-theme data, encrypted frame capture, and real-client validation remain unverified.
- C# success serialization currently throws `NotSupportedException` to prevent accidental incorrect success handshakes.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_VERSION_CHECK_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmVersionCheck.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmVersionCheckTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1989-Completion.md`
- `docs/Phase-6-Session-1989-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.serverpackets.SM_VERSION_CHECK`
- `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_VERSION_CHECK`
- `com.aionemu.gameserver.model.EventTheme`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ServerPackets.SmVersionCheck`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Model.EventTheme`
- `Aion.GameServer.Tests.SmVersionCheckTests`

## Parity Table Updates

- Added Session 1989 rows to `PHASE-6-PROGRESS.md` for:
  - `SM_VERSION_CHECK.INTERNAL_VERSION`
  - `SM_VERSION_CHECK.writeImpl` incompatible-version branch
  - server opcode `0`
  - `CM_VERSION_CHECK.runImpl` incompatible-version response boundary

## Next Recommended Unit of Work

- Next sequential task: continue Work Discovery for a deterministic `SM_VERSION_CHECK` success sub-slice only if Java bytes can be produced by controlling dynamic dependencies, or choose another compact planner/parser/model boundary with Java golden evidence.

Safe alternative candidates:

- Inspect another compact unported parser/factory or enum/model dependency with Java golden evidence.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim full `SM_VERSION_CHECK` parity from UOW-1989; only the incompatible-version branch is covered.
- The success branch should only be ported when dynamic Java dependencies can be controlled or represented with explicit packet snapshot inputs.
