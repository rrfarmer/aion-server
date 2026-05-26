# Phase 6 Bind-Point Teleport Known-List Packet Fact Plan Service

Date: May 26, 2026
Unit of Work: UOW-1273
Scope: Add a disabled packet construction fact planner for supplied viewer/subject player snapshots.
Source of truth: Java project.

## Summary

UOW-1273 adds `PlayerKnownListPacketConstructionFactPlanService`, a non-live bridge that creates `PlayerKnownListOperationSideEffectPacketConstructionFacts` from supplied viewer and subject `Player` snapshots.

The service is intentionally conservative. It does not send packets, read live connections, execute Java controllers, hydrate live effect-controller entries, or mutate known-list state. When required inputs are missing, it returns blocked metadata instead of inventing facts.

## Java Source Findings

- Java `SM_PLAYER_INFO.writeImpl` is viewer-sensitive because it reads `AionConnection.getActivePlayer()`.
- Java `PlayerController.sendPlayerInfoPackets` sends `SM_PLAYER_INFO`, `SM_MOTION`, optional ride `SM_EMOTION`, and optional `SM_PLAYER_STANCE`.
- Java ride `SM_EMOTION` captures movement speed and attack-speed facts from live `PlayerGameStats`.
- Java `SM_ABNORMAL_EFFECT` reads mask/effect entries from `EffectController`.
- Java effect entries include effector id, skill id, level, target slot ordinal, and remaining display time.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPacketConstructionFactPlanService.cs`:

- `PlayerKnownListPacketConstructionFactPlanService`
- `PlayerKnownListPacketConstructionFactPlanRequest`
- `PlayerKnownListPacketConstructionFactPlan`
- `PlayerKnownListPacketConstructionAttackSpeedFacts`
- `PlayerKnownListPacketConstructionFactPlanStatus`
- `PlayerKnownListPacketConstructionFactBlocker`

The planner:

- requires supplied viewer and subject players;
- builds `SmPlayerInfoViewerContext` from supplied viewer race/enemy/neutral facts;
- uses active `Player.Motions` for `SmMotion` metadata;
- derives ride movement speed from `PlayerMovementSpeedResolver` when not explicitly supplied;
- requires explicit ride attack-speed facts for ride-mode subjects;
- requires explicit abnormal-effect entries and mask when abnormal effects are requested;
- remains non-live and marks Java controller parity as false.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPacketConstructionFactPlanServiceTests|PlayerKnownListPopulationPlanServiceTests|PlayerKnownListOperationSideEffectPacketConstructionServiceTests" --nologo` passed 17 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance|SmAbnormalEffect" --nologo` passed 286 tests.
- No Java runtime packet capture was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1273

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `Aion.GameServer.Services.PlayerKnownListPacketConstructionFactPlanService` | Fact Planner / Controller Packet Prerequisite | Partial | Unit Tested | Partial Parity | C# can derive supplied-snapshot construction facts for player-info/motion/ride/stance paths. It does not execute live controller callbacks or send packets. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` | `SmPlayerInfoViewerContext` via fact planner | Viewer-Sensitive Packet Fact | Partial | Unit Tested | Needs Verification | Viewer race/enemy/neutral facts are supplied to the planner. No live `AionConnection.activePlayer`, faction/enemy service, or Java runtime comparison is used. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION` | `Player.Motions` through fact planner | Motion Fact Source | Partial | Unit Tested | Needs Verification | Planner forwards active C# motions. Java motion-map expiration and live update timing remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` ride branch | `Player.RideInfo`; `PlayerMovementSpeedResolver`; supplied attack-speed facts | Ride Fact Source | Partial | Unit Tested | Needs Verification | Planner derives ride movement speed from supplied snapshots but requires attack-speed facts. No Java-equivalent live stat container or runtime comparison exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | `Player.StanceSkillId` / direction facts via fact planner | Stance Fact Source | Partial | Unit Tested | Needs Verification | Planner can carry facts for stance packet construction, but Java `StanceObserver` lifecycle remains unverified. |
| `com.aionemu.gameserver.controllers.effect.EffectController` / `SM_ABNORMAL_EFFECT` | supplied `SmAbnormalEffectEntry` plus mask | Effect Fact Source | Partial | Unit Tested | Needs Verification | Planner requires supplied entries/mask and blocks when missing. No live effect map, no-show toggle filtering, target-slot model, or remaining-time computation is ported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_WithSuppliedSnapshotsCreatesNonLivePacketConstructionFacts` | Unit / Fact Planner | `PlayerController.sendPlayerInfoPackets`; `SM_PLAYER_INFO`; `SM_MOTION`; ride `SM_EMOTION` | Supplied viewer/subject snapshots produce non-live construction facts with viewer context, active motions, ride movement speed, and supplied attack speed. | Source-derived fact mapping. | No live connection, stat container, controller callback, or Java runtime capture. |
| `Plan_RideSubjectWithoutRideInfoOrAttackSpeedBlocksFactConstruction` | Unit / Guard | Java ride branch reads `player.ride` and `PlayerGameStats` | Missing ride info and attack-speed facts block fact construction. | Source-derived required inputs. | Java would read live state rather than a plan blocker. |
| `Plan_AbnormalEffectsWithoutEntriesOrMaskBlocksFactConstruction` | Unit / Guard | `SM_ABNORMAL_EFFECT(Creature)` reads `EffectController` mask/effects | Missing abnormal-effect entries or mask blocks fact construction. | Source-derived required inputs. | No live effect-controller hydration. |
| `Plan_AbnormalEffectsWithSuppliedEntriesAndMaskCreatesFacts` | Unit / Fact Planner | `SM_ABNORMAL_EFFECT` supplied constructor shape | Supplied abnormal entries/mask/slots are carried into construction facts. | Source-derived fact mapping. | Remaining time and slot ids are supplied. |
| `Plan_MissingViewerOrSubjectBlocksExplicitly` | Unit / Guard | Java packet construction requires active viewer and subject player | Missing viewer/subject snapshots return explicit blockers. | C# safety boundary. | Java uses live object references and active connection state. |

## Remaining Risks

- Planner is non-live and unwired from population composition.
- Viewer enemy/neutral facts are supplied, not computed from live factions/custom state.
- Attack speed must be supplied; no shared Java-equivalent stat resolver hydrates it.
- Motion expiration and live active-motion timing remain unverified.
- Abnormal-effect entries, slot ids/ordinals, no-show filtering, and remaining time remain supplied.
- Threading and locking remain metadata-only, not Java `KnownList`/`EffectController` synchronization.
- Serialization is indirectly covered by existing packet constructors; no Java runtime golden packet capture was performed.
- Date/time behavior for motion/effect remaining time remains unverified.
- Reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled fact-plan service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live active-viewer context adapter, 1 reusable stat resolver, 1 live motion timing verifier, 1 live effect-controller entry/timer hydrator, 1 live known-list packet dispatcher, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Compose `PlayerKnownListPacketConstructionFactPlanService` with population packet construction as metadata: allow callers to supply per-direction viewer/subject fact-plan requests, build the per-subject facts dictionary for each candidate, and keep all blocked fact plans explicit. Do not wire live dispatch.

## Update After UOW-1274

Population fact-plan composition is now present. `PlayerKnownListPopulationCandidateFact` can carry per-direction fact-plan requests, candidate plans expose `SideEffectFactPlans`, and completed fact plans supplement packet construction facts without overriding request-level explicit facts. Blocked fact plans remain visible as metadata.
