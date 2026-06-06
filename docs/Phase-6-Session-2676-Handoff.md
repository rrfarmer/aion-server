# Phase 6 Session 2676 Handoff

## Completed UOW

[Phase 6] UOW-2676: Purge expired Atreian Passport login rows.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: enter-world Passport login now deletes expired available restored Passport rows before the login snapshot is sent.
- Java source/runtime path: AtreianPassportService.onLogin -> purgeExpiredPassports -> rewardExpireMinutes deadline -> passport.setPersistentState(DELETED) -> PassportsList.removePassport -> AccountPassportsDAO.storePassportList.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, IPlayerEnterWorldRepository.DeleteAccountPassportAsync, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: expired available Passport rows are removed from live player state, persisted through account_passports deletion, and omitted from the login SM_ATREIAN_PASSPORT snapshot.
- Why this is runtime progress: it mutates live player/account Passport state, persists deletion through the existing database shape, and changes a real server packet sent during enter-world.
```

## Commit

`[Phase 6][UOW-2676] Purge expired passport login rows`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2676-Completion.md`
- `docs/Phase-6-Session-2676-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result:

- Passed: 77
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.purgeExpiredPassports` in this checkout.

## Conservative Parity Status

- Enter-world Passport login now purges expired available rows before DAILY processing and snapshot send.
- The purge skips rewarded rows, fake-stamp rows, unknown templates, and non-expiring templates.
- Successful purge deletion removes the row from live player state, calls account Passport delete persistence, and omits the row from `SM_ATREIAN_PASSPORT`.
- Complete Passport parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.purgeExpiredPassports` | `Aion.GameServer.Services.PlayerEnterWorldService.PurgeExpiredAtreianPassportsAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Expired available rows are deleted before login snapshot. DAO failure ordering differs: C# removes after successful repository delete. |
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, stamp mutation, and packet intent. CUMULATIVE, ANNIVERSARY, fake stamps, and limit cleanup remain incomplete. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassportList` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.DeleteAccountPassportAsync` | Repository delete | Partial | Unit Tested | Partial Parity | Existing Java-shaped delete path is reused for login purge. Needs opt-in live DB evidence. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- CUMULATIVE Passport creation/fake stamp behavior remains incomplete.
- ANNIVERSARY Passport creation/fake stamp behavior remains incomplete.
- `checkPassportLimit` excess cleanup and reward-remove system message remain unported.
- Java audit logging for invalid claim attempts is still absent.
- Login purge uses focused repository capture but not live MySQL integration evidence.
- C# currently removes expired rows only after repository delete succeeds; Java removes from memory and then invokes DAO, logging failures.
- The runtime clock is UTC/injectable; exact Java `ServerTime.now()` timezone behavior has not been runtime-compared.

## Next Recommended Runtime UOW

Recommended candidate: port the real CUMULATIVE Passport login reward row branch when `attend_num == passportStamps + 1`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: C# login currently creates DAILY rows but does not create Java CUMULATIVE reward rows when the next attendance threshold is reached.
- Java source/runtime path: AtreianPassportService.onLogin -> CUMULATIVE branch -> doReward && atp.getAttendNum() == pa.getPassportStamps() + 1 -> new Passport -> PersistentState.NEW -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact likely involved: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync, and enter-world Passport packet tests.
- Client-visible/state/persistence effect expected: eligible login creates a real cumulative reward row, persists it with DAILY rows and stamp state, and includes it in the login SM_ATREIAN_PASSPORT snapshot.
- Why this is not preview-only/test-only/documentation-only: it mutates live player/account Passport state, persists a new runtime row, and changes a real login packet.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture is added or discovered. Broad-validation trigger is live enter-world Passport state/persistence/packet behavior, but start with the focused Passport and enter-world tests.

## Other Safe Runtime Candidates

- Port CUMULATIVE fake stamp rows for already earned and upcoming rewards.
- Port ANNIVERSARY Passport login rows.
- Port `checkPassportLimit` excess cleanup after all login row creation branches are live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only Passport followups unless the user explicitly asks for them or they directly unblock a same-session runtime change.
