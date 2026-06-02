# Phase 6 Session 2311 Handoff - Portal Find-Group Recruit Dialog

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2311-Completion.md`
- `docs/Phase-6-Session-2311-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive. Current parity/progress state is in the latest session completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2311, `OPEN_INSTANCE_RECRUIT` portal find-group masks.

Completed behavior:

- Live C# `CM_DIALOG_SELECT` action `105` now maps to Java `DialogAction.OPEN_INSTANCE_RECRUIT`.
- The live boundary resolves the target portal NPC through `World`.
- It asks `AutoGroupTable.GetRecruitableInstanceMaskIds(npc.TemplateId)`.
- If masks exist, it sends `SmFindGroup` action `26`.
- If masks are missing, it sends no find-group packet.
- The portal overload does not send action `10`, matching Java `FindGroupService.showInstanceGroups(Player, Npc)`.

Still not proven:

- Verified parity for full `CM_DIALOG_SELECT`.
- Real-client or encrypted socket bytes for the dialog path.
- Full portal AI handler parity.
- `PortalDialogAI.INSTANCE_PARTY_MATCH` and `SELECT1_1`.
- Full group/alliance invite response lifecycle parity.

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2311-Completion.md`
- `docs/Phase-6-Session-2311-Handoff.md`

## Java Artifacts Touched

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.portals.PortalDialogAI.onDialogSelect` action `OPEN_INSTANCE_RECRUIT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | Action `105` now sends portal mask action `26` when masks exist. Other portal AI branches remain pending. |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `OPEN_INSTANCE_RECRUIT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | Shared action `105` behavior is covered; full Beshmundir instance-entry behavior is not. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, Npc)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForPortal` and `FindGroupConnectionClientActionCompositionPlanService.CreatePortalInstanceGroupShowPlan` | Service | Partial | Unit Tested / Boundary Tested | Partial Parity | Sends action `26` only for non-null portal masks; no action `10` from this overload. |
| `com.aionemu.gameserver.model.DialogAction.OPEN_INSTANCE_RECRUIT` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.OpenInstanceRecruit` | Constant | Complete | Boundary Tested | Verified Parity | Java value `105` matched and exercised through parsed `CM_DIALOG_SELECT`. |

## Validation From Last UOW

Focused C# boundary validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_OpenInstanceRecruitSendsPortalMaskListOnly" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# planner validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ShowInstanceGroupsForPortal_PlansActionTwentySixOnlyWhenPortalMasksExist" --no-restore
```

Result: passed 1, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped. This does not fully prove portal AI caller behavior; that caller was verified by Java source review.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary and planner tests covered the scoped behavior and supplied the compile signal.

## Next Sequential UOW

Recommended next concrete runtime scope: port/check `PortalDialogAI.SELECT1_1` (`1012`).

Java behavior to inspect:

- `game-server/data/handlers/ai/portals/PortalDialogAI.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/dataholders/AutoGroupData.java`
- Any C# team-state helpers needed to determine "not in team".

Expected Java behavior:

- On `SELECT1_1`, if `!player.isInTeam()` and the portal NPC has recruitable masks, Java sends `SM_DIALOG_WINDOW(getObjectId(), 1182)` and returns `true`.
- If the player is in a team or no masks exist, this branch does not send that dialog packet.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDialogWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs` or a small portal-dialog helper
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs` or a more specific portal dialog boundary test

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_DIALOG_SELECT` action `1012` sends `SmDialogWindow(targetObjectId, 1182)` only when the active player is not in a team and the portal NPC has recruitable instance masks.

Recommended C# command after adding coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_SelectOneOneShowsOpenInstanceRecruitDialog" --no-restore
```

Narrow to the exact edited test name. Add one adjacent packet test only if `SmDialogWindow` packet shape changes.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If discovery finds a narrower Java portal dialog fixture, use that instead. Otherwise document Java source review as the direct source-of-truth evidence and the Maven command as find-group source-tree sanity only.

Broad-validation trigger: none expected unless packet primitives, shared dialog routing, persistence, crypto, scheduler, or broad world state are changed.

## Safe Candidates

- `PortalDialogAI.SELECT1_1` action `1012` dialog-window behavior.
- `PortalDialogAI.INSTANCE_PARTY_MATCH` action `77` if C# has `SM_AUTO_GROUP` and `AutoGroupType` enough for a focused boundary unit.
- Group/alliance invite response lifecycle after find-group action `12`.

Avoid evidence-only units unless a specific parity claim is blocked by missing evidence.
