# Phase 6 Session 2412 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2412`: Added leave-world non-dead duel-loss branch wiring and tests.

## Commits Made
- `[Phase 6][UOW-2412] Align leave-world duel loss`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionDuelRequestTests.cs`
- `docs/Phase-6-Session-2412-Completion.md`
- `docs/Phase-6-Session-2412-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.DuelService`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.PlayerDuelRequestService`
- `Aion.GameServer.Tests.GameServerConnectionDuelRequestTests`
- Adjacent validation: `Aion.GameServer.Tests.GameServerConnectionKiskReviveWorkflowTests`

## What Changed
- Leave-world now uses the Java branch shape after kisk logout:
  - dead players run logout revive;
  - non-dead dueling players lose the duel through `PlayerDuelRequestService.LoseDuel`.
- Duel-loss packet intents are sent through the existing duel packet path.
- Tests cover non-dead logout duel loss and dead logout suppressing duel loss.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionDuelRequestTests|FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests" --no-restore`
  - Result: 25 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused duel/logout and adjacent dead-logout workflow tests passed; broad trigger was production connection/runtime-state behavior, but focused evidence isolated the change.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Kisk offline binding, dead-player revive branch, and non-dead duel-loss branch are now pinned. Full logout sequence remains partial. |
| `com.aionemu.gameserver.services.DuelService` | `Aion.GameServer.Services.PlayerDuelRequestService` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Logout reaches modeled lose-duel packet/removal behavior. Java effect cleanup, summoned-object cancellation, and draw-task cancellation remain incomplete. |

## Known Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.
- Duel end side effects are incomplete beyond result packets and duel-map cleanup.
- Soul sickness and special revive destinations from `PlayerReviveService` remain incomplete.
- Inventory/warehouse/account-warehouse owner cleanup at the end of Java leave-world remains unpinned.
- Replacement readiness still needs broader real-client gameplay coverage beyond this workflow slice.

## Next Recommended UOW
- `UOW-2413`: Continue leave-world cleanup with request/response cancellation ordering around `player.getResponseRequester().denyAll()` and already-modeled pending request fields, or choose a narrower modeled logout cleanup hook if source review shows `denyAll` is too broad.

## Suggested Discovery For UOW-2413
- Java:
  - `PlayerLeaveWorldService.leaveWorld(Player player)`
  - `ResponseRequester.denyAll()`
  - Request handlers for duel, group, alliance, exchange, recall, teleport, and kisk pending questions.
- C#:
  - `GameServerConnection.LeavePlayerWorldAsync(...)`
  - `QuestionResponseRegistry`
  - `PlayerEnterWorldService` pending request cleanup around logout/persistence.
  - Tests for duel, group/alliance invite, exchange, teleport-to-npc, kisk bind, and question response cleanup.

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: leave-world clears or denies modeled pending question requests in Java ordering before later friend/offline/world removal effects, without over-claiming unmodeled request handlers.
- Focused C# command:
  - Start with `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestionResponse|FullyQualifiedName~GameServerConnectionDuelRequestTests|FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests" --no-restore`
  - If that filter is too broad, narrow to the edited logout workflow test plus the closest request/response registry test class.
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: production connection/request-state cleanup may apply if live pending-request cleanup is wired; still start focused.

## Safe Candidate UOWs
- Add FindGroup logout cleanup connection wiring if source review identifies a narrow already-modeled service hook.
- Audit owner/null cleanup for inventory, warehouse, and account warehouse at the end of Java leave-world.
- Continue revive logout parity for instance-handler `onReviveEvent` only if a narrow modeled handler hook exists.
