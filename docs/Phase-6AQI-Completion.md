# Phase 6AQI Completion - Guarded Nearby Refresh Adapter

Date: 2026-05-28
Unit of Work: UOW-1615
Status: Complete after focused validation

## Scope

This unit added a guarded non-live adapter for Java `PlayerController.updateNearbyQuests`. The adapter accepts an optional `Player`, optional `WorldMapInstanceRuntimeState`, and optional `StaticData`, then returns `NearbyQuestRefreshPlan` metadata by delegating to the existing nearby refresh planner with `StaticData.NearbyQuestTemplates`.

It does not wire production controller dispatch, map-region position lookup, `PacketSendUtility`, live `SM_NEARBY_QUESTS` sends, quest mutation, or repository writes.

## Completed Work

- Added `NearbyQuestRefreshInputAdapterService.CreatePlan`.
- Added `NearbyQuestRefreshInputAdapterResult` and `NearbyQuestRefreshInputAdapterStatus`.
- Guarded missing player and missing static data before calling the plan service.
- Delegated to `NearbyQuestRefreshPlanService.CreatePlan(player, worldInstance, staticData.NearbyQuestTemplates)`.
- Added adapter tests for marker projection, event quest rejection, empty world quest-id packet intent, and missing dependency guards.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused nearby/XP/static-data tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestRefreshInputAdapterServiceTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~QuestXpLevelChangeContextFactoryServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~WorldMapRuntimeStateTests"
```

Result: 33 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Guarded nearby-refresh input adapter | new service/test | Low | Yes | Directly composes StaticData into existing non-live nearby planner. |
| WorldMapInstance delayed refresh audit | Java/C# world runtime files | Medium | No | Best next nearby follow-up; keep scheduling non-live. |
| ItemCharge storage-location audit | read-only Java/C# charge files | Low | No | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/observer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because the adapter is deliberately thin and docs/test context is coupled.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_UsesStaticDataQuestTemplatesWithoutLiveDispatch` | Added | StaticData supplies nearby templates; event quest references are rejected; context remains metadata-only. | Source-derived from Java `PlayerController.updateNearbyQuests` and `QuestsData`. |
| `CreatePlan_ReturnsEmptyPacketIntentWhenStaticDataHasNoWorldQuestIds` | Added | Empty world quest ids produce empty packet intent metadata. | Source-derived from Java unconditional `SM_NEARBY_QUESTS` send. |
| `CreatePlan_GuardsMissingPlayerAndStaticData` | Added | Missing player/static data fail closed without packet intent. | C# guard regression; Java call sites normally provide both dependencies. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshInputAdapterService`; `NearbyQuestRefreshPlanService` | Controller / Adapter | Partial | Unit Tested | Partial Parity | Adapter returns non-live refresh metadata from player/world/static-data inputs. Production map-region lookup and packet send remain disabled. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` via `NearbyQuestRefreshPlanService` | Service / Condition Filter | Partial | Unit Tested through adapter and plan service | Needs Verification | Unsupported XML start conditions, inventory checks, repeat timing, dynamic scripts, and Java runtime comparison remain gaps. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.StaticData.NearbyQuestTemplates` consumed by adapter | Static Data Repository | Partial | Unit Tested through adapter | Partial Parity | StaticData quest-template summaries are consumed. Full Java `QuestTemplate` graph and JAXB/script behavior remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests` intent via `NearbyQuestRefreshPlan.WouldSendPacket` | Packet Dependency | Partial | Existing Regression Tested | Needs Verification | Adapter marks packet intent only; no live send, byte comparison, encrypted frame, or socket ordering was executed. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | Runtime State | Partial | Regression Tested with nearby adapter dependencies | Needs Verification | Runtime state supplies quest ids. Java delayed refresh scheduling, map-region parent lookup, synchronization, and ThreadPool timing remain unported. |

## Remaining Risks

- Adapter remains non-live metadata; production `PlayerController.updateNearbyQuests` is not wired.
- Real player position/map-region parent lookup is not implemented.
- Java `WorldMapInstance` delayed refresh scheduling and ThreadPool spam-prevention behavior remain unported.
- Full `QuestService.checkStartConditions` parity is partial, especially dynamic quest handlers and unsupported XML/inventory/repeat conditions.
- Packet bytes, encryption/frame validation, socket ordering, threading, date/time handling, and serialization remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 guarded non-live nearby-refresh input adapter plus 3 focused unit tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live nearby refresh dispatch, production controller wiring, map-region parent lookup, Java delayed refresh scheduling, full QuestService dynamic condition parity, packet-byte comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| WorldMapInstance delayed refresh audit/plan | Java `WorldMapInstance.addObject`; C# `WorldMapInstanceRuntimeState` | Keep non-live; record whether delayed refresh would be scheduled. |
| ItemCharge storage-location audit | read-only Java `Inventory`/`Equipment` lifecycle and C# selected-charge lookup | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1615] Add guarded nearby refresh adapter
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRefreshInputAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestRefreshInputAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQI-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
