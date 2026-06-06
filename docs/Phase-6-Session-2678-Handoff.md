# Phase 6 Session 2678 Handoff

## Completed UOW

[Phase 6] UOW-2678: Create cumulative Atreian Passport fake rows.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: enter-world Passport login now creates Java-style fake CUMULATIVE rows for active cumulative templates missing from the account snapshot.
- Java source/runtime path: AtreianPassportService.onLogin -> CUMULATIVE else-if -> !pa.getPassportsList().isPassportPresent(atp.getId()) -> new Passport -> setFakeStamp(true) -> setRewarded(true) when atp.getAttendNum() <= pa.getPassportStamps() -> addPassport -> SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync filtering, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: login now adds fake taken/upcoming cumulative rows to live player state and the Passport snapshot; only real non-fake new rows are persisted with stamp updates, matching Java PersistentState behavior.
- Why this is runtime progress: it mutates live player/account Passport state and changes a real server packet sent during enter-world.
```

## Commit

`[Phase 6][UOW-2678] Create cumulative passport fake rows`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2678-Completion.md`
- `docs/Phase-6-Session-2678-Handoff.md`

## Validation

Narrow service command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorld_AtreianPassportCumulativeLoginCreatesFakeTakenAndUpcomingRows|FullyQualifiedName~EnterWorld_AtreianPassportCumulativeFakeRowsAreClientVisibleWithoutDailyRewardPersistence" --no-restore
```

Result:

- Passed: 2
- Failed: 0
- Skipped: 0

Narrow packet command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot" --no-restore
```

Result:

- First parallel attempt failed due to an `Aion.Commons.dll` build file lock.
- Rerun alone passed: 1/1.

Focused adjacent validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result:

- Passed: 81
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.onLogin` fake cumulative rows in this checkout.

## Conservative Parity Status

- Enter-world Passport login now creates fake cumulative rows for missing active cumulative Passport ids.
- Fake rows are added to live player state and appear in `SM_ATREIAN_PASSPORT` as TAKEN or UPCOMING according to Java `Passport.getRewardStatus`.
- Fake rows are not inserted into `account_passports`; only real non-fake rows are passed to the login mutation while stamp updates still persist on reward-eligible logins.
- Complete Passport parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, real CUMULATIVE threshold rows, fake CUMULATIVE rows, stamp mutation, and packet intent. ANNIVERSARY rows and limit cleanup remain incomplete. |
| `com.aionemu.gameserver.model.account.Passport` | `Aion.GameServer.Model.Account.PlayerPassport` | Model | Partial | Unit Tested | Partial Parity | Fake taken/upcoming reward-status mapping is covered through service and packet tests. Full model parity is not claimed. |
| `com.aionemu.gameserver.model.account.PassportsList` | `Aion.GameServer.Services.PlayerEnterWorldService.HasAtreianPassport` | Collection behavior | Partial | Unit Tested | Partial Parity | CUMULATIVE fake branch now uses id-only presence checks. Other PassportsList methods remain partially ported elsewhere. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | C# filters fake rows out of account_passports inserts to mirror Java fake rows' NOACTION persistence state. Needs opt-in live DB evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAtreianPassport` | Server packet | Partial | Unit Tested | Partial Parity | Snapshot includes fake taken/upcoming and real available cumulative rows. Full login packet ordering remains unverified. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- ANNIVERSARY row creation and anniversary fake rewarded rows are still missing.
- `checkPassportLimit` excess cleanup and reward-remove system message remain unported.
- Java audit logging for invalid claim attempts is still absent.
- Login cumulative insert/filtering uses focused repository capture but not live MySQL integration evidence.
- The runtime clock is UTC/injectable; exact Java `ServerTime.now()` timezone behavior has not been runtime-compared.

## Next Recommended Runtime UOW

Recommended candidate: port ANNIVERSARY Passport login rows.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: C# login currently handles DAILY and CUMULATIVE Passport rows, but ignores Java ANNIVERSARY rows.
- Java source/runtime path: AtreianPassportService.onLogin -> ANNIVERSARY branch -> getAccountAgeInMonths(player, now.toLocalDate()) -> isPassportPresent guard -> real row when monthsAlive == attendNum, fake rewarded row when monthsAlive > attendNum -> addPassport -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact likely involved: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.CreationDate, Player.Passports, SaveAccountPassportLoginMutationAsync filtering, and enter-world Passport packet tests.
- Client-visible/state/persistence effect expected: eligible login creates real anniversary rows or fake rewarded anniversary rows in live player state, persists only real non-fake rows when Java would mark them NEW, and includes them in SM_ATREIAN_PASSPORT.
- Why this is not preview-only/test-only/documentation-only: it mutates live player/account Passport state, can persist new real Passport rows, and changes a real login packet.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

If that command is slow, first run only the new anniversary service test and one adjacent connection packet test, then rerun the adjacent class filter with a longer timeout if needed. Java/Maven is not expected unless a narrow Java fixture is added or discovered. Broad-validation trigger is live enter-world Passport state/persistence/packet behavior, but start with focused Passport tests.

## Other Safe Runtime Candidates

- Port `checkPassportLimit` excess cleanup after ANNIVERSARY row creation is live.
- Add opt-in live MySQL evidence for Passport login insert/delete mutations once row behavior is complete.
- Add a focused no-reward packet test for fake rows if packet coverage needs to be split further.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only Passport followups unless the user explicitly asks for them or they directly unblock a same-session runtime change.
