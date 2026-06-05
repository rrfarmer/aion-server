# Phase 6 Session 2582 Completion

## UOW

[Phase 6] UOW-2582: Filter NPC-faction daily selection by handler availability

## Status

Completed and validated with a focused handler-availability and NPC-faction abandon filter. The live
`CM_DELETE_QUEST` random NPC-faction daily replacement path now uses a runtime `QuestHandlerAvailabilityTable` loaded
with static data and passes `IsHaveHandler` into `QuestAbandonService`, so C# no longer assigns, sends, or persists a
random daily quest candidate when the loaded handler availability table does not contain that quest id.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestsData.getQuestsByNpcFaction filters random NPC-faction daily candidates through QuestEngine.isHaveHandler before assignment.
- Java source method or runtime path: QuestHandlerLoader.postLoad, QuestEngine.addQuestHandler, QuestEngine.isHaveHandler, XMLQuest.register, QuestsData.getQuestsByNpcFaction.
- C# runtime artifact wired or fixed: QuestHandlerAvailabilityTable, DataManager/XmlDataLoader/StaticData loading, GameServerConnection.HandleDeleteQuestAsync handler predicate wiring, QuestAbandonService candidate filtering.
- Client-visible/state/persistence effect changed: live NPC-faction random daily abandon selection now refuses handler-unavailable candidates before packet send, faction assignment mutation, and persistence.
- Why this is not preview-only/test-only/documentation-only: handler availability is loaded into runtime static data and consumed by live CM_DELETE_QUEST abandon selection.
```

## Java Source Reviewed

- `QuestHandlerLoader.postLoad`:
  - skips null, abstract, interface, and non-public classes;
  - instantiates public concrete `AbstractQuestHandler` subclasses;
  - registers them through `QuestEngine.addQuestHandler`.
- `AbstractQuestHandler(int questId)`:
  - stores the handler quest id used by `QuestEngine.addQuestHandler`.
- `QuestEngine.init`:
  - loads Java quest handler classes from `GSConfig.QUEST_HANDLER_DIRECTORY`;
  - registers XML quest scripts via `DataManager.XML_QUESTS.getAllQuests()`;
  - `isHaveHandler` checks the resulting handler map.
- `XMLQuests.afterUnmarshal`:
  - indexes quest script entries by `id`.
- `QuestsData.getQuestsByNpcFaction`:
  - filters NPC-faction candidates through `QuestEngine.isHaveHandler` before start conditions.

## C# Changes

- Added `QuestHandlerAvailabilityTable`:
  - scans merged `quest_scripts` XML ids from the static-data cache;
  - scans Java handler source files for public concrete `AbstractQuestHandler` classes and their constructor `super(questId)` ids;
  - exposes Java-shaped `IsHaveHandler(int questId)`.
- Added `XmlDataLoaderOptions.QuestHandlerDirectory`.
- `DataManager.LoadAsync(repoRoot, ...)` now passes `game-server/data/handlers/quest` into static-data loading.
- `StaticData` now exposes `QuestHandlers`.
- `GameServerConnection.HandleDeleteQuestAsync` now passes `staticData.QuestHandlers.IsHaveHandler` into `QuestAbandonService.Abandon`.
- Updated the NPC-faction random-branch test to use the new runtime table as the handler predicate.

## Known Gaps

- This is a source/static-data availability table, not dynamic C# quest handler execution. It matches Java's handler-id gate for the live selector but does not execute quest handlers.
- Java's compiled runtime can instantiate handlers whose constructor quest id is not a literal or simple int constant; the C# source scanner intentionally fails closed for unsupported patterns.
- Full Java/C# loaded handler count parity was not verified in this UOW.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Validation Decision

```text
Validation decision:
- Changed surface: production runtime static-data loading, live quest abandon candidate filtering, focused tests.
- Specific behavior/contract: runtime handler availability is loaded from Java handler/XML quest sources and used by NPC-faction random daily selection to reject unavailable quest ids before packet/state/persistence effects.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~QuestHandlerAvailabilityTableTests|FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests|FullyQualifiedName~QuestNpcStartRegistrationSourceLoaderTests|FullyQualifiedName~NearbyQuestTemplateTableTests" --no-restore -> 34/34 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet/state/persistence selection behavior and runtime static-data loading changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered handler availability parsing/loading plus abandon candidate filtering.
- Why this scope is sufficient: tests cover concrete public Java handler id extraction, abstract/non-public skip behavior, XML quest-script id loading, StaticData exposure, and `QuestAbandonService` filtering through the runtime table.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestHandlerLoader.postLoad` / `QuestEngine.addQuestHandler` | `QuestHandlerAvailabilityTable.TryReadJavaHandlerQuestId` / `Load` | Runtime loading | Partial | Unit Tested | Partial Parity | Public concrete Java handler ids are loaded from source into runtime availability data; dynamic instantiation/execution is not ported. |
| `XMLQuests.afterUnmarshal` / `XMLQuest.register` | `QuestHandlerAvailabilityTable.Load` XML quest-script scan | Runtime loading | Partial | Unit Tested | Partial Parity | XML quest script ids are included in the runtime availability table; full XML quest handler execution remains unported. |
| `QuestEngine.isHaveHandler` | `QuestHandlerAvailabilityTable.IsHaveHandler` | Service/table | Partial | Unit Tested | Partial Parity | Live selector consumes this predicate; count parity against Java runtime is not yet verified. |
| `QuestsData.getQuestsByNpcFaction` handler filter | `GameServerConnection.HandleDeleteQuestAsync` / `QuestAbandonService.Abandon` | Live handler/selection | Partial | Unit Tested | Partial Parity | Random NPC-faction daily selection now filters through runtime handler availability before packet/state/persistence effects. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TryReadJavaHandlerQuestId_MatchesJavaQuestHandlerLoaderConcretePublicHandlers` | Unit | `QuestHandlerLoader.postLoad`, `AbstractQuestHandler(int)` | Public concrete Java handler source contributes its `super(questId)` id | Source-reviewed Java + unit assertion | Does not instantiate Java classes. |
| `TryReadJavaHandlerQuestId_ResolvesNamedConstructorConstant` | Unit | Representative Java handler constructors | Simple int constant passed to `super` resolves | Source-reviewed Java + unit assertion | More complex expressions fail closed. |
| `TryReadJavaHandlerQuestId_SkipsClassesJavaLoaderWouldSkip` | Unit | `QuestHandlerLoader.isValidClass` | Abstract, package-private, and non-handler classes are skipped | Source-reviewed Java + unit assertion | Java reflection modifiers not executed. |
| `Load_CombinesJavaHandlerIdsAndXmlQuestScriptIds` | Unit | `QuestEngine.init`, `XMLQuests.afterUnmarshal` | Availability table combines Java handler ids and XML quest script ids, excluding unrelated XML ids | Source-reviewed Java + unit assertion | Synthetic XML/source fixture only. |
| `StaticDataLoadFromCache_CarriesQuestHandlerAvailabilityIntoRuntimeData` | Unit | C# runtime loading path | `StaticData.QuestHandlers` is populated during cache load | Runtime C# unit assertion | Synthetic fixture only. |
| `Abandon_NpcFactionQuestRandomBranchHonorsHandlerAvailabilityFilter` | Unit | `QuestsData.getQuestsByNpcFaction` | NPC-faction random branch filters candidates through runtime availability table | Source-reviewed Java + packet/state assertion | Live socket capture not added. |

