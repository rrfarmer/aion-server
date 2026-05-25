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

UOW-966 adds `docs/ItemPurification-AP-Quest-Readiness-Audit.md`, which records the source-reviewed Java callback ordering: remove notifications fire only through `Storage.delete` when material/base item counts reach zero, target get notifications fire only for actor-backed CUBE adds after the storage update packet, and `QuestEngine.onItemRemoved` only refreshes nearby quests for `questUpdateItems` rather than invoking a symmetric remove-handler map.

UOW-967 adds `ItemPurificationApplicationPlanService.ProjectQuestNotifications`, a pure metadata projection that emits Java-ordered `ItemRemoved` candidates for exhausted material/base deletes and `ItemGet` candidates for target add. This does not wire a dispatcher, invoke quest handlers, refresh nearby quests, or satisfy the automatic-dispatch gate.

UOW-974 adds an explicit opt-in quest-notification seam: `IItemPurificationQuestMutationNotifier` and `NoOpItemPurificationQuestMutationNotifier`. When a notifier is supplied to explicit live execution, the path projects Java-ordered candidates after a successful mutation send and returns a dispatch result. Automatic production dispatch still passes no notifier, so no real quest handler invocation or nearby-quest refresh occurs.

UOW-976 adds `docs/ItemPurification-QuestUpdateItems-Audit.md`, a source audit for Java `QuestEngine.questUpdateItems`. Java builds the update-item membership set from quest XML `<inventory_items><inventory_item item_id=...>` during `QuestEngine.init`; C# currently parses quest drops/collect items but does not expose this set. Automatic production dispatch remains disabled.

UOW-977 adds `StaticData.QuestUpdateItems` backed by `QuestUpdateItemTable` and focused static-data tests. The C# loader now collects distinct quest inventory `item_id` values in Java first-seen order and ignores optional `count`, matching the source-reviewed static-data membership behavior. This does not satisfy the quest-callback gate: no real nearby-quest refresh, get-item handler dispatch, or automatic production dispatch is wired.

UOW-978 adds `PlanningItemPurificationQuestMutationNotifier` and `ItemPurificationNearbyQuestRefreshPlan`, which filter projected ItemPurification get/remove notifications through `StaticData.QuestUpdateItems` and report which candidates would request nearby refresh. This still does not satisfy the quest-callback gate: no player-controller refresh, dynamic quest handler dispatch, or automatic production dispatch is wired.

UOW-979 adds `IItemPurificationNearbyQuestRefreshDispatcher` and `NoOpItemPurificationNearbyQuestRefreshDispatcher`, letting explicit callers consume a refresh plan through a no-op dispatch seam. This still does not satisfy the quest-callback gate: no player-controller refresh, dynamic quest handler dispatch, or automatic production dispatch is wired.

