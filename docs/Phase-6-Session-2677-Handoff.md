# Phase 6 Session 2677 Handoff

## Completed UOW

[Phase 6] UOW-2677: Create cumulative Atreian Passport login rows.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: enter-world Passport login now creates real CUMULATIVE reward rows when the next attendance threshold is reached.
- Java source/runtime path: AtreianPassportService.onLogin -> CUMULATIVE branch -> doReward && atp.getAttendNum() == pa.getPassportStamps() + 1 -> new Passport -> PersistentState.NEW -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: eligible login creates a real non-fake cumulative Passport row, persists it with the stamp mutation, and includes it in the login Passport snapshot.
- Why this is runtime progress: it mutates live player/account Passport state, persists new runtime rows through the existing database shape, and changes a real server packet sent during enter-world.
```

## Commit

`[Phase 6][UOW-2677] Create cumulative passport login rows`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2677-Completion.md`
- `docs/Phase-6-Session-2677-Handoff.md`

## Validation

Initial combined focused command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result: timed out after 120 seconds before returning a result.

Narrow commands run after timeout:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~EnterWorld_AtreianPassportCumulativeLoginCreatesThresholdRowAndPersistsIt" --no-restore
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleInfrastructurePacketAsync_EnterWorldIncludesCumulativePassportThresholdRowInLoginSnapshot" --no-restore
```

Result:

- Passed: 1/1 for the new service test.
- Passed: 1/1 for the new connection/packet test.

Focused adjacent validation rerun with a longer timeout:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result:

- Passed: 79
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.onLogin` cumulative login rows in this checkout.

## Conservative Parity Status

- Enter-world Passport login now creates the real cumulative threshold row before incrementing stamps.
- The row is non-rewarded and non-fake, uses the normalized login timestamp, is included in the existing login mutation, and appears in `SM_ATREIAN_PASSPORT`.
- Complete Passport parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Includes disabled gate, purge, DAILY rows, real CUMULATIVE threshold rows, stamp mutation, and packet intent. CUMULATIVE fake stamps, ANNIVERSARY rows, and limit cleanup remain incomplete. |
| `com.aionemu.gameserver.model.templates.event.AtreianPassport` | `Aion.GameServer.Dataholders.AtreianPassportSummary` | Static data DTO | Partial | Unit Tested | Partial Parity | Runtime-loaded Java XML fields used for active CUMULATIVE threshold selection. Full template parity is not claimed. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | Existing login mutation persists new Passport rows and stamp state. Needs opt-in live DB evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_PASSPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmAtreianPassport` | Server packet | Partial | Unit Tested | Partial Parity | Snapshot now includes real cumulative threshold rows created during enter-world. Full login packet ordering remains unverified. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- CUMULATIVE fake stamp rows are still missing.
- ANNIVERSARY row creation and anniversary fake rewarded rows are still missing.
- `checkPassportLimit` excess cleanup and reward-remove system message remain unported.
- Java audit logging for invalid claim attempts is still absent.
- Login cumulative insert uses focused repository capture but not live MySQL integration evidence.
- The runtime clock is UTC/injectable; exact Java `ServerTime.now()` timezone behavior has not been runtime-compared.

## Next Recommended Runtime UOW

Recommended candidate: port CUMULATIVE fake stamp rows for already earned and upcoming rewards.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: C# login currently creates real DAILY and real CUMULATIVE threshold rows, but does not create Java fake CUMULATIVE rows for non-threshold cumulative templates missing from the account snapshot.
- Java source/runtime path: AtreianPassportService.onLogin -> CUMULATIVE else-if -> !pa.getPassportsList().isPassportPresent(atp.getId()) -> new Passport -> setFakeStamp(true) -> setRewarded(true) when atp.getAttendNum() <= pa.getPassportStamps() -> addPassport -> AccountPassportsDAO.storePassport(Account) -> SM_ATREIAN_PASSPORT.
- C# runtime artifact likely involved: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, SaveAccountPassportLoginMutationAsync, and enter-world Passport packet tests.
- Client-visible/state/persistence effect expected: eligible login creates fake cumulative Passport rows for already earned or future thresholds, persists them with the login mutation when doReward is true, and includes them in the login SM_ATREIAN_PASSPORT snapshot with Java-shaped reward status.
- Why this is not preview-only/test-only/documentation-only: it mutates live player/account Passport state, persists runtime rows, and changes a real login packet.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

If that command is slow, first run only the new fake-stamp service test and one adjacent connection packet test, then rerun the adjacent class filter with a longer timeout if needed. Java/Maven is not expected unless a narrow Java fixture is added or discovered. Broad-validation trigger is live enter-world Passport state/persistence/packet behavior, but start with the focused Passport and enter-world tests.

## Other Safe Runtime Candidates

- Port ANNIVERSARY Passport login rows.
- Port `checkPassportLimit` excess cleanup after all login row creation branches are live.
- Add opt-in live MySQL evidence for Passport login insert/delete mutations once row behavior is complete.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only Passport followups unless the user explicitly asks for them or they directly unblock a same-session runtime change.
