# Phase 6 Session 2672 Completion

## UOW

[Phase 6] UOW-2672: Grant Atreian Passport reward items.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_ATREIAN_PASSPORT claims now grant the Java-defined reward item instead of only marking the passport TAKEN.
- Java source/runtime path: CM_ATREIAN_PASSPORT.runImpl -> AtreianPassportService.takeReward -> DataManager.ATREIAN_PASSPORT_DATA.getAtreianPassportId(passId) -> ItemService.addItem(player, rewardItemId, rewardItemCount, true, ITEM_COLLECT/INC_PASSPORT_ADD) -> Passport.setRewarded(true) -> AccountPassportsDAO.storePassportList.
- C# runtime artifact wired: StaticData.AtreianPassports, AtreianPassportTable, GameServerConnection.HandleAtreianPassportAsync, InventoryAddService, PlayerEnterWorldService.SaveInventoryRewardMutationAsync, IPlayerEnterWorldRepository.UpdateAccountPassportRewardedAsync, SmInventoryAddItem/SmInventoryUpdateItem, and CmAtreianPassportTests.
- Client-visible/state/persistence effect: a matching restored passport claim adds or stacks the reward item in live player inventory, persists the inventory reward mutation, persists the passport as rewarded, sends the inventory add/update packet, and then sends SM_ATREIAN_PASSPORT with RewardStatus.TAKEN.
- Why this is runtime progress: it loads Java XML/static data into live C# runtime structures, mutates and persists live inventory/passport state, and sends real inventory/passport packets from the live handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ATREIAN_PASSPORT.java`
  - `runImpl` delegates claims to `AtreianPassportService.takeReward`.
- `game-server/src/com/aionemu/gameserver/services/AtreianPassportService.java`
  - `takeReward` resolves the passport template, checks inventory full, checks permit level, checks reward expiry, grants the item through `ItemService.addItem`, marks the passport rewarded, stores changed passport rows, and sends the refreshed passport snapshot through `onLogin`.
- `game-server/src/com/aionemu/gameserver/dataholders/AtreianPassportData.java`
  - JAXB `login_events/login_event` rows are indexed by id after unmarshal.
- `game-server/src/com/aionemu/gameserver/model/templates/event/AtreianPassport.java`
  - Defines `reward_item`, `reward_item_num`, `reward_item_expire_time`, and `reward_permit_level` attributes used by `takeReward`.
- `game-server/data/static_data/events/login_events.xml`
  - Source XML for the reward item mapping used by the C# runtime loader.

## C# Changes

- Added `AtreianPassportTable` and `AtreianPassportSummary`.
- Extended `StaticData` to parse `login_events/login_event` rows from the merged Java XML cache.
- Extended `GameServerConnection.HandleAtreianPassportAsync` so a matching claim now:
  - resolves the Java passport template by id;
  - resolves the reward item template;
  - sends Java's invalid-level system message (`1402573`) when the permit level is not met;
  - skips expired reward rows for now rather than granting stale rewards;
  - creates an inventory grant plan using `InventoryAddService`;
  - sends `FullInventory` and stops the current claim loop when inventory is full;
  - persists inventory reward mutations before marking the passport rewarded;
  - persists the passport rewarded state;
  - sends live inventory add/update packets before the refreshed passport snapshot.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleInfrastructurePacketAsync_AtreianPassportClaimsMatchingRestoredPassport` | Unit / live handler and packet serialization | `AtreianPassportService.takeReward`, `AtreianPassportData`, `ItemService.addItem`, `SM_ATREIAN_PASSPORT.writeImpl` | The live handler loads the Java reward mapping, persists an added reward item, mutates player inventory, persists the passport rewarded state, sends `SmInventoryAddItem`, and sends a TAKEN passport snapshot. | Socket-backed handler invocation plus real `DataManager.LoadAsync`, repository captures, player inventory assertions, and packet ordering assertions. | Does not exercise live MySQL or Java scheduler/login passport creation. |

## Validation Decision

```text
- Changed surface: static data loading, live in-game packet handler, inventory reward persistence path, and passport claim tests.
- Specific behavior/contract: a requested restored passport row grants its Java XML reward item and then becomes TAKEN.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmAtreianPassportTests|FullyQualifiedName~InventoryAddServiceTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for AtreianPassportService.takeReward in this checkout.
- Broad-validation trigger: live handler and static data loader changed.
- Broad .NET decision: skipped after focused validation; the filtered tests compiled the affected projects, covered the live handler path with real static data, and covered inventory add planning.
- Why this scope is sufficient: the UOW is narrow to Passport reward grants and inventory add contracts; remaining service-level gaps are documented and should be handled as separate runtime UOWs.
```

Results:

- Focused C# validation passed: 16/16 tests.
- `git diff --check` passed.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `AtreianPassportData` | `StaticData.AtreianPassports` / `AtreianPassportTable` | Static runtime data | Partial | Unit/Runtime Tested | Partial Parity | Loads reward mapping from Java XML for live claim handling. Full service scheduling/use of active windows is not ported. |
| `AtreianPassport` template | `AtreianPassportSummary` | Template model | Partial | Unit/Runtime Tested | Partial Parity | Captures fields needed by claim reward path. |
| `AtreianPassportService.takeReward` | `GameServerConnection.HandleAtreianPassportAsync` | Service behavior | Partial | Unit Tested | Partial Parity | Grants reward item, handles inventory full, handles permit-level rejection, skips expired rewards, persists reward/passport state, and sends inventory/passport packets. Disabled-service gate, DAO delete of expired rows, audit logging, and full `onLogin` side effects remain incomplete. |
| `ItemService.addItem(... ITEM_COLLECT/INC_PASSPORT_ADD)` | `InventoryAddService` plus `SmInventoryAddItem` / `SmInventoryUpdateItem` | Inventory mutation/packet path | Partial | Unit Tested | Partial Parity | Existing add service is now used by the passport claim path and persists item mutation before packet send. |

## Known Gaps

- `AtreianPassportService.isAtreianPassportDisabled` is not yet wired; C# still answers Passport packets even when Java would disable the service after the final configured reward window.
- Expired claim rows are skipped in C# but not deleted/persisted as Java `PersistentState.DELETED` rows.
- Java audit logging for invalid/missing/already rewarded claims is not ported.
- `AtreianPassportService.onLogin` remains incomplete: daily stamp reset, new passport generation, fake-stamp generation, excess passport purge, and login reward system messages are still open.
- No live MySQL integration test was run for the combined inventory grant plus passport rewarded update.
- No narrow Java/Maven validation exists for this path in the checkout.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 6
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Persist/delete expired Atreian Passport claim rows during `CM_ATREIAN_PASSPORT` handling, matching Java's `PersistentState.DELETED` branch and `AccountPassportsDAO.storePassportList`.
2. Wire the Atreian Passport disabled-service gate so live C# stops processing/sending Passport state after Java's calculated expiration window.
3. Port the login-time `AtreianPassportService.onLogin` slice that creates a daily/cumulative passport and persists account stamp state.
