# ADR-0005: Jetpack Compose with Material 3

- **Status:** Accepted
- **Date:** 2026-10-14
- **Deciders:** Patrick Baumgartner

## Context

The app needs three screens (Home, Study, Import), a full-screen flashcard experience, light/dark theming, and a custom design language (Manrope, BrandCyan palette) inherited from the watchdog reference app. Options:

1. **Jetpack Compose + Material 3** — declarative UI, current Android standard.
2. **XML Views** — legacy, imperative, more boilerplate.
3. **Compose + Material 2** — older component set.

Constraints: Compose BOM 2026.09.00, Kotlin 2.4.20, single-activity navigation, system-aware themes, transparent system bars.

## Decision

Build the entire UI with **Jetpack Compose and Material 3**, in a single `MainActivity` with Compose Navigation. The watchdog design language is ported into a Compose theme (`ui/theme/`: `Color.kt`, `Font.kt` with Manrope, `Theme.kt`). Reusable pieces — `FlashcardView`, `RatingButtons` — live in `ui/components/`.

## Consequences

**Positive**

- Declarative UI fits the flashcard state model (question → answer → rating) naturally.
- Theme tokens centralized once; light/dark and corner radii come free with Material 3.
- Preview tooling and UI tests operate on composables directly.
- Consistent with the reference project and the Kotlin 2.x stack.

**Negative**

- Recomposition performance requires care on the study screen (stability annotations, keys in lists).
- Compose version churn can force periodic UI adjustments.
