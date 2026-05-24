# Parallelization Strategy

This project is a Java-to-C# 1:1 parity migration.

Parallel work is allowed only when file ownership is clear and merge conflicts are unlikely.

The Orchestrator owns:
- Work selection
- Agent boundaries
- File ownership
- Integration
- Testing
- Documentation
- Commits

Sub-agents execute isolated tasks only.

## Primary Rule

Do not parallelize work unless each agent can operate on separate files with separate responsibilities.

If two agents need the same file, same subsystem, or same shared abstraction, the work must be sequential.

## Safe Parallel Work

Usually safe to split across agents:

- DTO/model ports by Java package or domain
- Enum ports
- Constants/static data classes
- Independent utility classes
- Independent test files
- Java behavior analysis with no code changes
- Documentation review
- Golden file generation
- Test data discovery

Examples:

| Work Type | Safe Split |
|---|---|
| DTOs | By package/domain |
| Enums | By package/domain |
| Tests | By test class/file |
| Java analysis | By Java package |
| Documentation | By section/file |
| Golden files | By scenario/domain |

## Unsafe Parallel Work

Do not parallelize these unless one agent has exclusive ownership:

- Dependency injection setup
- Project files
- Build scripts
- Shared base classes
- Shared interfaces
- Shared utilities
- Serialization framework logic
- Date/time conversion helpers
- Reflection helpers
- Threading/concurrency logic
- Authentication/authorization
- Error handling framework
- Logging framework
- Global configuration
- Repo-wide formatting
- Large renames
- Public API contract changes

These areas create hidden merge and behavior risk.

## Conditional Parallel Work

These may be parallelized only after the Orchestrator defines strict boundaries.

### Services

Safe only when:
- Each service is independent
- Shared interfaces are already stable
- Dependency injection does not need broad changes
- Tests can be isolated

Unsafe when:
- Multiple services modify the same interfaces
- Shared base services are involved
- Service registration is incomplete
- Business behavior depends on shared state

### Repositories

Safe only when:
- Interfaces are already defined
- Each repository maps to isolated artifacts
- No shared query framework changes are needed

Unsafe when:
- Shared query helpers change
- Shared transaction behavior changes
- Connection/session management changes
- Entity mappings overlap

### Serialization

Safe only when:
- Work is limited to isolated DTO attributes or tests
- Shared converters are not modified

Unsafe when:
- Shared serializers/converters are modified
- JSON/XML naming policies change
- Java serialization behavior is uncertain
- Precision/date handling is involved

### Tests

Safe when:
- Each agent writes separate test files
- Test fixtures are stable
- Shared test infrastructure is not modified

Unsafe when:
- Multiple agents need the same fixture/base class
- Test infrastructure must be refactored
- Golden files are being reorganized

## Required File Ownership Map

Before spawning sub-agents, the Orchestrator must create a file ownership map.

Format:

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Agent A | Specific task | Exact file paths/globs | Shared files/docs/other agent files | Code/tests/notes |
| Agent B | Specific task | Exact file paths/globs | Shared files/docs/other agent files | Code/tests/notes |

Rules:
- Allowed files must be specific.
- Forbidden files must include shared docs unless assigned.
- No two agents may own the same file.
- If ownership is unclear, do not spawn the sub-agent.
- If an agent discovers it must edit a forbidden file, it must stop and report.

## Agent Sizing

Sub-agent tasks should be small.

Good sub-agent task:
- Port one Java class and its direct C# equivalent
- Add tests for one already-ported class
- Analyze one Java package and report dependencies
- Generate parity notes for one subsystem
- Port a small enum group

Bad sub-agent task:
- “Port the billing module”
- “Fix all tests”
- “Clean up serialization”
- “Refactor services”
- “Finish Phase 6”
- “Make parity verified”

## Dependency Ordering

Prefer this order:

1. Java behavior analysis
2. DTOs/enums/constants
3. Interfaces/contracts
4. Utilities
5. Services/repositories
6. Integration points
7. Tests
8. Documentation/parity updates
9. Commit

Do not port high-level services before required lower-level artifacts are understood.

