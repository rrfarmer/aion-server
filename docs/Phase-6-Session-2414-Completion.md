# Phase 6 Session 2414 Completion - Leave-World Storage Owner Cleanup Audit

## Scope
- Audited Java leave-world inventory, warehouse, and account-warehouse owner cleanup.
- Compared Java `PlayerStorage.actor` owner references with the current C# `Player` storage model.
- Determined that C# does not currently model live storage owner back-references, so no production cleanup hook was added.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
- `game-server/src/com/aionemu/gameserver/model/items/storage/PlayerStorage.java`
  - `setOwner(Player actor)`
  - actor-backed inventory mutation overloads
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
  - `setOwner(Player player)`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerService.java`
  - account warehouse owner assignment before storage load

## Findings
- Java logout calls:
  - `player.getInventory().setOwner(null)`
  - `player.getWarehouse().setOwner(null)`
  - `player.getAccount().getAccountWarehouse().setOwner(null)`
- These calls occur after `PlayerService.storePlayer(player)` and before final old-level / last-online / online-flag DAO updates.
- Java `PlayerStorage` stores a live `Player actor` reference and routes no-actor storage mutations through that actor.
- Java enter-world restores the account warehouse owner with `account.getAccountWarehouse().setOwner(player)` before loading account warehouse storage.
- C# `Player` currently models storage as `IReadOnlyList<InventoryItem>` snapshots plus storage persistent-state flags and deleted-row lists.
- C# `InventoryItem.OwnerId` is a persisted database owner id, not a live `Player` object reference.
- No C# account model or storage wrapper currently holds a live inventory/warehouse/account-warehouse owner pointer equivalent to Java `PlayerStorage.actor`.

## Implemented
- No production code changes.
- No tests added.
- Added this audit record so the Java owner-null cleanup is not treated as silently ported.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Services.PlayerEnterWorldService` | Logout service | Partial | Manual Only | Partial Parity | Java storage owner nulling was reviewed. C# has no equivalent live storage-owner references to clear; logout persistence remains modeled through item snapshots and dirty/deleted state. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | `Aion.GameServer.Model.GameObjects.Player` | Storage model | Partial | Manual Only | Needs Verification | Java has a mutable `actor` owner pointer. C# uses storage item lists and persisted `OwnerId` values; live owner-pointer behavior is not modeled. |
| `com.aionemu.gameserver.model.items.storage.IStorage` | `Aion.GameServer.Model.GameObjects.Player` | Storage interface/model | Not Started | Manual Only | Needs Verification | No C# `IStorage`/`PlayerStorage` equivalent exists for the `setOwner(Player)` contract. |
| `com.aionemu.gameserver.services.player.PlayerService` | `Aion.GameServer.Services.PlayerEnterWorldService` | Enter-world service | Partial | Regression Tested | Partial Parity | C# loads inventory, warehouse, and account warehouse rows, but does not assign a live account-warehouse owner reference because the account warehouse owner wrapper is not modeled. |

## Validation Decision
- Changed surface: documentation-only.
- Specific behavior/contract: Java leave-world storage owner nulling is reviewed and explicitly marked unmodeled in C#.
- Focused C# command: not applicable; no C# code or tests changed.
- Focused Java/Maven command: skipped; no Java source or fixture changed, and the Java source review directly identified the cleanup calls.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; documentation-only UOW.
- Why this scope is sufficient: the work records a source-of-truth audit and avoids adding a misleading no-op production hook for storage owner references that do not currently exist in C#.

## Validation Result
- `git diff --check`
  - Passed; only normal CRLF working-copy warnings were emitted.

## Known Remaining Gaps
- C# has no live storage owner wrapper equivalent to Java `PlayerStorage.actor`.
- If a future C# storage wrapper is introduced, it must model Java enter-world owner assignment and leave-world owner clearing together.
- Account-level warehouse ownership remains represented only through loaded item rows, not an account warehouse object.
- Inventory/warehouse persistence parity remains partial and should be validated through concrete item dirty/deleted row behavior rather than owner-pointer cleanup.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 4.
- Total C# artifacts changed in this UOW: 0.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
