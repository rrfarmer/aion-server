# Phase 6 Session 2038 Handoff - Group Data Exchange Socket Adapter Boundary

Date: 2026-06-01
Unit of Work: UOW-2038
Status: Completed

## What Changed

- Added an opt-in `GroupDataExchangeFanoutSocketAdapterService` for UOW-2037 group-data fanout plans.
- The adapter is disabled by default and is not wired into `GameServerConnection`.
- Enabled nearby action `1` execution calls `BroadcastToVisiblePlayersAsync` with source included.
- Enabled team execution sends planned recipients in order through `SendPacketToPlayerAsync`.
- Added tests for disabled, enabled, missing-registry, no-packet, and missing-connection outcomes.

## Validation

- Focused C# planner/adapter tests passed with 14 tests.
- Focused Java `CM_GROUP_DATA_EXCHANGE` parser and `SM_GROUP_DATA_EXCHANGE` writer tests passed with 4 test methods.
- Focused C# group-data writer/planner/adapter tests passed with 17 tests.
- Java/Maven commons plus game-server reactor passed with 1 commons test and 111 game-server tests.
- Broad C# game-server suite passed with 5163 tests.

## Known Gaps

- This is opt-in adapter boundary evidence only. `GameServerConnection` still does not dispatch `CmGroupDataExchange`.
- No production socket dispatch, encrypted-frame validation, real-client behavior, or persistent Java known-list parity is proven.
- Java `Player.isOnline()` and client-connection availability are approximated by registry results.
- Nearby fanout relies on the current C# visible-player registry if enabled, which is not the same as Java's persistent known-list membership.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/GroupDataExchangeFanoutSocketAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GroupDataExchangeFanoutSocketAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2038-Completion.md`
- `docs/Phase-6-Session-2038-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect whether a guarded `GameServerConnection` composition seam can create planner+disabled-adapter evidence for `CmGroupDataExchange` without enabling production sends.

Safe alternative candidates:

- Add Java-side runImpl test doubles for `CM_GROUP_DATA_EXCHANGE` routing if feasible.
- Inspect `SM_GROUP_MEMBER_INFO` or nearby alliance packet writer gaps with Java packet evidence.
- Return to a narrow non-live `FindGroupService` planner with service mutation and broadcast side effects deferred.

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, this completion document, and this handoff before choosing the next UOW.
- Treat UOW-2036 as packet-writer evidence, UOW-2037 as planner evidence, and UOW-2038 as opt-in adapter boundary evidence only.
- Do not claim live `CM_GROUP_DATA_EXCHANGE` parity until `GameServerConnection` dispatch, known-list/team online filtering, socket encryption/frame order, and real-client behavior have objective coverage.
