# HANDOFF — current state

The single rolling state doc. Updated in place every Unit of Work (git history holds the past). Must be usable with zero prior conversation. Read after the other canonical docs in `csharp-port.md`.

Last updated: 2026-06-07

## Current position

- Phase 6, **re-baselined** to the Port Fidelity & Remediation Plan. The earlier Phase-6 work is behaviorally faithful but structurally over-decomposed (plan-service slop) and partly fused into a god-class; it is being re-ported to 1:1 Java fidelity.
- **Phase A (foundation) — mostly done:**
  - A1 doctrine: `Port-Fidelity-Remediation-Plan.md`. DONE.
  - A2 golden pipeline: DONE, proven end-to-end. Java generator `game-server/test/.../serverpackets/GoldenPacketFixtureGeneratorTest.java` → fixtures in `parity-artifacts/golden/packets/`; C# consumer `dotnetConversion/tests/Aion.GameServer.Tests/GoldenPacketFixtureTests.cs` asserts byte-for-byte. Currently covers SM_GROUP_DATA_EXCHANGE, SM_GF_WEBSHOP_TOKEN_RESPONSE.
  - A3 structural audit: DONE. `scripts/parity/structural_audit.py` → `Structural-Audit-Scorecard.md`.
  - **A4 CI guardrail: NOT built.** Formula golden capture: NOT built.
- Build: C# GameServer builds green (nullable warnings only). Golden test passes 2/2.

## Last unit

- Set up Phase A (doctrine, golden pipeline, audit tool) and rewrote the canonical docs to the fidelity workflow with single-rolling-handoff discipline. Not yet committed at time of writing.

## Next unit (pick one)

Recommended order:
1. **A4 — CI guardrail** (the enforcement whose absence caused the drift): a check that fails when a new `Services/` file uses banned slop vocabulary without a matching Java type, or has no resolvable Java parent. Then **formula golden capture** (extend the harness to `StatFunctions` pure methods).
2. Then **Phase C remediation**, top of `Structural-Audit-Scorecard.md`. First concrete target: **`services/teleport/BindPointTeleportService.java`** (148 Java lines) → one `Services/Teleport/BindPointTeleportService.cs`, delete the 38-file `BindPoint*` slop cluster, golden/audit against Java.

For the chosen unit, fill in:
- Fidelity Gate answers: (1:1 shape / no invented abstraction / packets isolated / structure reduced / breadcrumb).
- Live Gate answer (Phase D only) or "remediation — Fidelity Gate only."
- Exact validation command + whether Java/Maven is needed.

## Blockers / risks

- A4 not built yet → nothing mechanically prevents new slop; rely on the gates + review until it exists.
- Phase C/D porting depends on the runtime layer (controllers, scheduler, KnownList) and `model` (801 Java → 89 C#) that are largely absent; some re-ports will surface missing dependencies — port the dependency rather than stubbing with a new abstraction.
- Golden harness currently covers deterministic, constructor-driven packets only; singleton/time-dependent packets need a deterministic config harness first.
