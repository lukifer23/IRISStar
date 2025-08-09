# Iris UI Design System

This document tracks the unified UI system and refactor plan. It defines tokens, patterns, and component guidance to keep the app clean, modern, fluid, and consistent.

## Tokens

- Dimensions: spacing, paddings, icon sizes, stroke widths
- Shapes: radii and chat bubble shapes
- Elevation: semantic elevation levels
- Motion: durations, easing, animation specs

Source files (Compose):
- `app/src/main/java/com/nervesparks/iris/ui/theme/Dimensions.kt`
- `app/src/main/java/com/nervesparks/iris/ui/theme/Shapes.kt`
- `app/src/main/java/com/nervesparks/iris/ui/theme/Elevation.kt`
- `app/src/main/java/com/nervesparks/iris/ui/theme/Motion.kt`

Existing `ComponentStyles` is retained, but now delegates to these tokens so legacy code compiles unchanged. New code should prefer the tokens directly.

## Global patterns

- Prefer Material3 surface roles (surfaceContainer, surfaceContainerHigh) and outlineVariant for strokes
- Use dynamic color on Android 12+, fallback brand palette on older devices
- Dark theme supports AMOLED-dark optionally
- Edge-to-edge; respect insets; 48dp minimum touch targets

## Components (guidelines)

- Top app bar: headlineMedium title, subtle surface background, action chips use surfaceContainer
- Chat bubbles: unified corner radii, outgoing subtle tint of primaryContainer, incoming surfaceContainer. Group consecutive messages to reduce visual noise
- Markdown: inline code with surfaceVariant background; fenced code in a card with mono font and copy button
- Input bar: pill shape, elevated, animated between single/multiline
- Cards/Sheets/Dialogs: consistent corners/elevation, blur scrim for modals, spring slide-in
- Lists: Material3 list items with supporting text; section headers; consistent spacings

## Phases

1) Tokens + groundwork (done by code refs below)
2) Chat polish: grouped bubbles, markdown code styling, long-press actions
3) Cards/Modals/Sheets unification
4) Navigation transitions and edge-to-edge
5) Settings/list visual pass and accessibility tweaks

## Acceptance

- Hard-coded dp removed from components in favor of tokens
- Uniform radii, strokes, color roles
- Streaming smooth; markdown readable; code blocks copyable


