# Phase 6OP Completion Handoff - Decompose Artifact Projection Guide

Date: May 25, 2026
Unit of Work: UOW-894
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-894] Document decompose artifact projection modes`)

## Status

Phase 6 is still in progress. This unit added an artifact projection guide so future selectable-decompose Java artifacts explicitly state whether they use the logical static-data override, the real Java XML candidate, or another deterministic fixture.

No Java runtime artifact was captured in this environment. Java capture remains blocked locally by Java 8 and missing Maven.

## Files Changed

- `docs/Phase-6-Decompose-Artifact-Projection-Guide.md`
- `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OP-Completion.md`

## What Changed

- Added `docs/Phase-6-Decompose-Artifact-Projection-Guide.md`.
- Updated the live-server runbook to require `fixture.projection.mode`.
- Defined projection modes:
  - `logical_static_override`: Java test data matches `101 -> 201 x2 / 202 x3`
  - `real_java_xml_candidate`: Java real XML source `188051516`, rewards `164000076 x100` and `164000073 x100`
  - `custom_java_fixture`: future deterministic selectable source after explicit C# projection support
- Documented allowed `fixture.id_mapping` uses for ids/object ids.
- Explicitly forbade reward-count normalization through `fixture.id_mapping`.
- Added stop conditions for ambiguous projection mode, count mapping, random counts, reward filtering, packet noise, and reward-add trailing cube update.
- No production Java/C# code changed.

## Tests

No tests were run because this was a documentation-only contract unit.

Validation:

```powershell
git diff --check
```

Result: passed aside from normal CRLF warnings.

Latest full C# validation remains from UOW-893:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1456 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `game-server/data/static_data/decomposable_items/decomposable_items.xml` | `docs/Phase-6-Decompose-Artifact-Projection-Guide.md` / `SelectableDecomposeTestData.RealJavaXmlCandidate` | Static Data / Fixture Contract | Partial | Manual Only for guide; Unit Tested in C# projection | Needs Verification | Guide documents exact real-candidate ids/counts and forbids count normalization through id mapping. Java runtime static-data loading remains unverified. |
| `game-server/data/static_data/items/item_templates.xml` | `docs/Phase-6-Decompose-Artifact-Projection-Guide.md` / generated C# fixture templates | Static Data / Item Template Contract | Partial | Manual Only for guide; Unit Tested in C# projection | Needs Verification | Guide records names and ids needed by future artifacts. Full template attributes and serialization effects remain unverified. |
| `com.aionemu.gameserver.model.templates.item.ResultedItem` | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests.SelectableDecomposeTestData` | DTO / Static Data Model | Partial | Manual Only for guide; Unit Tested in C# projection | Needs Verification | Guide relies on source-reviewed Java `max_count` normalization to explain `x100` rewards. Runtime JAXB behavior, random ranges, and race/class filtering remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / artifact comparison contract | Client Packet Handler | Partial | Manual Only for guide; Regression Tested in C# projection | Partial Parity | Guide defines artifact projection modes for the existing two selectable scenarios. No live Java handler artifact exists. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Contract Guide | Java XML/source audit and C# UOW-893 projection test | Documents artifact projection mode rules and count-mapping stop conditions. | Static source review plus previously passing C# projection tests. | No new runtime validation; no Java artifact; no packet byte comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The guide is only as useful as future artifact authors following `fixture.projection.mode`.
- Real XML candidate behavior may still be affected by live item restrictions, inventory capacity, event systems, server config, or packet side effects.
- Full item-info blob serialization, byte-level payload/frame parity, object-id allocation, persistence, threading, date/time, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 0 production code artifacts; 1 artifact projection guide added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, Java static-data override setup, SQL fixture execution, reward-add trailing cube update implementation decision, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, generate selectable-decompose artifacts using:

- `docs/Phase-6-Decompose-Java-Capture-Contract.md`
- `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`
- `docs/Phase-6-Decompose-Live-Server-SQL-Fixture-Appendix.md`
- `docs/Phase-6-Decompose-Real-XML-Candidate-Audit.md`
- `docs/Phase-6-Decompose-Artifact-Projection-Guide.md`

Then run the guarded C# comparison.

If tooling remains blocked, continue an isolated non-decompose Phase 6 gameplay slice that avoids shared decompose comparison helpers and progress docs until commit time.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Runtime capture should be controlled by one owner. |
| Non-decompose gameplay slice | isolated code/test files | Maybe | Avoid shared decompose comparison helpers. |
| Java static-data override design | docs only or isolated Java test profile files | Maybe | Runtime/profile work needs Java-capable environment. |
| AP rank-change legion contribution design | AP/rank service files and focused tests | Maybe | Safe if scoped away from decompose docs until final bookkeeping. |

## Do Not Parallelize

- Java observer implementation with live-server artifact capture unless one owner controls both.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, decompose capture docs, UOW-892 audit, UOW-893 handoff, this projection guide, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose an isolated non-decompose Phase 6 gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
