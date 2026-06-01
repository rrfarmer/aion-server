# Phase 6 Session 2073 Handoff - Find Group AutoGroup Static Data Facts

Date: 2026-06-01
Unit of Work: UOW-2073
Status: Completed

## Startup Context Rule

Future Phase 6 sessions should not read `PHASE-6-PROGRESS.md` during normal startup.

Read these instead:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Completion.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`

`docs/PHASE-6-PROGRESS.md` is a historical archive. Open it only for targeted archaeology when the latest completion/handoff docs do not contain enough context.

## Test Selection Rule

Focused validation is the default.

For ordinary small Units of Work, run focused C# tests for the edited area and focused Java/Maven parity tests where they directly evidence the touched Java source, packet, or parser behavior.

Do not run the broad .NET suite or full solution build by default. Run broad C# validation only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common state/model changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

When broad validation is skipped, document the focused commands that ran and why they were sufficient for the scoped risk. For documentation-only UOWs, use repository hygiene checks such as `git diff --check` and state that runtime tests were not applicable.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, and now auto-group static-data facts.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupClientActionPlanService` composes disabled plans for Java `CM_FIND_GROUP.runImpl` actions.
- `FindGroupClientActionRuntimeFacts` packages caller-supplied active player, time, lookup delegates, team/member snapshots, and config/data snapshots into disabled composition.
- `FindGroupConnectionClientActionCompositionPlanService` extracts `GameServerConnection.ActivePlayer`, sources `World.getPlayer`-equivalent resolver facts, sources group/alliance runtime team/member facts, and can source Java `DataManager.AUTO_GROUP`-equivalent mask facts when supplied with `AutoGroupTable`.
- C# still lacks a global `GroupConfig` config class for `gameserver.instance_group.form_anywhere`; the disabled adapter still receives `formInstanceGroupAnywhere` explicitly.

## Latest Completed Work

- UOW-2069: disabled connection-adjacent composition adapter for parsed `CmFindGroup`.
- UOW-2070: default world-backed player resolver fact for the disabled find-group adapter.
- UOW-2071: current-team/member snapshot facts for the disabled find-group adapter.
- UOW-2072: alliance-backed test evidence for current-team/member facts.
- UOW-2073: `AutoGroupData`/static-data mask facts for action `10` disabled composition.

## Recent Commits

- `784cfeeb1 [Phase 6][UOW-2072] Add find group alliance snapshot evidence`
- `b68189665 [Phase 6][UOW-2071] Source find group team snapshot facts`
- `4c7454029 [Phase 6][UOW-2070] Source find group world player resolver`

## Validation In UOW-2073

- Focused C# static-data/find-group adapter/planner tests passed:
  - 49 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` passed:
  - 12 tests passed.
- Broad .NET validation was skipped under the focused validation policy because focused static-data and find-group tests covered the changed surfaces; no packet primitives, crypto, persistence writes, live connection dispatch case, packet sends, or live side effects were enabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.AutoGroupData` | `Aion.GameServer.Dataholders.AutoGroupTable` | Data Holder | Partial | Unit Tested | Partial Parity | C# indexes by mask id and recruitable portal NPC id; tests cover Java recruitable predicate and missing portal null behavior. JAXB lifecycle, full corpus counts, and all Java callers remain partially verified only. |
| `com.aionemu.gameserver.model.autogroup.AutoGroup` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Static Data DTO | Partial | Unit Tested | Partial Parity | C# parses scalar XML attributes used by `AutoGroupData`; tests cover ids, level bounds, registration flags, NPC ids, and `isRecruitableInstance` branches. Full AutoGroupType enum behavior is not ported. |
| `com.aionemu.gameserver.dataholders.StaticData` / `DataManager.AUTO_GROUP` | `Aion.GameServer.Dataholders.StaticData.AutoGroups` | Static Data Bridge | Partial | Unit Tested | Partial Parity | C# `StaticData` now exposes auto-group summaries for disabled planner facts. Full Java `DataManager.AUTO_GROUP` static singleton semantics and startup logging are not represented. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, boolean)` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Disabled adapter can now source Java-equivalent action `10` target-NPC/all-recruitable mask facts when `AutoGroupTable` is supplied. `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` remains explicit; live packet dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Java golden confirms action `10` packet parsing. C# tests cover action `10` disabled composition with auto-group facts. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- C# still lacks a global Java-equivalent `GroupConfig` config class for `gameserver.instance_group.form_anywhere`.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside this unit.
- Static-data full corpus count parity for auto groups has not been separately documented.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.

## Next Recommended Unit of Work

- Next sequential task: add a narrow C# config surface for Java `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` (`gameserver.instance_group.form_anywhere`) and source it into the disabled find-group adapter host path without enabling live dispatch.

Safe alternative candidates:

- Add full corpus auto-group static-data assertions against `game-server/data/static_data/auto_group/auto_group.xml`.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.

## Files Changed In UOW-2073

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2073-Completion.md`
- `docs/Phase-6-Session-2073-Handoff.md`
