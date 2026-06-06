# Phase 6 Session 2671 Completion

## UOW

[Phase 6] UOW-2671: Persist Atreian Passport reward claims.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT now claims matching restored passport rows instead of only echoing the current snapshot.
- Java source/runtime path: CM_ATREIAN_PASSPORT.runImpl -> AtreianPassportService.takeReward -> Passport.setRewarded(true) -> AccountPassportsDAO.storePassportList(accountId, toRemove) -> updatePassport.
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync, PlayerPassport.ClaimReward, IPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync, MySqlPlayerEnterWorldRepository, and GameClientSocketServer repository injection.
- Client-visible/state/persistence effect: a requested unclaimed passport row is marked rewarded in live player state, persisted to account_passports.rewarded, and the refreshed SM_ATREIAN_PASSPORT snapshot reports RewardStatus.TAKEN.
- Why this is runtime progress: this mutates live player passport state, persists through the existing database shape, and sends a real server packet from the live handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT.java`
  - `runImpl` calls `AtreianPassportService.takeReward(player, passports)` for the active player.
- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` locates the passport by id/timestamp, skips missing/already rewarded/deleted rows, grants the item, sets `rewarded = true`, marks `UPDATE_REQUIRED`, stores changed rows, then sends a passport snapshot.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - `storePassportList` dispatches `UPDATE_REQUIRED` rows to `updatePassport`.
  - `updatePassport` writes `rewarded` by `(account_id, passport_id, arrive_date)`.
- `game-server/src/com/aionemu/gameserver/model/account/PassportsList.java`
  - `getPassport` matches by passport id and `arriveDate.getTime() / 1000`.

## C# Changes

- Added `PlayerPassport.ClaimReward()` for the Java `Passport.setRewarded(true)` state transition.
- Added `IPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync` and a MySQL implementation using Java's `account_id/passport_id/arrive_date` key.
- Threaded `IPlayerEnterWorldRepository` from `GameClientSocketServer` into `GameServerConnection`.
- Replaced the Atreian Passport handler's snapshot-only branch with `HandleAtreianPassportAsync`, which:
  - matches requested id/timestamp pairs against restored `Player.Passports`;
  - skips already rewarded or fake-stamp rows;
  - persists `rewarded = 1` before mutating in-memory state;
  - sends the refreshed `SM_ATREIAN_PASSPORT` snapshot.

This is still a narrow claim slice. Reward item grants, inventory-full handling, level checks, expiry removal, and daily stamp creation are not claimed as ported in this UOW.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportClaimsMatchingRestoredPassport` | Unit / live handler and packet serialization | `CM_ATREIAN_PASSPORT.runImpl`, `AtreianPassportService.takeReward`, `AccountPassportsDAO.updatePassport`, `SM_ATREIAN_PASSPORT.writeImpl` | The live handler matches a requested passport by id/timestamp, persists the rewarded update, mutates the active player, and sends a TAKEN snapshot. | Focused handler invocation plus repository capture and packet byte assertions derived from reviewed Java logic. | Does not grant the reward item or run against live MySQL. |

## Validation Decision

```text
- Changed surface: live in-game packet handler, repository interface/implementation, socket-server dependency threading, and player passport state.
- Specific behavior/contract: a requested restored passport row is claimed by id/timestamp, persisted as rewarded, and reflected as TAKEN in SM_ATREIAN_PASSPORT.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~PlayerEnterWorldRepositoryPassportRestoreTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.takeReward/AccountPassportsDAO in this checkout.
- Broad-validation trigger: live handler wiring and repository interface changed.
- Broad .NET decision: skipped after focused validation; the filtered test compiled the affected project and test project, covered the edited handler path, the adjacent restore packet path, and the only full-interface test fake needed for compile.
- Why this scope is sufficient: the command proves the new live mutation/persistence contract and packet result while avoiding unrelated Phase 6 surfaces; remaining risk is documented as missing item-grant/live-DB coverage.
```

Results:

- Initial focused run failed because a full-interface test fake needed the new repository method.
- Second focused run failed due a nullable tuple assertion in the new test.
- Final focused C# validation passed: 7/7 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_ATREIAN_PASSPORT.runImpl` | `GameServerConnection.HandleAtreianPassportAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Claims restored unclaimed rows and sends refreshed snapshot. Item grants, invalid-level messages, full-inventory handling, and expiry branches remain unported. |
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Implements id/timestamp match plus rewarded mutation/persistence for restored rows only. Does not execute full Java reward service semantics. |
| `AccountPassportsDAO.updatePassport` | `MySqlPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync` | Repository update | Partial | Unit/Compile Tested | Partial Parity | SQL key and rewarded write match Java source. Needs live DB integration evidence and store-list handling for NEW/DELETED rows. |
| `Passport.setRewarded` | `PlayerPassport.ClaimReward` | Model mutation | Partial | Unit Tested | Partial Parity | Immutable replacement sets Rewarded=true; persistent-state tracking is not modeled. |

## Known Gaps

- The reward item is not granted yet. Java calls `ItemService.addItem` before setting the passport rewarded.
- Inventory-full handling and `STR_WAREHOUSE_FULL_INVENTORY` are not wired.
- Reward permit-level validation and `STR_MSG_ATTEND_REWARD_INVALID_LEVEL` are not wired.
- Expired passport deletion during claim is not wired.
- No live MySQL integration test was run for `UpdateAccountPassportRewardedAsync`.
- Daily login stamp creation and new passport generation remain unported.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Add the reward item grant slice for claimed Atreian Passport rows using existing `InventoryAddService`/inventory persistence and `SmInventoryAddItem` packet behavior where available.
2. Add live DB integration coverage for `account_passports.rewarded` updates when the opt-in MySQL fixture is enabled.
3. Port the inventory-full and permit-level rejection branches for `AtreianPassportService.takeReward`.
