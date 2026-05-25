# ItemPurification Automatic Dispatch Readiness

Date: May 25, 2026
Unit of Work: UOW-960

## Purpose

This document records the policy gates that must be satisfied before C# production packet dispatch can wire `CM_ITEM_PURIFICATION` to live mutation and persistence.

Java remains the source of truth. This policy does not enable automatic dispatch and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ITEM_PURIFICATION.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPurificationService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/ItemStorage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/ItemStoneListDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/AbyssRankDAO.java`

## Current C# State

Current C# source breadcrumbs:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistentLiveExecutionService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveExecutionService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveMutationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistencePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `docs/ItemPurification-Java-Observer-Design.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`

Implemented opt-in seams:

- `HandleItemPurificationAsync` builds the handler workflow/application/packet plan without live mutation.
- `HandleItemPurificationLiveExecutionAsync` explicitly executes live send/mutation without persistence.
- `ItemPurificationPersistentLiveExecutionService.ExecuteAsync` explicitly composes live execution, persistence payload generation, and repository save.
- `HandleItemPurificationPersistentLiveExecutionAsync` exposes that persistent live path only to explicit tests/callers.

Production dispatch remains disabled:

```csharp
case CmItemPurification itemPurification:
	if (_activePlayer != null)
		await HandleItemPurificationAsync(_activePlayer, itemPurification);
	break;
