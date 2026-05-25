# Phase 6ON Completion Handoff - Decompose Real XML Candidate Audit

Date: May 25, 2026
Unit of Work: UOW-892
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-892] Audit decompose real XML candidates`)

## Status

Phase 6 is still in progress. This unit audited real Java selectable-decompose XML candidates for the future live-server capture path.

No Java runtime artifact was captured in this environment. The audit found a usable deterministic real XML candidate, but also found that no selectable Java XML entry matches the current C# logical fixture reward counts exactly. That means the next comparison step must choose either a Java static-data override or narrow C# projection support for real Java reward counts.

## Files Changed

- `docs/Phase-6-Decompose-Real-XML-Candidate-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ON-Completion.md`

## What Changed

- Added `docs/Phase-6-Decompose-Real-XML-Candidate-Audit.md`.
- Reviewed Java:
  - `game-server/data/static_data/decomposable_items/decomposable_items.xml`
  - `game-server/data/static_data/decomposable_items/decomposable_items.xsd`
  - `game-server/data/static_data/items/item_templates.xml`
  - `DecomposableItemsData`
  - `DecomposableItemInfo`
  - `ResultedItem`
- Confirmed Java has 187 selectable decomposable XML entries.
- Confirmed Java selectable loader uses the first `<items>` group only.
- Confirmed Java `ResultedItem` normalizes missing `max_count` to `min_count`.
- Confirmed no real selectable XML entry matches the current C# logical fixture's first-two reward counts of `2` and `3`.
- Identified `188051516` (`Smart Greater Scroll Bundle`) as the best low-noise real XML candidate:
  - reward index 0: `164000076 x100` (`Greater Running Scroll`)
  - reward index 1: `164000073 x100` (`Greater Courage Scroll`)
- Documented that counts are deterministic but differ from the current C# projection.
- No production Java/C# code changed.

## Tests

No tests were run because this was a documentation-only static-data audit.

Latest known C# validation remains from UOW-891:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 34 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1455 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` / guarded comparison fixture | Data Holder | Partial | Manual Only for audit; Regression Tested in C# comparison helper | Needs Verification | Java loader source reviewed: selectable entries use only first `<items>` group and return copied reward lists. Real XML runtime loading was not executed locally. |
| `com.aionemu.gameserver.model.templates.item.DecomposableItemInfo` | `Aion.GameServer.Data.StaticData` decomposable item model | DTO / Static Data Model | Partial | Manual Only | Needs Verification | XML attributes `item_id` and `selectable` reviewed. C# static-data equivalent was not changed in this unit. |
| `com.aionemu.gameserver.model.templates.item.ResultedItem` | `Aion.GameServer.Data.StaticData` resulted-item model / decompose fixture projection | DTO / Static Data Model | Partial | Manual Only | Needs Verification | Java count normalization and race/class filtering reviewed. No runtime comparison; count behavior remains a future projection concern if real XML ids are used. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / guarded comparison helper | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for XML audit | Partial Parity | Audit selects possible real source/reward ids for future handler capture. No Java runtime artifact generated. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Static Data Audit | Java XML, Java XSD, `DecomposableItemsData`, `ResultedItem` source review | Identifies real selectable XML candidates and documents count mismatch with current C# logical fixture. | Static source/XML inspection only. | No Java XML loader execution, no runtime artifact, no C# projection change, no Java/C# comparison. |

## Remaining Risks

- Java runtime capture remains unavailable locally.
- The recommended real XML candidate has deterministic counts, but they differ from the current C# logical fixture.
- Item template existence was checked by source text, not Java runtime static-data loading.
- Real live-server behavior may still be affected by item use restrictions, inventory capacity, event systems, login rewards, or server configuration.
- Count parameterization must be carefully scoped; broad numeric normalization could hide real parity mismatches.
- Byte-level payload/frame parity, full item-info blob parity, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 0 production code artifacts; 1 real XML candidate audit document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 4
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, Java static-data override setup, C# real-count projection support, SQL fixture execution, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, choose between:

- Java static-data override matching the existing logical fixture: source `101`, reward index 0 `201 x2`, reward index 1 `202 x3`
- Real XML candidate capture: source `188051516`, reward index 0 `164000076 x100`, reward index 1 `164000073 x100`

Then execute the live-server runbook with:

- `docs/Phase-6-Decompose-Java-Capture-Contract.md`
- `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`
- `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`
- `docs/Phase-6-Decompose-Live-Server-SQL-Fixture-Appendix.md`
- `docs/Phase-6-Decompose-Real-XML-Candidate-Audit.md`

If tooling remains blocked, add narrowly scoped C# projection support for real Java selectable-decompose source/reward ids and counts so a future artifact using `188051516` can be compared without abusing `fixture.id_mapping`.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Runtime capture should be controlled by one owner. |
| Real-count C# projection support | `GameServerConnectionInventoryExpansionUseItemTests.cs` | No | Shared comparison helper; do sequentially. |
| Non-decompose gameplay slice | isolated code/test files | Maybe | Only if it avoids shared decompose comparison helpers and progress docs. |
| Java static-data override design | docs only or isolated Java test profile files | Maybe | Docs are parallel-safe; Java static-data/profile changes need clear ownership. |

## Do Not Parallelize

- Java observer implementation with live-server artifact capture unless one owner controls both.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, decompose capture docs, this audit, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose real-count C# projection support or an isolated non-decompose Phase 6 gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
