# Phase 6 Session 2566 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2566: Port the SM_LEGION_HISTORY server packet (byte-exact, golden-tested)

## Session Summary

| UOW | Summary |
|-----|---------|
| 2564 | Partial-stack exchange trade splits. |
| 2565 | Faction-prefixed names in player search. |
| 2566 | Ported `SmLegionHistory` (opcode 12) with a golden byte-layout test (5 cases). Live CM_LEGION_HISTORY wiring stays deferred (no legion-history data source + no legion rank); handler now no-ops, which matches Java for the no-legion case the port can represent. |

## What changed (UOW-2566)

Java source of truth:
- `network/aion/serverpackets/SM_LEGION_HISTORY.java` (`writeImpl`, `findEntriesForCurrentPage`, `ENTRIES_PER_PAGE = 8`).
- `network/aion/clientpackets/CM_LEGION_HISTORY.java` (`runImpl` guards).
- `network/aion/ServerPacketsOpcodes` line 30: `addPacketOpcode(12, SM_LEGION_HISTORY.class)`.
- `model/team/legion/LegionHistoryEntry` (`int id, int epochSeconds, LegionHistoryAction action, String name, String description`).
- `model/team/legion/LegionHistoryAction.Type` enum: `LEGION(0), REWARD(1), WAREHOUSE(2)`.
- `AionServerPacket.writeS(text, fixedLength)` + `byteLengthForFixedString(n) = (n + 1) * 2` (verified: 32-char fields write 66 bytes; the empty-string branch writes the same 66 zero bytes, so the `WriteFixedS` loop+terminator pattern is byte-exact for both).

C# additions:
- New `SmLegionHistory : GameServerPacket` (opcode 12) + `LegionHistoryEntryRow(int EpochSeconds, byte ActionId, string Name, string Description)`.
  - `writeImpl`: `WriteD(totalEntries)`, `WriteD(page)`, `WriteD(pageEntries.Count)`, per entry `WriteD(epoch)`,
    `WriteC(actionId)`, `WriteC(0)`, `WriteFixedS(name,32)`, `WriteFixedS(description,32)`, `WriteH(0)`; trailing `WriteH(typeOrdinal)`.
  - `FindEntriesForCurrentPage` mirrors Java page slicing (8/page; out-of-range page → empty list; `totalEntries` always full size).
- `GameServerConnection` CM_LEGION_HISTORY handler comment updated to reference the now-ported packet and record the
  exact blockers for live wiring. Behavior unchanged (still no-op).

### Parity scope / intentional boundary (documented, conservative)

The handler is **not** wired to send a live response this UOW. Java `runImpl` either (a) returns silently when
`player.getLegion() == null`, or (b) sends the legion's actual history (with a `REWARD`-type brigade-general
guard). The C# port has **no legion-history data source** (`Legion.getHistory`) and **does not model legion
member rank**, so neither the populated response nor the REWARD/brigade-general guard can be reproduced
faithfully. Sending an "empty" history would be an invented behavior with an un-portable guard, so it was not
done. The current no-op exactly matches Java for the no-legion case — the only case the port can currently
represent. This is a deferral, not an intentional behavioral difference; the packet is ready for wiring once a
legion-history model + legion rank are ported.

## Validation Decision (UOW-2566)

- Changed surface: production-code (one new server packet + a comment-only handler edit + a new test class).
- Specific behavior/contract: SM_LEGION_HISTORY byte layout — header (total/page/count), per-entry fields incl.
  two fixed 32-char UTF-16 strings + terminator, page slicing at 8/page, out-of-range page yields an empty page
  while total reports the full size, trailing type ordinal; opcode = 12.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmLegionHistoryTests"` → 5/5 passed (re-run after the handler comment edit for the post-edit compile signal).
- Focused Java/Maven command: none. No isolated Maven test exists for `SM_LEGION_HISTORY.writeImpl`; parity was
  established by direct review of `writeImpl` + `findEntriesForCurrentPage` + `writeS`/`byteLengthForFixedString`,
  with the byte layout asserted in the golden test.
- Broad-validation trigger: none.
- Broad .NET decision: skipped. The filtered test built `Aion.GameServer` (where the new packet + edited handler
  live) and the test project; the packet is additive and self-contained.
- Why sufficient: the golden test asserts the exact wire bytes for empty, single-entry, multi-page, and
  page-beyond-end cases, plus the opcode — the full Java-derived contract for this packet.

## Files Changed (UOW-2566)

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionHistory.cs` — new packet + row record.
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` — handler comment only.
- `dotnetConversion/tests/Aion.GameServer.Tests/SmLegionHistoryTests.cs` — new golden test (5 cases).

