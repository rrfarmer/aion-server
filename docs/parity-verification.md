# Parity Verification

This project is a Java-to-C# 1:1 parity migration.

The Java implementation is always the source of truth.

Do not claim verified parity unless there is objective evidence.

## Parity Status Definitions

Use only these statuses:

### Unknown

Behavior has not been reviewed enough to determine parity.

Use when:
- Java behavior was not inspected
- C# equivalent is unclear
- Dependencies are missing
- The artifact was discovered but not analyzed

### Needs Verification

The artifact appears ported or partially ported, but parity has not been objectively confirmed.

Use when:
- Code compiles but behavior was not compared
- Tests exist but do not prove Java-equivalent behavior
- Java edge cases were not checked
- Serialization/date/threading/precision behavior is uncertain

### Partial Parity

Some behavior matches Java, but known gaps remain.

Use when:
- Some methods are implemented
- Some tests pass
- Known methods/branches are missing
- Some edge cases differ
- Some Java behavior is unsupported

### Verified Parity

Behavior is confirmed equivalent to Java by objective evidence.

Allowed evidence:
- Unit tests based on Java behavior
- Integration tests based on Java behavior
- Runtime comparison against Java output
- Golden file comparison
- Deterministic manual confirmation from reviewed Java logic

Do not use this status based only on:
- Code compiles
- Names match
- Structure looks similar
- The port appears complete
- Tests exist but do not validate Java behavior

### Intentional Difference

C# behavior intentionally differs from Java.

Must document:
- The exact difference
- Why the difference exists
- Whether it is safe
- Whether callers are affected
- Whether tests cover the difference

## Verification Evidence Levels

Use the strongest available evidence.

### Level 0: No Evidence

Examples:
- Artifact discovered only
- No C# equivalent exists
- No Java inspection completed

Allowed status:
- Unknown
- Needs Verification

### Level 1: Static Review

Examples:
- Java and C# source compared manually
- Method signatures reviewed
- Control flow appears equivalent

Allowed status:
- Needs Verification
- Partial Parity
- Verified Parity only for trivial deterministic artifacts

### Level 2: Unit Test Evidence

Examples:
- Unit tests validate expected behavior from Java logic
- Edge cases from Java source are covered
- Exceptions/defaults/null behavior are tested

Allowed status:
- Partial Parity
- Verified Parity if coverage is complete for artifact scope

### Level 3: Golden File Evidence

Examples:
- Java output captured as expected data
- C# output compared against Java-generated file
- Serialization or formatted output matches exactly

Allowed status:
- Partial Parity
- Verified Parity

### Level 4: Runtime Comparison

Examples:
- Same inputs executed against Java and C#
- Outputs compared deterministically
- Differences documented

Allowed status:
- Verified Parity

## Required Verification Questions

For every touched Java artifact, answer:

1. Was the Java source reviewed?
2. Was the C# equivalent reviewed?
3. Are all Java public methods represented?
4. Are constructors/defaults equivalent?
5. Are null behaviors equivalent?
6. Are exceptions equivalent?
7. Are collection ordering behaviors equivalent?
8. Are equality/hash behaviors equivalent?
9. Are string comparisons/case sensitivity equivalent?
10. Are date/time/timezone behaviors equivalent?
11. Are precision/rounding behaviors equivalent?
12. Are serialization names/formats equivalent?
13. Are threading/concurrency behaviors equivalent?
14. Are reflection/dynamic behaviors equivalent?
15. Are file/path/encoding behaviors equivalent?
16. Are tests present?
17. Do tests validate Java behavior?
18. Are any differences intentional?

If any answer is unknown, do not mark Verified Parity unless the unknown area is irrelevant and documented.

## Artifact Type Guidance

### DTOs / Models

Verify:
- Field/property names
- Types
- Defaults
- Required/optional behavior
- Null behavior
- Serialization names
- Serialization inclusion/exclusion
- Date/time formats
- Numeric precision
- Equality/hash behavior if applicable

Common risks:
- Java primitive defaults vs nullable C# types
- Java getter/setter behavior vs C# auto-properties
- Jackson/Gson annotations vs System.Text.Json/Newtonsoft
- Date serialization
- Decimal/double differences
- Collection mutability

### Enums

Verify:
- All values exist
- Numeric/string values match
- Serialization format matches
- Unknown/default handling matches
- Case sensitivity matches

Common risks:
- Java enum names serialized as strings
- C# enum numeric default of 0
- Missing unknown/default value
- Custom Java enum methods

### Utilities

Verify:
- Method inputs/outputs
- Edge cases
- Null handling
- Exception behavior
- String culture/case behavior
- Date/time behavior
- Rounding/precision
- File/path/encoding behavior

Common risks:
- Java `BigDecimal` vs C# `decimal`
- Java `double` vs C# `double`
- Java timezone APIs vs .NET DateTime/DateTimeOffset
- Java regex differences
- Java string comparison vs .NET culture-sensitive behavior

### Services

Verify:
- Public methods
- Business rules
- Dependency behavior
- Side effects
- Error handling
- Logging/auditing if behaviorally meaningful
- Transaction boundaries
- Ordering of operations
- Retry behavior

Common risks:
- Hidden Java side effects
- Different exception handling
- Async behavior introduced in C#
- Dependency injection lifecycle differences
- Transaction behavior differences

### Repositories / Data Access

Verify:
- Query filters
- Sorting
- Joins/includes
- Null handling
- Transaction behavior
- Connection/session behavior
- Mapping behavior
- Lazy/eager loading assumptions

