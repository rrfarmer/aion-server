# Phase 6ALD Completion - Kisk Revive Live Aggro Clear

Date: 2026-05-27
Unit of Work: UOW-1480
Status: Complete after validation.

## Scope

Wire the newly introduced player-owned aggro list into production kisk revive cleanup.

## Completed Work

- Updated `GameServerConnection.HandleReviveAsync` to clear `player.AggroList` through `PlayerReviveCleanupAdapterService`.
- The live clear runs after kisk revive resource/resurrection-state restore and before movement updates / revive emotion fanout.
- Added `HandleReviveAsync_KiskReviveClearsLivePlayerAggro`.
- Preserved existing kisk revive direct packet order.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --no-restore --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerOwnedAggroListTests|FullyQualifiedName~PlayerReviveCleanupAdapterServiceTests"`.
- Result: passed 18 tests.

## Migration Parity Table - UOW-1480

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `Aion.GameServer.Network.Aion.GameServerConnection` / `PlayerReviveCleanupAdapterService` | Service / Connection Flow | Partial | Regression Tested | Partial Parity | C# kisk revive now clears live `player.AggroList` after resource/resurrection-state restore and before movement/emotion fanout, matching the currently modeled Java `revive` order. Other revive types and broader side effects remain pending. |
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | `Aion.GameServer.Model.GameObjects.PlayerOwnedAggroList` | Model / Aggro List | Partial | Unit Tested / Regression Tested | Partial Parity | Production kisk revive now clears the owned list. Known-list integration, Java `Creature` references, target selection, geo visibility, and scheduled hate decay remain missing. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `PlayerOwnedAggroList` / `PlayerReviveCleanupAdapterService` | Base Aggro List | Partial | Unit Tested / Regression Tested | Needs Verification | Live clear empties entries and cancels represented hate-reduction state. Java `ConcurrentHashMap` and `Future.cancel(true)` behavior are represented, not runtime-equivalent. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REVIVE` | `CmRevive` / `GameServerConnection.HandleReviveAsync` | Packet Handler | Partial | Regression Tested | Partial Parity | Kisk revive path now includes live aggro clear for revive id 4. Non-kisk revive ids remain intentionally ignored in this handler slice. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReviveAsync_KiskReviveClearsLivePlayerAggro` | Regression / workflow | `PlayerReviveService.kiskRevive` -> `revive` -> `player.getAggroList().clear()` | Kisk revive clears live C# `Player.AggroList`, clears represented hate-reduction task state, teleports to the kisk, and preserves existing direct packet order. | Deterministic C# workflow regression from Java source audit. | Does not compare Java runtime packet bytes or real Java `Future` cancellation. |
| Existing `GameServerConnectionKiskReviveWorkflowTests` | Regression / workflow | `CM_REVIVE`, `PlayerReviveService.kiskRevive`, `TeleportService.teleportTo` | Existing kisk revive charge, removal, target cleanup, movement-update, fanout, and object-id release behavior remained stable. | Focused workflow suite passed with aggro/model tests. | Full revive socket ordering and non-kisk revive types remain broader. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Live aggro clear is now wired only for kisk revive; bind/instance/skill/item/duel revive paths remain unported or outside this handler slice.
- C# uses object-id aggro snapshots rather than Java live `Creature` references and `KnownList`.
- Java threading behavior for `AggroList.clear` is represented by state, not real scheduled task cancellation.
- `PlayerReviveService.revive` order is only partially represented because C# still lacks `onBeforeSpawn`, protection task, instance/legion leave, and full teleport/despawn/spawn lifecycle support.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 production kisk revive live aggro-clear hook and 1 workflow regression
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, non-kisk revive live aggro clears, live known-list integration, scheduled hate decay, full revive/teleport side effects, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue revive parity by modeling the next `PlayerReviveService.revive` side-effect boundary that has enough C# support.
- Recommended first slice: `onBeforeSpawn` / protection-task metadata planning, because it sits immediately after aggro clear in Java order.
- Alternative slice: flying-before-death restoration planning for kisk/skill/item revive, but this requires more fly-controller state.

## Suggested Acceptance Criteria

- Re-audit Java `PlayerController.onBeforeSpawn`, related protection task setup, and where `PlayerReviveService.revive` calls it.
- Keep live mutation disabled unless the C# runtime has enough state to execute it safely.
- Preserve kisk revive packet order and re-run `GameServerConnectionKiskReviveWorkflowTests`.
- Update parity table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `onBeforeSpawn` / protection metadata planning | player controller/revive service docs and new planner tests | Medium | Start read-only; production wiring should stay sequential. |
| B | Flying-before-death revive planning | fly controller/player state tests | Medium | Needs Java fly-controller audit. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker. |

## Do Not Parallelize

- Shared `GameServerConnection` revive flow.
- Shared kisk revive workflow fixture edits.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1480] Wire kisk revive aggro clear`.
- Files changed in UOW-1480:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionKiskReviveWorkflowTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ALD-Completion.md`