## Shared File Policy

Shared files require exclusive ownership.

Examples:
- `.csproj`
- solution files
- dependency injection/bootstrap files
- global configuration files
- shared converters
- shared base classes
- central test fixtures
- progress docs
- handoff docs
- parity tables

Only the Orchestrator should normally edit shared progress/handoff/parity docs.

## Sub-Agent Stop Conditions

A sub-agent must stop and report if:

- It needs to edit a forbidden file
- It finds missing Java behavior
- It finds conflicting C# behavior
- It discovers a shared dependency not in scope
- Tests require shared fixture changes
- The task is larger than assigned
- It cannot verify parity
- It would need architectural changes

Stopping is better than guessing.

## Integration Rules

After sub-agents complete, the Orchestrator must:

1. Review every changed file.
2. Confirm ownership boundaries were respected.
3. Resolve any conflicts.
4. Run relevant builds/tests.
5. Compare behavior to Java where possible.
6. Update parity documentation.
7. Update progress documentation.
8. Commit the completed Unit of Work.

Do not commit sub-agent output blindly.

## Parallel Work Templates

### Template: Independent DTO Port

Agent task:
Port Java DTO `x.y.FooDto` to C# equivalent.

Allowed:
- Target C# DTO file
- Target DTO test file if separate

Forbidden:
- Shared serializers
- DI files
- Progress/handoff docs
- Other DTOs unless directly required

Expected output:
- Files changed
- Java artifact reviewed
- C# artifact created/modified
- Null/default behavior notes
- Serialization differences
- Test status
- Parity status recommendation

### Template: Independent Enum Port

Agent task:
Port Java enum `x.y.StatusType`.

Allowed:
- Target enum file
- Target enum tests

Forbidden:
- Shared converters
- Global serialization settings
- Shared docs

Expected output:
- Enum values
- Java naming/value behavior
- C# mapping
- Serialization notes
- Missing/extra values
- Parity recommendation

### Template: Java Behavior Analysis

Agent task:
Analyze Java artifact without editing code.

Allowed:
- Read-only inspection

Forbidden:
- Code changes
- Docs changes unless explicitly assigned

Expected output:
- Behavior summary
- Dependencies discovered
- Edge cases
- Exceptions
- Serialization/date/precision concerns
- Suggested C# port plan
- Whether parallel porting is safe

### Template: Test Additions

Agent task:
Add parity tests for already-ported artifact.

Allowed:
- Specific test file
- Test data file if assigned

Forbidden:
- Production code unless approved
- Shared fixtures unless assigned
- Progress/handoff docs

Expected output:
- Test names
- What each validates
- Java behavior source
- Whether tests are deterministic
- Remaining gaps

## Recommended Parallelization Decision

Before every Unit of Work, ask:

1. Can this be split by file without overlap?
2. Are shared dependencies stable?
3. Are tests isolated?
4. Can each agent finish without touching global config?
5. Can the Orchestrator integrate without a merge problem?

If any answer is no, do the work sequentially.

# Parallel Work Discovery

Before selecting the next Unit of Work, the Orchestrator must perform a Parallel Work Discovery pass.

The goal is to identify multiple independent workstreams that can be executed by sub-agents without file overlap or merge conflicts.

Do not default to a single linear next task unless no safe parallel work exists.

## Discovery Process

The Orchestrator must inspect the Java source, current C# port, progress docs, parity table, and latest handoff to identify:

1. Independent Java packages/classes not yet ported
2. Java artifacts already ported but needing tests
3. Java artifacts needing parity verification only
4. DTOs/enums/constants that can be ported independently
5. Test-only work that can happen separately from production code
6. Documentation/parity cleanup that can happen separately
7. Java behavior analysis tasks that require no code changes

The Orchestrator should produce a batch of possible tasks, not just one next task.

## Required Output: Parallel Task Candidate List

