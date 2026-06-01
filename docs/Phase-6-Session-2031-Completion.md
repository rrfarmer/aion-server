# Phase 6 Session 2031 Completion - Find Group Prepare Window Packet Slice

Date: 2026-06-01
Unit of Work: UOW-2031
Status: Completed

## Work Discovery

- Re-read the required migration, orchestration, parity, Phase 6 progress, and latest handoff/completion documents before choosing work.
- Inspected Java `SM_FIND_GROUP.writeImpl` actions `23` and `24`.
- Inspected Java `FindGroupService` call sites for current live action usage.
- Reviewed Java `ServerWideGroup` and the current reflected Java golden setup.
- Reviewed current C# `SmFindGroup` writer and tests.

## What Changed

- Added Java golden payload vectors for `SM_FIND_GROUP` action `23`, `destroyPrepareForEntryWindow`, and action `24`, `updatePrepareForEntryWindow`.
- Added C# `SmFindGroup.DestroyPrepareForEntryWindow` and `SmFindGroup.UpdatePrepareForEntryWindow` factories.
- Added C# prepare-window member snapshot DTOs for the action `24` serialized fields.
- Added C# byte-for-byte tests matching the Java-emitted payloads.

## Validation

Executed:

- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_FIND_GROUP_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmFindGroupTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Java `SM_FIND_GROUP` golden test passed with 9 test methods.
- Focused C# `SmFindGroup` test passed with 10 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 105 game-server tests.
- Broad C# game-server suite passed with 5142 tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/model/gameobjects/findGroup/ServerWideGroup.java`.
- Objective byte evidence now covers `SM_FIND_GROUP` actions `1`, `5`, `11`, `14`, `18`, `22`, `23`, `24`, and `26`.
- The action `23` vector covers the false enter-message flag path through the `action, entries` constructor shape.
- The action `24` vector covers one deterministic offline member with admin name tags disabled.
- No verified live parity is claimed for `FindGroupService` dispatch, world broadcasts, encrypted frames, socket dispatch, or real-client behavior.

## Files Changed

- `game-server/test/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP_GoldenTest.java`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmFindGroupTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2031-Completion.md`
- `docs/Phase-6-Session-2031-Handoff.md`

## Remaining Risks

- `SM_FIND_GROUP` actions `0`, `4`, `10`, and `16` remain unported.
- Action `23` true-flag payload is source-reviewed only; Java's boolean constructor does not populate `entries`, so no current normal constructor path produces that payload with an instance group.
- Action `24` multi-member ordering, online-state derivation, and team-backed member lookup remain unverified.
- Live find-group service calls and packet fanout remain deferred.

## Next Recommended Unit

- Inspect `SM_FIND_GROUP` action `16` member-info writer with a minimal member snapshot and conservative handling of the current-time header.

Safe alternatives:

- Inspect action `10` instance-group list with parsed timestamp-header assertions.
- Inspect actions `0`/`4` recruitment/application lists if their timestamp headers can be asserted conservatively.
- Inspect `SM_GROUP_DATA_EXCHANGE` writer parity before any live group-data fanout work.
