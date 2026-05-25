# Phase 6PN Completion Handoff - Trade AP Formula Planner

Date: May 25, 2026
Unit of Work: UOW-918
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-918] Add Trade AP formula planner`)

## Status

Phase 6 is still in progress. This unit adds a pure formula planner for Trade AP shop-buy costs, AP resale rewards, and trade-in AP deltas using Java `TradeList.calculateAbyssRewardBuyList` and `TradeService` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/TradeApFormulaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TradeApFormulaServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PN-Completion.md`

## What Changed

- Added `TradeApFormulaService`.
- Added Java-breadcrumbed formula coverage for AP shop-buy cost accumulation.
- Preserved Java double-to-int narrowing before final integer division.
- Added Java `Math.round(float)` semantics for AP resale rewards.
- Added trade-in AP delta calculation with positive-difference-only spend planning.
- Left live Trade/Purification wiring out of scope because C# still lacks the needed static data, packet handlers, validation, inventory mutation, and item mutation surfaces.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter TradeApFormulaServiceTests
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1561 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.trade.TradeList.calculateAbyssRewardBuyList` | `Aion.GameServer.Services.TradeApFormulaService.CalculateAbyssBuyRequiredAp` | Utility / Formula Planner | Partial | Regression Tested in C# | Partial Parity | Models AP/ABYSS acquisition cost formula and accumulation. Live acquisition filtering, required items, enough-AP guard, messages, inventory mutation, and packet-handler wiring remain missing. |
| `com.aionemu.gameserver.services.TradeService.performSellForAPToShop` | `TradeApFormulaService.CalculateApResaleReward` | Service Formula Projection | Partial | Regression Tested in C# | Partial Parity | Models AP resale `Math.round(float)` formula. Config gate, purchase-list validation, inventory deletion, and live AP add remain missing. |
| `com.aionemu.gameserver.services.TradeService.performBuyFromTradeInTrade` | `TradeApFormulaService.CalculateTradeInApDelta` | Service Formula Projection | Partial | Regression Tested in C# | Partial Parity | Models positive AP delta after subtracting required trade-in item AP value. Validation, packet compatibility, item grant/removal, enough-AP guard, and live AP spend remain missing. |
| `com.aionemu.gameserver.services.trade.PricesService.getVendorBuyModifier` | `TradeApFormulaService` `vendorBuyModifier` input | Pricing Dependency / Input Projection | Partial | Regression Tested in C# | Needs Verification | Formula accepts the projected modifier; Java's player/vendor/context calculation is not ported. |
| `com.aionemu.gameserver.model.templates.item.Acquisition` | `Aion.GameServer.Services.TradeApCostComponent` | DTO / Input Projection | Partial | Regression Tested in C# | Needs Verification | Represents only `requiredAp` and count. Acquisition type filtering and required item stacks remain outside this slice. |
| `com.aionemu.gameserver.model.templates.item.AcquisitionType.AP` / `AcquisitionType.ABYSS` / `AcquisitionType.ABYSS_KINAH` | `TradeApFormulaService` rate inputs | Enum / Input Projection | Partial | Regression Tested in C# | Needs Verification | Tests cover caller-supplied normal and abyss-kinah rates. Template/enum caller selection logic is not ported. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | Not invoked in this unit | Service / AP Mutation | Not Started for Trade live path | No Tests in this unit | Needs Verification | Java Trade callers spend or award AP after validation. This unit computes amounts only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_ITEM` | Not ported in this unit | Client Packet Handler | Not Started | No Tests | Unknown | Live shop-buy AP spend remains unported. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BUY_TRADE_IN_TRADE` | Not ported in this unit | Client Packet Handler | Not Started | No Tests | Unknown | Live trade-in AP spend remains unported. |
| `com.aionemu.gameserver.services.item.ItemPurificationService` | Not ported in this unit | Service / AP Spend Caller | Not Started | Manual Analysis | Needs Verification | AP precheck/spend was discovered but not ported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CalculateAbyssBuyRequiredAp_MatchesJavaDoubleNarrowingAndFinalIntegerDivision` | Regression | Java `TradeList.calculateAbyssRewardBuyList` source review | Validates AP shop-buy formula, double narrowing, final integer division, and component accumulation. | Deterministic C# regression grounded in Java source arithmetic. | No Java runtime artifact; live spend path not covered. |
| `CalculateAbyssBuyRequiredAp_UsesAbyssKinahModifierInputLikeJavaCaller` | Regression | Java acquisition rate selection source review | Validates normal versus abyss-kinah rate input behavior. | Deterministic C# regression for projected inputs. | Enum/template selection logic is not ported. |
| `CalculateApResaleReward_MatchesJavaMathRoundAndCountCast` | Regression | Java `TradeService.performSellForAPToShop` source review | Validates Java `Math.round(float)` behavior and count narrowing. | Deterministic C# regression grounded in Java source arithmetic. | Live config, validation, inventory, and AP add not covered. |
| `CalculateTradeInApDelta_SpendsOnlyPositiveDifference` | Regression | Java `TradeService.performBuyFromTradeInTrade` source review | Validates target AP cost minus trade-in item AP value and zero floor. | Deterministic C# regression grounded in Java source arithmetic. | Required item validation, item grant/removal, and live AP spend not covered. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `TradeApFormulaService` is formula-only; it does not validate player AP, mutate AP, remove inventory, grant items, send messages, or handle packets.
- Trade/goods static data, acquisition template projection, purchase-list validation, and old trade-in packet compatibility remain unported.
- Formula parity is source-reviewed and regression-tested in C#, but not Java-runtime compared.
- Upstream callers must preserve Java input ordering, integer overflow behavior, rate selection, and count narrowing.
- ItemPurification AP precheck/spend remains unported.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 10
- Total artifacts ported: 1 pure Trade AP formula planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, live Trade packet wiring, trade/goods static data, acquisition template projection, inventory/item mutation, ItemPurification data/mutation, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Trade AP formulas | `TradeList`, `TradeService` | `TradeApFormulaService.cs`, tests | Formula Planner | No within same files | Low-Medium | Completed in UOW-918 as a pure planner. |
| B | ItemPurification AP precheck/spend | `ItemPurificationService` | new planner/test files only | Formula Planner | Yes if isolated | Medium | Static data and item mutation remain missing, but pure AP precheck/spend can be isolated. |
| C | Trade live adapter discovery | `CM_BUY_ITEM`, `CM_BUY_TRADE_IN_TRADE`, `TradeService` | packet/static-data surfaces | Analysis / Service Port | Maybe | High | Live wiring crosses missing data loaders, inventory mutation, packet handlers, and validation surfaces. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a pure ItemPurification AP precheck/spend planner from Java `ItemPurificationService`, scoped to AP validation/spend amount only and without item upgrade mutation wiring.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java `ItemPurificationService` AP precheck/spend and material mutation boundaries | read-only Java/C# inspection | edits, docs | Source-truth map and blocked-dependency list. |
| Agent B | Inspect C# item/template surfaces for a possible pure purification planner | read-only C# inspection | edits, docs | Candidate C# input DTO shape and test cases. |
| Orchestrator | Integrate one selected pure planner slice | exact new service/test files plus docs | live packet/inventory mutation unless explicitly scoped | Code, tests, docs, commit. |

## Do Not Parallelize

- Live Trade packet wiring with purification formula work.
- Multiple agents editing one planner file.
- Progress and handoff docs.
- Inventory or item mutation ports without a broader static-data and packet-handler plan.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue pure ItemPurification AP precheck/spend planning or another narrow AP caller boundary.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
