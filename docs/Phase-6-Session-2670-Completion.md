# Phase 6 Session 2670 Completion

## UOW

[Phase 6] UOW-2670: Restore Atreian Passport state on enter-world.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: Atreian Passport snapshots no longer depend only on default in-memory passport state; enter-world now restores account_passports/account_stamps data into the live Player model used by SM_ATREIAN_PASSPORT.
- Java source/runtime path: AccountPassportsDAO.loadPassport(Account) and AtreianPassportService.sendPassport(player).
- C# runtime artifact wired: MySqlPlayerEnterWorldRepository.LoadPlayerAsync, Player.Passports, Player.PassportStamps, Player.LastPassportStamp, and SmAtreianPassport.
- Client-visible/state/persistence effect: a live CM_ATREIAN_PASSPORT response can include passport rows and stamp counts restored from the existing database schema; missing account_stamps rows are inserted with Java-equivalent zero/null defaults.
- Why this is runtime progress: this restores and persists runtime account passport state from the live database shape into the player model used by a real server packet; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - `loadPassport(Account)` selects `passport_id`, `rewarded`, and `arrive_date` from `account_passports`.
  - It selects `stamps` and `last_stamp` from `account_stamps`.
  - If no stamp row exists, it inserts `account_id`, `0`, and `null`.
- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `sendPassport(player)` uses the account passport list, account passport stamp count, and player creation date when sending `SM_ATREIAN_PASSPORT`.

## C# Changes

- `MySqlPlayerEnterWorldRepository.LoadPlayerAsync` now closes the common player-data reader, loads account passport rows on the same open connection, restores them onto the returned `Player`, and returns that hydrated runtime object.
- Added `AccountPassportRestoreSnapshot` plus `RestoreAccountPassportState` for the repository restore step.
- Added `Player.LastPassportStamp` so the Java `account_stamps.last_stamp` value is represented in runtime state.
- Added Java-equivalent insertion of a missing `account_stamps` row with `stamps = 0` and `last_stamp = null`.

Reward claiming, item grants, stamp incrementing, and passport store/update/delete semantics remain unported.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RestoreAccountPassportState_HydratesPlayerSnapshotUsedByAtreianPassportPacket` | Unit / runtime state plus packet serialization | `AccountPassportsDAO.loadPassport(Account)`, `SM_ATREIAN_PASSPORT.writeImpl` | Restored passport list, stamp count, and last stamp land on `Player`, then the same state serializes into the Java-shaped passport packet. | Focused state assertions and packet byte assertions against reviewed Java write order. | Does not run against a live MySQL database. |

## Validation Decision

```text
- Changed surface: live enter-world repository load path and modeled player passport state.
- Specific behavior/contract: Java loads account passport rows and stamps before passport snapshots are sent; C# now hydrates the live Player state consumed by SM_ATREIAN_PASSPORT.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~PlayerEnterWorldRepositoryPassportRestoreTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AccountPassportsDAO/SM_ATREIAN_PASSPORT in this checkout.
- Broad-validation trigger: repository load path touched, but the change is isolated to passport account rows after common player load and the filtered run compiles the affected project.
- Broad .NET decision: skipped; the focused tests cover the existing live packet send plus the new restore-to-packet state path.
- Why this scope is sufficient: it validates the exact player fields that the live handler sends and compiles the repository changes without running unrelated Phase 6 test-only surfaces.
```

Results:

- Focused C# validation passed: 6/6 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AccountPassportsDAO.loadPassport(Account)` | `MySqlPlayerEnterWorldRepository.LoadAccountPassportStateAsync` | Repository restore | Partial | Unit/Compile Tested | Partial Parity | Selects passport rows and stamp row, inserts missing stamp row. Needs live DB integration evidence and store/update/delete semantics. |
| `Account.passportStamps/lastStamp` | `Player.PassportStamps/LastPassportStamp` | Runtime state | Partial | Unit Tested | Partial Parity | Restored onto live player model; daily stamp mutation remains unported. |
| `AtreianPassportService.sendPassport` | `GameServerConnection` + `SmAtreianPassport` | Live packet path | Partial | Unit Tested | Partial Parity | Packet now consumes DB-restored state after enter-world; reward claiming remains unported. |

## Known Gaps

- No live MySQL integration test was run for the new passport queries or missing-row insert.
- `AtreianPassportService.takeReward` reward validation, item grants, passport rewarded mutation, persistence, and system messages remain unported.
- Timestamp timezone behavior for `account_passports.arrive_date` still relies on the repository's current `DateTime` handling and needs future Java/C# runtime verification.
- `SM_ATREIAN_PASSPORT` still lacks a Java golden/runtime packet fixture.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port a narrow `CM_ATREIAN_PASSPORT -> takeReward` mutation slice for already-restored, unclaimed passport rows, including persistence of rewarded status.
2. Add live DB integration coverage for account passport restore/store when a local MySQL fixture is enabled.
3. Continue deferred packet discovery and select a packet branch that can send a deterministic server packet or mutate already-modeled state.
