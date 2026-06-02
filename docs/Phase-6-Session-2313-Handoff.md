# Phase 6 Session 2313 Handoff - Portal Instance Party Match Dialog

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2313-Completion.md`
- `docs/Phase-6-Session-2313-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive. Current parity/progress state is in the latest session completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2313, portal instance party match dialog.

Completed portal find-group/auto-group dialog behavior:

- Action `105` `OPEN_INSTANCE_RECRUIT`: sends `SmFindGroup` action `26` only when the portal NPC has recruitable masks.
- Action `1012` `SELECT1_1`: sends `SmDialogWindow(target, 1182)` only when the player is not in a team and the portal NPC has recruitable masks.
- Action `77` `INSTANCE_PARTY_MATCH`: sends optional `SmAutoGroup(maskId)` when Java-equivalent auto-group lookup finds a match, then always sends `SmDialogWindow(target, 0)`.

Still not proven:

- Verified parity for full `PortalDialogAI`.
- Real-client or encrypted socket bytes for these dialog paths.
- Portal path fall-through behavior.
- Full `AutoGroupType` service behavior and auto-instance creation.
- Full group/alliance invite response lifecycle parity.

## Commits Made

- `925c9ce89 [Phase 6][UOW-2311] Wire portal find-group recruit masks`
- `[Phase 6][UOW-2312] Wire portal recruit option dialog`
- `[Phase 6][UOW-2313] Wire portal instance party match`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Dataholders/AutoGroupTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmAutoGroup.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2313-Completion.md`
- `docs/Phase-6-Session-2313-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.portals.PortalDialogAI.onDialogSelect` action `INSTANCE_PARTY_MATCH` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends optional `SmAutoGroup` followed by `SmDialogWindow(target, 0)` for action `77`. |
| `com.aionemu.gameserver.model.autogroup.AutoGroupType.getAutoGroup(int, int)` | `Aion.GameServer.Dataholders.AutoGroupTable.GetAutoGroupForNpc` | Enum Lookup / Data Holder | Partial | Boundary Tested | Partial Parity | C# models Java enum mask order for this lookup. Other `AutoGroupType` methods remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP` window `0` | `Aion.GameServer.Network.Aion.ServerPackets.SmAutoGroup` | Server Packet | Partial | Packet Shape Tested | Partial Parity | Window `0` payload shape is covered. Other windows/constructors need future focused tests before use. |
| `com.aionemu.gameserver.model.DialogAction.INSTANCE_PARTY_MATCH` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.InstancePartyMatch` | Constant | Complete | Boundary Tested | Verified Parity | Java value `77` matched and exercised through parsed `CM_DIALOG_SELECT`. |

## Validation From Last UOW

Focused C# boundary validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_InstancePartyMatchSendsAutoGroupThenDialogClose" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent packet validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmAutoGroup_WritesJavaWindowZeroPayload" --no-restore
```

Result: passed 1, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped. This is find-group source-tree sanity, not a portal `INSTANCE_PARTY_MATCH` fixture.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary and packet-shape tests covered the scoped behavior and supplied the compile signal.

## Next Sequential UOW

Recommended next concrete runtime scope: group/alliance invite response lifecycle after find-group action `12`.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Java group/alliance invite request services called from `sendInstanceApplicationResult`
- `TemporaryPlayerTeam` handling around instance applications
- Java response packet/system-message sequence for accept and decline

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryDispatchAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- Existing group/alliance invite runtime services and tests

## Focused Validation Recipe For Next UOW

Specific behavior to prove: Java-equivalent action `12` accept/decline response lifecycle side effects for group/alliance-backed instance applications.

Recommended C# command after adding coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ActionTwelve|FullyQualifiedName~GroupInvite" --no-restore
```

Narrow to the exact edited test names after discovery. Do not run an unfiltered project test.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If discovery finds or adds a narrower Java fixture for action `12` response lifecycle, use that instead.

Broad-validation trigger: none expected unless shared group/alliance runtime state, persistence, packet primitives, or broad connection dispatch changes.

## Safe Candidates

- Group/alliance invite response lifecycle after find-group action `12`.
- Portal path fall-through coverage for false `SELECT1_1` conditions if the portal-entry fixtures are small enough.
- `SmAutoGroup` non-zero windows only when a concrete Java caller needs them.

Avoid evidence-only units unless a specific parity claim is blocked by missing evidence.
