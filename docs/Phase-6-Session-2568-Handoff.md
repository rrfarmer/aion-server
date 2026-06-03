# Phase 6 Session 2568 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2568: Port group/team kinah-distribution decision logic + SM_SYSTEM_MESSAGE split factories

## Session Summary

| UOW | Summary |
|-----|---------|
| 2564 | Partial-stack exchange trade splits. |
| 2565 | Faction-prefixed names in player search. |
| 2566 | SM_LEGION_HISTORY server packet (golden-tested). |
| 2567 | Legion member rank loaded at enter-world (Player.IsBrigadeGeneral). |
| 2568 | Pure `GroupKinahDistributionPlanService` mirroring `TeamKinahDistributionEvent` + `SmSystemMessage.MsgSplitMeToB`/`MsgSplitBToMe` factories. 10 unit tests. Handler documents the exact remaining live-wiring step. |

## Why this scope (Work Discovery)

CM_QUEST_SHARE (the prior handoff's #1) is **blocked**: it needs a general quest-template holder with
`isCannotShare`/`getTarget()`, which the C# port does not have (only `NearbyQuestTemplateTable` + reward
services). Porting that is a large quest-XML-extraction unit. Pivoted to a fully-grounded gameplay mechanic
whose dependencies exist: **group kinah distribution** (CM_GROUP_DISTRIBUTION → `TeamKinahDistributionEvent`).
Group/alliance/league runtimes, inventory kinah, and SM_SYSTEM_MESSAGE are all present.

## What changed (UOW-2568)

Java source of truth:
- `network/aion/clientpackets/CM_GROUP_DISTRIBUTION.runImpl`: `amount < 2` guard, `PlayerRestrictions.canTrade`,
  then `partyType` switch (1 = group or alliance-in-group, 2 = alliance, 3 = league) → `*.distributeKinah`.
- `model/team/common/events/TeamKinahDistributionEvent`:
  - `checkCondition()` = `team.hasMember(distributor)`.
  - `handleEvent()`: if `getKinah() < amount` → `STR_NOT_ENOUGH_MONEY`, return; else if
    `onlineMembers.size() > 1 && amount >= onlineMembers.size()`: `rewardPerPlayer = amount / size`
    (truncating long division), `tryDecreaseKinah(amount)`, each member `increaseKinah(rewardPerPlayer)`,
    distributor gets `STR_MSG_SPLIT_ME_TO_B(amount, size, reward)`, others get
    `STR_MSG_SPLIT_B_TO_ME(distributorName, amount, size, reward)`.
- Message ids: `STR_MSG_SPLIT_ME_TO_B = 1390247` (long, int, long); `STR_MSG_SPLIT_B_TO_ME = 1390248`
  (String, long, int, long); `STR_NOT_ENOUGH_MONEY = 1300388` (already ported). Confirmed SM_SYSTEM_MESSAGE
  writes each param via `param.toString()` (writeS) — the C# all-strings encoding is byte-faithful.

C# additions:
- `GroupKinahDistributionPlanService.Plan(amount, distributorKinah, onlineMemberCount, isTeamMember)` →
  `GroupKinahDistributionPlan(Outcome, Amount, OnlineMemberCount, RewardPerPlayer)` with outcomes
  `Ignored` (not a member), `NotEnoughMoney`, `NoDistribution` (≤1 online or amount < count), `Distribute`.
- `SmSystemMessage.MsgSplitMeToB(long, int, long)` (1390247) and `MsgSplitBToMe(string, long, int, long)` (1390248).
- `GameServerConnection` CM_GROUP_DISTRIBUTION handler comment now points at the planner + factories and spells
  out the live-wiring step (multi-player kinah mutation + per-member messages). Behavior unchanged (still no-op).

### Parity scope / boundary (documented, zero-divergence)

This UOW ports the **decision logic + messages** as a tested planner; it does **not** wire the live handler,
because distribution mutates multiple players' kinah state (multi-player persistence). The live path is
unchanged (still a no-op), so no behavioral divergence is introduced. This mirrors the codebase's established
planner/dispatch separation. The `amount < 2` and `canTrade` guards are handler-level (Java) and are documented
as upstream of the planner.

## Validation Decision (UOW-2568)

- Changed surface: production-code (new pure planner + two SM_SYSTEM_MESSAGE factories + a comment-only handler edit + new tests).
- Specific behavior/contract: the TeamKinahDistributionEvent decision (member guard, insufficient-kinah,
  ≤1-online / amount<count no-op, truncating even/uneven split, boundary `amount == count`, `kinah == amount`
  strict guard) and the two split message ids + ordered parameters.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GroupKinahDistributionPlanServiceTests"` → 10/10 passed.
- Focused Java/Maven command: none. No isolated Maven test targets `TeamKinahDistributionEvent`; parity is from
  direct event review, with the split math, guards, and message ids asserted in the unit tests.
- Broad-validation trigger: none.
- Broad .NET decision: skipped. The filtered test built `Aion.GameServer` (the message factories + edited
  handler comment) and the test project. The planner is pure and additive.
- Why sufficient: every branch of the Java event is covered by a unit test, and the message factories are
  asserted for id + parameter order/content. No live mutation was introduced, so there is no wider blast radius.

## Files Changed (UOW-2568)

