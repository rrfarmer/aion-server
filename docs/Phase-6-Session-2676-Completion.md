# Phase 6 Session 2676 Completion

## UOW

[Phase 6] UOW-2676: Purge expired Atreian Passport login rows.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: enter-world Passport login now deletes expired available restored Passport rows before the login snapshot is sent.
- Java source/runtime path: AtreianPassportService.onLogin -> purgeExpiredPassports -> rewardExpireMinutes deadline -> passport.setPersistentState(DELETED) -> PassportsList.removePassport -> AccountPassportsDAO.storePassportList.
- C# runtime artifact wired: PlayerEnterWorldService.ApplyAtreianPassportLoginAsync, Player.Passports, IPlayerEnterWorldRepository.DeleteAccountPassportAsync, and the live enter-world SM_ATREIAN_PASSPORT packet path.
- Client-visible/state/persistence effect: expired available Passport rows are removed from live player state, persisted through account_passports deletion, and omitted from the login SM_ATREIAN_PASSPORT snapshot.
- Why this is runtime progress: it mutates live player/account Passport state, persists deletion through the existing database shape, and changes a real server packet sent during enter-world.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `onLogin` calls `purgeExpiredPassports(player)` before daily/cumulative/anniversary login row processing.
  - `purgeExpiredPassports` skips rewarded and fake-stamp rows.
  - It skips missing templates and templates with `rewardExpireMinutes <= 0`.
  - It deletes rows when `now` is after `arriveDate + rewardExpireMinutes`.
  - Deleted rows are removed from `PassportsList` and persisted through `AccountPassportsDAO.storePassportList`.
- `game-server/src/com/aionemu/gameserver/dao/AccountPassportsDAO.java`
  - `storePassportList` routes `PersistentState.DELETED` rows to `deletePassport`.

## C# Changes

- Added `PurgeExpiredAtreianPassportsAsync` to the live Passport login path in `PlayerEnterWorldService`.
- The purge runs after the disabled-service gate and before DAILY reward row creation.
- The purge skips rewarded rows, fake-stamp rows, unknown templates, and non-expiring templates.
- Expired available rows call `DeleteAccountPassportAsync` and are removed from `Player.Passports` before `SM_ATREIAN_PASSPORT` is sent.
- Extended the enter-world test repository fake to capture Passport delete calls.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_AtreianPassportLoginDeletesExpiredAvailableRowsBeforeSnapshot` | Unit / live enter-world service | `AtreianPassportService.purgeExpiredPassports` | Login deletes only expired available rows, keeps rewarded/fake/non-expiring rows, avoids a same-day reward mutation, and preserves stamp state. | Uses runtime-loaded Java XML static data plus repository delete capture. | Does not cover DAO failure semantics or live MySQL. |
| `HandleInfrastructurePacketAsync_EnterWorldOmitsExpiredAtreianPassportFromLoginSnapshot` | Unit / live connection and packet serialization | `AtreianPassportService.onLogin`, `SM_ATREIAN_PASSPORT.writeImpl` | Successful `CM_ENTER_WORLD` removes the expired row and sends a Passport snapshot with zero rows and no attendance reward message. | Socket-backed connection invocation, repository delete capture, and serialized packet assertion. | Does not assert full login packet ordering. |

## Validation Decision

```text
- Changed surface: live enter-world Passport service branch and focused service/connection tests.
- Specific behavior/contract: expired available Passport rows are deleted before login Passport snapshot emission.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmAtreianPassportTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.purgeExpiredPassports in this checkout.
- Broad-validation trigger: live handler/service/persistence behavior changed, but the risk is isolated to the Passport login branch and packet snapshot.
- Broad .NET decision: skipped after focused validation; the filtered command built the affected project and covered the edited live service plus connection packet paths.
- Why this scope is sufficient: the UOW is limited to the purge branch; focused tests prove state mutation, persistence call, skip rules, and packet omission while other Passport login branches remain documented gaps.
```

Results:

- Focused C# validation passed: 77/77 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.AtreianPassportService.purgeExpiredPassports` | `Aion.GameServer.Services.PlayerEnterWorldService.PurgeExpiredAtreianPassportsAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Expired available rows are deleted before login snapshot. C# removes rows after repository delete succeeds; Java removes in memory before DAO logging catches failures. |
| `com.aionemu.gameserver.services.AtreianPassportService.onLogin` | `Aion.GameServer.Services.PlayerEnterWorldService.ApplyAtreianPassportLoginAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Now includes disabled gate, purge, DAILY rows, stamp mutation, and packet intent. CUMULATIVE, ANNIVERSARY, fake stamps, and limit cleanup remain incomplete. |
| `com.aionemu.gameserver.dao.AccountPassportsDAO.storePassportList` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.DeleteAccountPassportAsync` | Repository delete | Partial | Unit Tested | Partial Parity | Reuses existing delete method and SQL shape from prior UOWs. No live MySQL evidence in this UOW. |

## Known Gaps

- Full Atreian Passport service parity is not claimed.
- CUMULATIVE Passport behavior remains incomplete: real cumulative rewards, fake upcoming stamps, and rewarded fake stamps.
- ANNIVERSARY Passport behavior remains incomplete.
- `checkPassportLimit` excess reward-box cleanup is not ported.
- Java audit logging for invalid/missing/already rewarded/deleted claim attempts is still absent.
- No live MySQL integration test was run for the login purge delete path.
- C# removes expired rows only after repository delete succeeds; Java removes in-memory first and logs DAO failures.
- Runtime clock behavior uses an injectable UTC clock; exact Java `ServerTime.now()` timezone parity remains unverified.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port the real CUMULATIVE Passport login reward row branch when `attend_num == passportStamps + 1`.
2. Port CUMULATIVE fake stamp rows for already earned and upcoming rewards.
3. Port ANNIVERSARY Passport login rows after cumulative behavior is stable.