Common risks:
- Java ORM behavior differs from EF/Dapper
- SQL generation differs
- Case sensitivity differs by database
- Date/time comparisons differ
- Ordering absent in Java but assumed in C#

### Interfaces / Abstract Classes

Verify:
- All members exist
- Signatures are equivalent
- Generic constraints are equivalent
- Default methods are handled
- Inheritance hierarchy is represented
- Callers can preserve Java behavior

Common risks:
- Java default interface methods
- Java checked exceptions
- Java generics erasure
- C# nullable reference behavior
- Covariance/contravariance differences

### Threading / Async

Verify:
- Execution order
- Shared state
- Locking behavior
- Cancellation behavior
- Exception propagation
- Thread safety assumptions

Common risks:
- Java synchronized vs C# lock
- Java futures vs C# Tasks
- Java thread pools vs .NET async
- Race conditions introduced by async conversion

### Serialization

Verify:
- Property names
- Case policy
- Null inclusion
- Default value inclusion
- Enum representation
- Date/time format
- Number formatting
- Collection ordering
- Unknown fields
- Required fields
- Custom converters

Common risks:
- Jackson vs System.Text.Json/Newtonsoft differences
- Missing annotations
- Timezone shifts
- Null/default omission
- Dictionary ordering
- Floating point formatting

## Test Documentation Requirements

Whenever tests are added, document:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|

Test Type:
- Unit
- Integration
- Regression
- Golden File
- Runtime Comparison
- Manual

Java Behavior Source examples:
- Java source review
- Java unit test
- Java runtime output
- Golden file from Java
- Existing documented behavior

## Golden File Rules

Use golden files when output must match exactly.

Good candidates:
- JSON output
- XML output
- CSV output
- formatted strings
- reports
- deterministic calculations
- serialized DTOs

Golden file requirements:
- Identify how Java output was generated
- Store input and expected output
- Compare exact output unless tolerance is documented
- Document any intentional formatting differences

## Numeric Precision Rules

For calculations:

Prefer exact comparison for:
- integers
- IDs
- counts
- currency using decimal/BigDecimal-equivalent behavior

Use tolerance only when:
- Java uses floating point
- C# uses floating point
- Exact binary equality is not realistic

When tolerance is used, document:
- Tolerance value
- Why tolerance is acceptable
- Whether business behavior is affected

Common warning:
Java BigDecimal should usually map to C# decimal, not double, when business/currency precision matters.

## Date/Time Rules

For date/time behavior, document:

- Java type used
- C# type used
- Timezone behavior
- Local vs UTC behavior
- Formatting behavior
- Parsing behavior
- Default value behavior

Do not mark Verified Parity if timezone behavior is unknown and date/time affects behavior.

Common warning:
Java LocalDate, LocalDateTime, Instant, Date, Calendar, and ZonedDateTime do not map to one single C# type.

## Exception Rules

Verify:
- Exception type
- When exception is thrown
- Message importance
- Whether Java checked exceptions are represented
- Whether C# returns null/default instead

Do not silently replace exceptions with defaults unless documented as intentional.

## Null Handling Rules

Verify:
- Java null accepted/rejected
- C# nullable behavior
- Default values
- Empty string vs null
- Empty collection vs null
- Missing JSON field behavior

Common warning:
Java primitives cannot be null, but C# nullable/reference behavior may differ.

## Collection Rules

Verify:
- Ordering
- Duplicates
- Mutability
- Null elements
- Empty collection handling
- Set/list/map behavior

Common warning:
Java HashMap/HashSet ordering must not be assumed unless Java code relies on deterministic order elsewhere.

## Reflection/Dynamic Behavior Rules

Verify:
- Class/member lookup
- Naming conventions
- Access modifiers
- Annotation/attribute mapping
- Runtime type behavior
- Generic type handling

Do not mark Verified Parity if Java reflection behavior has not been inspected.

## Manual Verification Rules

Manual verification is acceptable only when deterministic and documented.

Document:
- Exact Java logic reviewed
- Exact C# logic reviewed
- Inputs considered
- Edge cases considered
- Why runtime test was not needed
- Remaining gaps

Manual verification should not be used for complex serialization, date/time, concurrency, or calculations unless no other option exists.

## Parity Table Notes Examples

Good notes:
- `Java source reviewed; all enum values ported; serialization as string verified by StatusTypeSerializationTests.`
- `Partial parity: main calculation ported, but Java rounding mode HALF_UP still needs test coverage.`
- `Needs Verification: DTO ported, but Jackson @JsonInclude behavior not yet compared to C# serializer.`
- `Intentional Difference: C# uses DateTimeOffset instead of DateTime to preserve Java Instant timezone semantics.`

Bad notes:
- `Done`
- `Looks good`
- `Should match`
- `Probably complete`
- `Needs tests` without saying which behavior needs tests

## Required Parity Table Fields

Every touched artifact must include:

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|

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
- Golden File Tested
- Runtime Compared

Parity Status:
- Unknown
- Needs Verification
- Partial Parity
- Verified Parity
- Intentional Difference

## Verification Before Commit

Before committing a Unit of Work, the Orchestrator must answer:

1. Did the build pass?
2. Did relevant tests pass?
3. Was focused validation chosen for the changed surface, or was a broad-validation trigger documented?
4. If the broad .NET suite/build was skipped, were the focused commands and rationale documented?
5. Were touched Java artifacts documented?
6. Were touched C# artifacts documented?
7. Was parity status conservative?
8. Were gaps explicitly listed?
9. Were next verification steps documented?

If not, either fix it or document the blocker.

## Final Principle

Code completion is not parity completion.

Parity requires evidence.
When uncertain, mark Needs Verification.
Accuracy beats optimism.
