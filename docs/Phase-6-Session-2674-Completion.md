# Phase 6 Session 2674 Completion

## UOW

[Phase 6] UOW-2674: Gate disabled Atreian Passport service.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT now returns immediately once Java's Passport service expiration window has elapsed.
- Java source/runtime path: AtreianPassportService.isAtreianPassportDisabled -> calculatePassportExpireDate -> findLastRewardTime over DataManager.ATREIAN_PASSPORT_DATA DAILY/CUMULATIVE period_end values -> takeReward returns before reward processing or packet emission.
- C# runtime artifact wired: AtreianPassportTable expiry calculation and GameServerConnection.HandleAtreianPassportAsync.
- Client-visible/state/persistence effect: during disabled periods the live handler no longer grants inventory rewards, marks/restores/deletes passport rows, calls Passport persistence methods, or sends SM_ATREIAN_PASSPORT.
- Why this is runtime progress: it changes live packet-handler control flow and suppresses real state mutation, persistence, and server packet output according to Java runtime behavior.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - The service computes `expireDate` from the latest `period_end` among daily and cumulative passport entries.
  - `calculatePassportExpireDate` uses the end of the latest reward date plus 14 days.
  - `isAtreianPassportDisabled` returns true only when the checked time is after the computed expiration.
  - `takeReward` returns immediately when the Passport service is disabled.

## C# Changes

- Added `AtreianPassportTable.ExpireDate` and `IsDisabled(DateTime)` based on the Java daily/cumulative latest reward calculation.
- Wired `GameServerConnection.HandleAtreianPassportAsync` to return before claim processing when static Passport data is disabled.
- Added an injectable Passport clock to `GameServerConnection` for focused live-handler validation.
- Added a disabled-gate test proving that live claim processing sends no packet, mutates no player inventory/passport state, and calls no Passport persistence methods.
- Kept active Passport grant/delete tests on an active clock so their existing runtime branches remain covered.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportDisabledGateSuppressesSnapshotAndClaims` | Unit / live handler and packet serialization | `AtreianPassportService.isAtreianPassportDisabled`, `AtreianPassportService.takeReward` | Disabled Passport service returns before reward grant, passport mutation, persistence, delete, or `SM_ATREIAN_PASSPORT` send. | Socket-backed handler invocation with real Java XML-loaded static data and repository/packet/state assertions. | Does not runtime-compare the exact expiration timestamp against Java. |

## Validation Decision

```text
- Changed surface: Java XML-backed Passport static table, live in-game Passport packet handler, and focused Passport handler tests.
- Specific behavior/contract: disabled service gate suppresses claim processing and Passport snapshot emission.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.isAtreianPassportDisabled/takeReward in this checkout.
- Broad-validation trigger: live handler behavior changed, but the edited branch is narrow to Passport handler control flow.
- Broad .NET decision: skipped after focused validation; the filtered test compiled the affected project and exercised active reward, expired delete, and disabled-service branches.
- Why this scope is sufficient: the UOW is limited to the Java disabled gate for CM_ATREIAN_PASSPORT; the focused suite proves the live packet/state/persistence contract while remaining login-time Passport gaps are documented separately.
```

Results:

- Focused C# validation passed: 8/8 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AtreianPassportService.isAtreianPassportDisabled` | `AtreianPassportTable.IsDisabled` | Service/data gate | Partial | Unit Tested | Partial Parity | Uses Java XML daily/cumulative reward dates to compute the disabled gate. Exact timezone/runtime comparison is not golden-tested. |
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Adds the disabled-service early return to prior reward grant and expired-delete slices. Audit logging and full `onLogin` remain incomplete. |

## Known Gaps

- Full Java Passport service parity is not claimed.
- `AtreianPassportService.onLogin` remains incomplete: daily/cumulative passport creation, fake stamps, stamp persistence, excess passport purge, and login system messages are still open.
- Java audit logging for missing/already rewarded/deleted claim attempts is not ported.
- No live MySQL integration test was run for Passport update/delete paths.
- The disabled gate uses UTC clock injection in tests and `DateTimeOffset.UtcNow` in runtime; exact Java `ServerTime.now()` timezone behavior has not been golden-compared.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 1
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port the smallest `AtreianPassportService.onLogin` slice that creates a daily/cumulative passport, increments account stamps, persists account stamp state, and sends the attendance reward message plus snapshot.
2. Wire Java audit logging for invalid/missing/already rewarded Passport claim attempts if it is connected to live handler logging.
3. Add opt-in DB integration for Passport reward/delete persistence only if it directly supports a same-session runtime Passport UOW.
