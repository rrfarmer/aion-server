# Phase 6 Session 2196 Completion - FindGroup Mutation Java Trace Scenario Builders

Date: 2026-06-02
Unit of Work: UOW-2196
Status: Completed

## Scope

This unit added deterministic Java fixture scenario builders for future `CM_FIND_GROUP` action `2` and `6` mutation-post trace capture.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactWriter.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

This UOW does not edit production Java source, does not call hooks from `CM_FIND_GROUP` or `FindGroupService`, does not write repository artifact files, does not produce runtime-backed Java rows, does not compare Java/C# rows, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostTraceCaptureScenarioBuilder` under `game-server/test/com/aionemu/gameserver/services/findgroup`.
- The scenario builder provides deterministic action `2` and `6` fixture scenarios with:
  - parsed `CM_FIND_GROUP` payload fields,
  - active-player object id and race,
  - server epoch second,
  - mutated entry id,
  - post-mutation visible entry ids,
  - mutation kind,
  - posted system message id,
  - refreshed list action,
  - stable artifact file name,
  - Java source note.
- Updated serializer, writer, and validator fixture tests to reuse the scenario rows instead of repeated ad hoc sample values.
- Verified the default repository artifact directory was not created by the focused test run.
- Updated live-dispatch design notes to include the deterministic scenario builder while keeping production hooks, runtime-backed artifacts, C# repository artifact validation, live C# rows, and comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused Java test-side scenario builder plus non-live design/session documentation.
- Focused C# command: not run. No C# code or C# test changed in this UOW.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, production Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally because no broad trigger applied and no C# runtime surface changed.
- Why this scope is sufficient: the focused Maven command compiles the new Java scenario builder and proves the fixture can reuse deterministic action `2`/`6` rows across serializer, writer, and validator checks without touching unrelated Java tests or broad .NET validation.

Result:

- Java/Maven: passed 20, failed 0, skipped 0.
- Existing unrelated Java `Unsafe`/deprecation warnings were emitted.
- Repository artifact path check: `parity-artifacts/find-group/mutation-post/java` did not exist after the test run.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureScenarioBuilder.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Test-Side Java Scenario Builder | Partial | Unit Tested | Partial Parity | The builder centralizes Java-derived parsed payload and trace-row facts for action `2`/`6`, but production `CM_FIND_GROUP` does not call hooks and no runtime-backed artifact row is captured. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureScenarioBuilder.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Mutation-Post Scenario Builder | Partial | Unit Tested | Partial Parity | The builder preserves Java-derived action mappings for `addRecruitment` and `addApplication`, but production hook integration, repository artifact generation, C# artifact validation from disk, live C# rows, registry observation, and comparison execution are missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceCaptureTest.scenarioBuildersExposeDeterministicJavaPayloadAndMutationRows` | Unit | `CM_FIND_GROUP.readImpl/runImpl`; `FindGroupService.addRecruitment/addApplication` review | Action `2` and `6` scenario payload fields, action mappings, artifact file names, and Java source notes. | Focused Java fixture assertion. | Fixture scenarios only; no production runtime hook. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 production artifacts and 6 Java test scaffold artifacts
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 production artifacts plus Java scaffold artifacts
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Production Java hook integration, runtime-backed Java artifact generation, C# repository artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java scenario builder creates deterministic fixture rows only; it is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add C# focused tests that feed deterministic Java fixture JSON strings into `FindGroupMutationPostJavaTraceArtifactValidatorService`, proving the Java fixture schema shape remains aligned with the C# artifact validator without running broad .NET validation.

Safe candidates:

- Add production hook integration only after deciding exact no-op hook placement and proving it does not alter `PacketSendUtility` ordering.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add fixture-side payload byte construction only if future Java packet parser capture needs deterministic raw packet bytes.

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureScenarioBuilder.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2196-Completion.md`
- `docs/Phase-6-Session-2196-Handoff.md`
