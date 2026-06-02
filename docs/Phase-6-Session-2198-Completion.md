# Phase 6 Session 2198 Completion - FindGroup Mutation Java Hook Placement Preflight

Date: 2026-06-02
Unit of Work: UOW-2198
Status: Completed

## Scope

This unit added a Java test-side source-order preflight for the future action `2` and `6` mutation-post trace hook placement.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInstrumentation.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

This UOW does not edit production Java source, does not add production hooks, does not write repository artifact files, does not edit C# code, does not run repository Java artifact validation, does not produce runtime-backed Java rows, does not compare Java/C# live rows, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostTraceCaptureHookPlacementPreflight`.
- The preflight records the exact production Java statements needed for future trace hooks:
  - action `2`: `recruitments.put(...)`, `STR_PARTY_MATCH_OFFER_PARTY_POSTED`, `showRecruitments(player)`, and `new SM_FIND_GROUP(0, recruitments)`,
  - action `6`: `applications.put(...)`, `STR_PARTY_MATCH_SEEK_PARTY_POSTED`, `showApplications(player)`, and `new SM_FIND_GROUP(4, applications)`.
- Added focused JUnit coverage confirming those source statements exist and preserve Java mutation-before-posted-message-before-refresh-call ordering, with the expected refreshed-list send statements present in the refresh methods.
- Kept `runtimeInstrumentationImplemented()` false and the preflight report `productionHooksIntegrated=false`.
- Updated live-dispatch design notes to record the preflight while keeping production hook integration, runtime-backed artifacts, live C# rows, registry observation, and comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: Java test-only hook-placement preflight plus non-live design/session documentation.
- Focused C# command: not run; no C# files changed.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. No production Java source, C# production code, live connection dispatch, live side effects, packet primitive, common runtime base, persistence, or shared infrastructure changed.
- Broad .NET decision: skipped intentionally because no C# files changed and no broad trigger applied.
- Why this scope is sufficient: the targeted Maven command compiles and runs the Java fixture class that owns the new preflight and directly inspects the Java source-of-truth statements under review.

Result:

- First focused Maven run exposed a test path bug when Surefire resolved paths from the `game-server` module base. The preflight was fixed to resolve both module-base and repository-root layouts.
- Final Java/Maven result: passed 22, failed 0, skipped 0. Existing Java `Unsafe` and deprecation warnings were emitted from unrelated golden-test helpers during test compilation.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | Java test-side `FindGroupMutationPostTraceCaptureHookPlacementPreflight`; future C# mutation-post trace comparison chain | Hook Placement Preflight | Partial | Unit Tested | Partial Parity | The preflight confirms current Java source order for action `2`/`6` mutation before posted system message before refresh call, with expected refreshed-list send statements present. This is not runtime evidence; production hooks, Java artifact generation, C# live rows, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | Java test-side mutation-post fixture scaffold; future C# live trace row fixture | Mutation-Post Trace Preparation | Partial | Unit Tested | Partial Parity | Existing fixture tests continue to cover action `2`/`6` metadata and artifact targets. Live dispatch and runtime comparison remain blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceCaptureTest.hookPlacementPreflightNamesProductionJavaStatementsWithoutIntegratingHooks` | Java unit | `FindGroupService.addRecruitment/addApplication` source review | Planned hook-placement statements for actions `2` and `6` are named without integrating production hooks. | Focused Maven test. | Source-order preflight only; no runtime artifact rows. |
| `FindGroupMutationPostTraceCaptureTest.hookPlacementPreflightConfirmsJavaMutationBeforePostedBeforeRefreshOrdering` | Java unit | `FindGroupService.java` source inspection | Mutation statements appear before posted system messages, which appear before refreshed list calls; refreshed send statements exist. | Focused Maven test directly reads Java source. | Source inspection only; no production hook execution or Java/C# comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 production artifacts plus Java fixture/preflight artifacts
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 production artifacts plus Java/C# trace-preparation artifacts
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Production Java hook integration, runtime-backed Java artifact generation, C# repository artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The source-order preflight is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add no-op production Java hook integration behind the existing capture flag only after reviewing the preflight insertion points, proving default execution is unchanged, and keeping artifact output disabled unless explicitly capture-enabled.

Safe candidates:

- Add a focused Java fixture test proving disabled no-op hooks do not write repository artifact files.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add fixture-side payload byte construction only if future Java packet parser capture needs deterministic raw packet bytes.

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHookPlacementPreflight.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2198-Completion.md`
- `docs/Phase-6-Session-2198-Handoff.md`
