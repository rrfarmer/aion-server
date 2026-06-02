# Phase 6 Session 2307 Handoff - CM_FIND_GROUP Instance Applications

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2307-Completion.md`
- `docs/Phase-6-Session-2307-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger such as a shared packet primitive, crypto/session handshake, scheduler, persistence, or broad runtime dispatch change that focused tests cannot cover.

## Current State

UOW-2307 wired live C# `CM_FIND_GROUP` action `11`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0` through `11`, plus actions `13`, `15`, and `17`.
- Action `11` can send `SmFindGroup` action `11` to a non-active online recruiter through `IGameClientConnectionRegistry.SendPacketToPlayerAsync`.
- Action `11` sends nothing to the active applicant socket.
- Missing/offline action `11` targets emit no packets.
- Adjacent disabled planner tests for action `11` still pass.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior through the live boundary.
- Remaining `CM_FIND_GROUP` action `12`.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionElevenSendsInstanceApplicationToTarget" --no-restore
```

Result: passed 1, failed 0, skipped 0.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionEleven" --no-restore
```

Result: passed 2, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: live find-group dispatch expanded to a non-active direct registry send.

Broad .NET decision: full project/solution validation was skipped after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Port and validate `CM_FIND_GROUP` action `12`:

- action `12`: accept or decline an instance-group applicant

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `12`
- `FindGroupService.sendInstanceApplicationResult(Player responder, int applicantId, byte reply)`
- `PlayerGroupService.inviteToGroup`
- `PlayerAllianceService.inviteToAlliance`
- decline `SM_MESSAGE` whisper localized id `1400217`

Expected Java behavior from prior discovery:

- Action `12` resolves the applicant with `World.getInstance().getPlayer(applicantId)`.
- Missing applicant emits no packet side effects.
- If `reply == 1` and the responder has an instance group:
  - Java invites the applicant to group when `instanceGroup.getMinMembers() <= 6`.
  - Java invites the applicant to alliance when `instanceGroup.getMinMembers() > 6`.
- If `reply == 1` and the responder has no instance group, Java emits no invite side effects.
- If `reply == 0`, Java sends a decline whisper `SM_MESSAGE` to the applicant.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Important caution:

- The C# planner/disabled tests already model action `12` accept-group, accept-alliance, decline, and missing-applicant cases.
- The live boundary currently returns when `plan.InvitePlan != null`, so action `12` needs invite runtime dispatch review before wiring.
- Do not treat planner tests alone as live parity evidence.

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `12` matches Java's online/missing applicant behavior for representative accept and decline cases, including invite dispatch or documented runtime gap.

Expected C# command after adding live coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ActionTwelve" --no-restore
```

If the filter catches too much unrelated work, narrow it to the new live action `12` test plus the existing adjacent disabled action `12` planner tests and document both commands.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java action `12` fixture is found during discovery, use that instead and document the command.

Broad .NET validation remains optional. Run full project/solution tests only if action `12` wiring changes shared invite services, shared packet primitives, session dispatch rules beyond this branch, persistence, crypto, or scheduler behavior.

## Safe Candidates

- Wire action `12` only after confirming the live invite dispatch boundary and existing service shape.
- Add live focused action `12` tests for missing applicant, decline, accept group, and accept alliance if the runtime invite boundary is available.
- If live invite runtime dispatch is not available, document the gap and consider the next safer parity unit instead of faking parity.
- Add current-team id live coverage for action `1` and `3` if team runtime seeding is easy and does not broaden the unit.
- Add target-NPC action `10` mask lookup coverage if world/NPC setup is easy and does not broaden the unit.
