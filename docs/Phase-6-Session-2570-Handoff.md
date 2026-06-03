# Phase 6 Session 2570 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2570: Alliance kinah distribution (partyType 2 whole-alliance + partyType-1-in-alliance sub-group)

## Session Summary

| UOW | Summary |
|-----|---------|
| 2564 | Partial-stack exchange trade splits. |
| 2565 | Faction-prefixed names in player search. |
| 2566 | SM_LEGION_HISTORY server packet (golden-tested). |
| 2567 | Legion member rank loaded at enter-world. |
| 2568 | Group kinah-distribution decision planner + split messages. |
| 2569 | Live group kinah distribution (partyType 1, non-alliance). |
| 2570 | Live alliance kinah distribution: partyType 2 (whole alliance) and partyType-1-in-alliance (sub-group). CM_GROUP_DISTRIBUTION now complete except league (partyType 3). 4 alliance dispatch tests. |

## MILESTONE: CM_GROUP_DISTRIBUTION is complete except the league variant

| partyType | Java route | Status |
|-----------|------------|--------|
| 1 (not in alliance) | `PlayerGroupService.distributeKinah` | Live (UOW-2569) |
| 1 (in alliance) | `PlayerAllianceService.distributeKinahInGroup` (sub-group) | Live (UOW-2570) |
| 2 | `PlayerAllianceService.distributeKinah` (whole alliance) | Live (UOW-2570) |
| 3 | `LeagueService.distributeKinah` | Deferred (no League team modeled) |

## What changed (UOW-2570)