```

This must remain plan-only until the gates below are satisfied.

## Gating Requirements

### 1. Persistence Integration

Required before automatic dispatch:

- A live MySQL integration test, or equivalent deterministic database fixture, must verify `SaveItemPurificationMutationAsync` writes:
  - material item count updates
  - exhausted material deletes
  - base item update/delete
  - added target item rows
  - inherited target `item_stones` rows
  - updated abyss rank rows when AP is spent
- Repository failure behavior must be tested against the real transaction path, not only `EmptyPlayerEnterWorldRepository`.
- The C# transaction boundary must remain documented as an intentional safety difference unless Java runtime evidence proves partial category commits are required.

Current status: not satisfied. Fake repository, row-mapper tests, one live DB happy-path test, and one live DB rollback test exist, but Java runtime comparison, quest callbacks, AP side effects, packet ordering, and the automatic-dispatch failure policy are still missing.

UOW-963 adds and live-runs an opt-in game-server MySQL integration test for `SaveItemPurificationMutationAsync`, gated by `AION_GAMESERVER_DB_INTEGRATION=1`. It passed against a Docker-hosted Java-shaped `aion_gs` schema on `localhost:3307`, covering the happy-path repository writes for inventory rows, `item_stones`, and `abyss_rank`. This satisfies the first live DB smoke gate only; failure/rollback behavior, Java runtime comparison, quest callbacks, AP side effects, and automatic dispatch remain open.

UOW-964 adds and live-runs a second opt-in MySQL integration test that updates one material row, then forces a missing required-delete failure and verifies the earlier update is rolled back. This records the intentional C# one-transaction safety behavior against the Java-shaped schema. Java's `InventoryDAO` category-level commits remain source-reviewed only and still need Java runtime failure comparison before the transaction-boundary difference can be considered fully characterized.

### 2. Failure Policy

Required before automatic dispatch:

- The server must have an explicit policy for save failure after live packet send/mutation.
- The policy must state whether C# will:
  - keep send/mutate-before-save and surface/log failure without rollback
  - move persistence before packet sending
  - add rollback/reconciliation behavior
  - intentionally diverge from Java ordering for safety
- The selected policy must have handler-level tests.

Current status: staged policy selected, production gate still not satisfied. UOW-958 proves the current opt-in path sends/mutates before a repository save failure is surfaced, and UOW-965 records that this behavior is allowed only for explicit test/caller opt-in. Automatic packet dispatch must remain plan-only until either Java runtime evidence approves send/mutate-before-save without rollback, or a future unit deliberately selects and tests a safer production ordering such as persist-before-send or explicit reconciliation.

Staged failure policy as of UOW-965:

- `HandleInfrastructurePacketAsync` must keep `CM_ITEM_PURIFICATION` on the plan-only `HandleItemPurificationAsync` path.
- `HandleItemPurificationPersistentLiveExecutionAsync` remains an explicit opt-in helper for tests/callers, not production packet dispatch.
- The explicit opt-in helper may send packets and mutate in-memory player state before repository save, then return `PersistenceSaveFailed` if persistence fails.
- No rollback is attempted after packets/state mutation in the explicit opt-in helper.
- This policy is a temporary safety gate, not a claim of Java parity. Java runtime packet/DB artifacts are still required before production dispatch can choose a final failure behavior.

### 3. Quest Callbacks

Required before automatic dispatch:

- Java `Storage.decreaseItemCount` fires item-remove quest callbacks when materials or the base item are deleted.
- Java `Storage.add` fires item-get quest callbacks for the new target item.
- C# must either implement equivalent quest callback fanout or document a deliberate staged limitation that keeps automatic dispatch disabled.

Current status: not satisfied. Quest get/remove callbacks are not modeled in the ItemPurification path.

UOW-966 adds `docs/ItemPurification-AP-Quest-Readiness-Audit.md`, which records the source-reviewed Java callback ordering: remove notifications fire only through `Storage.delete` when material/base item counts reach zero, target get notifications fire only for actor-backed CUBE adds after the storage update packet, and `QuestEngine.onItemRemoved` only refreshes nearby quests for `questUpdateItems` rather than invoking a symmetric remove-handler map. C# has ordered `QuestNotification` metadata in the application plan, but no dispatcher is wired.

### 4. AP Side Effects

Required before automatic dispatch:

- AP spend must execute the side effects Java reaches through `AbyssPointsService.addAp`, including rank update packets and other supported rank-change side effects as their C# homes become available.
- Missing side effects must be explicitly listed, especially rank-limited equipment checks, abyss skill updates, Legion contribution, Siege callbacks, and ranking cache behavior.

Current status: not satisfied. C# carries `AbyssPointsAddPlan` metadata, but ItemPurification live execution does not currently send the AP spend packets or execute broader rank-change side effects.

UOW-966 records the source-reviewed AP gap list in `docs/ItemPurification-AP-Quest-Readiness-Audit.md`: Java sends `STR_MSG_USE_ABYSSPOINT` and `SM_ABYSS_RANK` from `AbyssPointsService.addAp`, broadcasts `SM_ABYSS_RANK_UPDATE`, checks rank-limited equipment, and refreshes abyss skills on rank change. Legion contribution and Siege callback are correctly absent for purification AP spend because Java only contributes positive AP and purification calls the plain `addAp(Player, int)` overload.

### 5. Packet Ordering And Runtime Comparison

Required before automatic dispatch:

- C# packet order for the success message, material updates/deletes, cube updates, target add, and AP packets must be compared against Java runtime or Java-generated artifacts.
- Object-id allocation differences for generated target items must be normalized or documented in comparison artifacts.
- Socket send failure behavior must be understood before enabling production dispatch.

Current status: not satisfied. Fake-registry tests cover C# packet type order, but Java runtime capture is still blocked locally by Java 8 and missing Maven.

UOW-962 adds `docs/ItemPurification-Java-Observer-Design.md` as the proposed Java packet/DB capture schema. Artifacts still need to be generated and compared before this gate is satisfied.

### 6. Storage Semantics

Required before automatic dispatch:

- C# must account for Java storage dirty state, deleted item queues, item-stone load cleanup, collection ordering, and concurrency semantics enough for production mutation.
- Any intentional C# differences, such as immutable snapshot replacement and one-transaction repository writes, must be documented and covered by targeted tests.

Current status: not satisfied. Several storage behaviors are intentionally simplified or unmodeled.

## Minimum Readiness Checklist

Do not wire `HandleInfrastructurePacketAsync` to `HandleItemPurificationPersistentLiveExecutionAsync` until all checklist items are complete or explicitly waived in a future handoff:

- [x] Live DB integration coverage for `SaveItemPurificationMutationAsync`.
- [x] Inserted target `item_stones` DB integration coverage.
- [x] Repository failure/rollback coverage against the real C# transaction path.
- [ ] Final automatic-dispatch failure policy selected and tested; UOW-965 records a staged "dispatch disabled, explicit opt-in only" policy.
- [ ] Quest get/remove callback strategy implemented or formally deferred.
- [ ] AP side-effect gap list updated and required side effects implemented for the dispatch scope.
- [ ] Java runtime packet/DB comparison artifacts generated or an approved temporary verification substitute recorded.
- [ ] Socket send failure behavior understood for the chosen ordering.
- [ ] Migration parity table updated with conservative statuses for every touched Java artifact.

## Next Safe Work

Recommended next units:

1. Generate Java runtime observer artifacts for ItemPurification packet/DB capture when Java 25/Maven tooling is available.
2. Generate Java runtime failure artifacts or deliberately choose a final production failure policy once packet/DB comparison evidence exists.
3. Add quest get/remove callback strategy or a formally documented staged limitation for ItemPurification.
4. Add AP spend packet/side-effect projection tests behind explicit opt-in live execution before production dispatch wiring.

Unsafe next work:

- Do not wire automatic production dispatch directly to live mutation/persistence.
- Do not treat fake repository tests as DB parity.
- Do not mark ItemPurification as verified parity without Java runtime or equivalent deterministic comparison.
