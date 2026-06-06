# Phase 6 Session 2657 Handoff

## Current Phase

Phase 6: Port Game Core.

## Last Completed UOW

[Phase 6] UOW-2657: Schedule live player periodic general and item saves after enter-world.

## Commit

```text
[Phase 6][UOW-2657] Schedule player periodic saves
```

## Runtime Progress Gate Passed

```text
- Deferred/live behavior advanced: successful enter-world now registers live periodic player general and item save tasks.
- Java source/runtime path: PlayerEnterWorldService.enterWorld schedules GeneralUpdateTask and ItemUpdateTask using PeriodicSaveConfig.PLAYER_GENERAL and PLAYER_ITEMS; PlayerLeaveWorldService.leaveWorld deletes the controller, and CreatureController.onDelete cancels registered tasks.
- C# runtime artifact wired: GameServerOptions.PeriodicSave.PlayerGeneralSeconds/PlayerItemsSeconds, PlayerEnterWorldService scheduler dictionaries, and IPlayerEnterWorldRepository periodic persistence methods.
- Client-visible/state/persistence effect: live player/common and dirty inventory state can be persisted during an online session at configured intervals, and leave-world cancels pending callbacks.
- Why this is not preview-only/test-only/documentation-only: it wires actual scheduler callbacks from live enter-world and writes modeled runtime state through the existing database-shaped repository.
```

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
  - Added `PlayerGeneralSeconds` and `PlayerItemsSeconds` to `GameServerPeriodicSaveOptions`.
  - Loaded `gameserver.periodicsave.player.general` and `gameserver.periodicsave.player.items`.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - Added optional `ThreadPoolManager`.
  - Schedules general/item periodic save tasks after successful enter-world.
  - Cancels periodic save tasks on leave-world.
  - Callback methods check live `World` membership before persisting.
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - Added periodic general/items methods to `IPlayerEnterWorldRepository`.
  - Implemented MySQL periodic general save without changing online/logout fields.
  - Implemented MySQL periodic item save for tracked dirty/deleted inventory rows.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
  - Covers defaults and `mygs.properties` overrides for general/items/pets periodic keys.
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
  - Covers scheduling, callback persistence, and leave-world cancellation.
- `docs/Phase-6-Session-2657-Completion.md`
  - Completion report.
- `docs/Phase-6-Session-2657-Handoff.md`
  - This handoff.

## Validation

Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerOptionsTests" --no-restore
```

Result: 69 passed, 0 failed, 0 skipped.

Existing nullable/analyzer warnings were emitted in unrelated surfaces. A narrow Java/Maven command was not run because this checkout has no focused Java fixture for enter-world periodic save scheduling; Java source was reviewed directly.

Broad-validation trigger: live scheduler and persistence surfaces changed.

Broad .NET decision: skipped after focused validation because the filtered suite built the affected projects and directly exercised the changed config, scheduler, callback, and cancellation paths.

## Conservative Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
|---|---|---|---|---|
| `PeriodicSaveConfig.PLAYER_GENERAL` | `GameServerPeriodicSaveOptions.PlayerGeneralSeconds` | Partial parity | Loaded and consumed by live enter-world scheduling. | Other periodic-save keys remain partial. |
| `PeriodicSaveConfig.PLAYER_ITEMS` | `GameServerPeriodicSaveOptions.PlayerItemsSeconds` | Partial parity | Loaded and consumed by live enter-world scheduling. | Item-stone persistence is not yet included in periodic item save. |
| `PlayerEnterWorldService.enterWorld` | `PlayerEnterWorldService.EnterWorldAsync` | Partial parity | Focused live service tests observe fixed-rate scheduling. | Many Java enter-world side effects remain separately partial. |
| `GeneralUpdateTask.run` | `SavePeriodicPlayerGeneralAsync` | Partial parity | Focused callback test proves live scheduled persistence method runs while online. | Abyss rank, full skill-list, quest-list, and house save parity incomplete. |
| `ItemUpdateTask.run` | `SavePeriodicPlayerItemsAsync` | Partial parity | Focused callback test proves live scheduled item persistence method runs while online. | Java `ItemStoneListDAO.save(player)` still missing. |
| `CreatureController.onDelete` task cancellation | `LeaveWorldAsync` periodic cancellation | Partial parity | Focused cancellation test proves callbacks do not run after leave-world cancellation. | No general Java-style controller task map yet. |

## Known Gaps

- Periodic general save does not yet call Java-equivalent abyss-rank, full skill-list, full quest-list, or house persistence.
- Periodic item save does not yet persist item stones.
- Service-local task dictionaries remain a narrower C# representation of Java controller task registration.
- No real MySQL validation was run for the new periodic methods.
- No real client validation was run.

## Next Runtime UOW

**Recommended UOW-2658: wire player item-stone persistence into live periodic item saves.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: periodic inventory saves should also persist item stones, matching Java ItemUpdateTask.
- Java source method or runtime path: PlayerEnterWorldService.ItemUpdateTask.run -> InventoryDAO.store(player) -> ItemStoneListDAO.save(player).
- C# runtime artifact to wire or fix: MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync and existing item-stone SQL/helper methods, or a focused new helper if no reusable method exists.
- Client-visible/state/persistence effect expected: manastone/fusion/godstone/idian changes on player items survive periodic save without requiring logout.
- Why this is not preview-only/test-only/documentation-only: it extends the live scheduled periodic item save callback to persist runtime inventory item-stone state through the existing database shape.
```

Expected focused validation recipe:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --no-restore
```

Behavior under validation: Java `ItemUpdateTask` item-stone persistence contract for live periodic item saves.

Java/Maven: not expected unless a narrow Java item-stone fixture is discovered; otherwise use direct Java source review of `ItemStoneListDAO.save(player)`.

Broad-validation trigger: persistence surface changes. Start with focused service/repository tests; document whether broad .NET validation remains unnecessary after focused evidence.

Safe runtime alternatives:

- Add live periodic abyss-rank persistence to `SavePeriodicPlayerGeneralAsync`, matching `GeneralUpdateTask -> AbyssRankDAO.storeAbyssRank(player)`.
- Wire `gameserver.periodicsave.legion.items` into live `PeriodicSaveService` only if the C# legion warehouse persistence runtime path can be made live in the same UOW.

## Stop Conditions / Blockers

- Do not add more config-only options unless they are wired to live runtime behavior in the same UOW.
- Do not claim verified parity for periodic saves until Java-equivalent data categories are covered with objective evidence.
- Do not use preview/readiness/plan-only services as Phase 6 progress.
