# Phase 6 Session 1679 Completion - Stagger/Stumble End-Effect Planner

Date: 2026-05-28
Unit of Work: UOW-1679
Status: Complete

## Scope

Add a non-live cleanup planner for Java `StaggerEffect.endEffect` and `StumbleEffect.endEffect`, covering the matching abnormal-state unset intent.

## Completed Work

- Added `dotnetConversion/src/Aion.GameServer/Services/StaggerStumbleEndEffectPlanService.cs`.
- Added planner contracts:
  - `StaggerStumbleEndEffectPlanStatus`
  - `StaggerStumbleEndEffectPlanInput`
  - `StaggerStumbleEndEffectPlan`
- Modeled Java cleanup metadata for:
  - `AbnormalState.STAGGER` unset intent
  - `AbnormalState.STUMBLE` unset intent
  - invalid-effected C# guard before unset intent
- Added focused tests in `dotnetConversion/tests/Aion.GameServer.Tests/StaggerStumbleEndEffectPlanServiceTests.cs`.

## Validation

Executed:

`dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~StaggerStumbleEndEffectPlanServiceTests|FullyQualifiedName~StaggerStumbleCalculatePlanServiceTests|FullyQualifiedName~ForcedMoveStartEffectPlanServiceTests"`

Result:

- 16 tests passed.
- Build succeeded.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.skillengine.effect.StaggerEffect.endEffect`
- `com.aionemu.gameserver.skillengine.effect.StumbleEffect.endEffect`
- `com.aionemu.gameserver.model.gameobjects.Creature`

## Migration Parity Table - UOW-1679

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.skillengine.effect.StaggerEffect.endEffect` | `Aion.GameServer.Services.StaggerStumbleEndEffectPlanService` | Effect Cleanup Boundary | Partial | Unit Tested | Partial Parity | Models non-live `STAGGER` abnormal unset intent and guards invalid effected ids. It does not call a live `EffectController`, mutate real abnormal state, or execute Java effect lifecycle objects. |
| `com.aionemu.gameserver.skillengine.effect.StumbleEffect.endEffect` | `Aion.GameServer.Services.StaggerStumbleEndEffectPlanService` | Effect Cleanup Boundary | Partial | Unit Tested | Partial Parity | Models non-live `STUMBLE` abnormal unset intent and guards invalid effected ids. It does not call a live `EffectController`, mutate real abnormal state, or execute Java effect lifecycle objects. |
| `com.aionemu.gameserver.model.gameobjects.Creature` | `StaggerStumbleEndEffectPlanInput.EffectedObjectId` | Model Boundary | Partial | Unit Tested boundary only | Needs Verification | C# uses an object-id guard instead of a live `Creature`/`Effected` reference. Live null behavior, effect-controller access, threading, and object lifecycle remain unverified. |

## Tests Added

| Test Name | What It Validates | Java-Equivalent Evidence | Test Type | Limitations |
|---|---|---|---|---|
| `CreatePlan_ForStaggerOrStumbleUnsetsMatchingAbnormalLikeJava` | Matching abnormal-state unset intent for both effect kinds. | Reviewed Java `endEffect` source | Unit | No live effect-controller mutation |
| `CreatePlan_BlocksInvalidEffectedBeforeUnsetIntent` | Invalid effected id blocks abnormal unset intent. | C# safety boundary for Java live effected requirement | Unit | Guard is C#-specific, not a direct Java branch |
| `CreatePlan_RejectsUnsupportedForcedMoveEffectKinds` | Planner rejects non-stagger/stumble effect kinds. | C# service scope guard | Unit | Not a Java branch |

## Risks / Gaps

- No live `StaggerEffect` or `StumbleEffect` lifecycle integration was added.
- Planner output is intent-only; `EffectController.unsetAbnormal` is not executed.
- Java threading/lifecycle behavior for active effects remains unverified.
- Java runtime/golden vectors for `SM_FORCED_MOVE` and calculate probe math are still desirable to strengthen evidence.
- Stumble's skill-specific no-send TODO remains unmodeled for start-effect packet planning.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/StaggerStumbleEndEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaggerStumbleEndEffectPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1679-Completion.md`
- `docs/Phase-6-Session-1679-Handoff.md`
