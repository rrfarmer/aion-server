# Phase 6 Session 1978 Completion - CM_QUESTION_RESPONSE Signed Padding

Date: 2026-06-01
Unit of Work: UOW-1978
Status: Completed

## What Changed

- Reviewed Java `CM_QUESTION_RESPONSE.readImpl`, where two ignored padding fields are consumed with signed `readH()`.
- Updated C# `CmQuestionResponse` to consume both padding fields with `PacketBuffer.ReadSignedH()`.
- Added C# parser/factory tests covering opcode `50` state gating and high-bit padding around meaningful question-response fields.
- Added a Java golden test proving high-bit padding leaves `questionid`, `response`, `senderid`, and remaining-byte consumption Java-shaped.
- Kept this parser-focused. No live question-response dispatch behavior was newly claimed.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmQuestionResponseSignedPaddingTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_QUESTION_RESPONSE_ReadSignedPaddingGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`

Results:

- Focused C# question-response padding parser/factory slice passed with 2 tests.
- Focused Java `CM_QUESTION_RESPONSE_ReadSignedPaddingGoldenTest` passed with 1 test method.
- First broad C# game-server suite run failed in unrelated `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` with duplicate `SmMove` broadcasts.
- Focused retry of the unrelated failing walker-route test passed with 1 test.
- Broad C# game-server suite rerun passed with 5045 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 35 game-server tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmQuestionResponse.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`.
- C# source reviewed: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- Objective evidence is limited to Java golden test, C# parser/factory tests, broad C# tests, and Maven game-server reactor tests.
- No verified live parity is claimed for `CM_QUESTION_RESPONSE.runImpl`, `ExchangeService.cancelExchange`, or generic `ResponseRequester` callback behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE_ReadSignedPaddingGoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmQuestionResponse.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmQuestionResponseSignedPaddingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1978-Completion.md`
- `docs/Phase-6-Session-1978-Handoff.md`

## Remaining Risks

- Live response-requester behavior, exchange cancellation, typed pending-request dispatch, packet fanout, encrypted frame capture, and real-client validation remain unverified.
- Because Java discards both padding values, the evidence proves field alignment and full read consumption rather than observable signed values.
- The first broad C# run exposed a transient unrelated walker-route duplicate-broadcast failure; it passed in focused retry and broad rerun but should be watched.
- Other Java signed `readH()` call sites remain separate work.

## Next Recommended Unit

- Continue the signed Java `readH()` audit by inspecting another ignored-padding or parser-only call site, with `CM_MANASTONE`, `CM_PING`, `CM_TELEPORT_SELECT`, `CM_UI_SETTINGS`, and unported `CM_TOGGLE_SKILL_DEACTIVATE` as candidates.

Safe alternatives:

- Inspect BUY_AGAIN live-send ordering only if a deterministic Java-side packet vector can be added safely.
- Continue private-store diagnostics by isolating Java `LinkedHashMap` ordering/store mutation timing if a deterministic non-live fixture can be built.
- Inspect another Java delete-path cube-size caller outside craft to ensure Kinah/storage-count assumptions remain scoped correctly.
- Inspect `CM_PET` actionType `3` autoloot composition only if it can remain disabled and source-reviewed.
