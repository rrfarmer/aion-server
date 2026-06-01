# Phase 6 Session 2070 Handoff - Find Group World Player Resolver Fact

Date: 2026-06-01
Unit of Work: UOW-2070
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, and connection-adjacent adapter composition only.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupClientActionPlanService` composes disabled plans for Java `CM_FIND_GROUP.runImpl` actions.
- `FindGroupClientActionDispatchPrerequisites` names the runtime facts and side-effect dispatchers needed before live `CM_FIND_GROUP` dispatch can be enabled.
- `FindGroupClientActionRuntimeFacts` packages caller-supplied active player, time, lookup delegates, team/member snapshots, and config/data snapshots into disabled composition.
- `FindGroupConnectionClientActionCompositionPlanService` extracts `GameServerConnection.ActivePlayer`, and now sources `World.getPlayer`-equivalent resolver facts from C# `World.TryGetObject` when a world is supplied.

## Latest Completed Work

- UOW-2066: live-dispatch prerequisite/readiness map for `CM_FIND_GROUP`.
- UOW-2067: typed runtime-facts boundary for disabled `CM_FIND_GROUP` composition.
- UOW-2068: durable focused-validation documentation update.
- UOW-2069: disabled connection-adjacent composition adapter for parsed `CmFindGroup`.
- UOW-2070: default world-backed player resolver fact for the disabled find-group adapter.

## Recent Commits

- `58e4bd158 [Phase 6][UOW-2069] Add find group connection composition adapter`
- `4bfe951cb [Phase 6][UOW-2068] Document focused validation policy`
- `100aa0a7b [Phase 6][UOW-2067] Add find group runtime facts boundary`

## Validation In UOW-2070

- Focused C# find-group adapter/runtime/prerequisite/composition tests passed:
  - 20 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` passed:
  - 12 tests passed.
- Broad .NET validation was skipped under the focused validation policy because this unit only changed a disabled find-group adapter and tests; it did not alter shared infrastructure, packet primitives, live connection dispatch, packet sends, or live side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java resolves the recipient with `World.getInstance().getPlayer(playerOrTeamId)`. C# disabled adapter now sources a `Player` from C# `World.TryGetObject` when no explicit resolver is supplied; no live packet send is enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java resolves the applicant with `World.getInstance().getPlayer(applicantId)`. C# disabled adapter now sources that runtime fact from C# world state for disabled planning only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Parsed C# actions `11` and `12` flow into the disabled world-backed adapter. Java parser golden tests cover payload layout. |

## Summary Metrics

- Total Java artifacts discovered in UOW-2070: 2.
- Total artifacts ported or represented in UOW-2070: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- The connection-adjacent adapter does not yet source group/team snapshots, config, target NPC masks, or `DataManager.AUTO_GROUP` facts from live runtime services.
- Live logout hook wiring and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: add runtime-fact sourcing for current-team and current-member snapshots into the disabled find-group connection adapter, still without live sends.

Safe alternative candidates:

- Source `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` and target NPC mask facts for action `10` from existing C# config/static-data surfaces if available.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.

## Files Changed In UOW-2070

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2070-Completion.md`
- `docs/Phase-6-Session-2070-Handoff.md`
