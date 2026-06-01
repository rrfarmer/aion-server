# Phase 6 Session 1971 Completion - CM_BUY_ITEM Signed Trade Action

Date: 2026-06-01
Unit of Work: UOW-1971
Status: Completed

## What Changed

- Reviewed Java `CM_BUY_ITEM.readImpl`, where `tradeActionId` is a signed `short` read with `readH()` and `amount` is an unsigned `readUH()`.
- Updated C# `CmBuyItem` so `TradeActionId` uses `PacketBuffer.ReadSignedH()`.
- Left `Amount` on unsigned `PacketBuffer.ReadH()`, preserving Java `readUH()` behavior already covered by UOW-1967.
- Added Java golden and C# parser tests for `tradeActionId = 0xFFFF`, `amount = 0`.
- This unit remains parser-only. No buy/sell/private-store/repurchase execution, audit logging side effects, packet dispatch, encrypted frame capture, or real-client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# buy-item parser slice passed with 28 tests.
- Focused Java `CM_BUY_ITEM_ReadGuardGoldenTest` passed with 9 test methods.
- Broad C# game-server suite passed with 5029 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 27 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`.
- Java source reviewed: `commons/src/com/aionemu/commons/network/packet/BaseClientPacket.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.Commons/Network/PacketBuffer.cs`.
- Objective evidence is limited to Java golden test, C# parser test, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_BUY_ITEM.runImpl`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1971-Completion.md`
- `docs/Phase-6-Session-1971-Handoff.md`

## Remaining Risks

- Live `CM_BUY_ITEM` target resolution and branch dispatch remain partial/diagnostic in several paths.
- Unsupported negative action runtime behavior was not compared beyond parser state with zero amount.
- Other Java signed `readH()` call sites remain separate work.
- Full Maven reactor validation was not rerun in this unit; only the game-server reactor was run.

## Next Recommended Unit

- Continue the signed Java `readH()` audit with a low-risk skipped/ignored field or `CM_MOVE_ITEM.slot` once its C# parser/runtime surface exists.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
- Run a full Maven reactor validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
