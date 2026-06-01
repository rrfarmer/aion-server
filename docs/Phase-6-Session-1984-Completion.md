# Phase 6 Session 1984 Completion - CM_REMOVE_ALTERED_STATE Parser Boundary

Date: 2026-06-01
Unit of Work: UOW-1984
Status: Completed

## What Changed

- Reviewed Java `CM_REMOVE_ALTERED_STATE.readImpl`, where unsigned skill id is followed by two trailing byte fields.
- Added C# `CmRemoveAlteredState` with Java-shaped unsigned `SkillId`, `Unknown1`, and `Unknown2` parsing.
- Registered C# opcode `35` as `IN_GAME` only, matching Java `AionClientPacketFactory`.
- Added a parser-only runtime boundary in `GameServerConnection` documenting that live EffectController altered-state removal is still unported.
- Added C# parser/factory tests and a Java golden test covering high-bit unsigned skill id and full payload consumption.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmRemoveAlteredStateTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_REMOVE_ALTERED_STATE_ReadUnsignedSkillGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# remove-altered-state parser/factory slice passed with 2 tests.
- Focused Java `CM_REMOVE_ALTERED_STATE_ReadUnsignedSkillGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5057 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 41 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REMOVE_ALTERED_STATE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- C# source reviewed: existing EffectController-related diagnostic surfaces under `dotnetConversion/src/Aion.GameServer`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for EffectController lookup, debuff audit logging, or ending non-debuff effects.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_REMOVE_ALTERED_STATE_ReadUnsignedSkillGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmRemoveAlteredState.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmRemoveAlteredStateTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1984-Completion.md`
- `docs/Phase-6-Session-1984-Handoff.md`

## Remaining Risks

- Java `CM_REMOVE_ALTERED_STATE.runImpl` EffectController lookup, debuff guard/audit, `effect.endEffect()`, encrypted frame capture, and real-client validation remain unverified.
- The C# port currently parses and registers the packet only; it intentionally does not mutate live effect state.
- Other unported client-packet parser and runtime boundaries remain separate work.

## Next Recommended Unit

- Continue Work Discovery for unported compact client packets, with `CM_BUY_TRADE_IN_TRADE`, `CM_HOUSE_SCRIPT`, or `CM_VERSION_CHECK` as candidates if they can remain parser/factory-focused with Java evidence.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
