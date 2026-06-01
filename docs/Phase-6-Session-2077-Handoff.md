# Phase 6 Session 2077 Handoff - Find Group No-RunImpl Action Evidence

Date: 2026-06-01
Unit of Work: UOW-2077
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
- Recent find-group work is intentionally conservative: disabled planner, disabled composition/readiness evidence, runtime-facts packaging, connection-adjacent adapter composition, world player resolution, team snapshots, alliance snapshots, auto-group static-data facts, form-anywhere config facts, auto-group corpus evidence, and parsed-but-no-runImpl adapter evidence.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionClientActionCompositionPlanService` can source active-player, world-player, team/member, `AutoGroupTable`, and `GameServerOptions.Instance.FormInstanceGroupAnywhere` facts for disabled plans.
- `StaticData.AutoGroups` has focused unit coverage and real Java static-data corpus assertions.
- Process docs explicitly favor focused validation over broad .NET runs for ordinary units.

## Latest Completed Work

- UOW-2073: `AutoGroupData`/static-data mask facts for action `10` disabled composition.
- UOW-2074: `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` config fact for action `10` disabled composition.
- UOW-2075: real Java auto-group static-data corpus assertions for `StaticData.AutoGroups`.
- UOW-2076: focused-test policy tightened in orchestration, parity, and startup docs.
- UOW-2077: connection-adjacent evidence for `CM_FIND_GROUP` actions `20` and `25` as parsed-but-no-runImpl.

## Recent Commits

- `[Phase 6][UOW-2077] Add find group no-runImpl adapter evidence` (current handoff commit; use `git log -1 --oneline` for the exact hash)
- `534290305 [Phase 6][UOW-2076] Tighten focused test policy`
- `5b7e242dd [Phase 6][UOW-2075] Add autogroup static data corpus evidence`
- `81592f88b [Phase 6][UOW-2074] Source find group form anywhere config`

## Validation In UOW-2077

- Focused C# find-group tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupClientActionDispatchPrerequisitesTests" --no-restore`
  - Result: 28 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven parser golden passed:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: 12 tests passed.
- Broad .NET validation was skipped:
  - Test-only UOW; no production code, shared infrastructure, packet primitive, serialization helper, crypto, scheduling, world state, persistence, connection dispatch, live side effect, or common model/state surface changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` actions `20` and `25` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `Aion.GameServer.Services.FindGroupClientActionPlanService`; `Aion.GameServer.Services.FindGroupConnectionClientActionCompositionPlanService` | Client Packet / Disabled Planner / Adapter | Partial | Unit Tested / Golden File Tested | Partial Parity | Java parses both actions but `runImpl` has no branch. C# parser preserves action `25` payload and disabled planner/composition maps both to `ParsedButNoRunImpl` with no live side effects. Live `CM_FIND_GROUP` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP` actions `18`, `22`, `23`, `24` | `Aion.GameServer.Network.Aion.ServerPackets.SmFindGroup`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Server Packet / Planner | Partial | Unit Tested / Golden File Tested | Partial Parity | Reviewed to confirm prepare-window actions are server-packet actions rather than missing `CM_FIND_GROUP.runImpl` client branches. Existing C# packet/planner tests cover these actions; this unit added no new server-packet coverage. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- `AutoGroupType`, `AutoGroupService`, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` live workflows remain outside recent units.
- No live `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- Action `25` should stay no-runImpl unless a separate Java caller proves a real instance-group ban side effect exists outside `CM_FIND_GROUP.runImpl`.

## Next Recommended Unit of Work

- Next sequential task: start a focused live-dispatch readiness checklist for `CM_FIND_GROUP`, now that parser/no-runImpl, current player/world/team/alliance, auto-group data, and form-anywhere config facts have connection-adjacent disabled evidence.

Safe alternative candidates:

- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified outside this packet.
- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.

## Files Changed In UOW-2077

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionClientActionCompositionPlanServiceTests.cs`
- `docs/Phase-6-Session-2077-Completion.md`
- `docs/Phase-6-Session-2077-Handoff.md`
