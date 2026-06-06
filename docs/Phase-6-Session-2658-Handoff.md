# Phase 6 Session 2658 Handoff

## Completed UOW

[Phase 6] UOW-2658: Persist live periodic player item stones.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live periodic player item persistence now writes item_stones rows after inventory row flushing.
- Java source/runtime path: PlayerEnterWorldService.ItemUpdateTask.run -> InventoryDAO.store(player) -> ItemStoneListDAO.save(player), with ItemStoneListDAO.save(player) reading Player.getAllItems().
- C# runtime artifact wired: MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync, GetPlayerItemStoneSnapshotItems, ReplaceInventoryItemStonesAsync, and existing BuildItemStonePersistenceRows.
- Client-visible/state/persistence effect: modeled manastone, fusion stone, godstone proc count, and idian polish state on current player items can persist during scheduled item saves without logout.
- Why this is runtime progress: the existing live periodic save callback now mutates the runtime database item_stones table; it is not preview-only/test-only/docs-only.
```

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryItemStonePersistenceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/Phase-6-Session-2658-Completion.md`
- `docs/Phase-6-Session-2658-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryItemStonePersistenceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests.SavePeriodicPlayerItemsAsync_ReplacesCurrentItemStonesEvenWhenItemRowIsCleanAgainstJavaSchema_WhenEnabled" --no-restore
```

Result:

- Passed: 4
- Failed: 0
- Skipped: 0
- Note: the gated database integration method returned early because `AION_GAMESERVER_DB_INTEGRATION` was not enabled.
- Existing unrelated nullable/analyzer warnings were emitted.

No Java/Maven command was run; no narrow Java test fixture exists for the reviewed runtime path in this checkout.

## Conservative Parity Status

- Periodic item-stone persistence is now partial runtime parity for modeled C# player item collections.
- The implementation uses delete+insert snapshots for each modeled current item's `item_stones` rows because C# does not currently model Java per-stone persistent state.
- Java `Player.getAllItems()` includes pet bag and cabinet storage items; C# coverage here is limited to inventory/equipment, regular warehouse, and account warehouse collections.

## Next Recommended Runtime UOW

Add live periodic abyss-rank persistence to `SavePeriodicPlayerGeneralAsync`, matching Java:

- `PlayerEnterWorldService.GeneralUpdateTask.run`
- `AbyssRankDAO.storeAbyssRank(player)`

Proposed Runtime Progress Gate:

```text
- Deferred/live behavior advanced: periodic general saves should persist live abyss AP/GP/rank/kill counters instead of waiting for logout or explicit mutations.
- Java source/runtime path: PlayerEnterWorldService.GeneralUpdateTask.run -> AbyssRankDAO.storeAbyssRank(player).
- C# runtime artifact to wire: MySqlPlayerEnterWorldRepository.SavePeriodicPlayerGeneralAsync and existing SaveAbyssRankAsync helper.
- Client-visible/state/persistence effect: live abyss rank state survives periodic general saves without logout.
- Why runtime: extends the scheduled live general save callback to persist modeled player abyss-rank state through the existing database shape.
```

Suggested discovery:

- Re-read required orchestration docs and this handoff.
- Inspect Java `AbyssRankDAO.storeAbyssRank` and `GeneralUpdateTask.run`.
- Inspect C# `Player.AbyssRank` model and `SaveAbyssRankAsync`.
- Confirm whether `SavePeriodicPlayerGeneralAsync` can safely call `SaveAbyssRankAsync` when `player.AbyssRank` is present.

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --no-restore
```

If that is too broad, narrow to the relevant periodic general service/repository tests after adding a focused test. Real DB validation still requires `AION_GAMESERVER_DB_INTEGRATION=1`.

## Safe Runtime Candidates

1. Periodic abyss-rank persistence for modeled `PlayerAbyssRank`.
2. Periodic skill-list persistence if a live C# skill list and SQL contract are already present.
3. Periodic quest-list persistence if a live C# quest state model and SQL contract are already present.

## Blockers / Watchouts

- Do not treat the existing planner/readiness services around legion warehouse item-stone persistence as migration progress.
- Do not claim full Java `Player.getAllItems()` parity until pet bag and cabinet runtime item collections are modeled and included in live persistence.
- Keep DB integration claims conservative unless `AION_GAMESERVER_DB_INTEGRATION=1` is enabled and the test body actually executes.
