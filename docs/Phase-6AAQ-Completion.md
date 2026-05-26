# Phase 6AAQ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1207
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, non-live runtime task/cooldown state semantics, and scheduled Kinah decrement/failure intent. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1207 added `BindPointTeleportScheduledKinahPlanService`, a source-derived non-live planner for the first side-effect branch inside Java's delayed bind-point `TaskId.SKILL_USE` callback. It models `tryDecreaseKinah(price, ItemPacketService.ItemUpdateType.DEC_KINAH_FLY)` success/failure, records update mask `0x4B`, and records `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` as the failure stop intent.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportScheduledKinahPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAQ-Completion.md`

## What Changed

- Added scheduled Kinah decrement intent:
  - enough Kinah -> `DEC_KINAH_FLY` decrement intent and continuation to cooldown/final flow;
  - exact Kinah -> decrement leaves zero and continues;
  - not enough Kinah -> not-enough-fee system-message intent and stop before cooldown/fanout/movement.
- Kept all behavior non-live and side-effect free.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportScheduledKinahPlanServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 51 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1207

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled Kinah branch | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService` | Service / Inventory Mutation Planner | Partial | Unit Tested | Needs Verification | Non-live planner models scheduled `tryDecreaseKinah(price, DEC_KINAH_FLY)` success and not-enough-fee failure branch. It does not mutate inventory, send item update packets, send system messages, or run from a live scheduler. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService` | Inventory Dependency | Partial | Unit Tested | Needs Verification | Planner uses scalar current/required Kinah facts and treats `currentKinah >= requiredPrice` as success. Live storage item object, persistence, packet ordering, and concurrency remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService.DecKinahFlyUpdateTypeName` / `DecKinahFlyUpdateTypeMask` | Enum / Packet Metadata Dependency | Partial | Unit Tested | Needs Verification | Records Java update type name and mask `0x4B` for future inventory packet use. No concrete inventory update packet is emitted in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService.NotEnoughFeeSystemMessage` | System Message Dependency | Partial | Unit Tested | Needs Verification | Failure intent records the Java system-message id and stop branch. No concrete `SmSystemMessage` packet or live send is emitted. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportScheduledKinahPlanServiceTests.CreatePlan_EnoughKinahRecordsDecKinahFlyIntent` | Success intent, update mask/name, and continuation to cooldown/final flow. | Source-derived only. |
| `BindPointTeleportScheduledKinahPlanServiceTests.CreatePlan_ExactKinahStillContinuesLikeTryDecreaseKinahSuccess` | Exact Kinah equality leaves zero and continues. | Source-derived only. |
| `BindPointTeleportScheduledKinahPlanServiceTests.CreatePlan_NotEnoughKinahSendsFeeMessageAndStops` | Not-enough-fee message intent and stop before cooldown/fanout/movement. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live scheduled Kinah planner plus focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 5 grouped categories: live connection dispatch, live scheduler/cooldown ownership, live inventory mutation/packets, persistent known-list parity, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` still does not dispatch `CmBindPointTeleport`.
- Scheduled Kinah planner consumes scalar current Kinah; live storage object, persistence, item update packet ordering, and concurrency remain unported.
- Runtime-state plans still do not create/cancel real scheduled tasks or mutate a real static cooldown map.
- Cooldown mutation/fanout execution, final death/about-to-die recheck, and movement remain unported.
- No Java runtime comparison was executed. Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Audit/model the final death/about-to-die movement gate for Java's inner delayed `TeleportService.teleportTo` call.
- Why: The scheduled callback now has task/cooldown metadata and Kinah success/failure intent, but the last movement branch is still gated by `!player.getLifeStats().isAboutToDie() && !player.isDead()`. That gate should be modeled before any live `PlayerTeleportService` call is considered.
- Required behavior:
  - input `playerIsDead` and `playerIsAboutToDie` facts;
  - both false -> final teleport intent with hotspot world/x/y/z facts;
  - either true -> no movement intent;
  - no live movement, no position mutation, no known-list fanout.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportFinalMovementPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportFinalMovementPlanServiceTests.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Final death/about-to-die movement gate | new service/test files | Medium | Best next step before live movement. |
| B | Compose scheduled callback metadata from runtime-state + Kinah + cooldown/fanout planners | existing/new composition files | Medium | Keep non-live. |
| C | Audit concrete system-message packet support for not-enough-fee | read-only packet/system-message files | Low/Medium | Useful before live send. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until final movement gate and callback composition exist.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until movement gate and packet ordering are modeled.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
  - `game-server/src/com/aionemu/gameserver/model/stats/container/PlayerLifeStats.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- Latest completed commits:
  - `71a9648a0 [Phase 6][UOW-1206] Add bind point teleport runtime state plan`
  - next commit should be `[Phase 6][UOW-1207] Add bind point teleport scheduled Kinah plan`
- Keep live bind-point behavior disabled until final movement gating, cooldown mutation/fanout execution, live inventory mutation/packets, and live known-list fanout each have focused parity slices.
