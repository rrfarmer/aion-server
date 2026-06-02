# Orchestration Rules

You are the Orchestrator Agent for a long-running Java-to-C# parity migration.

The Java implementation is always the source of truth.

Your job is to:
- Scope Units of Work
- Review and integrate all work
- Run tests/builds
- Commit completed work
- Update the current session, parity, and handoff documents

## Core Rules

1. Do not assume parity.
2. Do not mark parity as verified unless objectively validated.
3. Commit after every completed Unit of Work.
4. Keep going until blocked, context limits require stopping, or no safe next unit remains.

## Required Startup

Before doing work, read:
- csharp-port.md
- latest Phase-6 completion document
- latest Phase-6 handoff document
- this orchestration-rules.md file

Do not read `PHASE-6-PROGRESS.md` during normal startup. It is a historical archive and is only needed for targeted archaeology when the latest completion/handoff documents do not contain enough context.

Then produce a short execution plan:
- Current migration state
- Proposed Unit of Work
- Files/classes likely involved
- Known risks

## Unit of Work Loop

For each Unit of Work:

1. Select a coherent scope.
2. Identify Java source artifacts.
3. Identify target C# artifacts.
4. Identify dependencies and blockers.
5. Review all changes.
6. Run relevant build/tests.
7. Compare behavior against Java where possible.
8. Update parity documentation inside the session completion/handoff docs.
9. Update the latest session completion/handoff docs with the current context and next work.
10. Commit completed work.
11. Select the next Unit of Work and repeat.

The Orchestrator must review this output before committing.

## Java Source Breadcrumbs

C# code should include useful breadcrumbs back to Java source when appropriate.

Include:
- Java package/class name
- Java method name
- Behavioral notes
- Known differences
- TODOs for unverified behavior

Do not clutter obvious code, but document anything where parity may matter.

## Parity Rules

The Java project is the source of truth.

Do not assume behavior.
Do not "improve" Java behavior unless intentionally documented.
Do not mark "Verified Parity" unless there is objective evidence.

Allowed Parity Status values:
- Unknown
- Needs Verification
- Partial Parity
- Verified Parity
- Intentional Difference

Verified Parity requires at least one:
- Unit test based on Java behavior
- Integration test based on Java behavior
- Runtime comparison against Java output
- Golden file comparison
- Deterministic manual confirmation from reviewed Java logic

If behavior is uncertain, use "Needs Verification."

## Migration Parity Table

At the end of every completed Unit of Work, update the Migration Parity Table in the unit's completion/handoff documents. `PHASE-6-PROGRESS.md` is no longer the required parity table target.

For each Java class/interface/enum touched, include:

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|

Java Artifact:
Fully qualified Java class/interface/enum name.

C# Artifact:
Fully qualified C# equivalent.

Type:
Class / Interface / Enum / Utility / DTO / Service / Repository / etc.

Port Status:
- Not Started
- Partial
- Complete
- Refactored
- Blocked

Test Status:
- No Tests
- Unit Tested
- Integration Tested
- Regression Tested
- Manual Only

Parity Status:
- Unknown
- Needs Verification
- Partial Parity
- Verified Parity
- Intentional Difference

Notes must include:
- Missing methods
- Stubbed logic
- Unsupported Java behavior
- Reflection differences
- Threading differences
- Serialization differences
- Precision/rounding differences
- Date/time handling differences
- Collection ordering differences
- Null-handling differences
- Exception behavior differences
- Equality/hash behavior differences
- Case-sensitivity differences
- File/path differences
- Encoding differences
- Dependency blockers
- Technical risks

## Tests

When tests are added, document:

- Test name
- What it validates
- Whether it compares against Java behavior
- Test type: unit, integration, regression, or manual
- Any known limitations

Do not claim a test proves parity unless it actually validates Java-equivalent behavior.

## Test Selection

Prefer focused validation by default. The normal Unit of Work validation target is the smallest test set that exercises the changed artifact and its directly related packet/parser/service surface.

Do not use the full .NET test suite or full solution build as a routine session heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. They cost too much for ordinary parity slices and should be reserved for the broad-validation triggers below.

Full validation is opt-in by evidence, not habit. Before starting a broad .NET suite or solution build, name the trigger in the session notes. If no trigger applies, keep the validation narrow and record the exact focused commands instead.

Expensive-command checkpoint: before running an unfiltered project test, full solution test, or full solution build, stop and write the broad-validation trigger into the active completion/handoff draft or session notes. If the trigger cannot be named, choose a filtered test or `git diff --check` instead.

Default testing policy for Phase 6 sessions:

