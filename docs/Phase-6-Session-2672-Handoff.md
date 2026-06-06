# Phase 6 Session 2672 Handoff

## Completed UOW

[Phase 6] UOW-2672: Grant Atreian Passport reward items.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT claims now grant the Java-defined reward item before reporting the passport as TAKEN.
- Java source/runtime path: CM_ATREIAN_PASSPORT.runImpl -> AtreianPassportService.takeReward -> DataManager.ATREIAN_PASSPORT_DATA.getAtreianPassportId(passId) -> ItemService.addItem(player, rewardItemId, rewardItemCount, true, ITEM_COLLECT/INC_PASSPORT_ADD) -> Passport.setRewarded(true) -> AccountPassportsDAO.storePassportList.
- C# runtime artifact wired: StaticData.AtreianPassports, AtreianPassportTable, GameServerConnection.HandleAtreianPassportAsync, InventoryAddService, PlayerEnterWorldService.SaveInventoryRewardMutationAsync, IPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync, SmInventoryAddItem/SmInventoryUpdateItem, and CmAtreianPassportTests.
- Client-visible/state/persistence effect: a matching restored passport claim adds or stacks the reward item in live player inventory, persists the item mutation, persists the passport as rewarded, sends the inventory add/update packet, then sends SM_ATREIAN_PASSPORT with RewardStatus.TAKEN.
- Why this is runtime progress: it loads Java XML/static data into runtime C# structures used by live code, mutates/persists live inventory and passport state, and sends real server packets from the live handler.
```

## Commit

`[Phase 6][UOW-2672] Grant passport reward items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/AtreianPassportTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmAtreianPassportTests.cs`
- `docs/Phase-6-Session-2672-Completion.md`
- `docs/Phase-6-Session-2672-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~InventoryAddServiceTests" --no-restore
```

Result:

- Passed: 16
- Failed: 0
- Skipped: 0

Additional hygiene:

```powershell
git diff --check
```

Result: passed.

No Java/Maven command was run; no narrow Java fixture exists for `AtreianPassportService.takeReward` in this checkout.

## Conservative Parity Status

- `CM_ATREIAN_PASSPORT` now performs a narrow live reward grant slice for restored passport rows.
- Java XML `login_events/login_event` reward mappings are loaded into runtime C# static data and consumed by the live handler.
- Successful reward claims persist inventory mutation first, then persist `account_passports.rewarded`, then send inventory and passport packets.
- Full Java Passport service parity is not claimed. Disabled-service handling, expired-row deletion, audit logging, login-time stamp/passport generation, and full `onLogin` behavior remain open.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AtreianPassportData` | `StaticData.AtreianPassports` / `AtreianPassportTable` | Static runtime data | Partial | Unit/Runtime Tested | Partial Parity | Loads `login_event` rows by id for live claim reward resolution. |
| `AtreianPassport` | `AtreianPassportSummary` | Template model | Partial | Unit/Runtime Tested | Partial Parity | Captures reward item/count/expiry/level fields and attendance metadata. |
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Grants rewards and marks rows TAKEN for restored claims. Missing disabled gate, DAO delete for expired rows, audit logging, and full `onLogin`. |
| `ItemService.addItem(... ITEM_COLLECT/INC_PASSPORT_ADD)` | `InventoryAddService` + inventory packets | Inventory mutation/packet path | Partial | Unit Tested | Partial Parity | C# uses existing inventory add planning/persistence and sends add/update packets from the live Passport handler. |

## Known Gaps / Watchouts

- Do not claim complete Atreian Passport parity yet.
- Expired claim rows are skipped but not removed from memory or persisted as deleted.
- The service-wide disabled gate from Java is not wired; this is significant because Java stops processing after the final configured reward window plus grace period.
- The C# handler sends a refreshed snapshot even when no claim mutation occurred, preserving existing partial behavior; Java's disabled service would return without sending in disabled periods.
- The current reward grant path requires runtime static data, item templates, id factory, repository, and `PlayerEnterWorldService`; missing infrastructure causes a safe no-claim path.
- Live MySQL evidence for the combined inventory item grant plus passport rewarded update remains untested.

## Next Recommended Runtime UOW

Recommended candidate: persist/delete expired Passport claim rows in the live `CM_ATREIAN_PASSPORT` handler.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: expired Atreian Passport claim rows are currently skipped instead of being removed and persisted like Java.
- Java source/runtime path: AtreianPassportService.takeReward -> rewardExpireMinutes deadline check -> passport.setPersistentState(DELETED) -> PassportsList.removePassport(passport) -> AccountPassportsDAO.storePassportList(accountId, toRemove) -> deletePassport.
- C# runtime artifact likely involved: PlayerPassport expiry/delete representation or direct repository delete method, GameServerConnection.HandleAtreianPassportAsync, IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository, and CmAtreianPassportTests.
- Client-visible/state/persistence effect expected: an expired claim removes the passport from live player state, persists deletion through the existing account_passports key, and sends a refreshed SM_ATREIAN_PASSPORT without the expired row.
- Why this is not preview-only/test-only/documentation-only: it mutates live account passport state, persists deletion through the existing database shape, and changes the client-visible passport snapshot from the live handler.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Narrow or adjust the filter based on the final repository/service surface touched. Java/Maven is not expected unless a narrow fixture is added or discovered.

## Other Safe Runtime Candidates

- Wire Java's Atreian Passport disabled-service gate into the live handler and login flow, with tests proving disabled periods do not send/claim Passport state.
- Port the smallest `AtreianPassportService.onLogin` slice that creates a daily passport, increments account stamps, persists account passport stamp state, and sends the attendance reward message plus snapshot.
- Add live DB integration evidence for the combined reward grant and passport rewarded update if the opt-in MySQL fixture is available, but do not treat that as the only UOW unless it directly unblocks the next runtime behavior.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- The next UOW must pass the Runtime Progress Gate before editing.
- If a candidate becomes mostly evidence/readiness-only, reject it and re-plan from live runtime gaps.
