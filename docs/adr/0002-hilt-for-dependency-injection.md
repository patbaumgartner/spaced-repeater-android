# ADR-0002: Hilt for Dependency Injection

- **Status:** Accepted
- **Date:** 2026-10-14
- **Deciders:** Patrick Baumgartner

## Context

ViewModels, repositories, the scheduler, and the reminder service need wiring. Options:

1. **Hilt** — compile-time DI built on Dagger, first-class ViewModel/Compose/WorkManager support.
2. **Manual DI** — an app-scoped container passed by hand.
3. **Koin** — runtime service locator DSL.
4. **Koin/Dagger raw** — Dagger alone is verbose for Android entry points.

Requirements: minimal boilerplate, compile-time safety, and pattern parity with the watchdog reference project.

## Decision

Use **Hilt** for dependency injection. `SpacedRepeaterApp` is annotated `@HiltAndroidApp`; `MainActivity` uses `@AndroidEntryPoint`; ViewModels use `@HiltViewModel`; modules provide the database, DAOs, repositories, importer, and scheduler.

## Consequences

**Positive**

- Compile-time verification of the dependency graph; runtime wiring errors surface at build time.
- Standard integration with ViewModels and WorkManager workers removes manual factory plumbing.
- Consistent with the reference project's conventions.

**Negative**

- KAPT/KSP build-time cost and annotation-processor learning curve.
- Hilt-specific test setup (e.g. `@HiltAndroidTest`) is required for instrumented tests.
