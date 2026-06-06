# Phase 6 Session 2673 Completion

## UOW

[Phase 6] UOW-2673: Delete expired Atreian Passport claims.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: expired CM_ATREIAN_PASSPORT claims now delete the restored passport row instead of silently skipping it.
- Java source/runtime path: AtreianPassportService.takeReward -> rewardExpireMinutes deadline check -> passport.setPersistentState(DELETED) -> PassportsList.removePassport(passport) -> AccountPassportsDAO.storePassportList(accountId, toRemove) -> deletePassport.
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync, IPlayerEnterWorldRepository.DeleteAccountPassportAsync, EmptyPlayerEnterWorldRepository, MySqlPlayerEnterWorldRepository, and CmAtreianPassportTests.
- Client-visible/state/persistence effect: an expired requested passport is removed from live player state, persisted through a DELETE on account_passports by account/passport/arrive_date, and omitted from the refreshed SM_ATREIAN_PASSPORT snapshot.
- Why this is runtime progress: it mutates live account passport state, persists deletion through the existing database shape, and changes a real server packet emitted by the live handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` checks `rewardExpireMinutes`, computes a deadline from `passport.getArriveDate()`, marks expired passports `PersistentState.DELETED`, removes them from `PassportsList`, adds them to `toRemove`, and later calls `AccountPassportsDAO.storePassportList`.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - `storePassportList` dispatches `PersistentState.DELETED` rows to `deletePassport`.
  - `deletePassport` executes `DELETE FROM account_passports WHERE account_id = ? AND passport_id = ? and arrive_date = ?`.
- `game-server/src/com/aionemu/gameserver/model/account/Passport.java`
  - Expired claim branch uses existing id and arrive-date key; no reward item is granted.

## C# Changes

- Added `IPlayerEnterWorldRepository.DeleteAccountPassportAsync`.
- Added `EmptyPlayerEnterWorldRepository` capture fields for expired-claim delete assertions.
- Added `MySqlPlayerEnterWorldRepository.DeleteAccountPassportAsync` using the Java `account_id/passport_id/arrive_date` key.
- Updated `GameServerConnection.HandleAtreianPassportAsync` so expired claims:
  - call the repository delete method;
  - remove the passport from the active player snapshot only after persistence succeeds;
  - skip inventory reward grant and rewarded update;
  - still send the refreshed `SM_ATREIAN_PASSPORT` snapshot.
- Updated the full-interface test fake in `PlayerEnterWorldServiceTests`.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportDeletesExpiredRewardClaim` | Unit / live handler and packet serialization | `AtreianPassportService.takeReward`, `AccountPassportsDAO.deletePassport`, `SM_ATREIAN_PASSPORT.writeImpl` | The live handler deletes an expired requested passport, does not grant inventory, does not mark rewarded, removes the passport from the active player, and sends a snapshot with zero passport rows. | Socket-backed handler invocation plus repository capture and packet byte assertion based on reviewed Java logic. | Does not run against live MySQL. |

## Validation Decision

```text
- Changed surface: live in-game packet handler, repository interface/implementation, test fake, and passport claim tests.
- Specific behavior/contract: expired requested Passport rows are deleted/persisted and omitted from the refreshed Passport packet without inventory grant.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.takeReward/AccountPassportsDAO.deletePassport in this checkout.
- Broad-validation trigger: live handler and persistence contract changed.
- Broad .NET decision: skipped after focused validation; the filtered test compiled the affected project and exercised the edited live handler branch. PlayerEnterWorldServiceTests only received a full-interface fake method required for compile and has no behavior in this UOW.
- Why this scope is sufficient: the UOW is narrow to the Passport expired-claim branch; the focused test proves the live state, persistence call, and packet result while remaining gaps are documented separately.
```

Results:

- Focused C# validation passed: 7/7 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Adds the expired-claim delete branch to the existing reward grant slice. Disabled-service gate, audit logging, full `onLogin`, and live DB evidence remain open. |
| `AccountPassportsDAO.deletePassport` | `MySqlPlayerEnterWorldRepository.DeleteAccountPassportAsync` | Repository update | Partial | Unit/Compile Tested | Partial Parity | SQL key matches Java source. Needs opt-in DB integration evidence. |
| `PassportsList.removePassport` | `Player.Passports` replacement in `GameServerConnection` | Model/state mutation | Partial | Unit Tested | Partial Parity | Removes expired restored row from the active player snapshot after persistence succeeds. Full PassportsList behavior is not modeled. |

## Known Gaps

- `AtreianPassportService.isAtreianPassportDisabled` is not yet wired; C# still sends Passport packets during periods where Java returns without a response.
- Java audit logging for missing/already rewarded/deleted claim attempts is not ported.
- `AtreianPassportService.onLogin` remains incomplete: daily/cumulative passport creation, fake stamps, stamp persistence, excess passport purge, and login system messages are still open.
- No live MySQL integration test was run for `DeleteAccountPassportAsync`.
- Date/time behavior uses `DateTimeOffset.UtcNow` for the expiry comparison; this matches Java's `Instant.now()` intent for the tested branch but has not been runtime-compared against Java.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire the Atreian Passport disabled-service gate so live C# stops processing/sending Passport state after Java's calculated expiration window.
2. Port the smallest `AtreianPassportService.onLogin` slice that creates one daily/cumulative passport, increments account stamps, persists account stamp state, and sends the attendance reward message plus snapshot.
3. Add live DB integration evidence for the Passport delete/update paths if the opt-in MySQL fixture is available, but do not treat evidence alone as progress unless it directly unblocks the next runtime behavior.
