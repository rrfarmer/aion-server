# Phase 6CW Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CV and covers Sessions 544-545.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1156 tests.

---

## Recent Work Completed

- Added `PortalEntryInteractionService.HandleDialogSelectAsync`, a narrow caller boundary for Java `CM_DIALOG_SELECT -> NpcController.onDialogSelect -> PortalDialogAI.onDialogSelect -> PortalService.port`.
- Wired `GameServerConnection.HandleDialogSelectAsync` so quest-id-zero portal dialog selections can resolve the targeted NPC, resolve `PortalPathTable.GetPortalDialogPath`, invoke `PreparePortalEntryAsync`, send validation failure packets, and send required-item/kinah consumption packets in Java-derived order.
- Added an optional same-instance teleport delegate to `PortalEntryInteractionService` and wired `GameServerConnection` to provide same-instance portal teleport execution.
- Added `PlayerTeleportService.TeleportWithinSameInstance`, representing Java `TeleportService.teleportTo(..., TeleportAnimation.NONE)` for the same world and same instance.
- Added `GameServerConnection.TeleportSameInstancePortalAsync`, which keeps the player's current instance id, broadcasts a visible-player delete when a registry exists, mutates player position immediately, revalidates C# creature PVP-zone counters, and sends the existing same-world spawn packet sequence.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 545 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `3443b3198` - `Wire portal dialog preparation packets`
- `8c727d16a` - `Execute same-instance portal teleport`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `CM_DIALOG_SELECT.runImpl` | `GameServerConnection.HandleDialogSelectAsync` | Partial | Unit Tested | Partial Parity | Portal dialog selections now route into the portal preparation caller before existing charge-all behavior. Generic dialog service, quest auto-reward, admin dialog-info, unknown-dialog logging, and full controller dispatch remain incomplete. |
| `NpcController.onDialogSelect` | `PortalEntryInteractionService.HandleDialogSelectAsync` | Partial | Unit Tested | Partial Parity | C# checks targeted world NPC and talk range before portal handling. Full known-list security, Java AI event machine, and non-portal dialog actions remain incomplete. |
| `PortalDialogAI.onDialogSelect` | `PortalEntryInteractionService.HandleDialogSelectAsync` | Partial | Unit Tested | Partial Parity | Quest-id-zero portal path lookup and preparation are represented. Auto-group, find-group, quest/reward dialog handling, and AI overrides are not fully ported. |
| `Portal2Data.getPortalDialogPath` | `PortalPathTable.GetPortalDialogPath` | Partial | Unit Tested / Regression Tested | Partial Parity | Existing C# race fallback now feeds a caller boundary. Java JAXB population and every real portal path are not runtime-compared. |
| `PortalService.port` same-map branch | `PortalEntryInteractionService` plus `GameServerConnection.TeleportSameInstancePortalAsync` | Partial | Unit Tested | Partial Parity | Required item/kinah consumption packets are sent before same-instance teleport. Transfer/allocation branches, cooldown addition, and group/alliance/league fanout remain missing. |
| `TeleportService.teleportTo(..., TeleportAnimation.NONE)` | `PlayerTeleportService.TeleportWithinSameInstance` | Partial | Unit Tested | Needs Verification | Immediate same-instance position mutation and landing animation are represented. Java abort-player-actions, pet movement, world spawn/despawn internals, effect icon refresh, and protection task restart are incomplete. |
| `TeleportService.SpawnTask.run` same-world branch | Existing same-world completion packet sequence reused by `TeleportSameInstancePortalAsync` | Partial | Unit Tested / Regression Tested | Partial Parity | Sends `SM_CHANNEL_INFO`, `SM_PLAYER_INFO`, `SM_STATS_INFO`, and `SM_MOTION` surfaces. Live same-instance portal capture has not been run. |
| `PacketSendUtility.sendPacket` | `GameServerConnection.SendPacketAsync` / service send delegate | Partial | Unit Tested | Needs Verification | Service tests prove ordering of packet objects and teleport delegate invocation. Encrypted socket framing and live-client order are unverified. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1156 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: solo/open-world transfer allocation, cooldown addition after transfer, group/alliance/league fanout, full dialog/AI engine, Java action-abort side effects, pet teleport/spawn, known-list despawn/spawn parity, and live runtime/client comparison
- Estimated overall migration completion: Phase 6 remains about 62% complete; dialog portal entry now reaches preparation, sends pre-teleport packets, and executes the same-instance branch, but non-same-map instance transfer is still incomplete.

---

## Important Limits

- The C# dialog packet handler only covers portal dialog selections and the earlier charge-all branch. Generic dialog actions, quest auto-reward, class-change, AI scripts, and full `DialogService` parity are still missing.
- Same-instance teleport is wired only for the supported solo/open-world portal slice. Group/alliance/league portals remain explicitly unsupported by the plan helper.
- `TeleportSameInstancePortalAsync` does not fully implement Java `abortPlayerActions`: private-store close, current skill cancel, target clear, ride unset, and controller task handling are incomplete.
- Java pet movement/spawn, full world despawn/spawn known-list behavior, protection task restart, effect icon refresh, and full zone update callback depth remain partial or missing.
- Non-same-map portal transfer is not wired from `PortalEntryInteractionService` yet, although existing lower-level C# helpers exist for delayed teleports, instance allocation, and cooldown persistence.
- No encrypted socket integration test or live client capture proves the new dialog portal packet order.
- Persistence still reuses an existing item update/delete repository path and lacks portal-specific SQL integration coverage.
- Java threading/locking, reflection/JAXB behavior, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add the solo/open-world non-same-map portal transfer slice for `PortalEntryPlanAction.Continue`.

Suggested scope:

1. Re-read Java:
   - `PortalService.port` cases `maxPlayers` 0 and 1
   - `PortalService.port(Player requester, PortalLoc loc, boolean reenter, int maxPlayers)`
   - `PortalService.transfer`
   - `TeleportService.sendLoc`
   - `PortalCooldownList.addPortalCooldown`
2. Re-read C#:
   - `PortalEntryInteractionService.HandleDialogSelectAsync`
   - `GameServerConnection.QueueDelayedTeleportAsync`
   - `GameServerConnection.QueueInstancePortalTransferAsync`
   - `GameServerConnection.QueueAllocatedInstancePortalTransferAsync`
   - `InstanceRuntimeService.CreatePortalTransferInstance`
3. Add a narrow continuation delegate or result consumer that:
   - handles `PortalEntryPlanAction.Continue`
   - for open-world target maps, queues delayed teleport to the portal loc with `TeleportAnimation.FadeOutBeam`
   - for solo instance maps, reuses a registered instance when present or allocates/registers the player's instance before transfer
   - applies entrance cooldown after the teleport request, matching Java `PortalService.transfer`
   - still excludes group/alliance/league fanout
4. Tests:
   - open-world continue sends/queues teleport without instance allocation
   - solo instance continue allocates/registers and queues teleport
   - registered reentry transfers without consuming requirements and skips cooldown addition
   - teleport request is ordered before cooldown packet/persistence
5. Keep parity language conservative:
   - Do not claim verified parity without live Java/runtime comparison.
   - Document missing action-abort, pet, known-list, threading, and live-client behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~PortalEntryValidationServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 544-545, `docs/Phase-6CV-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
