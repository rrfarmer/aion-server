# Phase 6 Session 2679 Handoff

## Completed UOW

[Phase 6] UOW-2679: Create anniversary Atreian Passport login rows.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: enter-world Passport login now creates Java-style ANNIVERSARY rows for active anniversary templates.
- Java source/runtime path: AtreianPassportService.onLogin -> ANNIVERSARY branch -> getAccountAgeInMonths(player, now.toLocalDate()) -> isPassportPresent guard -> real row when monthsAlive == attendNum, fake rewarded row when monthsAlive > attendNum -> addPassport -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.CreationDate, Player.Passports, SaveAccountPassportLoginMutationAsync filtering, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: eligible login creates real anniversary rows or fake rewarded anniversary rows in live player state, persists only real non-fake rows during the reward mutation, and includes them in SM_ATREIAN_PASSPORT.
- Why this is runtime progress: it mutates live player/account Passport state, can persist new real Passport rows, and changes a real login packet.
```

## Commit

`[Phase 6][UOW-2679] Create anniversary passport login rows`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2679-Completion.md`
- `docs/Phase-6-Session-2679-Handoff.md`

## Validation

Narrow service command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorld_AtreianPassportAnniversaryLoginCreatesRealAndFakeRewardRows|FullyQualifiedName~EnterWorld_AtreianPassportAnniversaryRowsAreClientVisibleWithoutDailyRewardPersistence" --no-restore
```

Result:

- First parallel attempt failed due to an `Aion.Commons.dll` build file lock.
- Rerun alone passed: 2/2.

Narrow packet command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot" --no-restore
```

Result:

- Passed: 1
- Failed: 0
- Skipped: 0

Focused adjacent validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result:

- Passed: 83
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.onLogin` anniversary rows in this checkout.

## Conservative Parity Status

- Enter-world Passport login now creates anniversary rows from Java XML and full-month account age.
- Real anniversary rows are live AVAILABLE rows and are included in the login mutation when the Java reward persistence block runs.
- Older anniversary thresholds create fake TAKEN rows in live state and the login snapshot but are not inserted into `account_passports`.
- Complete Passport parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, real/fake CUMULATIVE rows, real/fake ANNIVERSARY rows, stamp mutation, and packet intent. `checkPassportLimit` remains incomplete. |
| `com.aionemu.gameserver.services.AtreianPassportService.getAccountAgeInMonths` | `Aion.GameServer.Services.AtreianPassportAttendPlanService.CreateAccountAgeInMonthsPlan` | Utility | Partial | Unit Tested | Partial Parity | Existing Java-equivalent full-month calculation is now used by live ANNIVERSARY login behavior. Timezone parity remains unverified. |
| `com.aionemu.gameserver.model.account.Passport` | `Aion.GameServer.Model.Account.PlayerPassport` | Model | Partial | Unit Tested | Partial Parity | Anniversary fake taken and real available reward-status mapping is covered through service and packet tests. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | C# filters fake anniversary rows out of account_passports inserts, matching Java fake rows' NOACTION state. Needs opt-in live DB evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAtreianPassport` | Server packet | Partial | Unit Tested | Partial Parity | Snapshot includes ANNIVERSARY fake taken and real available rows. Full login packet ordering remains unverified. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- `checkPassportLimit` excess cleanup and reward-remove system message remain unported.
- Java audit logging for invalid claim attempts is still absent.
- Login anniversary insert/filtering uses focused repository capture but not live MySQL integration evidence.
- The runtime clock is UTC/injectable; exact Java `ServerTime.now()` timezone behavior has not been runtime-compared.

## Next Recommended Runtime UOW

Recommended candidate: port `checkPassportLimit` excess cleanup.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: C# login now creates DAILY, CUMULATIVE, and ANNIVERSARY Passport rows but does not apply Java's reward-box limit cleanup after stamp increment.
- Java source/runtime path: AtreianPassportService.onLogin -> doReward block -> pa.increasePassportStamps -> pa.setLastStamp -> checkPassportLimit(player) -> remove excess reward-box rows and send SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_REMOVE().
- C# runtime artifact likely involved: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync/DeleteAccountPassportAsync as needed, and enter-world system-message/Passport packet tests.
- Client-visible/state/persistence effect expected: reward-eligible login removes excess Passport reward boxes from live state, persists deletions through the existing database shape, and sends the Java reward-remove system message before/with the login Passport snapshot.
- Why this is not preview-only/test-only/documentation-only: it mutates live player/account Passport state, persists deletes, and sends a real server packet.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

If that command is slow, first run only the new limit-cleanup service test and one adjacent connection packet/system-message test, then rerun the adjacent class filter with a longer timeout if needed. Java/Maven is not expected unless a narrow Java fixture is added or discovered. Broad-validation trigger is live enter-world Passport state/persistence/packet behavior, but start with focused Passport tests.

## Other Safe Runtime Candidates

- Add opt-in live MySQL evidence for Passport login insert/delete mutations once cleanup behavior is complete.
- Port remaining Java audit logging for invalid Passport claim attempts.
- Add a focused no-reward packet test for fake rows if packet coverage needs to be split further.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only Passport followups unless the user explicitly asks for them or they directly unblock a same-session runtime change.
