# Phase 6 Session 2714 Completion

## UOW

[Phase 6] UOW-2714: Wire `CM_LEGION_WH_KINAH` authority denial.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live opcode 76 / CM_LEGION_WH_KINAH now dispatches into runtime C# code instead of no-oping.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> activePlayer.getLegionMember() -> LegionMember.hasRights(WH_WITHDRAWAL / WH_DEPOSIT) -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT.
- C# runtime artifact wired: GameServerConnection.ProcessPacketAsync, HandleLegionWarehouseKinahAsync, Player loaded legion permission masks, SmSystemMessage.GuildWarehouseNoRight.
- Client-visible/state/persistence effect: legion members without the relevant warehouse right receive the Java authority-denial system message; no Kinah or inventory state is mutated. No-legion players return without packets like Java.
- Why this is runtime progress: this wires a deferred live client packet path and sends a real Java-equivalent server packet from live dispatch code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
  - Reads `amount` as `readQ()` and `actionType` as `readC()`.
  - Returns silently when `activePlayer.getLegionMember()` is null.
  - Action `0` requires `LegionPermissionsMask.WH_WITHDRAWAL`; action `1` requires `WH_DEPOSIT`.
  - Missing rights sends `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT()`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
  - Brigade general always has rights; other ranks check the matching legion permission mask.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
  - `WH_WITHDRAWAL = 0x4`, `WH_DEPOSIT = 0x1000`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
  - Stores deputy, centurion, legionary, and volunteer permission masks.

## C# Changes

- `ProcessPacketAsync` now dispatches `CmLegionWarehouseKinah` to a live handler.
- `HandleLegionWarehouseKinahAsync` mirrors Java's no-legion silent return and sends `GuildWarehouseNoRight` when loaded rank/masks fail the required action permission.
- `Player` now carries legion permission masks used by the live handler.
- `PlayerEnterWorldRepository` now selects and populates the four Java legion permission mask columns.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava` | Unit / live packet dispatch | `CM_LEGION_WH_KINAH.runImpl`, `LegionMember.hasRights`, `LegionPermissionsMask` source review | Opcode 76 dispatch sends `STR_GUILD_WAREHOUSE_NO_RIGHT` for a legion volunteer lacking `WH_WITHDRAWAL` and does not mutate inventory. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded `SmSystemMessage` id `1300322`. | Exact Java runtime bytes were not captured. |
| `ProcessPacketAsync_LegionWarehouseKinahNoLegionReturnsWithoutPacketLikeJava` | Unit / live packet dispatch | `CM_LEGION_WH_KINAH.runImpl` source review | No-legion active player returns without packets, correcting the prior handoff assumption. | Live `ProcessPacketAsync` with empty packet assertion. | Successful legion warehouse Kinah mutation remains deferred. |

## Validation Decision

```text
- Changed surface: live CM_LEGION_WH_KINAH dispatch, legion permission facts on Player, player-entry legion mask loading.
- Specific behavior/contract: Java rights-denial system message for action 0/1 when LegionMember.hasRights fails; no-legion return remains silent.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ClientPacketFactory_ParsesLegionWarehouseKinahPacket|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahNoLegionReturnsWithoutPacketLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live packet denial branch plus existing player-entry SELECT columns.
- Broad .NET decision: skipped; the filtered command built the affected projects and validated packet parsing plus live dispatch behavior.
- Why this scope is sufficient: tests exercise the parsed client packet path through ProcessPacketAsync and assert the Java-derived packet/no-packet outcomes and no state mutation for the denial branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION_WH_KINAH` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | No-legion return and missing-right denial are wired. Successful withdraw/deposit mutation and history remain deferred. |
| `com.aionemu.gameserver.model.team.legion.LegionMember.hasRights` | `GameServerConnection.HasLegionWarehouseRight` / `Player` legion masks | Permission check | Partial | Unit Tested indirectly | Partial Parity | Warehouse permission bits and rank masks are represented for this packet path only. Broader legion permissions remain incomplete. |
| `com.aionemu.gameserver.model.team.legion.Legion` permission mask fields | `PlayerEnterWorldRepository.LoadPlayerAsync` / `Player` | Runtime data load | Partial | Built by focused test | Needs Verification | Query loads the masks from the existing `legions` columns; no database integration test was run in this UOW. |

## Known Gaps

- Successful legion warehouse Kinah withdrawal/deposit remains deferred because live C# still lacks legion warehouse storage mutation and `LegionService.addHistory` equivalents.
- Legion warehouse item move/split/replace paths remain incomplete.
- Exact Java wire bytes for the denial packet were not captured; the message id and packet class are source-reviewed and decoded in C#.
- Broader legion permission usage remains partial outside this packet path.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Add a minimal live legion warehouse Kinah storage runtime model only if storage owner, persistence, and history dependencies can be scoped safely.
2. Wire a small legion warehouse item move/split denial branch using the same loaded permission masks.
3. Add database integration coverage for player-entry legion permission mask loading if it is paired with the next runtime handler that consumes those masks.
