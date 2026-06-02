# Phase 6 Session 2312 Handoff - Portal Recruit Option Dialog

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2312-Completion.md`
- `docs/Phase-6-Session-2312-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive. Current parity/progress state is in the latest session completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2312, portal recruit option dialog.

Completed behavior:

- Live C# `CM_DIALOG_SELECT` action `1012` maps to Java `DialogAction.SELECT1_1`.
- C# sends `SmDialogWindow(targetObjectId, 1182)` when:
  - the active player is not in a team,
  - the target object is a portal NPC in `World`,
  - `AutoGroupTable.GetRecruitableInstanceMaskIds(npc.TemplateId)` returns non-null.
- C# sends nothing for the branch when the player is in a team or the target portal NPC has no recruitable masks.
- Action `105` and action `1012` are now evaluated before portal-entry routing, matching Java portal AI ordering before `PortalService.port`.

Still not proven:

- Verified parity for full `PortalDialogAI`.
- Real-client or encrypted socket bytes for these dialog paths.
- `PortalDialogAI.INSTANCE_PARTY_MATCH`.
- Portal path fall-through for false `SELECT1_1` conditions.
- Full group/alliance invite response lifecycle parity.

## Commits Made

- `925c9ce89 [Phase 6][UOW-2311] Wire portal find-group recruit masks`
- `[Phase 6][UOW-2312] Wire portal recruit option dialog`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2312-Completion.md`
- `docs/Phase-6-Session-2312-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.portals.PortalDialogAI.onDialogSelect` action `SELECT1_1` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends `SmDialogWindow(targetObjectId, 1182)` for solo players at portal NPCs with recruitable masks. |
| `com.aionemu.gameserver.model.DialogAction.SELECT1_1` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.Select1_1` | Constant | Complete | Boundary Tested | Verified Parity | Java value `1012` matched and exercised through parsed `CM_DIALOG_SELECT`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW` page `1182` use | `Aion.GameServer.Network.Aion.ServerPackets.SmDialogWindow` | Server Packet | Partial | Boundary Tested | Partial Parity | Existing packet class is used; byte-golden parity was not rechecked in this UOW. |

## Validation From Last UOW

Focused C# boundary validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_SelectOneOneShowsOpenInstanceRecruitDialog" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_OpenInstanceRecruitSendsPortalMaskListOnly" --no-restore
```

Result: passed 1, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped. This is find-group source-tree sanity, not a portal `SELECT1_1` fixture.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary tests covered the scoped live behavior and supplied the compile signal.

## Next Sequential UOW

Recommended next concrete runtime scope: `PortalDialogAI.INSTANCE_PARTY_MATCH` (`77`).

Java artifacts to inspect:

- `game-server/data/handlers/ai/portals/PortalDialogAI.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoGroupType.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_AUTO_GROUP.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/dataholders/AutoGroupData.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs` if present
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDialogWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs` or a narrower portal dialog test class

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_DIALOG_SELECT` action `77` sends `SM_AUTO_GROUP(maskId)` when Java-equivalent auto-group lookup finds a match, then sends `SmDialogWindow(targetObjectId, 0)` regardless of lookup result.

Recommended C# command after adding coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_InstancePartyMatchSendsAutoGroupThenDialogClose" --no-restore
```

Narrow to the exact edited test name. Add adjacent packet tests only if `SmAutoGroup` packet shape is created or changed.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If discovery finds a narrower Java auto-group or portal dialog fixture, use that instead.

Broad-validation trigger: none expected unless packet primitives, shared dialog routing, persistence, crypto, scheduler, or broad world state are changed.

## Safe Candidates

- `PortalDialogAI.INSTANCE_PARTY_MATCH` action `77`.
- Group/alliance invite response lifecycle after find-group action `12`.
- Portal path fall-through coverage for `SELECT1_1` false conditions if the portal-entry static data fixtures are already small enough.

Avoid evidence-only units unless a specific parity claim is blocked by missing evidence.