## Summary Metrics

- Focused validation: 34 tests passed.
- Runtime progress: live random NPC-faction daily selection now uses loaded handler availability.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Handler availability is not yet dynamic C# quest-handler execution.
- No Java runtime handler count comparison was performed; parity remains partial.

## Next Runtime Candidate

UOW-2583: Persist quest work-item inventory deletions during live abandon.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.removeQuestWorkItems deletes all matching quest work-item cube stacks and Java later persists inventory storage changes.
- Java source method or runtime path: QuestService.abandonQuest -> removeQuestWorkItems -> Storage.decreaseByItemId/delete item row persistence through InventoryDAO.store.
- C# runtime artifact to wire or fix: GameServerConnection.HandleDeleteQuestAsync work-item deletion loop, PlayerEnterWorldService.DeleteInventoryItemAsync, existing inventory repository delete path.
- Client-visible/state/persistence effect expected: quest work-item stacks removed during live abandon will be deleted from the existing inventory database rows immediately alongside the current item-delete/cube-size packets.
- Why this is not preview-only/test-only/documentation-only: it persists a live inventory state mutation through the existing database shape from the live CM_DELETE_QUEST path.
```

Risks for UOW-2583:

- Confirm whether logout storage persistence already deletes `TrackDeletedItem` rows; avoid duplicate DB deletes.
- Preserve current packet order: Java removes work items before recipe delete and final abandon packet.
- Focused validation should use `QuestAbandonServiceTests`, `PlayerEnterWorldServiceTests`, and the closest `GameServerConnection` delete-quest test if one exists.
