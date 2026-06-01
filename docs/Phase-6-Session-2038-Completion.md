# Phase 6 Session 2038 Completion - Group Data Exchange Socket Adapter Boundary

Date: 2026-06-01
Unit of Work: UOW-2038
Status: Completed

## Work Discovery

- Re-read the latest UOW-2037 completion and handoff before choosing work.
- Confirmed the worktree was clean after UOW-2037.
- Inspected Java `CM_GROUP_DATA_EXCHANGE.runImpl`.
- Inspected Java `PacketSendUtility.sendPacket` and `broadcastPacketAndReceive`.
- Reviewed C# `IGameClientConnectionRegistry`, the deferred `GameServerConnection` branch, and existing disabled/opt-in adapter patterns.

## What Changed

- Added `GroupDataExchangeFanoutSocketAdapterService`.
- Kept the adapter disabled by default and not wired into production `GameServerConnection`.
- Mapped action `1` plans to `BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)` when explicitly enabled.
- Mapped team recipient plans to `SendPacketToPlayerAsync` in plan order when explicitly enabled.
- Added tests for disabled no-send behavior, enabled nearby broadcast, enabled group direct sends, missing recipient connections, no-packet plans, and enabled-without-registry handling.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GroupDataExchangeFanoutSocketAdapterServiceTests|FullyQualifiedName~GroupDataExchangeFanoutPlanServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_GROUP_DATA_EXCHANGE_ReadPayloadGoldenTest,SM_GROUP_DATA_EXCHANGE_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmGroupDataExchangeTests|FullyQualifiedName~GroupDataExchangeFanoutPlanServiceTests|FullyQualifiedName~GroupDataExchangeFanoutSocketAdapterServiceTests" --no-restore`
- `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false"`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused C# planner/adapter tests passed with 14 tests.
- Focused Java `CM_GROUP_DATA_EXCHANGE` parser and `SM_GROUP_DATA_EXCHANGE` writer tests passed with 4 test methods.
- Focused C# group-data writer/planner/adapter tests passed with 17 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 111 game-server tests.
- Broad C# game-server suite passed with 5163 tests.

## Parity Notes

- Java source reviewed: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_GROUP_DATA_EXCHANGE.java`.
- Java source reviewed: `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`.
- The adapter's nearby branch models Java `broadcastPacketAndReceive(player, packet)` through C# visible-player broadcast with source included.
- The adapter's team branch models the Java `for (Player member : players) if (!member.equals(player)) PacketSendUtility.sendPacket(member, packet)` loop through planned recipient IDs.
- No verified live parity is claimed for production dispatch, socket encryption, persistent known-list membership, real-client behavior, or disconnect/reconnect edge cases.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/GroupDataExchangeFanoutSocketAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GroupDataExchangeFanoutSocketAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2038-Completion.md`
- `docs/Phase-6-Session-2038-Handoff.md`

## Remaining Risks

- `GameServerConnection` still defers `CmGroupDataExchange`.
- The adapter is opt-in only; production sends are not enabled.
- Known-list fanout uses C# visible-player registry behavior when enabled, not Java persistent known-list membership.
- Java online-member filtering and real socket availability remain only approximated by registry return values.
- Encrypted-frame ordering and real-client behavior remain unverified.

## Next Recommended Unit

- Inspect whether a guarded `GameServerConnection` composition seam can create planner+disabled-adapter evidence for `CmGroupDataExchange` without enabling production sends.

Safe alternatives:

- Add Java-side runImpl test doubles for `CM_GROUP_DATA_EXCHANGE` routing if feasible.
- Inspect `SM_GROUP_MEMBER_INFO` or nearby alliance packet writer gaps with Java packet evidence.
- Return to a narrow non-live `FindGroupService` planner with service mutation and broadcast side effects deferred.
