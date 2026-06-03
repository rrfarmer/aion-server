# Phase 6 Session 2414 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2414`: Audited Java leave-world storage owner cleanup and documented that C# does not currently model equivalent live owner pointers.

## Commits Made
- `[Phase 6][UOW-2414] Audit leave-world storage owner cleanup`

## Files Changed
- `docs/Phase-6-Session-2414-Completion.md`
- `docs/Phase-6-Session-2414-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.model.items.storage.PlayerStorage`
- `com.aionemu.gameserver.model.items.storage.IStorage`
- `com.aionemu.gameserver.services.player.PlayerService`

## C# Artifacts Touched
- `Aion.GameServer.Model.GameObjects.Player` reviewed only.
- `Aion.GameServer.Model.GameObjects.InventoryItem` reviewed only.
- `Aion.GameServer.Services.PlayerEnterWorldService` reviewed only.
- `Aion.GameServer.Data.PlayerEnterWorldRepository` reviewed only.

## What Changed
- No production or test code changed.
- The Java storage owner cleanup was source-reviewed and recorded as currently unmodeled in C# because the C# storage model has no live owner object reference to clear.

## Tests Run
- `git diff --check`
  - Result: passed; only normal CRLF working-copy warnings were emitted.
- Java/Maven: skipped because no Java source or fixture changed.
- Broad .NET: skipped because this was documentation-only.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Services.PlayerEnterWorldService` | Logout service | Partial | Manual Only | Partial Parity | Java storage owner nulling was reviewed. C# has no equivalent live storage-owner references to clear; logout persistence remains modeled through item snapshots and dirty/deleted state. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | `Aion.GameServer.Model.GameObjects.Player` | Storage model | Partial | Manual Only | Needs Verification | Java has a mutable `actor` owner pointer. C# uses storage item lists and persisted `OwnerId` values; live owner-pointer behavior is not modeled. |
| `com.aionemu.gameserver.model.items.storage.IStorage` | `Aion.GameServer.Model.GameObjects.Player` | Storage interface/model | Not Started | Manual Only | Needs Verification | No C# `IStorage`/`PlayerStorage` equivalent exists for the `setOwner(Player)` contract. |
| `com.aionemu.gameserver.services.player.PlayerService` | `Aion.GameServer.Services.PlayerEnterWorldService` | Enter-world service | Partial | Regression Tested | Partial Parity | C# loads inventory, warehouse, and account warehouse rows, but does not assign a live account-warehouse owner reference because the account warehouse owner wrapper is not modeled. |

## Known Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.
- C# has no live storage owner wrapper equivalent to Java `PlayerStorage.actor`.
- If future C# storage wrappers are introduced, owner assignment/cleanup needs a paired enter-world and leave-world parity slice.
- Duel end side effects remain incomplete beyond result packets and duel-map cleanup.
- Soul sickness and special revive destinations from `PlayerReviveService` remain incomplete.
- Additional concrete logout persistence behavior remains to be pinned.

## Next Recommended UOW
- `UOW-2415`: Pin a concrete logout persistence behavior for inventory/warehouse dirty rows rather than Java owner-pointer cleanup.

## Suggested Discovery For UOW-2415
- Java:
  - `PlayerLeaveWorldService.leaveWorld(Player player)`
  - `PlayerService.storePlayer(Player player)`
  - `InventoryDAO.store(Player player)`
  - `Storage.getDeletedItems()`
  - `Persistable.PersistentState` transitions for item deletes/updates.
- C#:
  - `Player.GetDirtyItemsToUpdate()`
  - `Player.MarkDirtyItemsPersisted()`
  - `PlayerEnterWorldRepository.SavePlayerLogoutAsync(...)`
  - `PlayerEnterWorldServiceTests` logout persistence tests.
  - `PlayerEnterWorldRepositoryItemStonePersistenceTests` and repository database integration tests only if needed.

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: logout persistence gathers dirty/deleted inventory, warehouse, and account-warehouse rows in the same Java-derived storage-state shape before clearing dirty tracking.
- Focused C# command:
  - Start with `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryItemStonePersistenceTests" --no-restore`
  - If production repository SQL is touched, add the narrow `PlayerEnterWorldRepositoryDatabaseIntegrationTests` method only if its opt-in environment is available; otherwise document the skip.
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: production persistence changes would trigger broad consideration, but start focused on logout persistence tests.

## Safe Candidate UOWs
- Continue pending-request connection-boundary tests for another high-value modeled request kind.
- Add FindGroup logout cleanup connection wiring only if source review identifies a narrow already-modeled service hook.
- Continue revive logout parity for instance-handler `onReviveEvent` only if a narrow modeled handler hook exists.
