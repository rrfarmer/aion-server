# Phase 6 Session 1985 Completion - CM_BUY_TRADE_IN_TRADE Parser Boundary

Date: 2026-06-01
Unit of Work: UOW-1985
Status: Completed

## What Changed

- Reviewed Java `CM_BUY_TRADE_IN_TRADE.readImpl`, where seller object id, mask, item id, count, unsigned trade-in list count, and trade-in item object ids are read.
- Added C# `CmBuyTradeInTrade` with Java-shaped parser fields and ordered trade-in item object id storage.
- Registered C# opcode `88` as `IN_GAME` only, matching Java `AionClientPacketFactory`.
- Added a parser-only runtime boundary in `GameServerConnection` documenting that live trade-in service execution is still unported.
- Added C# parser/factory tests and a Java golden test covering trade-in list count/object id consumption.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyTradeInTradeTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_TRADE_IN_TRADE_ReadUnsignedCountGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# buy-trade-in parser/factory slice passed with 2 tests.
- Focused Java `CM_BUY_TRADE_IN_TRADE_ReadUnsignedCountGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5059 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 42 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_TRADE_IN_TRADE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `TradeService.performBuyFromTradeInTrade`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_TRADE_IN_TRADE_ReadUnsignedCountGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyTradeInTrade.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyTradeInTradeTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1985-Completion.md`
- `docs/Phase-6-Session-1985-Handoff.md`

## Remaining Risks

- Java `CM_BUY_TRADE_IN_TRADE.runImpl` `count < 1` guard, target/trade validation, inventory mutation, persistence, packet sends, encrypted frame capture, and real-client validation remain unverified.
- The C# port currently parses and registers the packet only; it intentionally does not execute live trade-in behavior.
- Other unported client-packet parser and runtime boundaries remain separate work.

## Next Recommended Unit

- Continue Work Discovery for unported compact client packets, with `CM_HOUSE_SCRIPT` or `CM_VERSION_CHECK` as candidates if they can remain evidence-backed and safely scoped.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
