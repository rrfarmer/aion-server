# Phase 6 Session 2129 Completion - FindGroup Action 12 Declined Message Payload Evidence

Date: 2026-06-02
Unit of Work: UOW-2129
Status: Completed

## Scope

- Added focused packet-payload evidence for the action `12` declined instance-application whisper.
- Strengthened the existing declined action `12` plan test so the planned `SmMessage` packet is compared against an explicit Java-shaped payload instead of another constructed C# packet.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplicationResult` sends `new SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)` to the applicant when `instanceApplicationReply != 1`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_MESSAGE.java`
  - `writeImpl` writes chat type, active-player-aware sender race filter, sender object id, sender name, message, and shout coordinates only for `ChatType.SHOUT`.
- `game-server/src/com/aionemu/gameserver/model/ChatType.java`
  - `WHISPER` id is `4`.

## What Changed

- Renamed `SendInstanceApplicationResult_DeclinePlansLocalizedWhisper` to `SendInstanceApplicationResult_DeclinePlansLocalizedWhisperWithJavaPacketPayload`.
- Replaced the previous C# packet-to-C# packet comparison with an explicit expected unencrypted payload:
  - `04`: `ChatType.WHISPER`
  - `01`: Elyos sender race filter for a non-staff responder
  - `07 03 02 01`: responder object id `0x01020307`
  - UTF-16LE `Responder`
  - UTF-16LE Java `ChatUtil.l10n(1400217)` encoded string
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record declined `SM_MESSAGE` payload evidence.

## Validation

- Changed surface:
  - Test-only packet evidence plus documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests" --no-restore`
  - Final result: passed, 77 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `FindGroupService.sendInstanceApplicationResult`, `SM_MESSAGE.writeImpl`, and `ChatType.WHISPER`; no focused Java packet-generation fixture was identified for this existing C# packet evidence slice.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped packet evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` declined branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `Aion.GameServer.Network.Aion.ServerPackets.SmMessage` | Service Method / Packet | Partial | Unit Tested | Partial Parity | Focused evidence now asserts the planned declined action `12` `SmMessage` unencrypted payload for Java `SM_MESSAGE.writeImpl` field order and `ChatType.WHISPER` id `4`. Live `CM_FIND_GROUP` dispatch and Java runtime/socket trace remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE.writeImpl` | `Aion.GameServer.Network.Aion.ServerPackets.SmMessage.WritePayload` | Packet | Partial | Unit Tested | Partial Parity | The action `12` declined whisper packet covers chat type, sender race filter, sender object id, sender name, and localized message string. Other `SM_MESSAGE` branches such as staff race suppression, manual system messages, truncation warning behavior, and active-player null early return remain outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.SendInstanceApplicationResult_DeclinePlansLocalizedWhisperWithJavaPacketPayload` | Unit | Java `FindGroupService.sendInstanceApplicationResult`; Java `SM_MESSAGE.writeImpl`; Java `ChatType.WHISPER` | Declined action `12` plans a whisper `SmMessage` to the applicant and serializes the Java-shaped payload for responder id/name/race and `ChatUtil.l10n(1400217)` | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket behavior, Java runtime trace, or the complete `SM_MESSAGE` packet surface |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 3 classes/enums, 2 methods/branches.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` declined whisper now has focused payload evidence, but live socket behavior remains unverified.
- Java runtime/socket trace was not produced in this UOW.
- Broader `SM_MESSAGE` behavior remains only partially covered.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2129-Completion.md`
- `docs/Phase-6-Session-2129-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
- Add a narrow adapter-result failure evidence slice for missing direct recipients or skipped invite recipients.
