# Phase 6 Session 2679 Completion

## UOW

[Phase 6] UOW-2679: Create anniversary Atreian Passport login rows.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: enter-world Passport login now creates Java-style ANNIVERSARY rows for active anniversary templates.
- Java source/runtime path: AtreianPassportService.onLogin -> ANNIVERSARY branch -> getAccountAgeInMonths(player, now.toLocalDate()) -> isPassportPresent guard -> real row when monthsAlive == attendNum, fake rewarded row when monthsAlive > attendNum -> addPassport -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.CreationDate, Player.Passports, SaveAccountPassportLoginMutationAsync filtering, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: eligible login creates real anniversary rows or fake rewarded anniversary rows in live player state, persists only real non-fake rows during the reward mutation, and includes them in SM_ATREIAN_PASSPORT.
- Why this is runtime progress: it mutates live player/account Passport state, can persist new real Passport rows, and changes a real login packet.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - ANNIVERSARY rows use `getAccountAgeInMonths(player, now.toLocalDate())`.
  - Existing Passport ids short-circuit the ANNIVERSARY branch.
  - `monthsAlive == attendNum` creates a real available row with `PersistentState.NEW`.
  - `monthsAlive > attendNum` creates a fake rewarded row.
- `game-server/src/com/aionemu/gameserver/model/account/Passport.java`
  - Fake rewarded rows serialize as `RewardStatus.TAKEN`.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - Fake rows remain `NOACTION`; only real NEW rows are inserted by `storePassportList`.
- `game-server/data/static_data/events/login_events.xml`
  - Active anniversary ids 14-25 cover months 1-12 from April 2014 onward.

## C# Changes

- Added ANNIVERSARY handling to `PlayerEnterWorldService.ApplyAtreianPassportLoginAsync`.
- Reused `AtreianPassportAttendPlanService.CreateAccountAgeInMonthsPlan` for Java-equivalent full-month age calculation.
- Real anniversary rows are added when account age equals `AttendNum`.
- Fake rewarded anniversary rows are added when account age is greater than `AttendNum`.
- Existing id-only Passport presence checks prevent duplicate anniversary rows.
- Existing persistence filtering saves only non-fake rows, so fake anniversary rows are client-visible but not inserted into `account_passports`.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_AtreianPassportAnniversaryLoginCreatesRealAndFakeRewardRows` | Unit / live enter-world service | `AtreianPassportService.onLogin` ANNIVERSARY branch | A three-month-old account creates fake taken month 1/2 rows, a real available month 3 row, and persists only real daily plus anniversary rows. | Uses runtime-loaded Java XML and repository mutation capture. | Does not cover live MySQL. |
| `EnterWorld_AtreianPassportAnniversaryRowsAreClientVisibleWithoutDailyRewardPersistence` | Unit / live enter-world service | `AtreianPassportService.onLogin` no-reward branch | Same-day login creates anniversary rows in live state and snapshot intent without stamp increment or repository mutation. | Runtime-loaded Java XML and repository call capture. | Packet-level no-reward anniversary snapshot not separately asserted. |
| `HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot` | Unit / live connection and packet serialization | `AtreianPassportService.onLogin`, `SM_ATREIAN_PASSPORT.writeImpl` | The enter-world snapshot includes anniversary fake taken and real available rows alongside cumulative rows. | Socket-backed connection invocation plus serialized packet status assertions. | Does not assert full login packet ordering. |

## Validation Decision

```text
- Changed surface: live enter-world Passport service branch and focused service/connection tests.
- Specific behavior/contract: ANNIVERSARY templates create real/fake rows from Java full-month account age, expose Java reward statuses in SM_ATREIAN_PASSPORT, and persist only non-fake NEW rows when reward persistence runs.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.onLogin anniversary rows in this checkout.
- Broad-validation trigger: live enter-world Passport state/persistence/packet behavior changed, but risk is isolated to Passport login state/snapshot.
- Broad .NET decision: skipped after focused validation; the filtered command built the affected project and covered the edited live service plus connection packet paths.
- Why this scope is sufficient: focused tests prove Java-derived full-month age selection, real/fake row creation, persistence filtering, no-reward behavior, and packet serialization.
```

Results:

- Narrow service validation initially failed due to a parallel build file lock, then passed when rerun alone:
  `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorld_AtreianPassportAnniversaryLoginCreatesRealAndFakeRewardRows|FullyQualifiedName~EnterWorld_AtreianPassportAnniversaryRowsAreClientVisibleWithoutDailyRewardPersistence" --no-restore`
- Narrow packet validation passed:
  `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot" --no-restore`
- Focused adjacent C# validation passed: 83/83 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, real/fake CUMULATIVE rows, real/fake ANNIVERSARY rows, stamp mutation, and packet intent. `checkPassportLimit` remains incomplete. |
| `com.aionemu.gameserver.services.AtreianPassportService.getAccountAgeInMonths` | `Aion.GameServer.Services.AtreianPassportAttendPlanService.CreateAccountAgeInMonthsPlan` | Utility | Partial | Unit Tested | Partial Parity | Existing Java-equivalent full-month calculation is now used by live ANNIVERSARY login behavior. Timezone parity remains unverified. |
| `com.aionemu.gameserver.model.account.Passport` | `Aion.GameServer.Model.Account.PlayerPassport` | Model | Partial | Unit Tested | Partial Parity | Anniversary fake taken and real available reward-status mapping is covered through service and packet tests. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | C# filters fake anniversary rows out of account_passports inserts, matching Java fake rows' NOACTION state. No live MySQL evidence in this UOW. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAtreianPassport` | Server packet | Partial | Unit Tested | Partial Parity | Snapshot includes ANNIVERSARY fake taken and real available rows. Full login packet ordering remains unverified. |

## Known Gaps

- Full Atreian Passport service parity is not claimed.
- `checkPassportLimit` excess reward-box cleanup is not ported.
- Java audit logging for invalid/missing/already rewarded/deleted claim attempts is still absent.
- No live MySQL integration test was run for the anniversary login insert/filtering path.
- Runtime clock behavior uses an injectable UTC clock; exact Java `ServerTime.now()` timezone parity remains unverified.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port `checkPassportLimit` excess cleanup now that DAILY, CUMULATIVE, and ANNIVERSARY login row creation branches are live.
2. Add opt-in live MySQL evidence for Passport login insert/delete mutations once cleanup behavior is complete.
3. Port remaining Java audit logging for invalid Passport claim attempts.
