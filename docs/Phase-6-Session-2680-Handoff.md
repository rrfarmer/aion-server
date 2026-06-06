# Phase 6 Session 2680 Handoff

## Completed UOW

[Phase 6] UOW-2680: Clean up excess Atreian Passport reward rows on login.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: enter-world Passport login now applies Java's post-stamp excess reward-box cleanup when the account reaches the Passport save limit.
- Java source/runtime path: AtreianPassportService.onLogin -> doReward block -> increasePassportStamps -> setLastStamp -> checkPassportLimit(player) -> remove oldest non-fake Passport, fallback oldest Passport -> AccountPassportsDAO.storePassportList(delete) -> SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_REMOVE_EXCESS.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, IPlayerEnterWorldRepository.DeleteAccountPassportAsync, AtreianPassportLoginResult, GameServerConnection enter-world send order, and SmSystemMessage.AttendRewardRemoveExcess.
- Client-visible/state/persistence effect: reward-eligible login removes one oldest Passport row from live state when the limit is reached, attempts the existing DB delete mutation, sends system message 1402627 with the reward item name, and omits the removed row from the live SM_ATREIAN_PASSPORT snapshot.
- Why this is runtime progress: it mutates live account Passport state, uses the existing persistence shape, and sends a real server packet from the live enter-world path.
```

## Commit

`[Phase 6][UOW-2680] Clean up excess passport rewards`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/EnterWorldModels.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2680-Completion.md`
- `docs/Phase-6-Session-2680-Handoff.md`

## Validation

Narrow new-test command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AtreianPassportLoginRemovesOldestRewardWhenPassportLimitIsReached|FullyQualifiedName~EnterWorldSendsPassportLimitRemovalBeforeAttendanceReward" --logger "console;verbosity=normal"
```

Result:

- Passed: 2
- Failed: 0
- Skipped: 0

Focused adjacent C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.EnterWorld_AtreianPassport|FullyQualifiedName~CmAtreianPassportTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 20
- Failed: 0
- Skipped: 0

Java/Maven compile command:

```powershell
mvn -pl game-server -am -DskipTests compile
```

Result:

- Build success for root, commons, and game-server.

Notes:

- An earlier broad filtered C# attempt timed out after 124 seconds.
- No narrow Java behavioral fixture exists for `AtreianPassportService.checkPassportLimit` in this checkout.
- Existing unrelated nullable/analyzer warnings were emitted by the C# test project.

## Conservative Parity Status

- Enter-world Passport login now runs the Java-style limit cleanup after stamp mutation.
- One oldest non-fake Passport row is removed at size >= 50; the implemented code falls back to the oldest row when every row is fake.
- The existing delete persistence shape is invoked for the removed row.
- The live connection sends `SmSystemMessage` id `1402627` before the normal attend reward message and before the login Passport snapshot.
- Complete Passport parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.checkPassportLimit` | `Aion.GameServer.Services.PlayerEnterWorldService.CheckAtreianPassportLimitAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Removes oldest non-fake row, falls back to oldest row in code, deletes through repository, and returns item name for packet send. All-fake fallback needs a focused test. |
| `SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_REMOVE_EXCESS` | `SmSystemMessage.AttendRewardRemoveExcess` | Server packet | Ported | Unit Tested | Partial Parity | Message id `1402627` and item-name parameter are covered through live enter-world send observation. |
| `AccountPassportsDAO.storePassportList(DELETED)` | `IPlayerEnterWorldRepository.DeleteAccountPassportAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | Existing delete shape is invoked from live login path. No live MySQL evidence in this UOW. |
| `AtreianPassportService.onLogin` doReward block | `PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Login runtime path | Partial | Unit Tested | Partial Parity | Stamp mutation, limit cleanup, new-row persistence, attend reward message, and snapshot are wired. Full service parity is still not claimed. |

## Known Gaps / Watchouts

- Do not claim full Atreian Passport parity.
- All-fake `checkPassportLimit` fallback is implemented but not separately asserted.
- Java audit logging for invalid Passport claim attempts is still absent.
- Login insert/delete behavior uses focused repository capture but not live MySQL integration evidence.
- Runtime clock behavior remains UTC/injectable; exact Java `ServerTime.now()` timezone behavior has not been runtime-compared.

## Next Recommended Runtime UOW

Recommended candidate: inspect the live Passport claim path against Java `AtreianPassportService.takeReward` and port the smallest missing runtime behavior.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: only proceed if C# reward claiming is missing a Java client-visible state, inventory, persistence, or packet effect.
- Java source/runtime path: AtreianPassportService.takeReward -> requested passport lookup -> template/reward item validation -> inventory reward grant -> passport rewarded/delete state update -> AccountPassportsDAO.storePassportList -> SM_ATREIAN_PASSPORT.
- C# runtime artifact likely involved: CmAtreianPassport handling, PlayerEnterWorldService claim helpers, inventory reward mutation, Player.Passports, repository reward/delete methods, and SM_ATREIAN_PASSPORT send.
- Client-visible/state/persistence effect expected: a claim-time Passport reward behavior changes live inventory/passport state, persists it through the existing database shape, or changes the emitted Passport snapshot.
- Why this is not preview-only/test-only/documentation-only: the UOW should only be selected if it wires or fixes live claim behavior; if discovery finds no runtime gap, stop and choose another candidate.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests"
```

## Other Safe Runtime Candidates

- Harden the all-fake `checkPassportLimit` fallback only if discovery finds a runtime mismatch; standalone fallback coverage would be test-only and should be skipped.
- Add opt-in live MySQL evidence for Passport login insert/delete mutations only if explicitly requested or paired with a same-session runtime persistence fix.
- Move to the next deferred enter-world packet/state gap from the latest Phase 6 discovery docs if Passport claim behavior is already complete.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- Ignore preview/metadata/evidence-only followups unless the user explicitly asks for them or they directly unblock a same-session runtime behavior change.
