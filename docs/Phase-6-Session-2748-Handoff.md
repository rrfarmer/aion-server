# Phase 6 Session 2748 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2748] Wire live legion permission edits

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2748-Completion.md`
- `docs/Phase-6-Session-2748-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 19
- Failed: 0
- Skipped: 0
- Existing warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this packet/service path.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x0D` is live alongside `0x07`, `0x08`, and `0x09`; remaining subactions are deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionPermissionChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Brigade-general guard, state mutation, and active send are live; online legion broadcast is missing. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Model.GameObjects.Player` legion permission snapshot fields | Runtime State | Partial | Unit Tested | Partial Parity | Active snapshot mutates; shared aggregate and cross-member sync remain absent. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionEdit` | Server Packet | Partial | Unit Tested | Partial Parity | Type `0x02` payload is covered from live handler output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added `STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT` id `1300283`. |

## Known Gaps

- No online legion-member fanout for permission edits.
- No shared `Legion` aggregate, so only the active player's loaded snapshot is updated.
- No direct persistence was added because Java `changePermissions` does not directly store the legion; persistence timing still needs verification before adding a DB write.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` exOpcode `0x0A` self-introduction changes.

Runtime Progress Gate:
- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x0A` would stop being parser-only and mutate the active legion member's self-introduction from live code.
- Java source of truth: `CM_LEGION.readImpl` case `0x0A`, `CM_LEGION.runImpl` case `0x0A`, `LegionService.changeSelfIntro`, `LegionRestrictions.canChangeSelfIntro`, `LegionMember.setSelfIntro`, `SM_LEGION_UPDATE_SELF_INTRO.writeImpl`, and `SM_SYSTEM_MESSAGE.STR_GUILD_WRITE_INTRO_DONE`.
- C# runtime artifact to wire/fix: `Player` legion member self-intro snapshot field if not already present, `GameServerConnection.HandleLegionAsync`, a new `SmLegionUpdateSelfIntro` packet, `SmSystemMessage` id `1300282`, and focused `CmLegionTests`.
- Client-visible/state effect expected: live self-intro edits update active runtime state, send the Java-shaped self-intro update packet, and send the Java success system message.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live client packet path, mutates live player/legion-member state, and sends real server packets from live code.

Focused validation recipe:
- Specific behavior/contract: exOpcode `0x0A` parses an empty D plus string, applies Java's valid self-intro guard, updates active member self-intro state, writes `SM_LEGION_UPDATE_SELF_INTRO` as player object id plus string, and sends message id `1300282`.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore`
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live connection dispatch and runtime state mutation.
- Broad .NET decision: start focused; do not run unfiltered project/solution validation unless focused evidence exposes wider packet or model risk.

Risks to watch:
- Java `LegionRestrictions.isValidSelfIntro` details need source inspection before implementing validation.
- Java broadcasts `SM_LEGION_UPDATE_SELF_INTRO` to all online legion members; C# may need active-connection-only behavior until safe legion fanout exists.
- Confirm whether self-intro is persisted elsewhere before adding repository writes; initial UOW should avoid inventing persistence if Java does not do it in this path.

Safe alternative candidates:
- Investigate online legion-member fanout only after confirming the C# connection registry can enumerate active players by legion id safely.
- Continue another `CM_LEGION` subaction only when it has a Java source path, active C# state, and a client-visible packet/state effect.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