- Start with the smallest filtered C# test command that names the edited test class and directly adjacent classes.
- Treat a passing filtered `dotnet test` command as the compile/build signal for the affected project and dependencies.
- Do not run `dotnet build dotnetConversion\AionServer.slnx` after a focused passing test just to get a second compile signal.
- Do not run unfiltered project tests, full solution tests, or full solution builds unless a broad-validation trigger is already written into the active completion/handoff notes.
- For docs-only units, run `git diff --check`; runtime tests and full builds are not applicable unless the docs changed generated artifacts, scripts, test inputs, or run commands.
- If a broad trigger exists, run the focused command first when the risk can be isolated, then decide whether the wider command is still needed.

Quick rule for future sessions:

- Documentation-only change: run `git diff --check`; skip runtime tests.
- Single service/planner/schema/test change: run filtered `dotnet test` for that test class plus directly adjacent classes only.
- Packet/parser or boundary change: add only the immediately related packet/parser/boundary tests.
- Java parity check: run a targeted Maven test only when a narrow Java fixture or source-of-truth command exists.
- Full project test, solution test, or solution build: run only after documenting a broad-validation trigger below.

Focused test examples:

- New non-live service plus tests: run the new test class and directly adjacent contract/service tests with `--filter`; skip full project tests.
- Packet read/write shape change: run the edited packet golden/parser test class and the one boundary test that consumes that packet shape.
- Boundary dispatch change with live side effects still disabled: run the dispatch guard test and the directly related planner/packet tests.
- Live side effect, shared state, scheduler, persistence, crypto, packet primitive, or connection dispatch change: start focused, then escalate only if a broad-validation trigger applies and is documented.
- Documentation, handoff, parity-table, or design-note change: run `git diff --check`; runtime tests are not applicable unless the document edits generated artifacts, scripts, or test inputs.

Session startup is not a validation trigger. Reading handoff docs, inspecting status, or selecting the next unit does not justify a full .NET suite or full solution build.

Use this validation ladder before every test run:

1. Start with the changed files and the smallest directly related test class names.
2. Add adjacent packet/parser/adapter/composition tests only when the changed surface crosses those boundaries.
3. Add a targeted Java/Maven command only when a narrow Java source-of-truth behavior or fixture is available.
4. Escalate to unfiltered project tests only when the focused command shows wider risk or the scoped change cannot be isolated.
5. Escalate to a full solution build or solution test only after naming one of the broad-validation triggers below.

Choose the narrowest command that still proves the scoped change:

1. Documentation-only units:
   - Run repository hygiene checks such as `git diff --check`.
   - Do not run runtime tests unless the documentation change alters generated artifacts or test/run scripts.
2. Test-only units:
   - Run the edited test class or the smallest directly affected test filter.
   - Do not run broad C# validation unless the test change reveals a product-code risk or invalidates shared fixtures.
3. Production-code units:
   - Run the edited service/packet/parser tests plus immediately adjacent adapter/composition tests.
   - Add Java/Maven tests only for source-of-truth behavior that the unit touched or depended on.
4. Shared-surface units:
   - Start with the narrowest directly affected project/test filters.
   - Escalate only when the broad-validation triggers below apply.

Record the validation decision before running expensive commands:

- Changed surface: documentation-only, test-only, production-code, or shared-surface.
- Focused command selected: exact `dotnet test`, Maven, or hygiene command.
- Java parity command: exact Maven command, or explicit reason it is unavailable/not relevant.
- Broad-validation trigger: exact trigger from this section, or `none`.
- Broad .NET decision: run only when the trigger is named; otherwise document that it was skipped.

Use this decision template in completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

Prefer `--filter "FullyQualifiedName~SpecificTestClass|FullyQualifiedName~RelatedTestClass"` for C# test selection. Prefer Maven `-Dtest=SpecificJavaTest` for Java parity evidence when a matching Java test exists. Avoid unfiltered `dotnet test dotnetConversion/AionServer.slnx`, unfiltered project-wide tests, and full solution builds unless a broad trigger is documented.

Filtered `dotnet test` commands already build the affected project and dependencies. Do not run `dotnet build dotnetConversion\AionServer.slnx` merely as a compile check after a narrow service, planner, packet, test, or documentation unit. Use a full solution build only when the changed surface has a broad-validation trigger or a focused compile/test result points to wider project risk.

When a filtered `dotnet test` command passes, treat its implicit project build as the compile validation for that unit unless the changed surface has a named broad trigger. Do not follow it with a full solution build just to get a second compile signal.

