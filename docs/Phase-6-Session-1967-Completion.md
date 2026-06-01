# Phase 6 Session 1967 Completion - CM_BUY_ITEM Unsigned Amount Guard

Date: 2026-06-01
Unit of Work: UOW-1967
Status: Completed

## What Changed

- Added Java golden coverage for `CM_BUY_ITEM.readImpl` when the `amount` field is encoded as `0xFFFF`.
- Added matching C# `CmBuyItem` parser coverage for the same high-bit amount payload.
- Confirmed Java `BaseClientPacket.readUH()` reads the field as unsigned, so `0xFFFF` becomes `65535`, triggers the amount audit guard, and returns before list creation or item reads.
- Confirmed C# already matches this amount behavior through unsigned `PacketBuffer.ReadH()` for this caller; no production runtime code was changed.
- This unit is parser-only. No live buy/sell/private-store/repurchase execution, audit logging side effects, packet dispatch, transaction, encrypted frame capture, or real-client validation was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmBuyItemTests|FullyQualifiedName~CmBuyItemHandlerCompositionPlanServiceTests|FullyQualifiedName~CmBuyItemSideEffectOutcomePlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_BUY_ITEM_ReadGuardGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `mvn test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# buy-item parser/composition slice passed with 61 tests.
- Focused Java `CM_BUY_ITEM_ReadGuardGoldenTest` passed with 8 test methods.
- Broad C# game-server suite passed with 5020 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 24 game-server tests.
- Full Maven reactor passed in the current workspace. This run did not force a clean login-server recompile.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM.java`.
- Java primitive reviewed: `commons/src/com/aionemu/commons/network/packet/BaseClientPacket.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBuyItem.cs` and `dotnetConversion/src/Aion.Commons/Network/PacketBuffer.cs`.
- Objective evidence is limited to Java golden test, C# unit test, broad C# tests, and Maven test runs.
- No verified live parity is claimed for audit logging, encrypted socket frames, malformed network behavior, or downstream buy/sell execution.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_BUY_ITEM_ReadGuardGoldenTest.java`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBuyItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1967-Completion.md`
- `docs/Phase-6-Session-1967-Handoff.md`

## Remaining Risks

- This unit verifies only `CM_BUY_ITEM.amount` high-bit parsing and guard behavior.
- Java signed `readH()` call sites are not covered by this unit; C# `PacketBuffer.ReadH()` is unsigned and may need separate audits where high-bit signed values matter.
- `tradeActionId` high-bit/signedness behavior remains unverified.
- Live `CM_BUY_ITEM` execution remains partial or disabled in several gameplay branches.

## Next Recommended Unit

- Inspect Java delete-path cube-size sends for a non-repurchase inventory diagnostic where `sendItemDeletePacket` is already represented by a C# planner, then add focused source-reviewed tests without enabling live mutation.

Safe alternatives:

- Inspect live `CM_PET` actionType 4 composition only if it can remain disabled and source-reviewed.
- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Audit signed Java `readH()` call sites where C# currently uses unsigned `PacketBuffer.ReadH()`.
- Run a clean Maven validation if the prior login-server `PlayerTransferService.java:42` compile observation needs root-cause proof.
