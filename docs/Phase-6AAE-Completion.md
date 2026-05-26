# Phase 6AAE Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1195
Status: Phase 6 continues; bind-point teleport distance pricing is staged, but requirements, cooldowns, packets, Kinah mutation, and live movement remain incomplete.

## Session Summary

UOW-1195 implemented a pure planner for Java `BindPointTeleportService.calculateTeleportationPrice`. The planner calculates bind-point teleport price from player/hotspot coordinates and client-sent price, including Java's one-Kinah drift tolerance and final max-price reconciliation, without executing live teleport side effects.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportPricePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportPricePlanServiceTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAE-Completion.md`

## What Changed

- Added `BindPointTeleportPricePlanService` and `BindPointTeleportPricePlan`.
- Ported Java bind-point price calculation:
  - Java `PositionUtil` style float-coordinate 3D distance;
  - `distanceCost = (long)(basePrice * distance / 1000d)`;
  - computed price minimum of `1`;
  - warning flag when computed and client-sent prices differ by more than `1`;
  - final price uses the greater of computed and client-sent prices.
- Kept the planner non-live (`IsLive=false`).
- Documented that Java `BindPointTeleportService.checkRequirements`, cooldown map/scheduling, `SM_BIND_POINT_TELEPORT`, Kinah decrement, and final movement are separate follow-up work.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportPricePlanServiceTests" --nologo` passed 4 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1195

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | `Aion.GameServer.Services.BindPointTeleportPricePlanService` | Service / Movement | Partial | Unit Tested | Needs Verification | `calculateTeleportationPrice` is staged and source-derived tested. Missing methods/logic: hotspot lookup, `checkRequirements`, cooldown map, `onLogin`, `teleport`, `cancelTeleport`, live Kinah decrement, packet sends, delayed task scheduling, and final `TeleportService.teleportTo`. |
| `com.aionemu.gameserver.utils.PositionUtil` | private distance helper in `Aion.GameServer.Services.BindPointTeleportPricePlanService` | Utility Dependency | Partial | Unit Tested | Needs Verification | Only the 3D float-coordinate `getDistance` slice used by bind-point pricing is mirrored. Other `PositionUtil` range, heading, bound-radius, talk-range, attack-range, and movement helpers remain unported here. Precision risk is limited to Java float intermediate math and long truncation, covered by source-derived tests but not runtime compared. |
| `com.aionemu.gameserver.model.templates.hotspot.HotspotTemplate` | scalar inputs to `Aion.GameServer.Services.BindPointTeleportPricePlanService` | DTO / Static Data Dependency | Partial | Unit Tested | Needs Verification | Planner consumes hotspot id, X/Y/Z, and base price as scalar facts. Java object lookup, race/world fields, and static-data normalization are not wired. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | scalar player coordinate inputs to `Aion.GameServer.Services.BindPointTeleportPricePlanService` | Model / Runtime Fact Source | Partial | Unit Tested | Needs Verification | Planner consumes player coordinates only. Java uses a live `Player` for position, world id, race enum, inventory Kinah, cooldown task state, death checks, and packet fanout. Threading/live object identity remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | future C# bind-point teleport packet/fanout surface | Packet / Movement | Not Started | No Tests | Unknown | Discovered dependency from Java `onLogin`, `teleport`, and `cancelTeleport`. No serializer, opcode, fanout order, or live packet tests were added in this unit. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BindPointTeleportPricePlanServiceTests.CreatePlan_UsesJavaDistanceCostAndIgnoresOneKinahClientDrift` | Unit | Java `BindPointTeleportService.calculateTeleportationPrice`; Java `PositionUtil.getDistance` | Validates 500m distance, distance-cost truncation, tolerated one-Kinah client drift, no warning, and final computed price. | Deterministic source-derived expectation. | No Java runtime execution or live hotspot/player objects. |
| `BindPointTeleportPricePlanServiceTests.CreatePlan_UsesHigherClientPriceAndFlagsWarning` | Unit | Java `calculateTeleportationPrice` warning and `Math.max(price, clientPrice)` branch | Validates warning threshold when prices differ by more than one and final price using the higher client-sent value. | Deterministic source-derived expectation. | No logger assertion; warning is staged as a boolean fact. |
| `BindPointTeleportPricePlanServiceTests.CreatePlan_UsesJavaLongTruncationForDiagonalDistance` | Unit | Java `PositionUtil.getDistance` and long cast of distance cost | Validates 3D diagonal distance and truncation to `173` for a base price of `100`. | Deterministic source-derived expectation. | No Java runtime comparison; only representative precision case covered. |
| `BindPointTeleportPricePlanServiceTests.CreatePlan_AppliesJavaOneKinahMinimumBeforeClientReconciliation` | Unit | Java `Math.max(1, basePrice + distanceCost)` | Validates one-Kinah minimum before client-price reconciliation. | Deterministic source-derived expectation. | Extreme Java double-to-long conversion behavior is not runtime compared. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 pure bind-point teleport price planner plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 3 grouped categories: live bind-point requirements/cooldown scheduling, packet fanout/order, and movement/inventory mutation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Bind-point `checkRequirements` remains unported: world id, race, Kinah balance, and cooldown checks are not represented yet.
- Java `cooldowns` is a static `HashMap` and scheduled task flow; C# threading/scheduling/concurrency behavior is not modeled.
- Live `SM_BIND_POINT_TELEPORT` packet serialization, broadcast order, cancel branch, login cooldown packet, Kinah decrement, and final movement remain unported.
- Hotspot static-data lookup and race enum normalization are not wired.
- No Java runtime comparison was executed. Reflection, serialization, and date/time behavior did not change in this unit.

