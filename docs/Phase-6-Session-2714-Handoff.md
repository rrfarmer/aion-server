# Phase 6 Session 2714 Handoff

## Completed UOW

[Phase 6] UOW-2714: Wire `CM_LEGION_WH_KINAH` authority denial.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: opcode 76 / CM_LEGION_WH_KINAH now dispatches into C# runtime logic instead of no-oping.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> activePlayer.getLegionMember() -> LegionMember.hasRights(WH_WITHDRAWAL / WH_DEPOSIT) -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT.
- C# runtime artifact wired: GameServerConnection.ProcessPacketAsync, HandleLegionWarehouseKinahAsync, Player legion permission masks, PlayerEnterWorldRepository legion mask loading.
- Client-visible/state/persistence effect: players in a legion without the required warehouse permission receive the Java authority-denial system message; no Kinah state mutates. Players without a legion return silently like Java.
- Why this is runtime progress: it wires a deferred live client packet path and sends a real Java-equivalent server packet from live code.
```

## Commit

`[Phase 6][UOW-2714] Send legion warehouse Kinah denial`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2714-Completion.md`
- `docs/Phase-6-Session-2714-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HasLegionWarehouseRight`
- `Aion.GameServer.Model.GameObjects.Player`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ClientPacketFactory_ParsesLegionWarehouseKinahPacket|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahNoLegionReturnsWithoutPacketLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0
- Existing warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- `CM_LEGION_WH_KINAH` has partial runtime parity for no-legion silent return and missing-right denial.
- Full legion warehouse Kinah parity is not claimed. Successful withdraw/deposit mutation, legion warehouse persistence, and history fanout remain deferred.
- The previous handoff's "no-legion denial" candidate was corrected after Java source review: Java returns without sending a packet when `getLegionMember()` is null.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Live dispatch sends `STR_GUILD_WAREHOUSE_NO_RIGHT` for failed warehouse rights and returns silently without legion membership. |
| `LegionMember.hasRights` | `HasLegionWarehouseRight` / `Player` masks | Permission check | Partial | Unit Tested indirectly | Partial Parity | Brigade-general and rank-mask checks are implemented for `WH_WITHDRAWAL` and `WH_DEPOSIT` only. |
| `Legion` permission fields | `PlayerEnterWorldRepository.LoadPlayerAsync` | Runtime data load | Partial | Built by focused test | Needs Verification | The player-entry SELECT now includes permission masks; database integration coverage was not run. |

## Known Gaps / Watchouts

- Legion warehouse Kinah successful withdraw/deposit still no-ops after the permission check because C# lacks live legion warehouse storage mutation and legion history write/fanout.
- Legion warehouse item move/split/replace paths still need permission checks, storage owner mapping, persistence, and packets.
- Exact Java wire bytes for `STR_GUILD_WAREHOUSE_NO_RIGHT` were not captured.
- Database integration coverage for the newly selected permission columns remains a useful follow-up only when paired with live behavior that consumes those fields.

## Next Recommended Runtime UOW

Recommended candidate: wire the smallest live legion warehouse item or Kinah denial branch that consumes the loaded permission masks.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: a parsed legion warehouse item/kinah action should send Java's authority-denial packet for loaded rank/mask failures instead of silently no-oping.
- Java source/runtime path: ItemRestrictionService / CM_LEGION_WH_KINAH / legion warehouse move packet path -> LegionMember.hasRights -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT.
- C# runtime artifact likely involved: GameServerConnection warehouse move/split/replace handler, Player legion permission masks, SmSystemMessage.GuildWarehouseNoRight.
- Client-visible/state/persistence effect expected: client receives a real system message denial from live dispatch; no inventory or warehouse state mutates.
- Why this is runtime progress: it wires a deferred live client packet path and sends a real Java-equivalent server packet from live code.
```

Suggested focused validation starting point:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehouse|FullyQualifiedName~GuildWarehouseNoRight" --logger "console;verbosity=minimal"
```

Only refine that filter after selecting the exact packet path. Java/Maven is not expected unless a narrow Java fixture exists or Java source changes. Broad-validation trigger: none for another isolated denial branch.

## Other Safe Runtime Candidates

- Scope a minimal live legion warehouse Kinah success path only after identifying C# storage list ownership, persistence, server packets, and legion history dependencies.
- Add database integration coverage for legion permission mask loading if paired with a runtime handler that uses the loaded masks.
- Inspect regular/account warehouse replace or split behavior and proceed only if a runtime mismatch remains.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `255364fdd [Phase 6][UOW-2713] Create missing Kinah move targets`
  - `f65ce73b0 [Phase 6][UOW-2712] Add moved items to regular warehouse`
  - `9b9957296 [Phase 6][UOW-2711] Move restored regular warehouse items`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
