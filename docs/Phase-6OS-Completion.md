# Phase 6OS Completion Handoff - Abyss Points Planning Slice

Date: May 25, 2026
Unit of Work: UOW-897
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-897] Add abyss points planning service`)

## Status

Phase 6 is still in progress. This unit added a reusable C# planning/mutation boundary for Java `AbyssPointsService.addAp` behavior and the contribution-update subset of `SM_LEGION_EDIT`.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/AbyssPointsService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionEdit.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AbyssPointsServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OS-Completion.md`

## What Changed

- Reviewed Java:
  - `com.aionemu.gameserver.services.abyss.AbyssPointsService`
  - `com.aionemu.gameserver.model.gameobjects.player.AbyssRank`
  - `com.aionemu.gameserver.services.SiegeService`
  - `com.aionemu.gameserver.model.team.legion.Legion`
  - `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT`
- Added `AbyssPointsService.AddAp`.
- Added `AbyssPointsService.AddApFromObject`.
- Added AP add plan records for:
  - player packets
  - rank update broadcast intent
  - rank-limit equipment and abyss-skill flags
  - legion contribution intent
  - siege callback intent
- Added `SmLegionEdit.Contribution(long contributionPoints)` for Java edit type `0x03`.
- Added `AbyssPointsServiceTests`.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~AbyssPointsServiceTests --no-restore
```

Result: passed, 7 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1466 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Unit Tested | Partial Parity | C# planner covers null-player no-op, AP gain/spend messages, rank packet intents, rank-change flags, legion contribution intent, and visible-object siege callback intent. Not yet wired into AP callers. Java logging for values over 30000, direct packet dispatch, equipment/skill side-effect execution, and runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.AbyssRank` | `Aion.GameServer.Model.GameObjects.PlayerAbyssRank` | Model | Partial | Unit Tested through AP planner and existing rank tests | Needs Verification | Existing `AddAp` behavior is reused. Java AP-cap config (`CustomConfig.ENABLE_AP_CAP` / `AP_CAP_VALUE`) is not represented in this slice. GP rank thresholds, daily/weekly positive-only AP updates, and zero-floor behavior remain source-reviewed but not runtime compared. |
| `com.aionemu.gameserver.services.SiegeService` | `Aion.GameServer.Services.AbyssPointsSiegeCallback` | Service / Intent | Partial | Unit Tested | Needs Verification | C# records a callback intent only for player sources or non-peace siege NPC sources, matching Java `onAbyssPointsAdded` gate. Active siege counter mutation is not ported here. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Services.AbyssPointsLegionContribution` | Model / Intent | Partial | Unit Tested | Needs Verification | C# records positive gained AP as legion contribution and computes a new contribution total from supplied current contribution. No full Legion aggregate, persistence, online-member lookup, or broadcast execution exists in this slice. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionEdit` | Server Packet | Partial | Unit Tested | Needs Verification | Only type `0x03` contribution update is ported and payload-tested (`C type`, `Q contribution`). Other Java edit types (`0x00`, `0x01`, `0x02`, `0x04`-`0x08`) remain unsupported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Unit Tested via planner intent and existing packet tests | Needs Verification | Planner emits rank packet intent when AP/rank changes. Byte-level packet parity relies on existing packet tests, not Java runtime output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRankUpdate` | Server Packet | Partial | Unit Tested via planner intent and existing packet tests | Needs Verification | Planner emits rank-change broadcast intent when rank changes. Visible-player fanout and equipment/skill side effects are flags only in this slice. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Unit Tested | Needs Verification | Planner tests cover Java AP gain (`1320000`) and spend (`1300965`) message ids. Message parameter payloads were not byte-compared against Java runtime. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `AddAp_GainsPointsSendsRankAndLegionContributionLikeJava` | Unit | Java `AbyssPointsService.addAp`, `AbyssRank.addAp`, `SM_LEGION_EDIT` source review | Positive AP gain mutates rank, emits gain/rank intents, flags rank side effects, and creates a contribution update packet with the new total. | Deterministic C# unit test grounded in Java source. | No Java runtime comparison; no full Legion aggregate or broadcast execution. |
| `AddAp_SpendingSendsUseMessageWithoutLegionContribution` | Unit | Java `AbyssPointsService.addAp` source review | Negative AP sends spend message, emits rank packet for AP change, and does not add legion contribution. | Deterministic C# unit test grounded in Java source. | AP-cap config and runtime message bytes not verified. |
| `AddApFromObject_CreatesSiegeCallbackOnlyForPlayerOrNonPeaceSiegeNpc` | Unit | Java `AbyssPointsService.addAp(Player, VisibleObject, int)` and `SiegeService.onAbyssPointsAdded` source review | Siege callback intent appears only for player or non-peace siege-NPC sources. | Deterministic C# unit test grounded in Java source. | Active siege counters are not ported. |
| `AddAp_NullPlayerDoesNothingLikeJava` | Unit | Java `AbyssPointsService.addAp` null guard | Null player returns no mutation or packets. | Deterministic C# unit test grounded in Java source. | None for this narrow branch; broader service still partial. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `AbyssPointsService` is not yet wired into `ApExtractService`, PVP, quest rewards, trade, item charge, purification, or NPC reward callers.
- Full legion model, persistence, online member broadcast, and other `SM_LEGION_EDIT` edit types remain unported.
- Active siege counter updates are represented only as intents.
- Java AP-cap config is not represented in `PlayerAbyssRank.AddAp` or the new planner.
- Rank-change side effects (`Equipment.checkRankLimitItems`, `AbyssSkillService.updateSkills`) are flags only; caller wiring remains future work.
- Packet bytes were tested for the new contribution packet but not compared against Java runtime output.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 partial AP planning service and 1 partial legion-edit packet
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 7 blocked/not-started categories, including caller wiring, full Legion aggregate/broadcast, active siege counters, Java AP cap config, rank-limit equipment execution, abyss-skill update execution, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Wire `AbyssPointsService` into the AP extraction path as the smallest existing AP caller. Preserve current inventory mutation behavior while adding AP packet/side-effect intents to the connection-level execution path.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| AP extraction caller wiring | `GameServerConnection.cs`, `PlayerEnterWorldService.cs`, `ApExtractService.cs`, AP tests | No | Likely touches shared execution path; keep sequential. |
| AP cap config audit | Java config/source reads, possible docs only | Yes if read-only | Useful before implementing cap support. |
| Remaining `SM_LEGION_EDIT` packet types | `SmLegionEdit.cs`, packet tests | No if AP caller wiring also touches packet | Could be a separate future packet-focused unit. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Runtime capture should be controlled by one owner. |
| Independent non-AP gameplay slice | isolated files only | Maybe | Safe if it avoids AP/emotion/decompose files and shared docs until final bookkeeping. |

## Do Not Parallelize

- AP caller wiring with other edits to `GameServerConnection.cs`.
- `SmLegionEdit.cs` with other legion packet work in the same parallel batch.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, wire `AbyssPointsService` into the AP extraction execution path or choose another isolated gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
