# Phase 6AJK Completion - Composition Scheduled No-Rollback Coverage

Date: 2026-05-27
Unit of Work: UOW-1435
Status: Complete after validation.

## Scope

Add connection-level coverage for Java's scheduled composition no-rollback behavior after UOW-1434 aligned consumed packet ordering. Java remains the source of truth; this unit does not claim runtime byte parity.

## Completed Work

- Added an opcode `208` test that schedules a valid composition request and removes the second stone before completion.
- Verified C# keeps earlier consume packet side effects, adds no reward, and still sends success end animation.
- Hardened composition connection tests by using high-level enchantment stones whose reward range has no fixture reward template, avoiding random reward merge noise in packet-order tests.
- No production changes were required in this unit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesKeepsEarlierConsumesWhenSecondStoneDisappearsBeforeCompletion|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs|FullyQualifiedName~CompositionServiceTests|FullyQualifiedName~GamePacketTests.ClientPacketFactory_ParsesCompositeStonesPacket"`.
- Result: passed 12 tests.

## Migration Parity Table - UOW-1435

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | `Aion.GameServer.Network.Aion.ClientPackets.CmCompositeStones` / `GameServerConnection.HandleCompositeStonesAsync` | Client Packet / Connection Handler | Partial | Regression Tested | Partial Parity | Connection coverage now includes a valid scheduled request whose second stone disappears before completion. Initial validation, scheduled callback behavior, and end animation are covered; Java runtime bytes remain unavailable. |
| `com.aionemu.gameserver.model.templates.item.actions.CompositionAction` | `Aion.GameServer.Network.Aion.GameServerConnection.CompleteCompositeStonesAsync` / `Aion.GameServer.Services.CompositionService` | Item Action / Service | Partial | Unit + Regression Tested | Partial Parity | C# now packet-tests Java's no-rollback edge for missing second consume after earlier tool/first consumes. Same-item repeated consumption and runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId` | `CompositionService.CreateMutationPlan` / `CompositionConsumedItemMutation` | Storage Mutation Descriptor | Partial | Unit + Regression Tested | Partial Parity | Ordered successful consumes are emitted even when a later consume fails. Coverage uses missing second item after scheduling; repeated same-item update-then-delete remains untested. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` completion packet | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` via composition completion | Packet | Partial | Regression Tested | Partial Parity | Test asserts Java-style success end animation after partial consume/no reward. Runtime bytes and cancel/race cases remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesKeepsEarlierConsumesWhenSecondStoneDisappearsBeforeCompletion` | Regression / connection packet serialization | `CM_COMPOSITE_STONES`, `CompositionAction.run`, `Storage.decreaseByItemId` | Valid scheduled request consumes tool and first stone, sends delete/cube packets, adds no reward after second stone is removed before completion, and still sends success end animation. | C# packet parsing against reviewed Java no-rollback behavior. | No Java runtime bytes; same-item repeated consume edge not covered. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes` | Regression / connection packet serialization | Same Java action and storage fanout | Regression slice verifies mixed delete/update/delete ordering remains Java-shaped and deterministic after high-level fixture stone change. | Deterministic C# packet assertions. | No runtime bytes. |
| `GameServerConnectionInventoryExpansionUseItemTests.ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs` | Regression / connection packet serialization | Same Java action, remaining-stack branch | Regression slice verifies all-remaining consumed updates still carry cleanup/seal flag `3`. | Deterministic C# packet assertions. | No runtime bytes. |

## Remaining Risks

- Same-item repeated composition consumption may emit update then delete for the same object in Java; still needs focused analysis/test if reachable.
- AP extraction delete/cube semantics remain unaudited.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed; 1 connection test fixture scenario added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: Java runtime artifact generation, same-item repeated composition consume coverage, AP extraction delete/cube audit
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit AP extraction target/source delete plus cube update semantics.
- Why: the recent extraction/composition work aligned several no-blob delete packet branches, but AP extraction has a separate Java action/helper and should not inherit assumptions from regular extraction.
- Candidate files:
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
  - `dotnetConversion/src/Aion.GameServer/Services/ApExtractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`

## Alternative Sequential Task

- Task: analyze whether same-item repeated composition consumption is reachable and add a focused test if it is.
- Why: Java `decreaseByItemId` can update then later delete the same object if multiple consumes hit one stack. C# ordered descriptors should handle this, but no focused test covers it.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AP extraction Java packet analysis | Java AP extraction/storage sources, read-only | Medium | Safe sidecar before changing C# AP extraction packets. |
| B | Same-item composition reachability analysis | Java/C# composition sources and static-data, read-only | Medium | Determine whether client/object validation permits same-stack repeated consume. |
| C | Toy-pet source consume readiness | Java/C# toy-pet/kisk sources, read-only | Medium | Scheduling/world-spawn packet order still needs careful mapping. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer A | Analyze AP extraction delete/cube behavior | Java AP extraction/storage sources and C# AP extraction sources, read-only | all writes, docs, commits |
| Explorer B | Analyze same-item composition repeated consume reachability | Java/C# composition sources and static data, read-only | all writes, docs, commits |
| Orchestrator | Implement one selected packet semantics slice after analysis | selected production/test files only | shared docs until validation; unrelated files |

## Do Not Parallelize

- `GameServerConnection.cs` item-use packet fanout changes.
- Shared item-use test fixture edits.
- Shared progress/handoff/audit docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1435] Cover composition scheduled no rollback`.
- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_COMPOSITE_STONES.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/CompositionAction.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- C# files changed in UOW-1435:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJK-Completion.md`
