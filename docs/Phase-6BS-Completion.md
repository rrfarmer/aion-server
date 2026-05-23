# Phase 6BS Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BR and covers Sessions 458-462.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 988 tests.

---

## Recent Work Completed

- Routed non-owner `CM_SHOW_DIALOG` for registered kisk NPC targets through a first `KiskAI.handleDialogStart` slice.
- Changed kisk bind question acceptance to resolve by kisk object id, so non-owners can accept pending bindstone questions.
- Added Java `Kisk.broadcastKiskUpdate` fanout planning and connection wiring for old/new kisk updates: online members outside the current NPC known-list callback plus same-race visible players now receive `SM_KISK_UPDATE` in addition to the acting player's direct packet.
- Added Java `KiskService.removeKisk` online cleanup: scheduled kisk despawn now preserves the removed runtime state, sends the creator a final `SM_KISK_UPDATE`, clears online bound/pending state, sends online members an obelisk bind-point replacement, and refreshes NPC visibility for `SM_DELETE` deltas.
- Added a narrow Java `Kisk.resurrectionUsed` runtime prerequisite: charge decrement, update-broadcast intent, and delete-on-zero intent.
- Added in-memory offline kisk binding restore parity for Java `KiskService.onLogout/onLogin`: logout stores the current runtime kisk binding, kisk removal clears offline entries for members, and enter-world restores the binding before obelisk/kisk bind-point packets.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 462 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `370799352` - `Route kisk show-dialog requests`
- `bc77fd218` - `Add kisk update fanout`
- `ddbc1cb25` - `Add kisk removal cleanup`
- `c0e4a182b` - `Add kisk resurrection charge planner`
- `29816e281` - `Restore offline kisk bindings`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `CM_SHOW_DIALOG` kisk route / `KiskAI.handleDialogStart` | `PlayerKiskDialogService` + `GameServerConnection.HandleShowDialogAsync` | Partial | Unit + Regression Tested | Partial Parity | Handles duplicate/full/no-authority/question start for known/interactable kisk NPCs. Full AI event machine and socket-loop tests remain absent. |
| `AIRequest.acceptRequest` for bindstone | `PendingKiskBindRequest` + object-id `HandleKiskBindQuestionResponseAsync` | Partial | Regression Tested | Partial Parity | Non-owner accepted questions can reach the bind service. Full encrypted `CM_QUESTION_RESPONSE` ordering remains unverified. |
| `Kisk.broadcastKiskUpdate` | `PlayerKiskUpdateFanoutService` + `BroadcastKiskUpdateAsync` | Partial | Unit Tested | Partial Parity | C# fans updates to direct members and same-race visible players. Exact Java kisk-owned known-list membership is still missing. |
| `KiskService.removeKisk` | `PlayerKiskRemovalCleanupService` + scheduled cleanup caller | Partial | Unit + Regression Tested | Partial Parity | Online creator/member cleanup is implemented. Dead-member resurrection-option refresh and full socket timing remain pending. |
| `Kisk.resurrectionUsed` | `PlayerKiskResurrectionService` | Partial | Unit Tested | Partial Parity | Runtime charge/depletion behavior is modeled as intent flags. Full `CM_REVIVE` caller wiring is still pending. |
| `KiskService.boundButOfflinePlayer` / `onLogout` / `onLogin` | `PlayerKiskRegistry` offline binding map + enter/logout wiring | Partial | Unit + Regression Tested | Partial Parity | In-memory restore now works for active runtime kisks. C# stores object ids rather than Java direct `Kisk` references. |
| `SM_KISK_UPDATE` | `SmKiskUpdate` | Ported Earlier, Wired Further | Packet + Regression Tested | Partial Parity | Packet writer is reused across bind, fanout, remove, restore, and resurrection-intent flows. Recipient ordering needs live validation. |
| `TeleportService.sendKiskBindPoint` | `SmBindPointInfo.Kisk` from bind/restore paths | Partial | Packet + Regression Tested | Partial Parity | Bind and restored-login paths send kisk bind points when positions are resolvable. Full TeleportService semantics remain broader. |