Java source of truth:
- `PlayerAllianceService.distributeKinah` → `TeamKinahDistributionEvent(alliance, ...)` (whole alliance).
- `PlayerAllianceService.distributeKinahInGroup` → `TeamKinahDistributionEvent(allianceGroup, ...)` (the
  distributor's `PlayerAllianceGroup` only). Both reuse the same event ported in UOW-2568.

C# changes:
- `HandleGroupDistributionAsync` now resolves the online-member set per partyType + membership:
  - partyType 1 + Group → group online members (UOW-2569).
  - partyType 1 + Alliance → the distributor's alliance sub-group online members
    (`GetMemberAllianceGroupId` + `GetOnlineMemberPlayersByGroupId`).
  - partyType 2 + Alliance → whole-alliance online members (`GetOnlineMemberPlayers`).
  - partyType 3 → not handled (League not modeled).
  The shared distribution body (planner decision + DEC_KINAH_BUY/INC_KINAH_COLLECT mutation + ME_TO_B/B_TO_ME
  messages) is unchanged and team-agnostic.
- `PlayerAllianceRuntime`: added `GetOnlineMemberPlayers(allianceId)`, `GetOnlineMemberPlayersByGroupId(allianceId, groupId)`, `GetMemberAllianceGroupId(allianceId, objectId)`.

### Documented gaps (carried + new)

- League distribution (partyType 3) deferred — no `League` team type is modeled.
- canTrade gate still not modeled (transient-state omission).
- Kinah persistence remains in-memory (logout/periodic save).

## Validation Decision (UOW-2570)

- Changed surface: production-code with live multi-player state mutation (alliance kinah) + three read-only runtime accessors + tests.
- Specific behavior/contract: whole-alliance even split moves kinah across all online alliance members; sub-group
  (partyType 1 in alliance) distributes within the distributor's alliance group; offline members are excluded;
  a non-member (None) does nothing via the alliance path.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAllianceDistributionTests"` → 4/4; with group + planner adjacency → 19/19.
- Focused Java/Maven command: none. The alliance methods reuse `TeamKinahDistributionEvent` (already reviewed);
  parity asserted via the dispatch tests.
- Broad-validation trigger: none. The change reuses the UOW-2569 distribution body; only the team-resolution
  switch and three read-only alliance accessors are new.
- Broad .NET decision: skipped. The filtered tests built `Aion.GameServer` and the test project.
- Why sufficient: the alliance dispatch tests cover whole-alliance, sub-group, offline-exclusion, and
  non-member cases; the split math is unit-covered (UOW-2568) and the mutation body is shared with the
  group path (UOW-2569 tests).

## Files Changed (UOW-2570)

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` — partyType/membership team resolution in `HandleGroupDistributionAsync`.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs` — 3 accessors.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAllianceDistributionTests.cs` — new (4 cases).

## Migration Parity Table (UOW-2570)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerAllianceService.distributeKinah` | `HandleGroupDistributionAsync` (partyType 2) | Handler | Complete | Unit Tested | Partial Parity | Live; kinah persistence in-memory |
| `PlayerAllianceService.distributeKinahInGroup` | `HandleGroupDistributionAsync` (partyType 1, alliance) | Handler | Complete | Unit Tested | Partial Parity | Sub-group via AllianceGroupId; in-memory persistence |
| `PlayerAlliance.getOnlineMembers` | `PlayerAllianceRuntime.GetOnlineMemberPlayers` | Service | Complete | Unit Tested | Verified Parity | Filters by Player.IsOnline |
| `PlayerAllianceGroup.getOnlineMembers` | `GetOnlineMemberPlayersByGroupId` | Service | Complete | Unit Tested | Verified Parity | Filters by IsOnline + AllianceGroupId |

## Summary Metrics (conservative)

- New automated coverage: 4 alliance dispatch tests (whole-alliance, sub-group, offline-exclusion, non-member).
- CM_GROUP_DISTRIBUTION is now live for group + alliance (all but league).
- Overall Phase 6 completion estimate: incremental; the group-distribution feature is essentially complete.

## Remaining Risks

- League distribution (partyType 3) deferred (no League team modeled).
- canTrade gate not modeled.
- Kinah persistence in-memory until logout/periodic save (shared with exchange + group distribution).
- Kinah update packets (DEC/INC) are template-gated and not asserted in the harness (count mutation + messages are).
- Carried: CM_QUEST_SHARE blocked on quest-template share metadata; legion-history live response blocked on a
  history data source; exchange + legion-rank DB reads unit-only (Needs Verification); XP/level-up not ported.

## Next Recommended UOW

1. **UOW-2571: Port general quest-template share metadata (`isCannotShare`, `QuestTarget`) → wire CM_QUEST_SHARE.**
   - The remaining quest-share dependency. Quest state, group runtime, and SM_QUEST_ACTION are already modeled.
   - **Verify first**: whether a general quest-template XML loader exists or must be added (only
     `NearbyQuestTemplateTable` + reward services exist today). If the full loader is absent, scope to adding the
     two share fields to whatever quest holder is reachable, or pick item 2.
2. **UOW-2571-alt: A self-contained gameplay handler whose deps are ported.** Candidates with satisfied deps:
   `CM_DELETE_QUEST` (quest abandon — PlayerQuestState modeled) or another deferred handler; do Work Discovery
   on the GameServerConnection deferred-case list (search `deferred`) and pick one whose service deps exist.
3. **DB integration tests** for exchange + legion-rank reads — opt-in MySQL; **broad-validation trigger applies**.

Recommended next: **UOW-2571 quest-share** if a quest-template loader is reachable; otherwise a deferred-handler
port with satisfied dependencies (Work Discovery on the `deferred` switch cases).

### Focused validation recipe for UOW-2571

- If quest-share — Behavior: CM_QUEST_SHARE emits SM_SYSTEM_MESSAGE 1100001 (cannot share), 1100000/1100005
  (no group/alliance members), and SM_QUEST_ACTION for a valid in-range group member.
  - Focused C# command: `dotnet test ... --filter "FullyQualifiedName~<CmQuestShareTests>"`.
- Java/Maven: not expected unless quest XML parsing is touched.
- Broad-validation trigger: none.

## Context Needed By Next Session

- CM_GROUP_DISTRIBUTION is live for group + alliance (group, alliance-whole, alliance-sub-group); only league
  (partyType 3) is deferred. The shared body is `HandleGroupDistributionAsync`.
- `PlayerAllianceRuntime` now exposes `GetOnlineMemberPlayers`, `GetOnlineMemberPlayersByGroupId`,
  `GetMemberAllianceGroupId`. The group runtime has `GetOnlineMemberPlayers`.
- Alliance members have `Player.CurrentTeamId = AllianceId`, `TeamMembership = Alliance`; alliance sub-groups are
  ids 1000..1003.
- Kinah mutation: cube kinah item (`ItemId == 182400001 && Location == 0`) → `CopyInventoryItem(count:)` →
  `ReplaceInventoryItemFor`; `DecreaseKinahBuy` (distributor) / `IncreaseKinahCollect` (members);
  `SendToPlayerOrActiveAsync` fans out.
- Trading fully live; player search faction-prefixes names; SM_LEGION_HISTORY ported; legion rank loaded.
