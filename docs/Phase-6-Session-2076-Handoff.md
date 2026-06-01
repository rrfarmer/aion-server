# Phase 6 Session 2076 Handoff - Focused Test Policy Tightening

Date: 2026-06-01
Unit of Work: UOW-2076
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

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Full .NET validation is reserved for documented broad-validation triggers.

Choose the narrowest command that still proves the scoped change:

- Documentation-only units: run repository hygiene such as `git diff --check`; runtime tests are not applicable unless generated artifacts, scripts, or executable docs changed.
- Test-only units: run the edited test class or smallest directly affected filter; do not broaden unless product-code risk is revealed.
- Production-code units: run edited service/packet/parser tests plus directly adjacent adapter/composition tests.
- Shared-surface units: start focused, then escalate only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common model/state changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

Prefer C# filters such as `--filter "FullyQualifiedName~SpecificTestClass|FullyQualifiedName~RelatedTestClass"` and Java Maven filters such as `-Dtest=SpecificJavaTest`. Avoid unfiltered `dotnet test dotnetConversion/AionServer.slnx`, unfiltered project-wide tests, and full solution builds unless the broad trigger is documented.

When broad validation is skipped, document the focused commands and why they were sufficient. When Java/Maven is skipped, document why a narrower Java parity command was unavailable or irrelevant.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, and auto-group corpus evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `StaticData.AutoGroups` has focused unit coverage and real Java static-data corpus assertions.
- Process docs now explicitly favor focused validation over broad .NET runs for ordinary units.

## Latest Completed Work

- UOW-2072: alliance-backed test evidence for current-team/member facts.
- UOW-2073: `AutoGroupData`/static-data mask facts for action `10` disabled composition.
- UOW-2074: `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` config fact for action `10` disabled composition.
- UOW-2075: real Java auto-group static-data corpus assertions for `StaticData.AutoGroups`.
- UOW-2076: focused-test policy tightened in orchestration, parity, and startup docs.

## Recent Commits

- `5b7e242dd [Phase 6][UOW-2075] Add autogroup static data corpus evidence`
- `81592f88b [Phase 6][UOW-2074] Source find group form anywhere config`
- `883f77539 [Phase 6][UOW-2073] Source find group autogroup mask facts`

## Validation In UOW-2076

- Repository hygiene passed:
  - `git diff --check`
- Runtime C# tests were not run:
  - Documentation-only UOW; no product code, test code, scripts, generated artifacts, packet layouts, serialization helpers, runtime dispatch, persistence, or live side effects changed.
- Java/Maven tests were not run:
  - Documentation-only UOW; no Java source-of-truth behavior, packet/parser shape, or Java fixture changed.
- Broad .NET validation was skipped:
  - No broad-validation trigger was touched.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| N/A | N/A | Process Documentation | N/A | Manual / Hygiene Checked | N/A | Documentation-only policy update; no Java or C# runtime artifact was changed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside recent units.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- Documentation policy now reduces unnecessary full-suite runs, but each future UOW still needs explicitly scoped validation evidence.

## Next Recommended Unit of Work

- Next sequential task: inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization and decide whether the disabled planner needs additional runtime facts before live find-group dispatch can be considered.

Safe alternative candidates:

- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Start a live-dispatch readiness checklist for `CM_FIND_GROUP` now that action `10` config/data facts are sourced.
- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.

## Files Changed In UOW-2076

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2076-Completion.md`
- `docs/Phase-6-Session-2076-Handoff.md`
