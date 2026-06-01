# Phase 6 Session 2074 Completion - Find Group Form Anywhere Config Fact

Date: 2026-06-01
Unit of Work: UOW-2074
Status: Completed

## Scope

- Added C# config loading for Java `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`.
- Let the disabled find-group connection adapter source the form-anywhere runtime fact from `GameServerOptions` when no explicit value is supplied.
- Preserved explicit caller-supplied facts as authoritative for tests and future host boundaries.
- Kept live `CM_FIND_GROUP` dispatch disabled.

## What Changed

- Added `GameServerInstanceOptions.FormInstanceGroupAnywhere`.
- Loaded `gameserver.instance_group.form_anywhere` through `GameServerOptions.LoadFromJavaConfig`, defaulting to Java's `false`.
- Added optional `GameServerOptions` to `FindGroupConnectionClientActionCompositionPlanService`.
- Changed adapter `formInstanceGroupAnywhere` parameters to nullable so:
  - explicit `true` or `false` overrides options;
  - omitted value falls back to `options.Instance.FormInstanceGroupAnywhere`;
  - absent options fall back to Java default `false`.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupClientActionRuntimeFactsTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests" --no-restore`
  - Result: passed, 33 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit touched config binding and disabled find-group adapter composition only.
  - Focused options tests covered Java-style default and `mygs.properties` override behavior.
  - Focused find-group adapter tests covered option sourcing and explicit override precedence.
  - No packet primitives, crypto, persistence writes, live connection dispatch case, packet sends, or live side effects were enabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` | `Aion.GameServer.Configuration.GameServerInstanceOptions.FormInstanceGroupAnywhere` | Config | Partial | Unit Tested | Partial Parity | C# loads `gameserver.instance_group.form_anywhere` with Java default `false` and Java-style `mygs.properties` override behavior. Other `GroupConfig` properties are not represented in this UOW. |
| `com.aionemu.gameserver.configs.main.GroupConfig` | `Aion.GameServer.Configuration.GameServerOptions.Instance` | Config Group | Partial | Unit Tested | Partial Parity | Only the form-anywhere field needed by find-group action `10` is ported here; group/alliance invite/remove-time/distance properties remain future work. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, boolean)` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Disabled adapter can source the form-anywhere fact from `GameServerOptions` when omitted, while explicit facts still override. Live packet dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Java golden confirms action `10` packet parsing. C# tests cover disabled composition with config-sourced form-anywhere fact. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit | Java `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` default value | C# default for `gameserver.instance_group.form_anywhere` is `false` | C# unit assertion from Java annotation default | Does not cover all `GroupConfig` fields |
| `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit | Java properties override order and `GroupConfig` property key | C# reads `gameserver.instance_group.form_anywhere = true` from `mygs.properties` | C# unit assertion through existing Java-style config loader | Does not run Java config loader at runtime |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_UsesGameServerOptionsForFormInstanceGroupAnywhere` | Unit | Java `FindGroupService.showInstanceGroups(player, false)` reads `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` | Disabled adapter uses options-sourced form-anywhere fact to compose action `26` mask intent before action `10` show plan | C# unit assertion plus Java parser golden | Live send remains disabled |
| `FindGroupConnectionClientActionCompositionPlanServiceTests.CreateDisabledPlan_ExplicitFormInstanceGroupAnywhereOverridesOptions` | Unit | Adapter runtime-fact boundary from prior UOWs | Explicit caller fact overrides options, preserving test/host authority | C# unit assertion | This is adapter-boundary behavior, not a Java behavior claim |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Only `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is represented; other Java `GroupConfig` properties are not ported here.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside this unit.
- Packet send, broadcast, invite side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2074-Completion.md`
- `docs/Phase-6-Session-2074-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add full-corpus auto-group static-data assertions against `game-server/data/static_data/auto_group/auto_group.xml` so `StaticData.AutoGroups` has source-file count and representative lookup evidence.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Start a live-dispatch readiness checklist for `CM_FIND_GROUP` once all required facts and packet side-effect adapters are sourced.