## Next Work Options

### Recommended Sequential Task

- Task: Add a pure bind-point teleport requirements planner from Java `BindPointTeleportService.checkRequirements`.
- Why: It is the next smallest piece needed before composing a full non-live bind-point teleport operation plan.
- Files:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRequirementsPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRequirementsPlanServiceTests.cs`
  - `docs/Phase-6-PricesService-Consumer-Map.md`
  - `docs/PHASE-6-PROGRESS.md`
  - next Phase 6 handoff

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Analyze Java `SM_BIND_POINT_TELEPORT` opcode/payload only | read-only Java packet/opcode files | Low | Read-only analysis can run in parallel with requirements planner. |
| B | Add pure requirements planner | new `BindPointTeleportRequirementsPlanService.cs` and tests | Medium | Avoid live packets/scheduling; scalar fact inputs only. |
| C | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Do not edit shared dialog routing or docs concurrently with teleport docs. |
| D | Broker rollback/order analysis | read-only Java/C# broker handler/repository files | Low/Medium | Analysis only is safe; implementation touches shared connection handler and should be sequential. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only `SM_BIND_POINT_TELEPORT` packet/opcode analysis | Java packet/opcode files read-only | all writes |
| Agent B | Implement bind-point requirements planner | `BindPointTeleportRequirementsPlanService.cs`, `BindPointTeleportRequirementsPlanServiceTests.cs` | shared docs, packet files, connection handlers |
| Orchestrator | Integrate, test, update docs, commit | shared docs and final review | do not overlap with Agent B files until handoff |

If sub-agent tooling is unavailable or the scope remains small, do the requirements planner sequentially.

### Do Not Parallelize

- `GameServerConnection` teleport handlers: live movement, packet order, and scheduling are high-conflict.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live routing: broad movement side effects and existing tests make it unsuitable for concurrent edits.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PositionUtil.java`
- Latest commit should be `[Phase 6][UOW-1195] Add bind point teleport price plan`.
- Keep `BindPointTeleportPricePlanService` non-live until requirements, packet fanout, cooldown scheduling, and Kinah mutation have their own parity slices.
