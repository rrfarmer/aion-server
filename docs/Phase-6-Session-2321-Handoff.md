# Phase 6 Session 2321 Handoff - Beshmundir Difficulty Accept Movement

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2321-Completion.md`
- `docs/Phase-6-Session-2321-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2321, Beshmundir accepted difficulty response movement for already registered group instances.

Completed Beshmundir/portal slices relevant to the next work:

- Beshmundir solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Beshmundir group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Beshmundir grouped non-leader closed-instance `INSTANCE_ENTRY`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- Beshmundir grouped non-leader open-instance `INSTANCE_ENTRY`: resolves `portal_use` path and transfers into the registered group instance.
- Beshmundir `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends Java-shaped `SmQuestionWindow`, then dialog `4762`.
- Beshmundir accepted difficulty response: removes the pending request and moves through the same portal-use path when a registered group instance exists.
- Registered group portal continuation: transfers player into existing team instance, queues teleport, and applies/skips cooldown according to reentry.

Still not proven:

- Fresh group-instance allocation and `registerTeam(group)`.
- Beshmundir leader difficulty acceptance when no registered instance exists.
- Difficulty-specific fresh instance selection.
- Generic portal dialog live routing for team plans.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `7a644b31f [Phase 6][UOW-2318] Register Beshmundir difficulty request`
- `c95ad6c8b [Phase 6][UOW-2319] Transfer registered group portals`
- `9d49df0a2 [Phase 6][UOW-2320] Move Beshmundir non-leader into open instance`
- `[Phase 6][UOW-2321] Move Beshmundir accepted difficulty response`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2321-Completion.md`
- `docs/Phase-6-Session-2321-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI` `AIRequest.acceptRequest` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirDifficultyQuestionResponseAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Accepted response now calls movement for registered group instances. Fresh group allocation and difficulty-specific allocation remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.respond` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.Respond` via `HandleQuestionResponseAsync` | Request Registry / Boundary | Partial | Focused Boundary Tested | Partial Parity | Existing removal semantics reused. This UOW did not re-audit all requester kinds. |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler Helper | Partial | Focused Boundary Tested | Partial Parity | Reused for accepted difficulty response. Difficulty byte is not yet used because fresh allocation remains unsupported. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultySelectionRegistersQuestionAndReopensDialog|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryMovesNonLeaderWhenGroupMemberInside|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime request branch. Java source review was used as source-of-truth evidence.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: question-response dispatch and portal continuation reuse.

Broad .NET decision: skipped full project/solution validation after focused tests passed. The focused command covered the edited request branch, the selection branch, and registered group continuation; no packet primitive or shared serialization code changed.

## Next Sequential UOW

Recommended next runtime scope: fresh group-instance allocation for Java `PortalService.port(...)` group paths.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/src/com/aionemu/gameserver/model/team2/group/PlayerGroup.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapRuntimeState.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Specific behavior to prove: Java `PortalService.port(...)` group branch with `maxPlayers` 3/6 and no registered group instance calls `InstanceService.getNextAvailableInstance(mapId, difficult, maxPlayers)`, registers the group/team, then transfers the player if capacity allows.

Focused C# command should start with a new `GameServerConnectionInstanceCooldownTests` fresh group allocation test and the closest Beshmundir difficulty-acceptance test:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstance|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultyAcceptMovesResponderWhenRegisteredInstanceExists" --no-restore
```

Replace the placeholder fresh-allocation test filter with the exact new test name after implementation. Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: possible if fresh allocation changes shared world runtime state allocation semantics. Name the trigger before any unfiltered project/solution validation.

## Safe Candidates

- Fresh group-instance allocation and `registerTeam(group)` for group max-player paths.
- Beshmundir leader difficulty acceptance with no registered instance, once fresh group allocation exists.
- Difficulty id propagation into group allocation, scoped to Beshmundir/portal path.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Alliance/league portal allocation until group allocation is proven.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
