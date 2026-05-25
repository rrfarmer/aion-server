# Phase 6OO Completion Handoff - Decompose Real XML Projection Support

Date: May 25, 2026
Unit of Work: UOW-893
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-893] Parameterize decompose real XML projection`)

## Status

Phase 6 is still in progress. This unit added narrowly scoped C# test projection support for the real Java selectable-decompose XML candidate found in UOW-892.

No Java runtime artifact was captured in this environment. The new support does not claim parity; it makes the C# observer capable of producing comparison data with the audited Java source/reward ids and counts without abusing broad `fixture.id_mapping` normalization.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OO-Completion.md`

## What Changed

- Added `SelectableDecomposeTestData` to parameterize the selectable-decompose fixture source item, reward item ids, and deterministic reward counts.
- Preserved the default logical fixture exactly as `101 -> 201 x2 / 202 x3`.
- Added `SelectableDecomposeTestData.RealJavaXmlCandidate`:
  - source: `188051516` (`Smart Greater Scroll Bundle`)
  - reward index 0: `164000076 x100` (`Greater Running Scroll`)
  - reward index 1: `164000073 x100` (`Greater Courage Scroll`)
- Updated `CaptureSelectableDecomposeObservationJsonAsync` and `InventoryExpansionUseItemFixture.CreateAsync` to accept the optional projection data.
- Added a Java source breadcrumb next to the real candidate constants.
- Added `CaptureSelectableDecomposeObservationJson_WithRealJavaXmlCandidate_ProjectsRealRewardCounts`.
- No production Java/C# code changed.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 35 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1456 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `game-server/data/static_data/decomposable_items/decomposable_items.xml` (`188051516`) | `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests.SelectableDecomposeTestData.RealJavaXmlCandidate` | Static Data / Fixture Projection | Partial | Unit Tested in C# projection | Needs Verification | Real candidate ids/counts are now projectable in C# tests: source `188051516`, reward index 0 `164000076 x100`, reward index 1 `164000073 x100`. This is static-source evidence only; Java XML loading and live handler output were not executed. |
| `game-server/data/static_data/items/item_templates.xml` (`188051516`, `164000076`, `164000073`) | Generated `static_data.xml` inside `InventoryExpansionUseItemFixture.CreateAsync` | Static Data / Item Templates | Partial | Unit Tested in C# projection | Needs Verification | C# fixture can emit test item templates with the Java ids. Template names, full Java attributes, item actions beyond decompose, and client item-info blob parity remain unverified. |
| `com.aionemu.gameserver.model.templates.item.ResultedItem` | Parameterized C# decomposable fixture XML in `InventoryExpansionUseItemFixture.CreateAsync` | DTO / Static Data Model | Partial | Unit Tested in C# projection | Needs Verification | Deterministic `min_count=max_count=100` can be projected. Java defaulting for omitted `max_count`, random count ranges, race/class filtering beyond existing default tests, and precision/rounding edge cases remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` loaded from generated fixture XML | Data Holder | Partial | Unit Tested in C# projection | Needs Verification | C# loader accepts parameterized selectable XML. Java first-`items` group selection and live static-data load remain unverified at runtime. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / `CaptureSelectableDecomposeObservationJsonAsync` | Client Packet Handler / Test Observer | Partial | Regression Tested in C# | Partial Parity | The C# observer can now produce future-comparable artifacts for the real Java candidate without using `fixture.id_mapping` for counts. No Java runtime artifact is present, so parity cannot be verified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` decoded by test observer | Server Packet | Partial | Regression Tested in C# | Needs Verification | New test validates projected reward ids/counts in decoded add-item packets. Full item-info blob serialization, byte-level payloads, and Java packet order remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` decoded by test observer | Server Packet | Partial | Regression Tested in C# | Needs Verification | Decrement scenario still emits source count update. Date/time, threading, persistence timing, and Java runtime dispatch order are unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` decoded by test observer | Server Packet | Partial | Regression Tested in C# | Needs Verification | Delete scenario still emits source deletion before cube update and reward add. Java runtime packet sequence remains uncaptured. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CaptureSelectableDecomposeObservationJson_WithRealJavaXmlCandidate_ProjectsRealRewardCounts` | Unit / Regression | Java `decomposable_items.xml` and `item_templates.xml` static-source audit | Validates C# projection can emit source `188051516`, reward index 1 `164000073 x100` for decrement, and reward index 0 `164000076 x100` for delete. | Deterministic C# projection from source-reviewed Java static data. | Does not compare against a live Java artifact; does not verify Java XML loading, packet bytes, reward object-id allocation, trailing reward-add cube update, or full item-info blob parity. |

## Remaining Risks

- Java runtime capture remains unavailable locally.
- This unit prepares C# comparison data for real Java ids/counts but does not prove Java live-server parity.
- The test fixture still uses simplified item templates; full Java item-template attributes and serialization effects may differ.
- Broad `fixture.id_mapping` remains intentionally limited to ids and must not be used to normalize reward counts.
- Java `ItemPacketService.sendStorageUpdatePacket` may still add a trailing reward `SM_CUBE_UPDATE`; the UOW-891 diagnostic remains the guard for that future artifact.
- Full item-info blob, byte-level frame parity, Java `IDFactory` allocation, persistence, threading/dispatcher ordering, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 0 production code artifacts; 1 C# selectable-decompose projection path parameterized for real Java XML ids/counts
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, Java static-data override setup, SQL fixture execution, reward-add trailing cube update implementation decision, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, execute the live-server runbook with either:

- real XML candidate source `188051516`, reward index 0 `164000076 x100`, reward index 1 `164000073 x100`
- Java static-data override source `101`, reward index 0 `201 x2`, reward index 1 `202 x3`

Generate `JD-SEL-DEC-001.json` and `JD-SEL-DEL-001.json`, then run the guarded C# comparison.

If tooling remains blocked, continue an isolated Phase 6 gameplay slice that does not touch shared decompose helpers, or add a small artifact-schema note documenting when to use the default logical fixture versus the real Java XML candidate projection.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Runtime capture should be controlled by one owner. |
| Artifact-schema note for real-vs-logical decompose fixtures | docs only | Maybe | Safe if it avoids `PHASE-6-PROGRESS.md` until commit time. |
| Non-decompose gameplay slice | isolated code/test files | Maybe | Only if it avoids shared decompose comparison helpers and progress docs. |
| Java static-data override design | docs only or isolated Java test profile files | Maybe | Runtime/profile work needs clear ownership. |

## Do Not Parallelize

- Java observer implementation with live-server artifact capture unless one owner controls both.
- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, decompose capture docs, UOW-892 audit, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose an isolated non-decompose Phase 6 gameplay slice or decompose artifact-schema documentation.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