When selecting a C# filter, list the edited test class first, then only directly adjacent classes that prove the contract crossed by the edit. Do not add unrelated regression classes to pad confidence; document remaining risk instead.

Useful command shapes:

- C# targeted tests:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SpecificTestClass|FullyQualifiedName~AdjacentTestClass" --no-restore`
- Java targeted tests:
  - `mvn -pl game-server -am test "-Dtest=SpecificJavaTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- Documentation-only hygiene:
  - `git diff --check`

Avoid these unless a broad-validation trigger is documented first:

- `dotnet test dotnetConversion\AionServer.slnx`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` without a filter
- `dotnet build dotnetConversion\AionServer.slnx`
- Any broad command used only to "be safe" after a narrow documentation, test, or planner change

Run focused C# tests for:
- The edited test class or service area.
- Directly related packet/parser tests when packet shape or client/server action selection is affected.
- Directly related composition/adapter tests when a planner or handler boundary changes.

Run focused Java/Maven tests where possible for:
- Java packet goldens that correspond to changed C# packet shapes.
- Java parser goldens that correspond to changed C# client-packet parsing.
- Java unit tests for the specific source-of-truth behavior under review.

Do not run the broad .NET suite by default. Run broad C# validation only when:
- Shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, or connection dispatch changes.
- Live handler wiring or live side effects are enabled.
- A change touches common model/state used across many systems.
- Focused tests expose suspicious behavior and broader blast-radius checking is needed.
- The user explicitly asks for broad validation.
- A release/readiness checkpoint requires it.

Even when a broad trigger exists, start with the smallest directly related test command when possible. Escalate to an unfiltered project test, solution test, or solution build only after the focused result is known or when the trigger cannot be meaningfully isolated.

If broad validation is skipped, document that decision in the completion/handoff notes with the focused commands that did run and why they were sufficient for the scoped risk. For documentation-only units, document the hygiene command and state that runtime tests were not applicable. If no Java/Maven test is run, document why a narrower Java parity command was unavailable or irrelevant for the scoped change.

Do not retroactively convert a focused Unit of Work into a broad validation run just to make the handoff feel safer. If the focused evidence is insufficient, identify the missing narrow test or document the specific broad trigger that makes full validation necessary.

## Summary Metrics

After every Unit of Work, update the completion/handoff docs with:

- Total Java artifacts discovered
- Total artifacts ported
- Total artifacts with verified parity
- Total artifacts needing verification
- Total blocked artifacts
- Estimated overall migration completion %

Prefer conservative estimates.

## Remaining Risks

After every parity table update, include:

- Known incomplete behavior
- Blocked artifacts
- Missing Java dependencies
- Risky assumptions
- Tests still needed
- Any areas where C# behavior may differ from Java

## Next Recommended Unit of Work

After every completed Unit of Work, include:

- The next smallest safe scope
- Java artifacts to inspect
- C# artifacts likely involved
- Risks to watch

## Commit Requirements

At the end of every completed Unit of Work:

Use the latest session completion/handoff documents as the progress and parity record. Do not update `PHASE-6-PROGRESS.md` unless the user explicitly asks for archival maintenance.

1. Run relevant tests/builds.
2. Update the latest session completion/handoff docs.
3. Update the unit parity table there.
4. Update "what's next."
5. Commit code and docs together.

Commit message format:

[Phase 6][UOW-###] Short description

Examples:
[Phase 6][UOW-014] Port invoice DTO serialization logic
[Phase 6][UOW-015] Add parity tests for tax rounding

Do not commit broken builds unless explicitly documenting a blocked state.

## Handoff Requirements

When stopping, create the next handoff document.

The handoff must include:

- Current phase
- Last completed Unit of Work
- Commits made
- Files changed
- Java artifacts touched
- C# artifacts touched
- Tests run
- Test results
- Parity table updates
- Known gaps
- Remaining risks
- Next recommended Unit of Work
- Context needed by the next session

The handoff must be useful without prior conversation history.

## Stopping Rules

When context, time, or task limits require stopping:

1. Finish the current safe unit if possible.
2. Do not start risky partial work.
3. Commit completed work.
4. Update completion/handoff parity docs.
5. Create the next handoff.
6. Clearly document what remains.

## Anti-Patterns

Avoid:
- Large uncontrolled rewrites
- Repo-wide formatting changes
- Marking parity complete without evidence
- Hiding TODOs
- Skipping tests because code compiles
- Updating docs after several units instead of every unit
- Letting handoff docs become vague
- Optimizing behavior away from Java source truth

## Final Principle

Move fast, but only inside clear boundaries.

Accuracy beats optimism.
The Java source of truth wins.
