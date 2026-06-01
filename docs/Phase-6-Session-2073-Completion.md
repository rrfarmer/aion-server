# Phase 6 Session 2073 Completion - Find Group AutoGroup Static Data Facts

Date: 2026-06-01
Unit of Work: UOW-2073
Status: Completed

## Scope

- Added a minimal C# `AutoGroupData` equivalent for Java `DataManager.AUTO_GROUP` recruitable-instance mask facts.
- Wired the disabled find-group connection adapter to source action `10` target-NPC and all-recruitable mask facts when an `AutoGroupTable` is supplied.
- Kept `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` as an explicit runtime fact because C# does not yet have a global `GroupConfig` config surface.
- Kept live `CM_FIND_GROUP` dispatch disabled.

## What Changed

- Added `AutoGroupTable` and `AutoGroupSummary`.
- Extended `StaticData` to parse `<auto_group>` entries from `<auto_groups>`.
- Added `StaticData.AutoGroups`.
- Extended `FindGroupConnectionClientActionCompositionPlanService` with optional `AutoGroupTable`.
- The adapter now mirrors Java `FindGroupService.showInstanceGroups(player, isUpdate)` data sourcing:
  - If the player target resolves to a C# `IWorldNpcObject`, use portal-specific masks from `AutoGroupTable.GetRecruitableInstanceMaskIds(npc.TemplateId)`.
  - If that returns null, fall back to all recruitable masks.
  - Explicit caller-supplied mask facts remain authoritative.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupClientActionRuntimeFactsTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests" --no-restore`
  - Result: passed, 49 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit touched static-data extraction and disabled find-group adapter composition.
  - Focused `StaticDataLoadingTests` covered the new loader/table behavior.
  - Focused find-group adapter/planner tests covered action `10` composition.
  - No packet primitives, crypto, persistence writes, live connection dispatch case, packet sends, or live side effects were enabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.AutoGroupData` | `Aion.GameServer.Dataholders.AutoGroupTable` | Data Holder | Partial | Unit Tested | Partial Parity | C# indexes by mask id and recruitable portal NPC id; tests cover Java recruitable predicate and missing portal null behavior. JAXB lifecycle, full corpus counts, and all Java callers remain partially verified only. |
| `com.aionemu.gameserver.model.autogroup.AutoGroup` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Static Data DTO | Partial | Unit Tested | Partial Parity | C# parses scalar XML attributes used by `AutoGroupData`; tests cover ids, level bounds, registration flags, NPC ids, and `isRecruitableInstance` branches. Full AutoGroupType enum behavior is not ported. |
| `com.aionemu.gameserver.dataholders.StaticData` / `DataManager.AUTO_GROUP` | `Aion.GameServer.Dataholders.StaticData.AutoGroups` | Static Data Bridge | Partial | Unit Tested | Partial Parity | C# `StaticData` now exposes auto-group summaries for disabled planner facts. Full Java `DataManager.AUTO_GROUP` static singleton semantics and startup logging are not represented. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, boolean)` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Disabled adapter can now source Java-equivalent action `10` target-NPC/all-recruitable mask facts when `AutoGroupTable` is supplied. `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` remains explicit; live packet dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Java golden confirms action `10` packet parsing. C# tests cover action `10` disabled composition with auto-group facts. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticDataLoadingTests.StaticData_LoadsAutoGroupsLikeJavaDataholder` | Unit | Java `AutoGroupData.afterUnmarshal` and `AutoGroup.isRecruitableInstance` source review | C# static data parses auto groups, indexes mask ids, portal-NPC masks, all recruitable masks, and null missing-portal lookup | C# unit assertions from Java logic | Does not compare full live corpus counts or startup logs |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesAutoGroupTableForTargetNpcInstanceMasks` | Unit | Java `FindGroupService.showInstanceGroups(player, false)` source review | Disabled adapter resolves player target NPC through world and uses portal-specific masks for action `10` | C# unit assertions plus Java parser golden | `GroupConfig` remains caller-supplied and live sends remain disabled |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_FallsBackToAllAutoGroupMasksWhenTargetNpcHasNoMasks` | Unit | Java `FindGroupService.showInstanceGroups(player, false)` source review | Disabled adapter falls back to all recruitable masks when target NPC has no portal-specific entry | C# unit assertions plus Java parser golden | Does not prove live `PacketSendUtility.sendPacket` behavior |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 5.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- C# still lacks a global Java-equivalent `GroupConfig` config class for `gameserver.instance_group.form_anywhere`; action `10` still requires the caller/adapter host to provide `formInstanceGroupAnywhere`.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside this unit.
- Static-data full corpus count parity for auto groups has not been separately documented.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2073-Completion.md`
- `docs/Phase-6-Session-2073-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add a narrow C# config surface for Java `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` (`gameserver.instance_group.form_anywhere`) and source it into the disabled find-group adapter host path without enabling live dispatch.

Safe alternative candidates:

- Add full corpus auto-group static-data assertions against `game-server/data/static_data/auto_group/auto_group.xml`.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