## Migration Parity Table (UOW-2566)

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `SM_LEGION_HISTORY` | `SmLegionHistory` | Packet | Complete | Golden File Tested | Verified Parity | Byte-exact writeImpl + paging; opcode 12; 5 golden cases |
| `LegionHistoryEntry` (wire fields) | `LegionHistoryEntryRow` | DTO | Partial | Golden File Tested | Partial Parity | Only the 4 wire fields (epoch/actionId/name/description); `id` not on the wire |
| `LegionHistoryAction.Type` (ordinals) | `typeOrdinal` int param | Enum | N/A | Golden File Tested | Verified Parity | LEGION=0/REWARD=1/WAREHOUSE=2 written as trailing H |
| `CM_LEGION_HISTORY.runImpl` | GameServerConnection `case CmLegionHistory` | Handler | Partial | No Tests | Partial Parity | No-op matches Java no-legion case; populated/REWARD path blocked on legion model + rank |

## Summary Metrics (conservative)

- New automated coverage this UOW: 5 golden-test cases for SM_LEGION_HISTORY.
- Packet goldens continue to grow (Phase 6 acceptance criterion).
- Overall Phase 6 completion estimate: unchanged (a packet building block; no new live gameplay path opened).

## Remaining Risks

- CM_LEGION_HISTORY produces no live response for legion members (potential client-side UI wait) until the
  legion-history data model + legion rank are ported. The packet is ready; the data layer is the blocker.
- Legion member rank (`legion_members.rank` / brigade-general) is not loaded or modeled in the C# port —
  blocks the REWARD-type guard and any other rank-gated legion behavior.
- Carried from prior UOWs: exchange persistence SQL unit-only (Needs Verification); kinah trade not immediately
  persisted; player-search NAME_TAGS base unmodeled; XP/level-up not ported.

## Next Recommended UOW

### Work Discovery already done this session (read before choosing)

- **CM_QUEST_SHARE is BLOCKED** as a full live port. Its `runImpl` needs
  `DataManager.QUEST_DATA.getQuestById(questId).isCannotShare()` and `.getTarget() == ALLIANCE`, but the C# port
  has **no general quest-template holder** with those fields — only `NearbyQuestTemplateTable` (a narrow subset)
  and quest-reward services. So the cannot-share (1100001) guard and the ALLIANCE/group message split
  (1100005 vs 1100000) cannot be reproduced faithfully. Also, the handoff's old fallback ("port SM_QUEST_ACTION")
  is **already done** — `SmQuestAction.cs` exists. Do not re-scope to that.
- Per-player quest state IS modeled (`PlayerQuestState` with `Status`/`IsComplete`); group membership IS modeled
  (`PlayerGroupRuntime`). The single missing dependency is the general quest template's share metadata.

### Recommended next: **UOW-2567 — Legion member rank loading** (smallest self-contained data-layer unit)

This is the cleanest unblocking unit and furthers gameplay parity. Read `legion_members.rank` at enter-world
into the Player model (e.g. `Player.LegionRank` + an `IsBrigadeGeneral` helper) so rank-gated legion behavior
(incl. the SM_LEGION_HISTORY REWARD guard ported this session) becomes reachable.

- Java source of truth: `model/team/legion/LegionMember` rank enum (e.g. `BRIGADE_GENERAL`), and the
  `legion_members` table load (`dao/LegionMemberDAO`).
- C# artifacts: `PlayerEnterWorldRepository` enter-world SELECT (already LEFT JOINs `legion_members lm`; add the
  rank column), `Player.cs`.
- **Verify first**: the exact `legion_members` rank column name in the live schema and the Java
  `LegionMemberRank` ordinal/name mapping.
- Focused recipe: filtered repository/enter-world test asserting the rank maps correctly
  (`--filter "FullyQualifiedName~PlayerEnterWorldServiceTests"` + any new rank-mapping test class). Java/Maven:
  not expected unless the schema/DAO is touched. Broad-validation trigger: none (in-memory mapping;
  the DB read itself is opt-in integration territory only if asserted against MySQL).

### Other safe candidates

- **Port the general quest-template share metadata** (`isCannotShare`, `QuestTarget`) into the quest data holder,
  then wire CM_QUEST_SHARE. Larger (touches XML extraction + static data); discovery-heavy.
- **DB integration test for exchange persistence** — opt-in MySQL; **broad-validation trigger applies**. Raises
  UOW-2564 SQL (`TransferItemOwnershipAsync`, trade-time `SaveItemSplitMutationAsync`) to Verified.

## Context Needed By Next Session

- `SmLegionHistory` (opcode 12) is ported and golden-tested; constructor takes `(IReadOnlyList<LegionHistoryEntryRow> history, int page, int typeOrdinal)` and handles paging (8/page) internally. Type ordinals: LEGION=0, REWARD=1, WAREHOUSE=2.
- Fixed UTF-16 strings on the wire use the `WriteFixedS(buffer, value, fixedLength)` loop + `WriteH(0)` terminator pattern = Java `writeS(text, fixedLength)` byte-exact (= `(fixedLength+1)*2` bytes).
- Legion member rank is not modeled; `legion_members` is joined at enter-world but only legion id/level/name/emblem are read (not rank). `Player.LegionId` is populated from `legion_id`.
- The 2565 handoff and earlier remain accurate; trading is fully live; player search faction-prefixes names for staff.