Before starting work, generate a table like this:

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | DTO ports | package/class list | exact C# files | Implementation | Yes/No | Low/Med/High | why |
| B | Enum ports | package/class list | exact C# files | Implementation | Yes/No | Low/Med/High | why |
| C | Parity tests | package/class list | exact test files | Tests | Yes/No | Low/Med/High | why |
| D | Java analysis | package/class list | none/read-only | Analysis | Yes/No | Low/Med/High | why |

## Task Types

Classify each candidate as one of:

- Java Analysis
- DTO Port
- Enum Port
- Utility Port
- Interface Port
- Service Port
- Repository Port
- Test Creation
- Parity Verification
- Documentation Update
- Integration Fix
- Build/Compile Fix

## Parallel Batch Selection

After generating candidates, select a Parallel Batch.

A Parallel Batch is a group of tasks that can run at the same time.

Rules:
- Prefer 2–6 parallel tasks.
- Each task must have separate file ownership.
- At least one task should be low-risk if possible.
- Avoid batching multiple high-risk tasks.
- Avoid batching tasks that all depend on the same shared file.
- Test-only work may run alongside implementation work if files do not overlap.
- Java analysis work is almost always safe to run in parallel.

## Required Output: Selected Parallel Batch

Generate a table like this:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Agent A | Port Foo DTOs | DTO Port | exact paths | shared files/docs | none | code + notes |
| Agent B | Add Bar tests | Test Creation | exact test paths | production files unless approved | Bar already ported | tests + notes |
| Agent C | Analyze Baz Java package | Java Analysis | read-only | all writes | none | behavior report |

If fewer than 2 parallel tasks are selected, explain why parallelism is not safe.

## Distance Rule

Prefer tasks that are far apart in the codebase.

Good parallel task combinations:
- DTO package A + enum package B + tests for already-ported package C
- Java analysis of package A + implementation of package B + documentation update for package C
- Utility tests + enum port + read-only dependency analysis

Bad parallel task combinations:
- Two services using the same shared interface
- Two tasks modifying the same project file
- Multiple tasks changing dependency injection
- Multiple tasks changing serialization policy
- Multiple agents touching the same test fixture
- Multiple agents touching the same parity/progress/handoff doc

## Parallelism Preference Order

When possible, choose work in this order:

1. Read-only Java analysis tasks
2. Test creation for already-ported isolated artifacts
3. DTO/model ports
4. Enum/constants ports
5. Independent utility ports
6. Independent interface ports
7. Independent services/repositories
8. Shared infrastructure work

Shared infrastructure work should usually be sequential.

## Batch Size Guidance

Use this guidance:

| Situation | Recommended Batch Size |
|---|---|
| Mostly read-only analysis | 4–8 agents |
| DTOs/enums/tests | 3–6 agents |
| Mixed implementation and tests | 2–4 agents |
| Service/repository work | 2–3 agents |
| Shared infrastructure | 1 agent only |

Do not maximize agent count at the expense of safety.

## Task Independence Checklist

Before assigning a task to a sub-agent, verify:

1. Does this task have clearly isolated files?
2. Does it avoid shared infrastructure?
3. Does it avoid files assigned to other agents?
4. Does it avoid dependency injection/global config?
5. Can it finish without modifying shared docs?
6. Can the Orchestrator review it independently?
7. Can tests be run without needing another agent's unfinished work?

Only assign tasks where the answer is yes.

## Fallback Rule

If safe parallel implementation work is not available, still look for parallel supporting work:

- Java behavior analysis
- Test gap analysis
- Parity table audit
- Documentation cleanup
- Golden file discovery
- Dependency mapping
- Risk review

The Orchestrator should not treat "implementation cannot be parallelized" as "nothing can be parallelized."

## Next Work Planning

At the end of each Unit of Work, the Orchestrator must update the next handoff with:

1. Next linear task
2. Safe parallel candidates
3. Unsafe shared areas
4. Suggested sub-agent batch
5. Files that must not be edited concurrently

Use this format:

```md
# Next Work Options

## Recommended Sequential Task

- Task:
- Why:
- Files:

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|

## Do Not Parallelize

- File/subsystem:
- Reason:

## Final Principle

Parallelism is a speed tool, not a correctness tool.

Correctness and Java parity come first.