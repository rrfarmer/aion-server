# Phase 6 Session 2075 Handoff - AutoGroup Static Data Corpus Evidence

Date: 2026-06-01
Unit of Work: UOW-2075
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, and now auto-group corpus evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `StaticData.AutoGroups` now has both focused unit coverage and real Java static-data corpus assertions.

## Latest Completed Work

- UOW-2071: current-team/member snapshot facts for the disabled find-group adapter.
- UOW-2072: alliance-backed test evidence for current-team/member facts.
- UOW-2073: `AutoGroupData`/static-data mask facts for action `10` disabled composition.
- UOW-2074: `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` config fact for action `10` disabled composition.
- UOW-2075: real Java auto-group static-data corpus assertions for `StaticData.AutoGroups`.

## Recent Commits

- `81592f88b [Phase 6][UOW-2074] Source find group form anywhere config`
- `883f77539 [Phase 6][UOW-2073] Source find group autogroup mask facts`
- `784cfeeb1 [Phase 6][UOW-2072] Add find group alliance snapshot evidence`

## Validation In UOW-2075

- Focused C# static-data tests passed:
  - 22 tests passed.
- Java/Maven was not run:
  - This unit was test-only C# evidence comparing directly against Java static-data XML and Java source-reviewed `AutoGroupData`/`AutoGroup.isRecruitableInstance` logic.
  - No Java packet/parser/runtime code changed, and no narrower Java test for `AutoGroupData` was available in this workspace.
- Broad .NET validation was skipped under the focused validation policy because this unit changed tests only; focused static-data tests exercised the real merged Java static-data manifest and the new auto-group corpus assertions.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.AutoGroupData` | `Aion.GameServer.Dataholders.AutoGroupTable` | Data Holder | Partial | Unit Tested / Regression Tested | Partial Parity | Corpus test compares Java `auto_group.xml` element count, C# table count, portal-NPC masks, and all recruitable masks. Full Java `DataManager.AUTO_GROUP` singleton lifecycle and all callers remain partially verified. |
| `com.aionemu.gameserver.model.autogroup.AutoGroup` | `Aion.GameServer.Dataholders.AutoGroupSummary` | Static Data DTO | Partial | Unit Tested / Regression Tested | Partial Parity | Corpus test compares representative XML attributes and Java `isRecruitableInstance` predicate branches against C# summary behavior. `AutoGroupType` enum behavior is still not ported. |
| `com.aionemu.gameserver.dataholders.StaticData` / `DataManager.AUTO_GROUP` | `Aion.GameServer.Dataholders.StaticData.AutoGroups` | Static Data Bridge | Partial | Regression Tested | Partial Parity | Real manifest load now asserts `auto_group` element count and C# table count match Java source XML. Startup logging/static singleton semantics remain out of scope. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside this unit.
- No Java runtime comparison of `DataManager.AUTO_GROUP` was performed.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.

## Next Recommended Unit of Work

- Next sequential task: inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization and decide whether the disabled planner needs additional runtime facts before live find-group dispatch can be considered.

Safe alternative candidates:

- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Start a live-dispatch readiness checklist for `CM_FIND_GROUP` now that action `10` config/data facts are sourced.
- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.

## Files Changed In UOW-2075

- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/Phase-6-Session-2075-Completion.md`
- `docs/Phase-6-Session-2075-Handoff.md`
