# Phase 6 Session 1975 Completion - CM_SPLIT_ITEM Signed Slot

Date: 2026-06-01
Unit of Work: UOW-1975
Status: Completed

## What Changed

- Reviewed Java `CM_SPLIT_ITEM.readImpl`, where `slotNum` is read with signed `readH()`.
- Added parser-only C# `CmSplitItem`.
- Registered opcode `157` as `IN_GAME`, matching Java `AionClientPacketFactory`.
- Matched Java packet field order for source item, amount, source storage, destination item, destination storage, and signed slot.
- Added an explicit parser-only no-op boundary in `GameServerConnection` for Java `CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem`.
- Added Java golden and C# parser/factory tests for high-bit slot values reading as signed shorts.
- Kept this parser-only. No stack split side effects were enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmSplitItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_SPLIT_ITEM_ReadSignedSlotGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# split-item parser/factory slice passed with 2 tests.
- Focused Java `CM_SPLIT_ITEM_ReadSignedSlotGoldenTest` passed with 1 test method.
- Broad C# game-server suite passed with 5039 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 32 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_SPLIT_ITEM.runImpl` or `ItemSplitService`.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM_ReadSignedSlotGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmSplitItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmSplitItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1975-Completion.md`
- `docs/Phase-6-Session-1975-Handoff.md`

## Remaining Risks

- Live stack splitting, item-count validation, storage selection, DB persistence, packet dispatch, encrypted frame capture, and real-client validation remain unverified.
- C# opcode `157` is intentionally parser-only until `ItemSplitService` parity is ported.
- Other Java signed `readH()` call sites remain separate work.

## Next Recommended Unit

- Continue the signed Java `readH()` audit by scanning remaining client packet call sites and selecting the smallest surface with either an existing C# parser or a safe parser-only registration.

Safe alternatives:

- Inspect ignored-padding signedness candidates such as `CM_APPEARANCE`, `CM_HOUSE_KICK`, `CM_MANASTONE`, `CM_QUESTION_RESPONSE`, or `CM_PING`.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
