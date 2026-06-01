# Phase 6 Session 2074 Handoff - Find Group Form Anywhere Config Fact

Date: 2026-06-01
Unit of Work: UOW-2074
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, and form-anywhere config facts.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupClientActionPlanService` composes disabled plans for Java `CM_FIND_GROUP.runImpl` actions.
- `FindGroupClientActionRuntimeFacts` packages caller-supplied active player, time, lookup delegates, team/member snapshots, and config/data snapshots into disabled composition.
- `FindGroupConnectionClientActionCompositionPlanService` extracts `GameServerConnection.ActivePlayer`, sources `World.getPlayer`-equivalent resolver facts, sources group/alliance runtime team/member facts, sources Java `DataManager.AUTO_GROUP`-equivalent mask facts when supplied with `AutoGroupTable`, and can source Java `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` from `GameServerOptions`.

## Latest Completed Work

- UOW-2070: default world-backed player resolver fact for the disabled find-group adapter.
- UOW-2071: current-team/member snapshot facts for the disabled find-group adapter.
- UOW-2072: alliance-backed test evidence for current-team/member facts.
- UOW-2073: `AutoGroupData`/static-data mask facts for action `10` disabled composition.
- UOW-2074: `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` config fact for action `10` disabled composition.

## Recent Commits

- `883f77539 [Phase 6][UOW-2073] Source find group autogroup mask facts`
- `784cfeeb1 [Phase 6][UOW-2072] Add find group alliance snapshot evidence`
- `b68189665 [Phase 6][UOW-2071] Source find group team snapshot facts`

## Validation In UOW-2074

- Focused C# options/find-group adapter/planner tests passed:
  - 33 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` passed:
  - 12 tests passed.
- Broad .NET validation was skipped under the focused validation policy because focused options and find-group tests covered the changed surfaces; no packet primitives, crypto, persistence writes, live connection dispatch case, packet sends, or live side effects were enabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` | `Aion.GameServer.Configuration.GameServerInstanceOptions.FormInstanceGroupAnywhere` | Config | Partial | Unit Tested | Partial Parity | C# loads `gameserver.instance_group.form_anywhere` with Java default `false` and Java-style `mygs.properties` override behavior. Other `GroupConfig` properties are not represented in this UOW. |
| `com.aionemu.gameserver.configs.main.GroupConfig` | `Aion.GameServer.Configuration.GameServerOptions.Instance` | Config Group | Partial | Unit Tested | Partial Parity | Only the form-anywhere field needed by find-group action `10` is ported here; group/alliance invite/remove-time/distance properties remain future work. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, boolean)` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Disabled adapter can source the form-anywhere fact from `GameServerOptions` when omitted, while explicit facts still override. Live packet dispatch remains disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Java golden confirms action `10` packet parsing. C# tests cover disabled composition with config-sourced form-anywhere fact. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Only `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is represented; other Java `GroupConfig` properties are not ported here.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside this unit.
- Static-data full corpus count parity for auto groups has not been separately documented.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.

## Next Recommended Unit of Work

- Next sequential task: add full-corpus auto-group static-data assertions against `game-server/data/static_data/auto_group/auto_group.xml` so `StaticData.AutoGroups` has source-file count and representative lookup evidence.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Start a live-dispatch readiness checklist for `CM_FIND_GROUP` once all required facts and packet side-effect adapters are sourced.

## Files Changed In UOW-2074

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2074-Completion.md`
- `docs/Phase-6-Session-2074-Handoff.md`
