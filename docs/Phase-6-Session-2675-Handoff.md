# Phase 6 Session 2675 Handoff

## Completed UOW

[Phase 6] UOW-2675: Create daily Atreian Passport login rows.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: successful enter-world now runs the Java DAILY Atreian Passport login branch instead of only restoring existing Passport rows.
- Java source/runtime path: PlayerEnterWorldService.enterWorld -> AtreianPassportService.onLogin -> checkOnlineDate -> DAILY add Passport -> AccountPassportsDAO.storePassport(Account) -> PacketSendUtility sends attendance message and SM_ATREIAN_PASSPORT.
- C# runtime artifact wired: PlayerEnterWorldService, IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository, GameServerConnection enter-world packet flow, SmSystemMessage, and PlayerEnterWorldResult.
- Client-visible/state/persistence effect: eligible login mutates live player Passport rows and stamp state, persists new account_passports rows plus account_stamps, sends STR_ATTEND_MSG_ATTEND_REWARD_GET, and sends SM_ATREIAN_PASSPORT before macro restore.
- Why this is runtime progress: it mutates live account/player state, persists through the existing database shape, and emits real server packets from the live enter-world path.
```

## Commit

`[Phase 6][UOW-2675] Create daily passport login rows`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/EnterWorldModels.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2675-Completion.md`
- `docs/Phase-6-Session-2675-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Result:

- Passed: 75
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.onLogin` or `AccountPassportsDAO.storePassport` in this checkout.

## Conservative Parity Status

- Enter-world now creates active DAILY Passport rows from runtime-loaded Java XML static data when the account has not stamped for the current attendance day.
- The live player snapshot now reflects incremented stamps, a truncated last stamp, and newly arrived Passport rows.
- C# persists the login mutation through `account_passports` and `account_stamps`.
- The live connection sends `STR_ATTEND_MSG_ATTEND_REWARD_GET` and `SM_ATREIAN_PASSPORT` before macro restore.
- Complete Passport parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | DAILY login reward rows and stamp mutation are live. CUMULATIVE, ANNIVERSARY, fake stamps, purge, limit cleanup, and exact timezone behavior remain incomplete. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassport` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveAccountPassportLoginMutationAsync` | Repository update | Partial | Unit/Compile Tested | Partial Parity | Uses Java-shaped insert/update SQL against existing tables. Needs opt-in live DB integration evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_ATTEND_MSG_ATTEND_REWARD_GET` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.AttendRewardGet` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1402601` is emitted from live login. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService.enterWorld` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleInfrastructurePacketAsync` | Connection flow | Partial | Unit Tested | Partial Parity | Passport login packets are sent from the live enter-world path before macro restore. Full retail login ordering remains broader work. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- Login-time `purgeExpiredPassports` is still absent and should be the next smallest runtime slice.
- CUMULATIVE Passport creation/fake stamp behavior remains incomplete.
- ANNIVERSARY Passport creation/fake stamp behavior remains incomplete.
- `checkPassportLimit` excess cleanup and reward-remove system message remain unported.
- Java audit logging for invalid claim attempts is still absent.
- `SaveAccountPassportLoginMutationAsync` has focused handler/service evidence but not live MySQL integration evidence.
- The runtime clock is UTC/injectable; exact Java `ServerTime.now()` timezone behavior has not been runtime-compared.

## Next Recommended Runtime UOW

Recommended candidate: wire Java's `AtreianPassportService.purgeExpiredPassports` into the live enter-world Passport login path.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: C# login currently sends restored available Passport rows even when Java would delete expired rows before the login snapshot.
- Java source/runtime path: AtreianPassportService.onLogin -> purgeExpiredPassports -> rewardExpireMinutes deadline -> passport.setPersistentState(DELETED) -> PassportsList.removePassport -> AccountPassportsDAO.storePassportList.
- C# runtime artifact likely involved: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, IPlayerEnterWorldRepository.DeleteAccountPassportAsync or a batched login delete helper, Player.Passports, and enter-world Passport packet tests.
- Client-visible/state/persistence effect expected: expired available Passport rows are removed from live player state, deleted from account_passports, and omitted from the login SM_ATREIAN_PASSPORT snapshot.
- Why this is not preview-only/test-only/documentation-only: it mutates live player/account Passport state, persists deletion through the existing database shape, and changes a real login packet.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture is added or discovered. Broad-validation trigger is live enter-world Passport state/persistence/packet behavior, but start with the focused Passport and enter-world tests.

## Other Safe Runtime Candidates

- Port the real CUMULATIVE login reward row branch when `attend_num == passportStamps + 1`.
- Port CUMULATIVE fake stamp rows for already earned and upcoming rewards.
- Port ANNIVERSARY Passport login rows after cumulative behavior is stable.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only Passport followups unless the user explicitly asks for them or they directly unblock a same-session runtime change.
