# Phase 6 Session 2678 Completion

## UOW

[Phase 6] UOW-2678: Create cumulative Atreian Passport fake rows.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: enter-world Passport login now creates Java-style fake CUMULATIVE rows for active cumulative templates missing from the account snapshot.
- Java source/runtime path: AtreianPassportService.onLogin -> CUMULATIVE else-if -> !pa.getPassportsList().isPassportPresent(atp.getId()) -> new Passport -> setFakeStamp(true) -> setRewarded(true) when atp.getAttendNum() <= pa.getPassportStamps() -> addPassport -> SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync filtering, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: login now adds fake taken/upcoming cumulative rows to live player state and the Passport snapshot; only real non-fake new rows are persisted with stamp updates, matching Java PersistentState behavior.
- Why this is runtime progress: it mutates live player/account Passport state and changes a real server packet sent during enter-world.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - CUMULATIVE real rows are created only when `doReward && attendNum == passportStamps + 1`.
  - Otherwise, missing cumulative template ids create fake rows.
  - Fake rows are marked rewarded when `attendNum <= passportStamps`, using the pre-increment stamp count.
- `game-server/src/com/aionemu/gameserver/model/account/Passport.java`
  - Fake rewarded rows serialize as `RewardStatus.TAKEN`.
  - Fake non-rewarded rows serialize as `RewardStatus.UPCOMING`.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - Fake rows do not call `setPersistentState(NEW)`, so `storePassportList` ignores them while still updating stamps.

## C# Changes

- Extended `PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` to process active Passport templates on every non-disabled login.
- Kept DAILY row creation gated by `doReward`.
- Added CUMULATIVE fake row creation for missing active cumulative templates:
  - `Rewarded = true, FakeStamp = true` when `AttendNum <= player.PassportStamps`.
  - `Rewarded = false, FakeStamp = true` for upcoming thresholds.
- Updated login persistence to save only non-fake new Passport rows, matching Java fake rows' `NOACTION` persistence state.
- Added a Java-equivalent `HasAtreianPassport` helper for id-only `PassportsList.isPassportPresent` behavior.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_AtreianPassportCumulativeLoginCreatesThresholdRowAndPersistsIt` | Unit / live enter-world service | `AtreianPassportService.onLogin` CUMULATIVE branch | Reward-eligible login creates active cumulative threshold row plus fake upcoming cumulative rows; only daily plus real threshold rows are persisted. | Uses runtime-loaded Java XML and repository mutation capture. | Does not cover live MySQL. |
| `EnterWorld_AtreianPassportCumulativeLoginCreatesFakeTakenAndUpcomingRows` | Unit / live enter-world service | `AtreianPassportService.onLogin`, `Passport.getRewardStatus` | Starting with 13 stamps creates fake taken id 40, real available id 41, fake upcoming ids 42/43, and persists only daily plus real id 41. | Runtime-loaded Java XML and model reward-status assertions. | Does not cover DAO failure behavior. |
| `EnterWorld_AtreianPassportCumulativeFakeRowsAreClientVisibleWithoutDailyRewardPersistence` | Unit / live enter-world service | `AtreianPassportService.onLogin` no-reward branch | Same-day login creates fake cumulative snapshot rows without stamp increment or repository mutation. | Runtime-loaded Java XML and repository call capture. | Packet-level no-reward fake snapshot not separately asserted. |
| `HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot` | Unit / live connection and packet serialization | `AtreianPassportService.onLogin`, `SM_ATREIAN_PASSPORT.writeImpl` | Enter-world snapshot includes cumulative fake taken, real available, and fake upcoming rows with Java reward-status ids. | Socket-backed connection invocation plus serialized packet status assertions. | Does not assert full login packet ordering. |

## Validation Decision

```text
- Changed surface: live enter-world Passport service branch and focused service/connection tests.
- Specific behavior/contract: CUMULATIVE fake rows are created for missing active cumulative templates, use Java reward-status mapping, are client-visible, and are not persisted as account_passports rows.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.onLogin fake cumulative rows in this checkout.
- Broad-validation trigger: live enter-world Passport state and packet behavior changed, but risk is isolated to Passport login state/snapshot.
- Broad .NET decision: skipped after focused validation; the filtered command built the affected project and covered the edited live service plus connection packet paths.
- Why this scope is sufficient: focused tests prove Java-derived taken/upcoming/available row statuses, state mutation, persistence filtering, no-reward behavior, and packet serialization.
```

Results:

- Narrow service validation passed: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorld_AtreianPassportCumulativeLoginCreatesFakeTakenAndUpcomingRows|FullyQualifiedName~EnterWorld_AtreianPassportCumulativeFakeRowsAreClientVisibleWithoutDailyRewardPersistence" --no-restore`
- Narrow packet validation first failed due to a parallel build file lock, then passed when rerun alone: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot" --no-restore`
- Focused adjacent C# validation passed: 81/81 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, real CUMULATIVE threshold rows, fake CUMULATIVE rows, stamp mutation, and packet intent. ANNIVERSARY rows and limit cleanup remain incomplete. |
| `com.aionemu.gameserver.model.account.Passport` | `Aion.GameServer.Model.Account.PlayerPassport` | Model | Partial | Unit Tested | Partial Parity | Fake taken/upcoming reward-status mapping is covered through service and packet tests. Full model parity is not claimed. |
| `com.aionemu.gameserver.model.account.PassportsList` | `Aion.GameServer.Services.PlayerEnterWorldService.HasAtreianPassport` | Collection behavior | Partial | Unit Tested | Partial Parity | CUMULATIVE fake branch now uses id-only presence checks. Other PassportsList methods remain partially ported elsewhere. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | C# now filters fake rows out of account_passports inserts to mirror Java fake rows' NOACTION persistence state. No live MySQL evidence in this UOW. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAtreianPassport` | Server packet | Partial | Unit Tested | Partial Parity | Snapshot includes fake taken/upcoming and real available cumulative rows. Full login packet ordering remains unverified. |

## Known Gaps

- Full Atreian Passport service parity is not claimed.
- ANNIVERSARY Passport behavior remains incomplete.
- `checkPassportLimit` excess reward-box cleanup is not ported.
- Java audit logging for invalid/missing/already rewarded/deleted claim attempts is still absent.
- No live MySQL integration test was run for the cumulative login insert/filtering path.
- Runtime clock behavior uses an injectable UTC clock; exact Java `ServerTime.now()` timezone parity remains unverified.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port ANNIVERSARY Passport login rows and rewarded fake rows.
2. Port `checkPassportLimit` excess cleanup after all login row creation branches are live.
3. Add opt-in live MySQL evidence for Passport login insert/delete mutations once row behavior is complete.
