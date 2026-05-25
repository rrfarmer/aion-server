# Phase 6RY Completion Handoff - Nearby Quest Packet Prerequisite

Date: May 25, 2026
Unit of Work: UOW-981
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-981] Add nearby quest packet prerequisite`)

## Status

Phase 6 is still in progress. This unit implements only the `SM_NEARBY_QUESTS` packet prerequisite for the nearby-quest refresh path.

Production `CM_ITEM_PURIFICATION` dispatch remains plan-only. No real player-controller nearby refresh, candidate calculation, dynamic quest handler dispatch, or live quest callback execution was implemented.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6RY-Completion.md`

## What Changed

- Added `SmNearbyQuests` with opcode `127`, sourced from Java `ServerPacketsOpcodes`.
- Added `NearbyQuestMarker` to carry the caller-provided quest id and Java level-requirement-difference value.
- Matched Java `SM_NEARBY_QUESTS.writeImpl` payload layout:
  - `C(0)`
  - `H(-count & 0xffff)`
  - each quest id as `D`
  - bit `1 << 17` set when level diff is positive
- Added packet tests for empty, available, and not-yet-available markers, plus opcode assertion.
- Updated readiness docs to mark only the packet prerequisite complete.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | `SmNearbyQuests` packet | `SM_NEARBY_QUESTS`, `ServerPacketsOpcodes` | new packet and `GamePacketTests.cs` | Implementation / Tests | No write parallelism | Low | Completed by orchestrator; shared packet test file should have one writer. |
| B | Side-effect persistence docs | AP rank equipment/abyss skill persistence paths | docs/read-only first | Analysis | Yes if read-only | Medium | Safe future sidecar candidate. |
| C | Equipment side-effect persistence implementation | Java AP rank equipment side effects | persistence services/repository/tests | Implementation | No | Medium | Sequential until repository contract is narrowed. |
| D | Java observer artifact generation | `CM_ITEM_PURIFICATION`, packet/DB capture | Java/tooling/docs | Parity Verification | No | Medium | Still blocked locally by Java 8/Maven gap. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GamePacketTests
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Results:

- Focused packet suite: passed, 91 tests.
- Full game-server suite: passed, 1665 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`; `Aion.GameServer.Network.Aion.ServerPackets.NearbyQuestMarker` | Server Packet / DTO | Complete | Unit Tested | Verified Parity | Deterministic byte layout verified against Java source for caller-provided marker order. Java `HashMap` iteration order is not claimed. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests.PacketOpCode` | Opcode Mapping | Complete | Unit Tested | Verified Parity | Java registers `SM_NEARBY_QUESTS` as opcode `127`; C# asserts `OpCode == 127`. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service/adapter consuming `SmNearbyQuests` | Controller / Quest UI | Not Started | Manual Only | Needs Verification | Packet prerequisite exists, but no candidate calculation, start-condition evaluation, player-controller method, or send path exists. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Not started for nearby quest UI | Service / Quest Predicate | Not Started | No Tests | Unknown | Still needed with `allowedDiffToMinLevel = 2`, `warn = false`, and no skip flags before real marker calculation can exist. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` empty case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Empty marker list writes `C(0)` and zero negative count. | Deterministic byte assertion from source-reviewed Java packet layout. | No Java runtime capture. |
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` available case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Single marker writes negative count and unflagged quest id. | Deterministic byte assertion from source-reviewed Java packet layout. | Candidate calculation is not implemented. |
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` not-yet-available case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Positive level diff sets bit `1 << 17` before writing quest id. | Deterministic byte assertion from source-reviewed Java packet layout. | Packet order follows provided marker order; Java `HashMap` runtime order is not claimed. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `SmNearbyQuests` is only a packet prerequisite; no production code sends it yet.
- No C# world-instance quest id registry has been verified for Java `WorldMapInstance.questIds`.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- No C# dynamic `QuestNpc.onQuestStart` handler registration table exists.
- The current ItemPurification dispatcher seam must remain no-op until candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported: 2 packet/opcode artifacts for the nearby-quest packet prerequisite
- Total artifacts with verified parity: 2
- Total artifacts needing verification: 2
- Total blocked artifacts: 3 blocked/not-started categories, including nearby quest candidate calculation, quest start-condition evaluation, and dynamic quest handler registration
- Estimated overall migration completion: Phase 6 remains about 70% complete

## Next Recommended Unit of Work

Recommended safe task:
- Audit or implement the next read-only nearby-refresh candidate source: Java `WorldMapInstance.questIds` and dynamic `QuestNpc.onQuestStart` registration equivalents in C#, keeping `QuestService.checkStartConditions`, player-controller sends, dynamic handlers, and production ItemPurification dispatch disabled.

Alternative safe task:
- Use the sidecar persistence-gap analysis to document or implement the next ItemPurification side-effect persistence prerequisite.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | World-instance quest-id candidate analysis | Java/C# world, NPC spawn, quest-handler files read-only | Medium | Safe as a read-only explorer while orchestrator owns docs. |
| B | Side-effect persistence docs | Java/C# persistence files read-only | Medium | Safe if no repository payload edits. |
| C | Equipment side-effect persistence implementation | persistence service/repository/tests | Medium | Sequential until contract is defined; do not parallelize with skill persistence contract edits. |
| D | Java observer artifact generation feasibility | Java/tooling files or docs | Medium | Only if Java 25/Maven tooling is available. |

## Do Not Parallelize

- Multiple agents editing `GamePacketTests.cs`, packet files, or Phase 6 docs.
- Production `CM_ITEM_PURIFICATION` automatic dispatch with quest callback work.
- Real dynamic quest handler invocation with nearby-refresh work.
- ItemPurification persistence contract changes across repository/service/test files until ownership is narrowed.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting the next write unit.
4. Keep production `CM_ITEM_PURIFICATION` automatic dispatch disabled.
5. Run focused and full tests for any C# code changes.
6. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
7. Create the next handoff and commit the completed unit.
