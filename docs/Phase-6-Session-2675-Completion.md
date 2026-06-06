# Phase 6 Session 2675 Completion

## UOW

[Phase 6] UOW-2675: Create daily Atreian Passport login rows.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful enter-world now runs the Java DAILY Atreian Passport login branch instead of only restoring existing Passport rows.
- Java source/runtime path: PlayerEnterWorldService.enterWorld -> AtreianPassportService.onLogin -> checkOnlineDate -> DAILY add Passport -> AccountPassportsDAO.storePassport(Account) -> PacketSendUtility sends attendance message and SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService, IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository, GameServerConnection enter-world packet flow, SmSystemMessage, and PlayerEnterWorldResult.
- Client-visible/state/persistence effect: eligible login mutates live player Passport rows and stamp state, persists new account_passports rows plus account_stamps, sends STR_ATTEND_MSG_ATTEND_REWARD_GET, and sends SM_ATREIAN_PASSPORT before macro restore.
- Why this is runtime progress: it mutates live account/player state, persists through the existing database shape, and emits real server packets from the live enter-world path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `onLogin` exits when disabled, checks the daily attendance reset day, adds active DAILY passports when the account has not stamped today, increments stamps, stores Passport state, sends `STR_ATTEND_MSG_ATTEND_REWARD_GET`, and sends `SM_ATREIAN_PASSPORT`.
  - `getAttendDay` subtracts the 9-hour reset offset.
  - `PassportsList.hasPassportForDay` checks same passport id and the stored arrive-date calendar day.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - `storePassport(Account)` stores NEW passport rows with Java's duplicate-key rewarded merge and updates `account_stamps`.
- `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
  - Login sequence calls `AtreianPassportService.onLogin` after mailbox/housing-bid login effects and before macro/recipe restore.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_ATTEND_MSG_ATTEND_REWARD_GET` uses message id `1402601`.

## C# Changes

- Added `AtreianPassportLoginResult` to carry the live Passport login packet intent from `PlayerEnterWorldService` to `GameServerConnection`.
- Added the DAILY branch of `AtreianPassportService.onLogin` to `PlayerEnterWorldService`:
  - honors the existing disabled gate;
  - applies Java's 9-hour attendance reset day;
  - creates active DAILY `PlayerPassport` rows from Java XML static data;
  - increments `Player.PassportStamps`;
  - updates `Player.LastPassportStamp`;
  - returns packet/send intent for the connection.
- Added `IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync`.
- Added MySQL persistence for new Passport rows and stamp update using the existing `account_passports` and `account_stamps` shape.
- Added `SmSystemMessage.AttendRewardGet()` for Java message id `1402601`.
- Wired `GameServerConnection` to send the attendance system message and `SM_ATREIAN_PASSPORT` from the successful enter-world flow before macro restore.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_AtreianPassportDailyLoginCreatesRowsAndPersistsStampState` | Unit / live enter-world service | `AtreianPassportService.onLogin`, `AccountPassportsDAO.storePassport` | Eligible login creates all active DAILY Passport rows from Java XML, increments stamps, sets last stamp, and calls Passport login persistence. | Uses runtime-loaded Java XML static data plus repository capture. | Does not cover cumulative, anniversary, purge, excess-limit cleanup, or live MySQL. |
| `HandleInfrastructurePacketAsync_EnterWorldSendsAtreianPassportLoginSnapshotAndRewardMessage` | Unit / live connection and packet send | `PlayerEnterWorldService.enterWorld`, `AtreianPassportService.onLogin`, `SM_SYSTEM_MESSAGE.STR_ATTEND_MSG_ATTEND_REWARD_GET`, `SM_ATREIAN_PASSPORT.writeImpl` | Successful `CM_ENTER_WORLD` sends the attendance message and a Passport snapshot after the DAILY login mutation. | Socket-backed connection invocation with sent-packet observer and serialized Passport payload checks. | Does not assert the full login packet order beyond the Passport branch. |

## Validation Decision

```text
- Changed surface: live enter-world service, live connection packet flow, repository interface/MySQL persistence, system-message packet helper, and focused tests.
- Specific behavior/contract: eligible Passport DAILY login creates runtime rows, persists stamps/new rows, and sends attendance + Passport packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.onLogin/AccountPassportsDAO.storePassport in this checkout.
- Broad-validation trigger: live handler/service/persistence behavior changed, but the risk is isolated to enter-world Passport login and CM_ATREIAN_PASSPORT.
- Broad .NET decision: skipped after focused validation; the filtered command built the affected project and covered the edited live service and connection packet paths.
- Why this scope is sufficient: the UOW is limited to the DAILY Passport login branch and packet emission; focused tests prove state mutation, persistence call, static-data selection, and packet visibility while broader Passport branches remain documented gaps.
```

Results:

- Focused C# validation passed: 75/75 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | DAILY row creation, stamp increment, disabled gate, attendance reset, and packet intent are wired. CUMULATIVE, ANNIVERSARY, fake stamps, purge, limit cleanup, and exact timezone comparison remain open. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository update | Partial | Unit/Compile Tested | Partial Parity | Inserts new rows with Java duplicate-key rewarded merge and updates `account_stamps`. No live MySQL integration evidence in this UOW. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_ATTEND_MSG_ATTEND_REWARD_GET` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.AttendRewardGet` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1402601` is sent from live enter-world; generic system-message serialization already existed. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService.enterWorld` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleInfrastructurePacketAsync` | Connection flow | Partial | Unit Tested | Partial Parity | Sends Passport login packets before macro restore. Full Java retail login ordering remains broader Phase 6 work. |

## Known Gaps

- Full Atreian Passport service parity is not claimed.
- `AtreianPassportService.purgeExpiredPassports` is not yet wired into C# login; expired available rows can still remain in login snapshots.
- CUMULATIVE Passport behavior remains incomplete: real cumulative rewards, fake upcoming stamps, and rewarded fake stamps.
- ANNIVERSARY Passport behavior remains incomplete.
- `checkPassportLimit` excess reward-box cleanup is not ported.
- Java audit logging for invalid/missing/already rewarded/deleted claim attempts is still absent.
- No live MySQL integration test was run for `SaveAccountPassportLoginMutationAsync`.
- Runtime clock behavior uses an injectable UTC clock; exact Java `ServerTime.now()` timezone parity remains unverified.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 7
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire `AtreianPassportService.purgeExpiredPassports` into enter-world login so expired available rows are deleted before the Passport snapshot.
2. Port the smallest CUMULATIVE Passport login slice: create the real cumulative reward row when `attend_num == stamps + 1`.
3. Port CUMULATIVE fake stamp rows for already earned/upcoming cumulative rewards.
