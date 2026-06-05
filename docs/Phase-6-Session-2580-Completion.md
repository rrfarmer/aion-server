# Phase 6 Session 2580 Completion

## UOW

[Phase 6] UOW-2580: Re-send assigned NPC-faction daily quest after abandon

## Status

Completed and validated with a focused quest-abandon/NPC-faction/packet filter. The live `CM_DELETE_QUEST` path now
sends Java `SM_QUEST_ACTION(int questId)` / `ActionType.UNK` for the `NpcFactions.sendDailyQuest()` reuse branch: an
active faction that becomes `NOTING` and still has a future assigned quest time re-sends its existing assigned daily
quest id.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: NpcFactions.abortQuest -> sendDailyQuest reusable assigned quest branch -> PacketSendUtility.sendPacket(owner, new SM_QUEST_ACTION(questId)).
- Java source method or runtime path: QuestService.abandonQuest, NpcFactions.abortQuest, NpcFactions.sendDailyQuest, SM_QUEST_ACTION(int questId).
- C# runtime artifact wired or fixed: PlayerNpcFactionsSnapshot.GetReusableDailyQuestIds, QuestAbandonService.NpcFactionDailyQuestPackets, GameServerConnection.HandleDeleteQuestAsync, SmQuestAction.Unknown.
- Client-visible/state/persistence effect changed: abandoning an NPC-faction quest can now send a real SM_QUEST_ACTION action id 6 packet to re-offer the still-assigned daily quest before work-item/recipe/abandon packets.
- Why this is not preview-only/test-only/documentation-only: live CM_DELETE_QUEST now emits a real server packet from the NPC-faction abort path.
```

## Java Source Reviewed

- `QuestService.abandonQuest(Player, int)`:
  - invokes `player.getNpcFactions().abortQuest(template)` before work-item deletion and final abandon packet.
- `NpcFactions.abortQuest(QuestTemplate)`:
  - sets the active matching faction row to `NOTING`;
  - immediately calls `sendDailyQuest()`.
- `NpcFactions.sendDailyQuest()`:
  - skips inactive/missing active slots and slots still blocked by `timeLimit`;
  - for `NOTING`, reuses `faction.getQuestId()` when `faction.getTime() > now`;
  - sends `new SM_QUEST_ACTION(questId)`.
- `SM_QUEST_ACTION(int questId)`:
  - uses `ActionType.UNK` id `6`;
  - writes quest id, then `H(1)` and `H(0)`.

## C# Changes

- Added `SmQuestAction.Unknown(int questId)` with Java `ActionType.UNK` payload shape.
- Added `PlayerNpcFactionsSnapshot.GetReusableDailyQuestIds(int currentEpochSeconds)` for Java's reusable assigned daily quest branch.
- Extended `QuestAbandonResult` with `NpcFactionDailyQuestPackets`.
- `QuestAbandonService.Abandon` now computes reusable daily quest packets after NPC-faction abort.
- `GameServerConnection.HandleDeleteQuestAsync` now sends those packets after timer-clear packets and before work-item deletion packets, matching Java's relative ordering.

## Known Gaps

- Java random replacement selection remains missing for the `questId == 0` branch:
  - `QuestsData.getQuestsByNpcFaction`;
  - `QuestEngine.isHaveHandler`;
  - `QuestService.checkStartConditions`;
  - `Rnd.get(quests)`.
- The C# packet currently covers only Java `SM_QUEST_ACTION(int questId)` / `UNK`; normal `ADD` remains the quest-state packet constructor.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Validation Decision

```text
Validation decision:
- Changed surface: production packet serialization, live quest abandon packet output, NPC-faction snapshot state query, focused tests.
- Specific behavior/contract: active NPC-faction abort with a future assigned quest time emits Java action id 6 payload `06 <questId> 0100 0000`; non-reusable branches do not fake random selection.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerNpcFactionsSnapshotTests|FullyQualifiedName~CompleteAscensionQuestPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore -> 97/97 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet send path and packet serialization changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered the edited packet, state, abandon, and persistence-adjacent contracts.
- Why this scope is sufficient: tests assert the Java packet bytes for `SM_QUEST_ACTION(int questId)`, the reusable faction slot selection, and the live abandon service result consumed by GameServerConnection.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `NpcFactions.sendDailyQuest` reusable branch | `PlayerNpcFactionsSnapshot.GetReusableDailyQuestIds` / `QuestAbandonService.Abandon` | State/packet selection | Partial | Unit Tested | Partial Parity | Reuses future assigned quest ids for active NOTING factions; random replacement selection remains missing. |
| `SM_QUEST_ACTION(int questId)` | `SmQuestAction.Unknown` | Packet | Partial | Unit Tested | Partial Parity | Action id 6 payload covered by byte assertion; extra-category suppression still uses existing C# flag, not runtime template lookup. |
| `QuestService.abandonQuest` NPC-faction packet ordering | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Unit Tested through service result | Partial Parity | Daily quest packet is sent before work-item/recipe/abandon packets; full socket capture not added. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Abandon_NpcFactionQuestSendsReusableDailyQuestPacketLikeJava` | Unit | `NpcFactions.sendDailyQuest`, `SM_QUEST_ACTION(int questId)` | Active NPC-faction abandon emits action id 6 packet bytes for a reusable assigned quest | Source-reviewed Java + exact C# packet bytes | No live socket capture. |
| `Abandon_NpcFactionQuestDoesNotRandomlySelectDailyQuestWithoutEligibleReuse` | Unit | `NpcFactions.sendDailyQuest` random branch | C# does not pretend random branch parity when no reusable quest is eligible | Source-reviewed Java branch boundary | Random branch still missing. |
| `GetReusableDailyQuestIds_MatchesJavaSendDailyQuestAssignedQuestBranch` | Unit | `NpcFactions.sendDailyQuest` slot loop | Daily then mentor slot ordering and future assigned quest reuse | Source-reviewed Java control flow | Does not select new random quest ids. |
| `GetReusableDailyQuestIds_SkipsBranchesThatRequireRandomSelectionOrNoPacket` | Unit | `NpcFactions.sendDailyQuest` switch | Expired/zero/start/complete branches are not emitted by the reuse helper | Source-reviewed Java control flow | Random replacement remains separate. |
| `SmQuestActionConstants_MatchJavaActionTypeIds` | Unit | `SM_QUEST_ACTION.ActionType` | Action ids 1 through 6 | Source-reviewed Java enum ids | Constants only; packet bytes covered by abandon test. |

## Summary Metrics

- Focused validation: 97 tests passed.
- Live abandon path now sends the reusable NPC-faction daily quest packet, and still mutates/persists quest and faction state.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Random NPC-faction daily replacement selection is still blocked by missing handler-aware faction quest candidate lookup.
- The current UOW does not add a full `CM_DELETE_QUEST` socket capture; packet ordering is wired in the live handler and packet bytes are covered at service/packet level.

## Next Runtime Candidate

UOW-2581: Implement the random NPC-faction daily quest replacement branch.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: NpcFactions.sendDailyQuest questId == 0 branch -> QuestsData.getQuestsByNpcFaction -> Rnd.get -> faction.setQuestId/time/state -> SM_QUEST_ACTION(questId).
- Java source method or runtime path: NpcFactions.sendDailyQuest, QuestsData.afterUnmarshal, QuestsData.getQuestsByNpcFaction, QuestEngine.isHaveHandler, QuestService.checkStartConditions.
- C# runtime artifact to wire or fix: NearbyQuestTemplateTable NPC-faction index, handler-availability predicate if present, random candidate selection seam, PlayerNpcFactionsSnapshot assignment mutation, QuestAbandonService/GameServerConnection packet output and persistence.
- Client-visible/state/persistence effect expected: when no reusable assigned quest exists, abandoning an NPC-faction quest can assign a new eligible daily quest, persist that assignment, and send the action id 6 packet.
- Why this is not preview-only/test-only/documentation-only: it mutates live NPC-faction assignment state, persists it, and sends a real server packet from live abandon code.
```

Risks for UOW-2581:

- Need a C# equivalent for Java `QuestEngine.isHaveHandler(questId)` or an explicit documented partial branch if handler availability is not available.
- Need deterministic test control for Java `Rnd.get(quests)` without changing runtime random behavior.
- Need to avoid selecting time-based faction quests; Java indexes only `npcfaction_id != 0 && !quest.isTimeBased()`.
