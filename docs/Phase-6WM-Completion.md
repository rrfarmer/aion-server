# Phase 6WM Completion - UOW-1099 Bonus Selection Envelope

Date: May 26, 2026

## Unit Of Work

UOW-1099: `[Phase 6][UOW-1099] Add quest bonus selection envelope`

## Summary

UOW-1099 adds a non-live selection envelope over the UOW-1098 bonus candidate plan. It reports Java `Chance.selectElement` input values and deterministic null-result statuses without performing weighted RNG, selecting a group/item, rolling counts, dispatching handlers, or mutating inventory.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestBonusSelectionEnvelopeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestBonusSelectionEnvelopeServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WM-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestBonusSelectionEnvelopeServiceTests\|QuestBonusCandidatePlanServiceTests\|QuestBonusItemGroupXmlProjectionExtractorTests" --nologo` | Passed: 10 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,163 tests. |

## Migration Parity Table - UOW-1099

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.Chance` | `QuestBonusSelectionEnvelopeService`; `QuestBonusSelectionEnvelope`; `QuestBonusSelectionGroupEnvelope` | Utility / RNG Boundary | Partial | Unit Tested | Partial Parity | C# preserves group/item chance inputs and flags deterministic null-result surfaces. Java RNG, selected element removal, and runtime comparison remain unimplemented. |
| `com.aionemu.gameserver.services.reward.BonusService.getQuestBonus` | `QuestBonusSelectionEnvelopeService` | Reward Service Dependency | Partial | Unit Tested | Partial Parity | Models the pre-RNG input surface after candidate filtering. It does not select a group/item, create `QuestItems`, call handlers, or mutate inventory. |
| `com.aionemu.gameserver.services.reward.BonusService.getMatchingItemsOfRandomGroup` | `QuestBonusCandidatePlan` consumed by `QuestBonusSelectionEnvelopeService` | Reward Service Dependency | Partial | Unit Tested | Partial Parity | Candidate filtering feeds this envelope. Java's selected empty-group retry ordering is still not executed. |
| `com.aionemu.gameserver.model.templates.itemgroups.BonusItemGroup` | `QuestBonusSelectionGroupEnvelope` | DTO / Chance Input | Partial | Unit Tested | Partial Parity | Group chance metadata is preserved. JAXB collection mutability and `Chance.selectElement(remainingGroups, true)` removal semantics remain unverified. |
| `com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry` | `QuestBonusSelectionItemEnvelope` | DTO / Chance Input | Partial | Unit Tested | Partial Parity | Item id, effective chance, and count metadata are preserved. Runtime subclass count randomness and `QuestItems` creation remain disabled. |
| `com.aionemu.gameserver.model.templates.rewards.FullRewardItem` | `QuestBonusSelectionItemEnvelope` | DTO / Chance Input | Partial | Unit Tested | Partial Parity | Positive and zero item chance surfaces are tested. Java default zero chance null-selection behavior is reported, not executed. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `QuestBonusSelectionEnvelope.Input` | Quest Template Dependency | Partial | Unit Tested | Needs Verification | Carries explicit candidate-plan input instead of production `QuestTemplate`. Production template/player/runtime integration remains unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestBonusSelectionEnvelopeServiceTests.CreateEnvelope_ReportsJavaChanceInputsWithoutSelectingGroupOrItem` | Group chance sum, item chance sums, item count metadata, and skipped item count are reported without selection. | Source-reviewed from `Chance.selectElement` and `BonusService.getQuestBonus`; no Java runtime comparison. |
| `QuestBonusSelectionEnvelopeServiceTests.CreateEnvelope_ReportsNoCandidateGroupsLikeJavaBonusServiceNullResult` | Empty candidate groups report a no-candidate status equivalent to Java no bonus. | Source-reviewed Java null branch; no runtime comparison. |
| `QuestBonusSelectionEnvelopeServiceTests.CreateEnvelope_ReportsNoPositiveGroupChanceLikeJavaChanceNullSelection` | Zero group chance sum is flagged before RNG. | Source-reviewed Java `sumOfChances <= 0` branch; no runtime comparison. |
| `QuestBonusSelectionEnvelopeServiceTests.CreateEnvelope_ReportsGroupWithNoPositiveItemChanceLikeJavaItemSelectionNull` | Zero item chance sum is flagged as an item-selection null-result risk. | Source-reviewed Java item `Chance.selectElement` branch; no runtime comparison. |

## Remaining Risks

- Java RNG and exact `Rnd.nextFloat(sumOfChances)` behavior are not ported or runtime-compared.
- Java selected empty-group retry ordering is still not modeled because filtering is currently precomputed.
- Random count rolls remain metadata only.
- `QuestEngine.onBonusApplyEvent`, `QuestItems` creation, `ItemService.addItem`, inventory capacity, packet sends, persistence, rollback, and production quest-finish wiring remain disabled.
- Non-positive and negative chance edge cases are detected conservatively but not compared against Java runtime behavior.
- JAXB/schema validation, invalid XML behavior, reflection/dynamic handler behavior, threading/player-ordering, date/time reward-repeat behavior, and Java runtime comparison remain open.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 partial non-live bonus selection envelope
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7
- Total blocked artifacts: 9 blocked/partial categories: Java RNG/Chance selection, selected-group retry execution, production socket integration, live item reward mutation, live non-item reward mutation, `QuestEngine.onBonusApplyEvent`, random count rolls, JAXB validation parity, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Analyze `QuestEngine.onBonusApplyEvent` registrations and bonus handlers read-only, then add a disabled handler-event outcome model if the Java event surface is small enough.

Keep live handler dispatch, RNG, `QuestItems` creation, inventory mutation, packet sends, and production quest-finish wiring disabled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Bonus handler event analysis | Java source only/read-only | Low/Medium | Recommended next step before handler-event modeling. |
| B | Disabled bonus handler outcome model | new service/tests | Medium | Only after A confirms small enough surface. |
| C | Production runtime input audit | read-only audit doc or isolated tests | Medium | Useful before composing bonus planner into quest-finish operation planning. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze bonus handler event registrations | Java source only | All writes |
| Orchestrator | Review analysis and decide next UOW | Docs only after implementation decision | Production reward planners unless selected exclusively |

## Do Not Parallelize

- `QuestBonusCandidatePlanService.cs`: owns deterministic candidate filtering.
- `QuestBonusSelectionEnvelopeService.cs`: owns selection-envelope contract.
- `QuestFinishRewardPlanService.cs` and `QuestFinishOperationPlanService.cs`: shared quest reward planner contracts.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1098 and UOW-1099 are non-live and must not be treated as full bonus reward parity.
- Candidate filtering and selection-envelope services are explicit-input planners; production quest/player/runtime integration is absent.
- Next safest work is read-only Java analysis of `QuestEngine.onBonusApplyEvent` and related handler registrations before modeling handler outcomes.
- Full solution validation after UOW-1099 passed 2,163 tests.
