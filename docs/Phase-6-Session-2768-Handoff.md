# Phase 6 Session 2768 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2768] Broadcast legion permission changes

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2768-Completion.md`
- `docs/Phase-6-Session-2768-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- First run failed to compile due to a syntax error in the edited handler.
- Final rerun passed: 82 passed, 0 failed, 0 skipped.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `LegionService.changePermissions` or `SM_LEGION_EDIT`.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x0D` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection.HandleLegionPermissionChangeAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, brigade-general rejection, active mutation, same-legion online propagation, requester packet, and bystander fanout are covered. No real-client verification. |
| `com.aionemu.gameserver.services.LegionService.changePermissions` | `GameServerConnection.HandleLegionPermissionChangeAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Ports permission mutation and broadcast update. Java shared `Legion` permission state is approximated by active and online same-legion `Player` permission fields. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` type `0x02` | `SmLegionEdit.Permissions` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Packet writes edit type and four permission shorts in Java order. No Java golden fixture. |

## Known Gaps

- No Java golden packet fixture was generated for `SM_LEGION_EDIT`.
- Full Java `Legion` aggregate permission state is still approximated by active and online same-legion `Player` permission fields.
- No real-client validation was performed for the permission edit broadcast.
- `CM_LEGION` announcement exOpcode `0x09` currently persists and updates only the requester; Java updates legion announcement state and broadcasts either `SM_LEGION_EDIT(announcement)` for non-empty announcements or `SM_LEGION_INFO` to other legion members after clearing.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Broadcast live `CM_LEGION` exOpcode `0x09` announcement changes to online same-legion members like Java.

Runtime Progress Gate:
- Deferred/live behavior advanced: C# announcement edits currently update/persist only the active player's announcement fields and send only a requester system message; Java `LegionService.changeAnnouncement` updates the legion announcement, persists it, then broadcasts `SM_LEGION_EDIT(announcement)` for non-empty announcements or `SM_LEGION_INFO` to other legion members when clearing.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x09`, `LegionService.changeAnnouncement`, `LegionDAO.saveAnnouncement`, `SM_LEGION_EDIT(announcement)`, and `SM_LEGION_INFO`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionAnnouncementChangeAsync`, active and online same-legion `Player.LegionAnnouncement`/`LegionAnnouncementEpochSeconds`, existing `SaveLegionAnnouncementAsync`, same-legion online fanout, `SmLegionEdit.Announcement`, and `SmLegionInfo.FromPlayer`.
- Client-visible/state/persistence effect expected: online same-legion members receive the announcement edit or refresh info packet, their runtime announcement state matches the new or cleared announcement, requester system messages remain Java-equivalent, and outsiders remain unchanged.
- Why this is not preview-only/test-only/documentation-only: it mutates live legion/player announcement state, persists through the existing repository shape, and sends real server packets from a live client handler.

Focused validation recipe:
- Add or update live handler tests for exOpcode `0x09`:
  - valid non-empty announcement mutates active and online same-legion announcement state, persists, sends requester done message, and broadcasts `SM_LEGION_EDIT(0x05)` to same-legion bystanders;
  - clear announcement mutates active and online same-legion announcement state, persists null, sends requester clear message, and sends a Java-equivalent legion refresh packet to bystanders;
  - outsider online players do not mutate or receive packets;
  - no-right rejection remains side-effect free.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `LegionService.changeAnnouncement`, `SM_LEGION_EDIT`, or `SM_LEGION_INFO` is discovered.

Risks to watch:
- Java excludes the active player from the clear-announcement `SM_LEGION_INFO` broadcast while sending the clear system message directly; preserve requester packet ordering and avoid duplicate active refresh.
- Java broadcasts non-empty announcement edits to the whole legion; C# may need to send `SmLegionEdit.Announcement` to requester and bystanders while preserving the active done system message.
- Announcement timestamps should stay clamped to C# packet int behavior while preserving Java's current-time semantics.

Safe alternative runtime candidates:
- Port `SM_LEGION_DOMINION_LOC_INFO` only if paired with a live send path or runtime load that client code actually consumes.
- Return to challenge quest-finish progress only after a live quest-finish socket/runtime hook exists; do not use planner placeholders as migration progress.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
