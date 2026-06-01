# Phase 6 Session 2000 Handoff - Summon Command Parser Golden

Date: 2026-06-01
Unit of Work: UOW-2000
Status: Completed

## What Changed

- Added Java golden parser coverage for `CM_SUMMON_COMMAND.readImpl`.
- Added C# `CmSummonCommand` and registered opcode `121` for `InGame`.
- Added C# factory/parser coverage for unsigned mode parsing with high-bit value `255`, two D padding fields, and target object id.
- Added a documented no-op handler boundary in `GameServerConnection`; live summon command handling remains unported.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_SUMMON_COMMAND_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesSummonCommand" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused Java `CM_SUMMON_COMMAND` parser golden passed with 1 test method.
- Focused C# summon-command factory test passed with 1 test.
- Broad C# game-server suite passed with 5107 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 58 game-server tests.

## Known Gaps

- This unit proves only parser field consumption and opcode registration.
- C# does not execute Java `CM_SUMMON_COMMAND.runImpl` live behavior: active summon lookup, `SummonMode` mapping, `SummonsService.doMode`, release scheduling, summon update sends, or live state mutation.
- Encrypted frame capture, real-client behavior, threading, and live mode-dispatch parity remain unverified.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_SUMMON_COMMAND_ReadPayloadGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmSummonCommand.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2000-Completion.md`
- `docs/Phase-6-Session-2000-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_SUMMON_COMMAND`
- `com.aionemu.gameserver.network.aion.AionClientPacketFactory`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmSummonCommand`
- `Aion.GameServer.Network.Aion.GameClientPacketFactory`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Tests.GamePacketTests`

## Parity Table Updates

- Added Session 2000 rows to `PHASE-6-PROGRESS.md` for:
  - `CM_SUMMON_COMMAND.readImpl` parser field order and unsigned mode semantics
  - `AionClientPacketFactory` opcode `121` registration
  - `CM_SUMMON_COMMAND.runImpl` no-op live handler boundary

## Next Recommended Unit of Work

- Next sequential task: choose another compact parser/factory/model boundary with Java golden evidence. The summon opcode cluster now has parser/factory coverage for command, movement, emotion, attack, and cast-spell.

Safe alternative candidates:

- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect private-store live side effects only as read-only readiness reporting, not mutation wiring.
- Inspect BUY_AGAIN live-send ordering only if a stronger deterministic Java runtime vector can be added without broad object graph setup.
- Inspect another compact unported enum/model dependency with Java golden evidence.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Do not claim live summon command parity from UOW-2000; only parser/factory parity is backed by objective evidence.
- UOW-1998 covered `CM_SUMMON_MOVE`, UOW-1999 covered `CM_SUMMON_EMOTION`, and UOW-2000 covered `CM_SUMMON_COMMAND` parser/factory coverage.
