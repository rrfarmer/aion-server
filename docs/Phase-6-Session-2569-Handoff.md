# Phase 6 Session 2569 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2569: Wire CM_GROUP_DISTRIBUTION live (group variant) — kinah is now distributed across online group members

## Session Summary

| UOW | Summary |
|-----|---------|
| 2564 | Partial-stack exchange trade splits. |
| 2565 | Faction-prefixed names in player search. |
| 2566 | SM_LEGION_HISTORY server packet (golden-tested). |
| 2567 | Legion member rank loaded at enter-world. |
| 2568 | Group kinah-distribution decision planner + split SM_SYSTEM_MESSAGE factories. |
| 2569 | **Live** group kinah distribution: CM_GROUP_DISTRIBUTION (partyType 1, non-alliance) now decreases the distributor's cube kinah and increases each online member's kinah, with DEC_KINAH_BUY / INC_KINAH_COLLECT updates and the ME_TO_B / B_TO_ME messages. 5 dispatch tests. |

## MILESTONE: Group kinah distribution is live

CM_GROUP_DISTRIBUTION partyType 1 (player not in an alliance) is now wired end-to-end using the UOW-2568
planner + messages. Alliance (partyType 2) and league (partyType 3) variants remain deferred.

## What changed (UOW-2569)

Java source of truth:
- `CM_GROUP_DISTRIBUTION.runImpl`: `amount < 2` returns; `PlayerRestrictions.canTrade`; partyType 1 →
  `PlayerGroupService.distributeKinah` (or `PlayerAllianceService.distributeKinahInGroup` if in alliance).
- `TeamKinahDistributionEvent.handleEvent`: distributor `tryDecreaseKinah(amount)` (Storage default
  `DEC_KINAH_BUY`), then each online member (incl. the distributor) `increaseKinah(rewardPerPlayer)` (Storage
  default `INC_KINAH_COLLECT`); distributor gets `STR_MSG_SPLIT_ME_TO_B`, others `STR_MSG_SPLIT_B_TO_ME`.

C# changes:
- `GameServerConnection.HandleGroupDistributionAsync(player, amount, partyType)`:
  - Guards: `amount < 2`; partyType 1 && `TeamMembership == Group` (alliance/league deferred); `CurrentTeamId != 0`.
  - Uses `GroupKinahDistributionPlanService.Plan(amount, distributorKinah, onlineMembers.Count, hasMember)`.
  - `NotEnoughMoney` → `SmSystemMessage.NotEnoughMoney()` to the distributor; `NoDistribution`/`Ignored` → nothing.
  - `Distribute`: decrease distributor cube kinah by `amount` (DEC_KINAH_BUY); for each online member increase
    cube kinah by `RewardPerPlayer` (INC_KINAH_COLLECT) and send the per-member split message.
  - In-memory kinah mutation reuses the exchange pattern (`CopyInventoryItem(count:)` + `ReplaceInventoryItemFor`).
- `GameServerConnection.SendToPlayerOrActiveAsync` — small fan-out helper (registry first, else active connection).
- `PlayerGroupRuntime.GetOnlineMemberPlayers(teamId)` — Java `TemporaryPlayerTeam.getOnlineMembers` (members with `Player.IsOnline`).
- Dispatch `case CmGroupDistribution` now calls the handler (was a no-op).

### Intentional differences / documented gaps

- **canTrade not modeled**: Java gates on `PlayerRestrictions.canTrade(player)`; the port has no equivalent yet.
  Documented omission — it only blocks distribution in transient restricted states (e.g. mid-action). Low risk.
- **Alliance/league deferred**: partyType 2/3 and the partyType-1-in-alliance branch are no-ops (no `League`
  team type is modeled; alliance distribution needs the alliance online-member fan-out). Documented.
- **Kinah persistence**: kinah counts are mutated in memory only (logout/periodic save), matching the exchange
  kinah model (UOW-2554/2563). Not immediately persisted.

## Validation Decision (UOW-2569)

- Changed surface: production-code with live multi-player state mutation (group kinah) + a new runtime accessor + tests.
- Specific behavior/contract: even/uneven (truncating) split moves kinah correctly across online members,
  distributor net = −amount + reward; insufficient kinah sends only NotEnoughMoney; amount < 2 and the
  alliance variant do nothing; ME_TO_B to distributor and B_TO_ME to others.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionGroupDistributionTests"` → 5/5; adjacency `...|GroupKinahDistributionPlanServiceTests|GameServerConnectionExchangeTradeTests` → 18/18.
- Focused Java/Maven command: none. No isolated Maven test targets the distribution event; parity from direct
  review of the event + Storage kinah update types, asserted via the dispatch tests.
- Broad-validation trigger: none. This change mutates multiple players' in-memory kinah, but it is isolated to the
  new handler + a new read-only runtime accessor; the focused multi-player dispatch tests cover the blast radius.
- Broad .NET decision: skipped. The filtered tests built `Aion.GameServer` (handler, runtime accessor, messages)
  and the test project.