- `dotnetConversion/src/Aion.GameServer/Services/GroupKinahDistributionPlanService.cs` — new planner.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs` — two split factories.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` — handler comment only.
- `dotnetConversion/tests/Aion.GameServer.Tests/GroupKinahDistributionPlanServiceTests.cs` — new tests (10 cases).

## Migration Parity Table (UOW-2568)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `TeamKinahDistributionEvent.handleEvent/checkCondition` | `GroupKinahDistributionPlanService` | Service | Partial | Unit Tested | Verified Parity (decision) | Decision logic verified; live multi-player kinah mutation not wired |
| `SM_SYSTEM_MESSAGE.STR_MSG_SPLIT_ME_TO_B` | `SmSystemMessage.MsgSplitMeToB` | Packet factory | Complete | Unit Tested | Verified Parity | id 1390247, params (amount, people, reward) |
| `SM_SYSTEM_MESSAGE.STR_MSG_SPLIT_B_TO_ME` | `SmSystemMessage.MsgSplitBToMe` | Packet factory | Complete | Unit Tested | Verified Parity | id 1390248, params (name, amount, people, reward) |
| `CM_GROUP_DISTRIBUTION.runImpl` | GameServerConnection `case CmGroupDistribution` | Handler | Partial | No Tests | Partial Parity | No-op; planner+messages ready; multi-player mutation deferred |

## Summary Metrics (conservative)

- New automated coverage: 10 unit cases (8 decision branches + 2 message factories).
- Overall Phase 6 completion estimate: unchanged (decision logic + messages ported; live path not yet wired).

## Remaining Risks

- Group kinah distribution is not live: the handler is still a no-op. Wiring requires multi-player kinah mutation
  + persistence (same in-memory kinah model as exchange; logout/periodic save).
- Alliance/league variants (`PlayerAllianceService.distributeKinah(InGroup)`, `LeagueService.distributeKinah`)
  reuse the same event but resolve a different team; only the group decision is unit-covered (the math is shared).
- Carried: CM_QUEST_SHARE blocked on quest-template share metadata; legion-history live response blocked on a
  history data source; exchange + legion-rank DB reads unit-only (Needs Verification); XP/level-up not ported.

## Next Recommended UOW

1. **UOW-2569: Wire CM_GROUP_DISTRIBUTION live (group variant, partyType 1 non-alliance).**
   - Use `GroupKinahDistributionPlanService` + `PlayerGroupRuntime.GetMemberPlayers(teamId)` (online member
     Players) and the connection registry to fan out messages. Reuse the exchange in-memory kinah mutation
     pattern (cube kinah item lookup → `CopyInventoryItem(count:)` → `ReplaceInventoryItemFor` → kinah update
     packet) for the distributor decrease and each member increase.
   - **Verify first**: how `PlayerGroupRuntime` reports *online* members (vs all members), and the canonical
     kinah-mutation helper used by exchange (UOW-2554/2563) to avoid divergence.
   - Risks: multi-player state mutation + persistence; ensure each member's kinah row is updated consistently
     (in-memory; logout/periodic save) and the `tryDecreaseKinah` atomicity (distributor must not pay if the
     decrease fails) is preserved.
2. **UOW-2569-alt: Port general quest-template share metadata** (`isCannotShare`, `QuestTarget`) to unblock
   CM_QUEST_SHARE — larger XML-extraction unit; discovery-heavy.
3. **DB integration tests** for exchange + legion-rank reads — opt-in MySQL; **broad-validation trigger applies**.

Recommended next: **UOW-2569 (wire group distribution live)** — the planner + messages are ready, so this is the
natural completion and delivers a live gameplay mechanic.

### Focused validation recipe for UOW-2569

- Behavior: a valid group distribution decreases the distributor's cube kinah by `amount`, increases each online
  member by `RewardPerPlayer`, sends MsgSplitMeToB to the distributor and MsgSplitBToMe to each other member;
  insufficient kinah sends only NotEnoughMoney; ≤1 online / amount<count sends nothing.
- Focused C# command: `dotnet test ... --filter "FullyQualifiedName~GroupKinahDistributionPlanServiceTests|FullyQualifiedName~<GameServerConnectionGroupDistributionTests>"` (reuse a TestConnectionPair + CapturingConnectionRegistry harness like GameServerConnectionExchangeTradeTests).
- Java/Maven: not expected.
- Broad-validation trigger: none (multi-player in-memory mutation; start focused on the dispatch test).

## Context Needed By Next Session

- `GroupKinahDistributionPlanService.Plan(amount, distributorKinah, onlineMemberCount, isTeamMember)` returns the
  Java `TeamKinahDistributionEvent` decision; `RewardPerPlayer = amount / onlineMemberCount` (truncating).
- `SmSystemMessage.MsgSplitMeToB(amount, people, reward)` (1390247) and `MsgSplitBToMe(name, amount, people, reward)` (1390248); `NotEnoughMoney()` (1300388).
- SM_SYSTEM_MESSAGE encodes all params as `toString()` strings in order — pass numbers stringified (InvariantCulture), matching the existing dice/search message factories.
- `PlayerGroupRuntime.GetMemberPlayers(teamId)` returns member Player objects; the connection registry
  (`SendPacketToPlayerAsync`) fans out to members. Reuse the exchange kinah-mutation pattern.
- Trading fully live; player search faction-prefixes names; SM_LEGION_HISTORY ported; legion rank loaded.
