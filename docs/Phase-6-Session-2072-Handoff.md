# Phase 6 Session 2072 Handoff - Find Group Alliance Snapshot Evidence

Date: 2026-06-01
Unit of Work: UOW-2072
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
- `FindGroupClientActionRuntimeFacts` packages caller-supplied active player, time, lookup delegates, team/member snapshots, and config/data snapshots into disabled composition.
- `FindGroupConnectionClientActionCompositionPlanService` extracts `GameServerConnection.ActivePlayer`, sources `World.getPlayer`-equivalent resolver facts, and sources group/alliance runtime team/member facts when supplied.
- C# static data currently does not appear to expose Java `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(...)`; action `10` static-data sourcing needs a separate static-data representation review.

## Latest Completed Work

- UOW-2068: durable focused-validation documentation update.
- UOW-2069: disabled connection-adjacent composition adapter for parsed `CmFindGroup`.
- UOW-2070: default world-backed player resolver fact for the disabled find-group adapter.
- UOW-2071: current-team/member snapshot facts for the disabled find-group adapter.
- UOW-2072: alliance-backed test evidence for current-team/member facts.

## Recent Commits

- `b68189665 [Phase 6][UOW-2071] Source find group team snapshot facts`
- `4c7454029 [Phase 6][UOW-2070] Source find group world player resolver`
- `58e4bd158 [Phase 6][UOW-2069] Add find group connection composition adapter`

## Validation In UOW-2072

- Focused C# find-group adapter/runtime/prerequisite/composition tests passed:
  - 25 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` passed:
  - 12 tests passed.
- Broad .NET validation was skipped under the focused validation policy because this unit added focused tests only; it did not alter production code, shared infrastructure, packet primitives, live connection dispatch, packet sends, or live side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Adapter Service | Partial | Unit Tested | Partial Parity | Java uses `player.getCurrentTeam()` when present before building `GroupRecruitment`. C# disabled adapter now has focused evidence for alliance runtime facts as well as group facts; no live send is enabled. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentSubject` via connection adapter | DTO / Adapter Fact | Partial | Unit Tested | Partial Parity | Alliance test covers team id, leader name/class, member count, race, and member level range from `PlayerAllianceRuntime`. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup.getMembers` | `Aion.GameServer.Services.FindGroupInstanceGroupMemberState` via connection adapter | DTO / Adapter Fact | Partial | Unit Tested | Partial Parity | Alliance test covers current alliance members projected to disabled instance group member states. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` plus composition adapter tests | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | Parsed C# actions `2` and `8` flow into the disabled alliance-backed adapter. Java parser golden tests cover payload layout. |

## Summary Metrics

- Total Java artifacts discovered in UOW-2072: 3.
- Total artifacts ported or represented in UOW-2072: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- C# does not yet have a sourced `AutoGroupData`/recruitable-instance-mask table for action `10` config/data facts.

## Next Recommended Unit of Work

- Next sequential task: inspect C# static-data loading for a minimal `AutoGroupData`/recruitable-instance-mask table representation, then source action `10` mask facts only if that representation can be added safely.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Add disabled adapter diagnostics documenting missing action `10` static-data source if static-data representation is still absent.

## Files Changed In UOW-2072

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2072-Completion.md`
- `docs/Phase-6-Session-2072-Handoff.md`
