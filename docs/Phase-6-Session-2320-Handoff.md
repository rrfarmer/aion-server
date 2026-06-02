# Phase 6 Session 2320 Handoff - Beshmundir Non-Leader Follow Entry

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2320-Completion.md`
- `docs/Phase-6-Session-2320-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2320, Beshmundir grouped non-leader follow-entry.

Completed Beshmundir/portal slices relevant to the next work:

- Beshmundir solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Beshmundir group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Beshmundir grouped non-leader closed-instance `INSTANCE_ENTRY`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- Beshmundir grouped non-leader open-instance `INSTANCE_ENTRY`: resolves `portal_use` path and transfers into the registered group instance.
- Beshmundir `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends Java-shaped `SmQuestionWindow`, then dialog `4762`.
- Registered group portal continuation: transfers player into existing team instance, queues teleport, and applies/skips cooldown according to reentry.

Still not proven:

- Beshmundir difficulty question acceptance movement.
- Fresh group-instance allocation and `registerTeam(group)`.
- Generic portal dialog live routing for team plans.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `7a644b31f [Phase 6][UOW-2318] Register Beshmundir difficulty request`
- `c95ad6c8b [Phase 6][UOW-2319] Transfer registered group portals`
- `[Phase 6][UOW-2320] Move Beshmundir non-leader into open instance`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2320-Completion.md`
- `docs/Phase-6-Session-2320-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` `INSTANCE_ENTRY` grouped non-leader follow branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Solo, leader path dialog, non-leader closed-instance rejection, and non-leader registered-instance follow-entry are covered. Difficulty question acceptance movement remains unported. |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler Helper | Partial | Focused Boundary Tested | Partial Parity | Resolves `portal_use` path and uses portal preparation/continuation. Java difficulty parameter is not yet wired for leader question acceptance. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group registered-instance branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Reused for Beshmundir follow-entry. Fresh group allocation, alliance/league branches, and generic team portal routing remain incomplete. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsSoloPlayer|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryShowsPathDialogForGroupLeader|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsNonLeaderWhenInstanceNotOpened|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryMovesNonLeaderWhenGroupMemberInside|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime handler branch. Java source review was used as source-of-truth evidence.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: live dialog dispatch and portal continuation reuse.

Broad .NET decision: skipped full project/solution validation after focused tests passed. The focused command covered the Beshmundir entry branches and registered group continuation paths; no broad packet primitive, serialization, schema, or shared infrastructure change was made.

## Next Sequential UOW

Recommended next runtime scope: Beshmundir difficulty question acceptance movement.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/ai2/AIRequest.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_QUESTION_RESPONSE.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingKiskBindRequest.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Specific behavior to prove: Java `BeshmundirsWalkAI.SELECT_NONE_1/2` registers question id `902050`; on accepted response, current Java code calls `moveToInstance(responder, (byte) 2)` for that request id, resolving the same NPC `portal_use` path and using `PortalService.port(...)`.

Focused C# command should target:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultySelectionRegistersQuestionAndReopensDialog|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryMovesNonLeaderWhenGroupMemberInside" --no-restore
```

Add the new difficulty-acceptance test name to that filter once implemented. If the implementation directly reuses registered group continuation, include only the closest registered group continuation test that covers the affected branch.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none unless the next UOW changes shared question-response dispatch, generic portal routing, or fresh instance allocation.

## Safe Candidates

- Beshmundir difficulty question acceptance movement using the existing pending request and movement helper.
- Beshmundir question denial/cleanup if Java source shows a narrow branch that can be ported without range observer fanout.
- Generic registered-team portal routing only if difficulty acceptance exposes a reusable narrow dependency.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Fresh group allocation until `registerTeam(group)` behavior is scoped and tested.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
