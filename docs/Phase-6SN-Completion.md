# Phase 6SN Completion - Nearby Quest Refresh Send Boundary Audit

Date: May 25, 2026
Unit of Work: UOW-996

## Session Summary

This unit added a read-only Java/C# audit for the future nearby quest refresh send boundary and production safety gates.

Java remains the source of truth. This unit does not send `SM_NEARBY_QUESTS`, wire `CM_LEVEL_READY`, implement NPC-spawn delayed refresh, wire production `StaticData`, or enable production ItemPurification dispatch.

## Completed Work

- Added `docs/NearbyQuestRefresh-SendBoundary-Audit.md`.
- Audited Java send triggers:
  - `CM_LEVEL_READY.runImpl` immediate owner `updateNearbyQuests()` call.
  - `WorldMapInstance.addObject(Npc)` delayed 1500 ms one-pending-task instance refresh.
  - `PacketSendUtility.sendPacket` online-player send gate.
- Audited C# boundaries:
  - `GameServerConnection.HandleLevelReadyAsync`.
  - `GameServerConnection.SendPacketAsync`.
  - `GameClientSocketServer.SendPacketToPlayerAsync`.
  - `SmNearbyQuests`.
  - `NearbyQuestMarkerProjectionService`.
  - `NoOpItemPurificationNearbyQuestRefreshDispatcher`.
- Updated nearby-refresh, nearby start-condition, AP/quest readiness, automatic-dispatch readiness, and Phase 6 progress docs.

## Validation

- Documentation/source-review only.
- No tests were run because this unit changed only documentation.

## Files Changed

- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SN-Completion.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY`
- `com.aionemu.gameserver.world.WorldMapInstance.addObject`
- `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS`
- `com.aionemu.gameserver.services.QuestService.checkStartConditions`
- `com.aionemu.gameserver.questEngine.QuestEngine.onItemGet` / `onItemRemoved`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLevelReadyAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.SendPacketAsync`
- `Aion.GameServer.Network.Aion.GameClientSocketServer.SendPacketToPlayerAsync`
- `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`
- `Aion.GameServer.Services.NearbyQuestMarkerProjectionService`
- `Aion.GameServer.Services.NearbyQuestStartConditionService`
- `Aion.GameServer.Services.NoOpItemPurificationNearbyQuestRefreshDispatcher`

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby refresh send service using `NearbyQuestMarkerProjectionService` and `SmNearbyQuests` | Controller / Quest UI Send Boundary | Not Started | Manual Only | Needs Verification | Java source reviewed for marker calculation and owner packet send. C# has staged marker projection only; no player-controller method, map-region lookup, production quest-template loading, packet send, or Java `HashMap` ordering parity. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleLevelReadyAsync` | Client Packet Handler / Enter-Map Trigger | Partial | Manual Only | Needs Verification | Java calls `updateNearbyQuests()` from level-ready. C# level-ready sends baseline map-ready packets but intentionally does not send nearby quest markers yet. |
| `com.aionemu.gameserver.world.WorldMapInstance.addObject` | `Aion.GameServer.World.WorldMapInstanceRuntimeState`; future NPC-spawn refresh scheduler | World Instance / Delayed Refresh Trigger | Partial | Regression Tested plus Manual Audit | Partial Parity | Staged quest-id storage/projection has tests, but production `addObject(Npc)`, `QuestEngine.getQuestNpc`, 1500 ms debounce scheduling, task reset, and per-player instance fanout are not wired. C# async/threading parity is unknown. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync`; `GameServerConnection.SendPacketAsync` | Packet Send Utility Boundary | Partial | Existing Regression Coverage Elsewhere plus Manual Audit | Needs Verification | C# has owner-send primitives used by other systems. A nearby-refresh caller has not been implemented. Java `player.isOnline()` gating must be matched by active connection/player checks. Socket send failure behavior remains unverified for this path. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests` | Server Packet / Serialization | Complete | Unit Tested | Verified Parity | Existing tests cover source-reviewed byte layout: `C(0)`, negative count, and `1 << 17` marker flag for positive level diff. Caller ordering remains not claimed because Java `HashMap` iteration order is not deterministic. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate Dependency | Partial | Unit Tested | Partial Parity | Current staged predicate covers early gates only and rejects unsupported dependencies. Full XML start conditions, inventory checks, combine-skill, NPC faction, exception/log behavior, and time-based repeat timing remain unsupported. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemGet` / `onItemRemoved` nearby refresh calls | `Aion.GameServer.Services.NoOpItemPurificationNearbyQuestRefreshDispatcher` | Quest Callback / ItemPurification Refresh Dependency | Partial | Unit Tested for Planning; Manual Audit for Send Boundary | Needs Verification | ItemPurification can plan refresh candidates through `questUpdateItems`, but dispatcher stays no-op. Real quest handlers and nearby marker sends remain disabled by design. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `docs/NearbyQuestRefresh-SendBoundary-Audit.md` | Manual | Java `PlayerController.updateNearbyQuests`, `CM_LEVEL_READY`, `WorldMapInstance.addObject`, `PacketSendUtility.sendPacket`, `SM_NEARBY_QUESTS` | Documents Java send triggers, C# send primitives, and production safety gates before packet wiring. | Source-reviewed manual audit. | No executable tests; no runtime packet send or Java comparison artifact. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# has no production nearby-refresh send method.
- `CM_LEVEL_READY` nearby marker send is absent in C#.
- NPC-spawn delayed refresh fanout and the Java 1500 ms debounce are absent in C#.
- Production quest-template loading and production quest-start source loading remain unwired.
- Unsupported nearby predicate dependencies remain broad and must fail closed.
- Java `HashMap`/set ordering is not deterministic; packet marker order parity is not claimed.
- C# async scheduling/threading for a future debounce needs dedicated tests.
- Reflection/dynamic Java quest-handler execution remains unported.
- No date/time behavior was added in this unit; repeat-cycle date/time remains unsupported from prior units.
- No serialization code changed in this unit; existing `SmNearbyQuests` tests remain the serialization evidence.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 0 new runtime artifacts in this unit
- Total artifacts with verified parity: 1 existing packet artifact referenced by this audit
- Total artifacts needing verification: 6
- Total blocked artifacts: 4 blocked/not-started categories: production send method, level-ready send trigger, delayed NPC-spawn refresh scheduler, and unsupported predicate dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit clarifies send-boundary blockers without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged real-data marker projection test for templates that have no unsupported dependencies, or implement a non-sending `NearbyQuestRefreshPlanService` that composes current staged world quest ids, staged templates, and marker projection into a send-ready plan with explicit failure reasons.

