# Phase 6 Session 2677 Completion

## UOW

[Phase 6] UOW-2677: Create cumulative Atreian Passport login rows.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: enter-world Passport login now creates real CUMULATIVE reward rows when the next attendance threshold is reached.
- Java source/runtime path: AtreianPassportService.onLogin -> CUMULATIVE branch -> doReward && atp.getAttendNum() == pa.getPassportStamps() + 1 -> new Passport -> PersistentState.NEW -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: eligible login creates a real non-fake cumulative Passport row, persists it with the stamp mutation, and includes it in the login Passport snapshot.
- Why this is runtime progress: it mutates live player/account Passport state, persists new runtime rows through the existing database shape, and changes a real server packet sent during enter-world.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `onLogin` computes `doReward` before incrementing account stamps.
  - For active `CUMULATIVE` templates, Java creates a real `Passport` row when `doReward` is true and `attendNum == passportStamps + 1`.
  - Java does not check `isPassportPresent` in this real cumulative branch; the fake-stamp branch still handles absent rows otherwise.
- `game-server/data/static_data/events/login_events.xml`
  - Active April 2014 cumulative threshold rows include id `40` at `attend_num="7"`.
  - Active April 2014 daily row id `44` remains created by the already-live DAILY branch.

## C# Changes

- Extended `PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` so active CUMULATIVE templates at `player.PassportStamps + 1` create real `PlayerPassport` rows during reward-eligible login.
- Kept the comparison against the pre-increment stamp count, matching Java.
- Left CUMULATIVE fake-stamp behavior unimplemented and documented as a remaining gap.
- Added focused service and connection tests using runtime-loaded Java XML for the April 2014 cumulative threshold.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_AtreianPassportCumulativeLoginCreatesThresholdRowAndPersistsIt` | Unit / live enter-world service | `AtreianPassportService.onLogin` CUMULATIVE real reward branch | Starting with 6 stamps on April 2, 2014 creates active cumulative id 40 plus active daily id 44, increments stamps to 7, and persists both new rows. | Uses runtime-loaded Java XML and repository login-mutation capture. | Does not cover fake cumulative rows, DAO failure semantics, or live MySQL. |
| `HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot` | Unit / live connection and packet serialization | `AtreianPassportService.onLogin`, `SM_ATREIAN_PASSPORT.writeImpl` | Successful `CM_ENTER_WORLD` at the cumulative threshold sends attendance message and includes the cumulative row in the serialized Passport snapshot. | Socket-backed connection invocation plus serialized packet row-id assertions. | Does not assert full login packet ordering. |

## Validation Decision

```text
- Changed surface: live enter-world Passport service branch and focused service/connection tests.
- Specific behavior/contract: CUMULATIVE templates whose attend_num equals pre-increment passportStamps + 1 create real login rows that are persisted and sent in SM_ATREIAN_PASSPORT.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.onLogin cumulative login rows in this checkout.
- Broad-validation trigger: live enter-world Passport state/persistence/packet behavior changed, but risk is isolated to the Passport login branch and packet snapshot.
- Broad .NET decision: skipped after focused validation; the filtered command built the affected project and covered the edited live service plus connection packet paths.
- Why this scope is sufficient: the UOW only adds the real cumulative threshold branch; focused tests prove state mutation, persistence capture, reward message, and packet row serialization while other Passport branches remain documented gaps.
```

Results:

- First combined focused C# command attempt timed out after 120 seconds before returning a result.
- Narrow new service test passed: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorld_AtreianPassportCumulativeLoginCreatesThresholdRowAndPersistsIt" --no-restore`
- Narrow new connection test passed: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot" --no-restore`
- Focused adjacent C# validation passed with a longer timeout: 79/79 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, real CUMULATIVE threshold rows, stamp mutation, and packet intent. CUMULATIVE fake stamps, ANNIVERSARY rows, and limit cleanup remain incomplete. |
| `com.aionemu.gameserver.model.templates.event.AtreianPassport` | `Aion.GameServer.Dataholders.AtreianPassportSummary` | Static data DTO | Partial | Unit Tested | Partial Parity | Runtime-loaded Java XML fields used for active CUMULATIVE threshold selection. Full template parity is not claimed. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | Existing login mutation persists new Passport rows and stamp state. No live MySQL evidence in this UOW. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAtreianPassport` | Server packet | Partial | Unit Tested | Partial Parity | Snapshot now includes real cumulative threshold rows created during enter-world. Full packet ordering across the whole login sequence remains unverified. |

## Known Gaps

- Full Atreian Passport service parity is not claimed.
- CUMULATIVE fake stamp rows for already earned and upcoming rewards remain incomplete.
- ANNIVERSARY Passport behavior remains incomplete.
- `checkPassportLimit` excess reward-box cleanup is not ported.
- Java audit logging for invalid/missing/already rewarded/deleted claim attempts is still absent.
- No live MySQL integration test was run for the cumulative login insert path.
- Runtime clock behavior uses an injectable UTC clock; exact Java `ServerTime.now()` timezone parity remains unverified.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port CUMULATIVE fake stamp rows for already earned and upcoming rewards.
2. Port ANNIVERSARY Passport login rows after cumulative behavior is stable.
3. Port `checkPassportLimit` excess cleanup after all login row creation branches are live.
