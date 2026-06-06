# Phase 6 Session 2671 Handoff

## Completed UOW

[Phase 6] UOW-2671: Persist Atreian Passport reward claims.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT now claims matching restored passport rows instead of only sending the current snapshot.
- Java source/runtime path: CM_ATREIAN_PASSPORT.runImpl -> AtreianPassportService.takeReward -> Passport.setRewarded(true) -> AccountPassportsDAO.storePassportList/updatePassport.
- C# runtime artifact wired: GameServerConnection.HandleAtreianPassportAsync, PlayerPassport.ClaimReward, IPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync, MySqlPlayerEnterWorldRepository, and GameClientSocketServer dependency threading.
- Client-visible/state/persistence effect: a requested unclaimed passport row is persisted as rewarded and the refreshed SM_ATREIAN_PASSPORT snapshot reports RewardStatus.TAKEN.
- Why this is runtime progress: this mutates live player state, persists through account_passports, and sends a real server packet from live handler code.
```

## Commit

`[Phase 6][UOW-2671] Persist passport reward claims`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Account/PlayerPassport.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2671-Completion.md`
- `docs/Phase-6-Session-2671-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~PlayerEnterWorldRepositoryPassportRestoreTests" --no-restore
```

Result:

- Passed: 7
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.takeReward` or `AccountPassportsDAO.updatePassport` in this checkout.

## Conservative Parity Status

- `CM_ATREIAN_PASSPORT` now mutates restored passport state for matching id/timestamp rows.
- `account_passports.rewarded` is updated through the existing database shape before in-memory mutation.
- The refreshed passport snapshot reports TAKEN for successfully persisted claims.
- Full Java reward parity is not claimed: reward item grants, full-inventory handling, level checks, expiry deletion, daily login stamp/passport generation, and live DB evidence remain open.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_ATREIAN_PASSPORT.runImpl` | `GameServerConnection.HandleAtreianPassportAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Executes a narrow claim slice and sends refreshed snapshot. Full service behavior remains incomplete. |
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Matches id/timestamp and persists rewarded state; no reward item grant or rejection branches yet. |
| `AccountPassportsDAO.updatePassport` | `MySqlPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync` | Repository update | Partial | Unit/Compile Tested | Partial Parity | SQL mirrors Java update key. Needs opt-in DB integration evidence. |
| `Passport.setRewarded` | `PlayerPassport.ClaimReward` | Model mutation | Partial | Unit Tested | Partial Parity | Immutable replacement updates Rewarded. Java persistent-state transitions are represented by direct repository call in this slice. |

## Known Gaps / Watchouts

- Do not claim Atreian Passport reward parity yet. A claimed passport currently becomes TAKEN without granting the reward item.
- The next Passport runtime work should either grant the reward item through live inventory mutation/persistence or add the Java rejection branches. Avoid preview/readiness-only Passport followups.
- `GameServerConnection` now receives `IPlayerEnterWorldRepository` from `GameClientSocketServer`; direct test connections without a repository will send snapshots but will not claim rows.
- Live MySQL update behavior was not integration-tested in this UOW.

## Next Recommended Runtime UOW

Recommended candidate: add the smallest safe reward item grant slice for `AtreianPassportService.takeReward` after a passport claim matches and before marking it rewarded.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: claimed Atreian Passport rewards currently mark the row TAKEN but do not grant the Java reward item.
- Java source/runtime path: AtreianPassportService.takeReward -> DataManager.ATREIAN_PASSPORT_DATA.getAtreianPassportId(passId) -> ItemService.addItem(player, rewardItemId, rewardItemCount, true, ITEM_COLLECT/INC_PASSPORT_ADD) -> passport.setRewarded(true).
- C# runtime artifact likely involved: static Atreian Passport data holder if present, GameServerConnection.HandleAtreianPassportAsync, InventoryAddService, Player.InventoryItems, PlayerEnterWorldService/Repository.SaveInventoryRewardMutationAsync, SmInventoryAddItem, and CmAtreianPassportTests or a focused inventory reward test.
- Client-visible/state/persistence effect expected: a successful passport claim adds or stacks the reward item in live inventory, persists the item mutation, sends the inventory add/update packet, then sends SM_ATREIAN_PASSPORT with RewardStatus.TAKEN.
- Why this is not preview-only/test-only/documentation-only: it mutates and persists live inventory state and sends real server packets from the live passport handler.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~InventoryAddServiceTests" --no-restore
```

Narrow the filter if the final implementation does not touch inventory-add planning directly. Java/Maven is not expected unless a narrow Java fixture is added or discovered.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- If static Atreian Passport template data is missing in C#, either load the Java XML/runtime data as part of the same reward-grant UOW or choose a different runtime UOW; do not stop at metadata/readiness scaffolding.