Keep actual packet sends, `CM_LEVEL_READY` integration, NPC-spawn delayed refresh, production `StaticData` integration, and production ItemPurification dispatch disabled until follow-up tests cover each gate.

## Next Work Options

### Recommended Sequential Task

- Task: Add a staged real-data marker projection for supported templates only, or a non-sending refresh-plan composer.
- Why: The send boundary is documented; the next safe step is more evidence about which markers can be projected without unsupported dependencies.
- Files: isolated test/service files plus docs. Avoid production send paths.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Supported-template marker projection audit | isolated test file/docs | Medium | Must exclude XML conditions, inventory items, combine skill, NPC faction, and time-based repeats. |
| B | Non-sending refresh-plan service | new service/test files | Medium | Must return explicit blocked reasons instead of sending packets. |
| C | XMLStartCondition dependency expansion | docs/read-only Java source | Low | Read-only dependency map; no production predicate changes. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement selected projection/plan slice and update shared docs | selected service/test files, Phase 6 docs | Production send paths unless this becomes the sole owner |
| Agent A | Read-only XMLStartCondition dependency expansion | Read-only source inspection | All writes |

If no sub-agent tool is available, do the recommended task sequentially.

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestMarkerProjectionService.cs`: one owner at a time if projection behavior changes.
- `GameServerConnection.cs`: production send path; one owner only.
- `GameClientSocketServer.cs`: connection registry/send infrastructure; one owner only.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6SN-Completion.md`, `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, `docs/QuestStartConditions-Nearby-Audit.md`, and `docs/NearbyQuestRefresh-SendBoundary-Audit.md`.
- `docs/commit-conventions.md` is still missing; use the commit format in `docs/orchestration-rules.md`.
- Production `CM_ITEM_PURIFICATION` dispatch and real nearby-refresh packet sends must remain disabled.
