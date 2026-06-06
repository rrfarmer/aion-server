# Phase 6 Session 2777 Handoff

## Current State

The latest completed Unit of Work is `[Phase 6][UOW-2777] Load persisted legion roster for invite member list`.

Live legion invite acceptance now sends `SM_LEGION_MEMBERLIST` from persisted roster state:

- `LoadLegionMembersAsync` loads `legion_members` joined to `players`.
- `SendLegionInviteMemberListAsync` uses persisted rows as the base roster.
- Online same-legion players from the connection registry override persisted row values.
- The accepted player is excluded, matching Java `updateLegionMemberList(player, false, player.getObjectId())`.
- Offline persisted members are now visible in the accepted player's roster packet.

## Commit

Expected commit message:

```text
[Phase 6][UOW-2777] Load persisted legion invite roster
```

## Validation Evidence

Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepository" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 126 total, 0 failed, 0 skipped.

`git diff --check` passed with line-ending warnings only.

Focused Java/Maven validation was not run because Java source was unchanged and no narrow Java unit fixture exists for this live invite roster path.

The opt-in MySQL integration test was compiled but not executed against a database because `AION_GAMESERVER_DB_INTEGRATION=1` was not set.

## Latest Runtime Progress Gate

- Deferred/live behavior advanced: accepted player's legion member-list packet previously missed offline persisted members.
- Java source of truth: `LegionService.updateLegionMemberList`, `Legion.getMembers`, `LegionMemberDAO.loadLegionMembers`, `LegionMemberDAO.loadLegionMember`, `PlayerDAO.loadPlayerCommonData`, and `SM_LEGION_MEMBERLIST.writeImpl`.
- C# runtime artifact wired/fixed: `IPlayerEnterWorldRepository.LoadLegionMembersAsync`, `MySqlPlayerEnterWorldRepository.LoadLegionMembersAsync`, `GameServerConnection.SendLegionInviteMemberListAsync`, and focused tests.
- Client-visible/state/persistence effect changed: accepted clients now receive persisted offline roster rows in the live member-list packet.
- Why this was not preview-only/test-only/documentation-only: the live handler now reads runtime DB state and sends real server packets with those rows.

## Conservative Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
| --- | --- | --- | --- | --- |
| `LegionService.updateLegionMemberList` | `GameServerConnection.SendLegionInviteMemberListAsync` | Partial parity | Live handler test asserts persisted offline row, accepted-player exclusion, and online overlay. | No 80-row chunking; house fields remain zero. |
| `LegionMemberDAO.loadLegionMembers/loadLegionMember` | `MySqlPlayerEnterWorldRepository.LoadLegionMembersAsync` | Partial parity | Opt-in DB integration test added and compiled. | DB test not run in this session; no exact Java runtime comparison. |
| `SM_LEGION_MEMBERLIST` | `SmLegionMemberList` | Partial parity | Packet serialization asserted through live handler. | Chunking and housing lookup gaps remain. |

## Remaining Runtime Gaps

- Invite acceptance member-list chunking is not Java-complete. Java chunks at 80 rows; C# still sends one packet.
- Roster rows still use zero house address and door-state ids.
- C# has no full Java-equivalent in-memory `Legion` aggregate cache with `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Invite eligibility still has broader Java gaps such as other-faction invite config and `DeniedStatus.GUILD` behavior.

## Next Runtime UOW Recommendation

[Phase 6][Next] Chunk legion invite member-list packets at Java size 80

Runtime Progress Gate:

- What deferred/live behavior is being advanced? C# now sends persisted roster rows but still sends them as one `SM_LEGION_MEMBERLIST`; Java splits roster sends into `FixedElementCountSplitList<>(allMembers, true, 80)`.
- What Java source method or runtime path is the source of truth? `LegionService.updateLegionMemberList` and `SM_LEGION_MEMBERLIST.writeImpl`.
- What C# runtime artifact will be wired or fixed? `GameServerConnection.SendLegionInviteMemberListAsync` packet fanout and focused invite acceptance tests.
- What client-visible, state, persistence, packet, handler, scheduler, or runtime-loading effect will change? Accepted clients with more than 80 visible roster rows will receive multiple Java-shaped member-list packets with correct first/last flags and signed counts.
- Why is this not preview-only/test-only/documentation-only? It changes live server packet fanout from invite acceptance.

Suggested C# artifacts:

- `GameServerConnection.SendLegionInviteMemberListAsync`
- `CmLegionTests`
- Existing `SmLegionMemberList` packet assertions

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionMemberList" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture for `FixedElementCountSplitList` packet chunks is created. Broad-validation trigger: none unless chunking changes shared packet serialization helpers.

## Safe Runtime Candidates

- Wire active-house address and door-state lookup into `SM_LEGION_MEMBERLIST` rows if a C# housing runtime lookup already exists and can be used by live packet code.
- Add Java-equivalent invite eligibility for other-faction invite config and guild denial status in the live invite request path.
- Investigate Java `legion.addBonus()` and wire the smallest observable live player-state effect if the relevant C# stats/bonus runtime exists.