UOW-980 adds `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, documenting that Java nearby refresh requires `SM_NEARBY_QUESTS`, world-instance quest id registration from `QuestNpc.onQuestStart`, `QuestService.checkStartConditions` with nearby-UI parameters, and level-difference marker flags. C# does not have those lower-level surfaces yet, so the dispatcher seam must remain no-op.

UOW-981 adds `SmNearbyQuests` packet serialization and tests for the Java `SM_NEARBY_QUESTS` payload layout. This satisfies only the packet prerequisite; candidate calculation, start-condition evaluation, dynamic quest handlers, player-controller sends, and automatic production dispatch remain disabled.

UOW-982 adds minimal `WorldMapInstanceRuntimeState` quest-id registry storage and tests for Java's duplicate-collapsing `questIds.add(id)` behavior. This satisfies only the storage prerequisite; dynamic `QuestNpc.onQuestStart` population, candidate filtering, player-controller sends, and automatic production dispatch remain disabled.

UOW-983 adds staged `QuestNpcStartTable` storage plus `QuestNpcStartRegistrationSource` metadata for future Java handler and XML quest-start extraction. This satisfies only the in-memory registration prerequisite; no Java/XML extractor, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-984 adds `QuestNpcStartXmlExtractor`, a pure XML quest-script extractor for `start_npc_ids` attributes, including Java `ReportToMany.register` suppression when `start_item_id` is nonzero. This satisfies only an offline XML source-extraction prerequisite; no Java handler source extractor, loader integration, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-985 adds `QuestNpcStartJavaHandlerExtractor`, a conservative Java source extractor for direct `registerQuestNpc(...).addOnQuestStart(...)` calls, including literal ids, simple `int` assignments, `int[]` indexes, and inherited `questId` via `super(...)`. This satisfies only an offline handler source-extraction prerequisite; no loader integration, unresolved-case triage, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-986 adds `QuestNpcStartRegistrationSourceLoader`, a staged offline loader that composes XML quest-script and Java handler extractor outputs from source directories while preserving unresolved handler rows. This satisfies only an offline source-aggregation prerequisite; no production `StaticData`/`DataManager` integration, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-987 adds a focused real-data audit for the staged loader. Current repository data yields 5184 resolved staged start sources, split into 4400 XML and 784 Java handler sources, with 6 unresolved Java handler registrations using `butlerId`. This satisfies only an offline audit baseline; no production integration, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-988 resolves the six `butlerId` Java handler registrations by supporting deterministic static integer-set iteration in the Java handler source extractor. Current repository data now yields 5214 resolved staged start sources, split into 4400 XML and 814 Java handler sources, with zero unresolved Java handler registrations. This improves the offline audit baseline only; no production integration, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-989 feeds the audited loader output into `QuestNpcStartTable` in a focused offline regression test, yielding 1668 registered NPC ids and 5214 registered NPC/quest start pairs. This improves the offline candidate-source baseline only; no production integration, NPC-spawn population, candidate filtering, player-controller send, or automatic production dispatch is wired.

UOW-990 adds `NearbyQuestCandidateProjectionService`, a staged helper that reads `QuestNpcStartTable` for NPC template ids and contributes matching start quest ids to `WorldMapInstanceRuntimeState`. The real-data audit projects 1668 NPC ids into 4503 distinct world-instance quest ids. This improves the offline nearby-candidate baseline only; no production integration, real NPC-spawn invocation, start-condition filtering, player-controller send, or automatic production dispatch is wired.

UOW-991 adds `docs/QuestStartConditions-Nearby-Audit.md`, documenting the Java nearby UI start-condition predicate and level-diff marker calculation. This clarifies the next gate only; no quest-template data boundary, XML start-condition predicate, NPC faction/combine-skill support, player-controller send, or automatic production dispatch is wired.

UOW-992 adds `NearbyQuestTemplateTable` and `NearbyQuestStartConditionService`, staging the early nearby predicate gates and Java level-diff helper. This remains offline and partial: no production quest-template loading, XML start conditions, inventory preconditions, combine skill, NPC faction checks, player-controller send, or automatic production dispatch is wired.

UOW-993 adds `NearbyQuestTemplateXmlExtractor`, a staged XML extractor for the nearby predicate fields represented by `NearbyQuestTemplateSummary`. This remains offline: no production `StaticData` integration, XML start-condition evaluation, player-controller send, or automatic production dispatch is wired.

UOW-994 adds a real-data audit for the staged nearby quest-template extractor, pinning current repository counts before production integration. This remains offline: no predicate invocation over live players, player-controller send, or automatic production dispatch is wired.

UOW-995 adds `NearbyQuestMarkerProjectionService`, a staged bridge from world quest ids through the partial nearby predicate into marker DTOs and rejection reasons. This remains offline: no player-controller send, socket packet write, production static-data integration, or automatic production dispatch is wired.

UOW-996 adds `docs/NearbyQuestRefresh-SendBoundary-Audit.md`, a read-only audit of the future nearby-refresh send boundary. Java sends nearby markers from `CM_LEVEL_READY` and from a delayed 1500 ms `WorldMapInstance.addObject(Npc)` refresh fanout. C# still has no live nearby-refresh send caller, no NPC-spawn refresh scheduler, and no production ItemPurification nearby-refresh dispatcher.

UOW-997 adds a supported-template real-data projection audit for staged nearby markers. The current repository projects 2072 quest ids with no currently unsupported nearby dependencies; one synthetic level-65 Elyos male Gladiator receives 920 staged markers and 1152 supported early-gate rejections. This remains offline and does not satisfy the production dispatch gate.

UOW-998 adds `NearbyQuestRefreshPlanService`, a non-sending plan boundary for staged nearby marker readiness and rejection reporting. It remains offline and does not satisfy the production dispatch gate.

UOW-999 adds staged nearby XML start-condition support for `finished`, `unfinished`, `noacquired`, `acquired`, Java nearby `equipped` no-op behavior, and `required_title`. It remains offline: production `StaticData`, live nearby sends, inventory/combine/NPC-faction predicates, time-based repeat behavior, and reward-group repository hydration are still not dispatch-ready.

UOW-1000 hydrates Java-schema `player_quests.reward` into `PlayerQuestState.RewardGroup`, supporting repository-loaded XML `finished reward` checks. This remains offline: production nearby sends, inventory/combine/NPC-faction predicates, and time-based repeat behavior are still not dispatch-ready.

UOW-1001 adds staged nearby inventory item precondition support, matching Java `inventoryItemCheck` by checking item-id presence only and ignoring optional XML count for this gate. This remains offline: production nearby sends, combine/NPC-faction predicates, and time-based repeat behavior are still not dispatch-ready.

UOW-1002 adds staged nearby repeat timing for completed quests, including `PlayerQuestState.NextRepeatTime`/`CompleteTime`, repository hydration for `next_repeat_time`/`complete_time`, repeat-cycle token preservation, and deterministic Java `QuestState.canRepeat()` edge-case tests. This remains offline: production nearby sends, combine/NPC-faction predicates, quest-finish repeat reset calculation, and timestamp timezone verification are still not dispatch-ready.

### 4. AP Side Effects

Required before automatic dispatch:

- AP spend must execute the side effects Java reaches through `AbyssPointsService.addAp`, including rank update packets and other supported rank-change side effects as their C# homes become available.
- Missing side effects must be explicitly listed, especially rank-limited equipment checks, abyss skill updates, Legion contribution, Siege callbacks, and ranking cache behavior.

Current status: not satisfied. C# carries `AbyssPointsAddPlan` metadata, but ItemPurification live execution does not currently send the AP spend packets or execute broader rank-change side effects.

UOW-966 records the source-reviewed AP gap list in `docs/ItemPurification-AP-Quest-Readiness-Audit.md`: Java sends `STR_MSG_USE_ABYSSPOINT` and `SM_ABYSS_RANK` from `AbyssPointsService.addAp`, broadcasts `SM_ABYSS_RANK_UPDATE`, checks rank-limited equipment, and refreshes abyss skills on rank change. Legion contribution and Siege callback are correctly absent for purification AP spend because Java only contributes positive AP and purification calls the plain `addAp(Player, int)` overload.

UOW-968 adds focused live-execution regression coverage for a rank-dropping AP spend. The test verifies C# produces the modeled spend system-message packet, `SmAbyssRank`, rank-update broadcast packet, rank-limit flag, and abyss-skill flag, but also verifies ItemPurification live execution still skips the AP metadata operation instead of sending those packets. This improves evidence for the AP gate but does not satisfy it.

UOW-969 emits `AbyssPointsAddPlan.PlayerPackets` from the explicit ItemPurification live-execution helper at the existing AP packet-plan slot. This improves the opt-in live path only. Automatic production dispatch remains disabled, and the AP gate is still open for rank-update broadcast execution, rank-limited equipment execution, abyss skill refresh execution, and Java runtime packet comparison.

UOW-970 emits `AbyssPointsAddPlan.RankUpdatePacket` through the visible-player broadcast boundary from explicit ItemPurification live execution, placed after AP owner packets and before later mutation packets. This still affects only the opt-in live path. Automatic production dispatch remains disabled, and the AP gate is still open for rank-limited equipment execution, abyss skill refresh execution, and Java runtime packet comparison.

UOW-971 invokes `EquipmentService.CheckRankLimitItems` from explicit ItemPurification live execution at the AP rank-change point and mutates the player inventory when rank-limited equipment is unequipped. This is still an opt-in live-path step only. Automatic production dispatch remains disabled, and the AP gate is still open for unequip packet fanout, persistence of rank-limited equipment changes, abyss skill refresh execution, and Java runtime packet comparison.

UOW-972 emits the explicit live-path rank-limit unequip fanout after the equipment rank-limit mutation: owner `SmInventoryUpdateItem` equip/unequip packet(s), owner `SmSystemMessage.UnequipRankItem` message(s), and visible-player `SmUpdatePlayerAppearance` broadcast when the equipment result asks for appearance refresh. Automatic production dispatch remains disabled, and the AP gate is still open for persistence of rank-limited equipment changes, stats packet refresh, abyss skill refresh execution, and Java runtime packet/order comparison.

UOW-973 invokes `AbyssSkillService.UpdateSkills` from explicit ItemPurification live execution after the equipment rank-limit pass when AP spend changes rank. The explicit path now mutates `player.Skills` and sends modeled `SmSkillRemove` and `SmSkillList` packets for the resulting deltas. Automatic production dispatch remains disabled, and the AP gate is still open for skill persistence, broader SkillEngine effect apply/remove fanout, configured transform-min-rank plumbing from production options, and Java runtime packet/order comparison.

UOW-975 plumbs `GameServerOptions.Custom.TopRankingXformMinRank` into the explicit ItemPurification live and persistent live helper paths from `GameServerConnection`. The static live-execution service keeps `AbyssSkillService.DefaultTransformMinRank` as its direct-call default. Automatic production dispatch remains disabled, and the AP gate is still open for skill persistence, broader SkillEngine effect apply/remove fanout, and Java runtime packet/order comparison.

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
3. Add quest get/remove callback strategy or a formally documented staged limitation for ItemPurification, next adding combine/NPC-faction nearby predicate support or broader refresh-plan coverage while preserving the no-op dispatcher seam until quest predicate and send-trigger parity exist.
4. Add AP spend packet/side-effect projection tests behind explicit opt-in live execution before production dispatch wiring.

Unsafe next work:

- Do not wire automatic production dispatch directly to live mutation/persistence.
- Do not treat fake repository tests as DB parity.
- Do not mark ItemPurification as verified parity without Java runtime or equivalent deterministic comparison.
