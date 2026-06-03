# Phase 6 Session 2413 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2413`: Added connection-boundary coverage for leave-world pending question denial.

## Commits Made
- `[Phase 6][UOW-2413] Cover leave-world pending request denial`

## Files Changed
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
- `docs/Phase-6-Session-2413-Completion.md`
- `docs/Phase-6-Session-2413-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.utils.response.ResponseRequester`
- `com.aionemu.gameserver.utils.response.RequestResponseHandler`

## C# Artifacts Touched
- `Aion.GameServer.Tests.GameServerConnectionKiskReviveWorkflowTests`
- Adjacent validation:
  - `Aion.GameServer.Services.QuestionResponseRegistry`
  - `Aion.GameServer.Services.PlayerEnterWorldService`

## What Changed
- The kisk/logout workflow fixture can now inject a `PlayerEnterWorldService`.
- A new connection-boundary test proves `GameServerConnection.LeavePlayerWorldAsync(...)` reaches `PlayerEnterWorldService.LeaveWorldAsync(...)` and therefore executes the modeled Java `ResponseRequester.denyAll()` cleanup.
- No production code changed in this UOW.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~QuestionResponseRegistryTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`
  - Result: 62 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped because this UOW changed only tests and focused validation covered the edited workflow plus adjacent service/registry behavior.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Kisk offline binding, dead-player revive, non-dead duel loss, and service-backed pending-question denial are now pinned. Full logout sequence remains partial. |
| `com.aionemu.gameserver.utils.response.ResponseRequester` | `Aion.GameServer.Services.QuestionResponseRegistry` | Runtime request registry | Partial | Unit Tested / Regression Tested | Partial Parity | `DenyAll()` clears request entries and exposes denial dispatches for modeled handlers. |
| `com.aionemu.gameserver.utils.response.RequestResponseHandler` | `Aion.GameServer.Services.PlayerEnterWorldService` | Logout cleanup bridge | Partial | Regression Tested | Partial Parity | Modeled denial side effects and typed pending-slot cleanup are covered at service level, with friend-invite coverage at the connection boundary. |

## Known Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.
- Pending-request side effects for all modeled request kinds are not all covered at the connection boundary.
- Unmodeled Java request handlers still need narrow review before parity can be claimed.
- Duel end side effects remain incomplete beyond result packets and duel-map cleanup.
- Soul sickness and special revive destinations from `PlayerReviveService` remain incomplete.
- Inventory/warehouse/account-warehouse owner cleanup at the end of Java leave-world remains unpinned.

## Next Recommended UOW
- `UOW-2414`: Audit inventory, warehouse, and account-warehouse owner/null cleanup at the end of Java leave-world, then pin the narrowest already-modeled C# behavior.

## Suggested Discovery For UOW-2414
- Java:
  - `PlayerLeaveWorldService.leaveWorld(Player player)`
  - `player.getInventory().setOwner(null)`
  - `player.getWarehouse().setOwner(null)`
  - `player.getAccountWarehouse().setOwner(null)`
  - Inventory/warehouse owner classes and any logout persistence assumptions around these calls.
- C#:
  - `Player` inventory/warehouse/account warehouse representations.
  - `PlayerEnterWorldService.LeaveWorldAsync(...)`
  - logout persistence tests around inventory, warehouse, and account warehouse.
  - any owner/reference cleanup helpers already present in inventory or storage services.

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: Java leave-world clears inventory/warehouse/account-warehouse owner references after world removal/persistence-relevant state is handled, but only for C# state that is actually modeled.
- Focused C# command:
  - Start with `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepository|FullyQualifiedName~Inventory" --no-restore`
  - Narrow if the filter is too broad after discovery.
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use source review by default.
- Broad-validation trigger: production logout persistence or inventory ownership mutation would trigger broad consideration; start focused either way.

## Safe Candidate UOWs
- Continue pending-request connection-boundary tests for another high-value modeled request kind if owner cleanup proves unmodeled.
- Add FindGroup logout cleanup connection wiring only if source review identifies a narrow already-modeled service hook.
- Continue revive logout parity for instance-handler `onReviveEvent` only if a narrow modeled handler hook exists.
