# Phase 6 Session 2680 Completion

## UOW

[Phase 6] UOW-2680: Clean up excess Atreian Passport reward rows on login.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: enter-world Passport login now applies Java's post-stamp excess reward-box cleanup when the account reaches the Passport save limit.
- Java source/runtime path: AtreianPassportService.onLogin -> doReward block -> increasePassportStamps -> setLastStamp -> checkPassportLimit(player) -> remove oldest non-fake Passport, fallback oldest Passport -> AccountPassportsDAO.storePassportList(delete) -> SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_REMOVE_EXCESS.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, IPlayerEnterWorldRepository.DeleteAccountPassportAsync, AtreianPassportLoginResult, GameServerConnection enter-world send order, and SmSystemMessage.AttendRewardRemoveExcess.
- Client-visible/state/persistence effect: reward-eligible login removes one oldest Passport row from live state when the limit is reached, attempts the existing DB delete mutation, sends system message 1402627 with the reward item name, and omits the removed row from the live SM_ATREIAN_PASSPORT snapshot.
- Why this is runtime progress: it mutates live account Passport state, uses the existing persistence shape, and sends a real server packet from the live enter-world path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `checkPassportLimit` returns only when `pl.size() < 50`; size 50 triggers cleanup.
  - The oldest non-fake Passport by `arriveDate` is preferred; if none exists, the oldest Passport is removed.
  - Java removes the selected Passport object from the account list, stores it as `PersistentState.DELETED`, and sends `STR_MSG_ATTEND_REWARD_REMOVE_EXCESS`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_MSG_ATTEND_REWARD_REMOVE_EXCESS(String name)` uses message id `1402627`.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - Deleted Passport rows are persisted through the existing `account_passports` delete shape.

## C# Changes

- Added Java-equivalent limit cleanup after reward stamp mutation in `PlayerEnterWorldService.ApplyAtreianPassportLoginAsync`.
- Removed exactly one selected live `PlayerPassport` row, matching Java's object removal behavior rather than deleting every value-identical row.
- Reused `DeleteAccountPassportAsync` for the persistence delete attempt.
- Added `AtreianPassportLoginResult.ExcessRewardRemovedItemNames` so the live connection can send removal notifications from the enter-world result.
- Added `SmSystemMessage.AttendRewardRemoveExcess` for system message id `1402627`.
- Sent excess-removal messages before the normal attendance reward message and before the Passport snapshot.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_AtreianPassportLoginRemovesOldestRewardWhenPassportLimitIsReached` | Unit / live enter-world service | `AtreianPassportService.checkPassportLimit` | Reward login with existing rows removes the oldest non-fake row, calls the delete mutation, records the removed reward item name, persists new non-fake rows, and updates stamp state. | Uses runtime-loaded Java XML and repository mutation capture. | Does not cover all-fake fallback branch. |
| `HandleInfrastructurePacketAsync_EnterWorldSendsPassportLimitRemovalBeforeAttendanceReward` | Unit / live connection and packet serialization | `AtreianPassportService.onLogin` plus `checkPassportLimit` send order | Enter-world sends system message `1402627` before `1402601`, deletes the selected row, and the serialized `SM_ATREIAN_PASSPORT` omits the removed row. | Socket-backed connection invocation plus serialized packet inspection. | No live client decode or MySQL integration evidence. |

## Validation Decision

```text
- Changed surface: live enter-world Passport login state mutation, persistence delete intent, system-message send order, and Passport snapshot output.
- Specific behavior/contract: Java checkPassportLimit removes one oldest non-fake row at size >= 50, persists a delete, and sends STR_MSG_ATTEND_REWARD_REMOVE_EXCESS before the normal attend reward packet/snapshot.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.EnterWorld_AtreianPassport|FullyQualifiedName~CmAtreianPassportTests" --logger "console;verbosity=minimal"
- Focused Java/Maven command: mvn -pl game-server -am -DskipTests compile
- Broad-validation trigger: live enter-world Passport state/persistence/packet behavior changed, but the affected surface is isolated to Passport login.
- Broad .NET decision: skipped after the focused Passport service/connection slice passed; it builds the edited project and exercises the live Passport flows around the changed path.
- Why this scope is sufficient: focused tests prove the live service mutation, repository delete call, server packet emission/order, and snapshot omission using runtime-loaded Java XML.
```

Results:

- Initial combined class filter timed out at 124 seconds before useful output.
- Narrow new-test validation passed: 2/2.
- Focused adjacent C# validation passed: 20/20.
- Java/Maven compile validation passed for root, commons, and game-server.
- No narrow Java behavioral fixture exists for `AtreianPassportService.checkPassportLimit` in this checkout.
- Existing unrelated nullable/analyzer warnings were emitted by the C# test project.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.checkPassportLimit` | `Aion.GameServer.Services.PlayerEnterWorldService.CheckAtreianPassportLimitAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Removes oldest non-fake row, falls back to oldest row in code, deletes through repository, and returns item name for packet send. All-fake fallback needs a focused test. |
| `SM_SYSTEM_MESSAGE.STR_MSG_ATTEND_REWARD_REMOVE_EXCESS` | `SmSystemMessage.AttendRewardRemoveExcess` | Server packet | Ported | Unit Tested | Partial Parity | Message id `1402627` and item-name parameter are covered through live enter-world send observation. |
| `AccountPassportsDAO.storePassportList(DELETED)` | `IPlayerEnterWorldRepository.DeleteAccountPassportAsync` | Repository mutation | Partial | Unit Tested | Partial Parity | Existing delete shape is invoked from live login path. No live MySQL evidence in this UOW. |
| `AtreianPassportService.onLogin` doReward block | `PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Login runtime path | Partial | Unit Tested | Partial Parity | Stamp mutation, limit cleanup, new-row persistence, attend reward message, and snapshot are wired. Full service parity is still not claimed. |

## Known Gaps

- Full Atreian Passport service parity is not claimed.
- The all-fake Passport limit fallback branch is implemented but not separately tested.
- Java audit logging for invalid/missing/already rewarded/deleted claim attempts remains absent.
- No live MySQL integration test was run for Passport login insert/delete mutations.
- Runtime clock behavior uses an injectable UTC clock; exact Java `ServerTime.now()` timezone parity remains unverified.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect the live Passport claim path against Java `AtreianPassportService.takeReward` and port the smallest missing client-visible state, inventory, persistence, or packet behavior found there.
2. Harden the all-fake `checkPassportLimit` fallback only if discovery finds a runtime mismatch; standalone fallback coverage would be test-only and should be skipped.
3. Add opt-in live MySQL evidence for Passport login insert/delete mutations only if explicitly requested or tied to a same-session persistence behavior fix.
