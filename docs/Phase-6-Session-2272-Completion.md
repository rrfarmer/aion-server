# Phase 6 Session 2272 Completion - Accepted Boundary Row Fields

## Scope

Added first-class accepted C# boundary row field metadata for `CM_FIND_GROUP` mutation-post action `2` and action `6` intake preflight. This defines the exact row fields required before guarded C# boundary rows can feed Java artifact pairing and runtime comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests.cs`

The boundary row intake preflight now exposes `RequiredAcceptedBoundaryRowFields`:

- `action`
- `mutationKind`
- `boundaryAccepted`
- `executorInvokedFromBoundary`
- `registrySendsObservedInOrder`
- `postedSystemMessageId`
- `refreshedFindGroupAction`
- `worldBroadcastCount`
- `inviteDispatchCount`
- `activePlayerObjectId`
- `mutatedEntryObjectId`
- `visibleEntryObjectIdsAfterMutation`

The required-evidence text now names concrete boundary row fields for boundary acceptance, executor observation, registry observation, posted/refreshed ordering, and Java artifact pairing identity.

No live dispatch, packet sends, value reads, comparison execution, materialization, result emission, Java capture, C# capture, or verified parity claim was added.

## Validation Decision

Changed surface:

- C# non-live boundary row intake preflight metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests" --no-restore
```

Result: passed 10, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live boundary row metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution build were skipped because the filtered C# command built the affected project and dependencies and covered the changed boundary intake preflight plus the directly adjacent runtime evidence checklist.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService` | Boundary Row Intake Metadata | Partial | Unit Tested | Partial Parity | Intake preflight now lists exact accepted C# boundary row fields required for action `2`/`6` before Java artifact pairing. Metadata only; no live boundary capture or runtime comparison was executed. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService` | Boundary Row Intake Metadata | Partial | Unit Tested | Partial Parity | Required fields include mutation identity, boundary acceptance, executor observation, registry send ordering, posted/refreshed packet fields, zero broadcast/invite counts, and visible entries after mutation. Still blocked by missing accepted live rows. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultPreflightBlocksUntilAcceptedBoundaryRowsExist` | Unit | Java action `2`/`6` mapping | Preflight exposes the exact accepted boundary row field list while blocking runtime comparison. | Non-live metadata. | No live boundary dispatch. |
| `Create_RejectedPacketShapeCannotSatisfyPostedRefreshedOrdering` | Unit | Java posted-message then refreshed-list ordering | Required evidence names concrete posted/refreshed row fields for action `2` and action `6`. | Synthetic row metadata. | No registry send observation. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves boundary evidence metadata but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2272] Define accepted boundary row fields
```

## Next Recommended UOW

Add a small non-live accepted-boundary-row handoff report that consumes `FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService` and states whether action `2`/`6` rows can feed Java artifact pairing, including the required accepted row fields. Keep it metadata-only and do not execute capture.

Safe candidates:

- Add another narrow command-decision consumer only if a current handoff or checklist still points directly to Java capture before `executorConsistencyAuditAccepted`.
- Review checklist strings for excessive length only if a future focused test or handoff becomes hard to read.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