- Why sufficient: the dispatch tests exercise the full live path (kinah mutation + message fan-out) for the
  distribute, insufficient, below-two, and alliance-deferred cases; the decision math is additionally unit-covered
  in UOW-2568.

## Files Changed (UOW-2569)

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` — `HandleGroupDistributionAsync` +
  `SendToPlayerOrActiveAsync`; dispatch case wired.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs` — `GetOnlineMemberPlayers`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionGroupDistributionTests.cs` — new (5 cases).

## Migration Parity Table (UOW-2569)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_GROUP_DISTRIBUTION.runImpl` (partyType 1, group) | `HandleGroupDistributionAsync` | Handler | Complete | Unit Tested | Partial Parity | Group variant live; alliance/league + canTrade deferred |
| `TeamKinahDistributionEvent.handleEvent` | `HandleGroupDistributionAsync` + planner | Service | Complete | Unit Tested | Verified Parity | Split math + DEC_KINAH_BUY/INC_KINAH_COLLECT + messages |
| `TemporaryPlayerTeam.getOnlineMembers` | `PlayerGroupRuntime.GetOnlineMemberPlayers` | Service | Complete | Unit Tested | Verified Parity | Filters members by Player.IsOnline |
| `Storage.increaseKinah/decreaseKinah` (kinah) | in-memory CopyInventoryItem + update packets | Service | Partial | Unit Tested | Partial Parity | In-memory only (logout/periodic persistence) |

## Summary Metrics (conservative)

- New automated coverage: 5 multi-player dispatch tests (even/uneven split, insufficient, below-two, alliance-deferred).
- Live gameplay mechanics now include group kinah distribution (in addition to trading, player search).
- Overall Phase 6 completion estimate: incremental; one live gameplay handler added.

## Remaining Risks

- canTrade gate not modeled (transient-state omission). If a restriction system is later ported, add the gate.
- Alliance/league distribution deferred; partyType-1-in-alliance routes to alliance distribution in Java (no-op here).
- Kinah persistence is in-memory until logout/periodic save (shared with exchange kinah).
- The kinah update packets (DEC/INC) require an ItemTemplate; the focused harness loads none, so those packets
  are not asserted (the count mutation + system messages are). Template-gated emission is exercised in production.
- Carried: CM_QUEST_SHARE blocked on quest-template share metadata; legion-history live response blocked on a
  history data source; exchange + legion-rank DB reads unit-only (Needs Verification); XP/level-up not ported.

## Next Recommended UOW

1. **UOW-2570: Alliance kinah distribution (partyType 2 + partyType-1-in-alliance).**
   - Java: `PlayerAllianceService.distributeKinah` / `distributeKinahInGroup` (the latter restricts to the
     distributor's alliance sub-group). Reuse `GroupKinahDistributionPlanService` + the same kinah mutation;
     resolve online members via `PlayerAllianceRuntime.GetMemberPlayers` (add an online filter like the group one)
     and, for `distributeKinahInGroup`, `GetMemberObjectIdsByGroupId`.
   - **Verify first**: the Java `distributeKinahInGroup` member scope (alliance sub-group vs whole alliance) and
     whether `PlayerAllianceRuntime` exposes online members + the player's alliance group id.
   - Focused recipe: a dispatch test mirroring `GameServerConnectionGroupDistributionTests` with an alliance runtime.
2. **UOW-2570-alt: Port general quest-template share metadata** to unblock CM_QUEST_SHARE (larger XML unit).
3. **DB integration tests** for exchange + legion-rank reads — opt-in MySQL; **broad-validation trigger applies**.

Recommended next: **UOW-2570 (alliance distribution)** — completes the CM_GROUP_DISTRIBUTION feature using the
now-proven pattern.

### Focused validation recipe for UOW-2570

- Behavior: partyType 2 distributes the distributor's kinah across the alliance's online members (and
  partyType-1-in-alliance across the alliance sub-group) with the same DEC/INC + messages.
- Focused C# command: `dotnet test ... --filter "FullyQualifiedName~<GameServerConnectionAllianceDistributionTests>|FullyQualifiedName~GroupKinahDistributionPlanServiceTests"`.
- Java/Maven: not expected.
- Broad-validation trigger: none.

## Context Needed By Next Session

- Group kinah distribution is live via `HandleGroupDistributionAsync`; reuse it as the template for alliance/league.
- `GroupKinahDistributionPlanService.Plan(...)` is team-agnostic (works for any team's online member count).
- `PlayerGroupRuntime.GetOnlineMemberPlayers(teamId)` filters by `Player.IsOnline`; add the analogous method on
  `PlayerAllianceRuntime` for UOW-2570.
- Kinah mutation pattern: cube kinah item lookup (`ItemId == 182400001 && Location == 0`) → `CopyInventoryItem(count:)`
  → `ReplaceInventoryItemFor`; updates use `DecreaseKinahBuy` (distributor) / `IncreaseKinahCollect` (members).
- `SendToPlayerOrActiveAsync(objectId, packet)` fans a packet to any player (registry first, else active connection).
- Trading fully live; player search faction-prefixes names; SM_LEGION_HISTORY ported; legion rank loaded.