Metrics from the current handoff window:

- Total focused sessions covered: 5
- Total commits covered: 5
- Current full validation baseline: 988 tests passing
- Total artifacts with verified packet parity in this window: none newly promoted to Verified Parity; packet writers reused from earlier packet-tested work
- Total blocked artifacts: group/alliance kisk use-mask member checks remain blocked on richer team membership lookup
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; full kisk revive dispatch, exact object known-list fanout, group/alliance member lookup, dedicated kisk controller/death state, full NPC/dialog AI, per-zone bind membership, world-map instance ownership, full socket-order harnesses, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Kisk runtime is still a lightweight `WorldNpc` plus `PlayerKiskRuntimeState`, not a dedicated Java `Kisk`/`SummonedObject` with controller, life stats, death state, AI attackability, or effects.
- `Kisk.broadcastKiskUpdate` is partial: C# uses per-player known-NPC state and distance visibility, not Java's kisk-owned known list.
- `CM_REVIVE` still does not perform kisk revive. The charge/depletion prerequisite exists, but full revive needs dead-state gates, HP/MP percent restore, DP/soul-sickness behavior, teleport, stats/speed refresh, resurrection emotion fanout, and zero-charge cleanup wiring.
- Dead-member `showResurrectionOptions()` refresh after kisk removal is not ported because `SM_DIE`/resurrection-option parity is not present.
- Group/alliance use-mask checks remain owner-only until a team membership lookup can prove the creator is in the player's group/alliance.
- Offline kisk binding is in-memory only, matching Java's runtime map; server restart recovery remains absent.
- Full encrypted socket-loop tests for kisk show-dialog, question response, enter-world restore ordering, and scheduled despawn ordering remain pending.

---

## Next Unit Of Work

Recommended next unit: begin the first `CM_REVIVE` kisk-revive caller slice around the new runtime service:

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REVIVE.java`
   - `game-server/src/com/aionemu/gameserver/services/player/PlayerReviveService.java`
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/Kisk.java`
   - `game-server/src/com/aionemu/gameserver/services/KiskService.java`
2. Re-read C#:
   - `GameServerConnection` `CmRevive` switch and kisk helpers
   - `PlayerKiskResurrectionService`
   - `PlayerKiskLifetimeService`
   - `PlayerKiskUpdateFanoutService`
   - `PlayerKiskRemovalCleanupService`
   - `PlayerKiskRegistry`
3. Start with safe gates and explicit deferrals:
   - active player exists and is dead
   - revive id is Java `ReviveType.KISK_REVIVE` (`4`)
   - bound runtime kisk exists and has remaining lifetime/charges
   - kisk world position exists
   - decrement charge through `PlayerKiskResurrectionService`
   - send/fanout `SM_KISK_UPDATE`
   - if depleted, run delete/cleanup through existing services
   - either implement or clearly defer HP/MP restore, teleport, stats, and `SM_EMOTION(RESURRECT)` packets

Strong alternatives:

1. Add exact object/kisk-owned known-list modeling to replace the current fanout approximation.
2. Add socket-loop coverage around kisk show-dialog/question/bind ordering.
3. Continue kisk controller/death state: HP/life stats, attackability, and controller delete paths.
4. Add group/alliance use-mask member lookup once team surfaces are available.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerKiskResurrectionServiceTests|FullyQualifiedName~PlayerKiskRegistryTests|FullyQualifiedName~PlayerKiskUpdateFanoutServiceTests|FullyQualifiedName~PlayerKiskRemovalCleanupServiceTests|FullyQualifiedName~PlayerKiskLifetimeServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerKiskDialogServiceTests|FullyQualifiedName~PlayerKiskAuthorizationServiceTests|FullyQualifiedName~PlayerKiskBindServiceTests|FullyQualifiedName~PlayerKiskSpawnServiceTests|FullyQualifiedName~NpcDialogRequestServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 458-462, `docs/Phase-6BR-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
