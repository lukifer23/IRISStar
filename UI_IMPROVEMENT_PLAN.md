# IRIS Star: UI Improvement Plan

This document outlines a focused, step-by-step plan to enhance the UI of the IRIS Star application. The goal is to improve consistency, modernize data flow, and implement key missing features in a safe and incremental manner.

---

## Phase 1: Theming & Consistency Cleanup

**Goal:** Ensure all UI components derive their colors from the central MaterialTheme to support theming, dynamic colors, and maintain a consistent look and feel.

- [ ] **Task 1.1: Refactor `ThinkingMessage` Colors**
    - [ ] Replace hardcoded colors (e.g., `Color(0xFF1E293B)`, `Color(0xFF0f3460)`) with appropriate colors from `MaterialTheme.colorScheme`.
    - [ ] Create new semantic color properties in the theme if suitable ones don't exist (e.g., `thinkingBackground`, `finalAnswerBackground`).

- [ ] **Task 1.2: Refactor `MessageBubble` Colors**
    - [ ] Verify that all colors used in the `MessageBubble` and `SystemMessageBubble` are sourced from `MaterialTheme.colorScheme`.
    - [ ] Remove any remaining hardcoded color values.

---

## Phase 2: State & Data Flow Refactoring

**Goal:** Improve architectural purity by ensuring ViewModels are the single source of truth for the UI, and that UI components are stateless and decoupled from data sources.

- [ ] **Task 2.1: Hoist `ThinkingMessage` State**
    - [ ] Remove the local `showThinkingTokens` state from the `ThinkingMessage` composable.
    - [ ] Ensure the `Switch` component's checked state is driven directly from the `MainViewModel`'s UI state.
    - [ ] Ensure the `onCheckedChange` callback for the `Switch` calls a method on the `MainViewModel` to update the state.

- [ ] **Task 2.2: Clean Up `MainChatScreen` Dependencies**
    - [ ] **Sub-task 2.2.1:** Move the `models: List<Downloadable>` parameter out of `MainChatScreen`. The list of models should be exposed via a StateFlow in the `MainViewModel`.
    - [ ] **Sub-task 2.2.2:** Move the `dm: DownloadManager` and `extFileDir: File?` parameters. Logic for downloading and managing models should be encapsulated within the `ModelRepository` and triggered by the `MainViewModel`, not handled directly in the UI.
    - [ ] **Sub-task 2.2.3:** Update the `AppNavigation` composable to reflect these changes, simplifying the `MainChatScreen` signature.

---

## Phase 3: Feature Implementation - Rich Content

**Goal:** Enhance the chat experience by adding support for rich text formatting like Markdown and code blocks.

- [ ] **Task 3.1: Implement Markdown Rendering**
    - [ ] Research and select a lightweight, Compose-compatible Markdown rendering library.
    - [ ] Create a new composable, `MarkdownText`, that takes a Markdown string and displays it as formatted text.
    - [ ] Integrate `MarkdownText` into `MessageBubble` and the output content section of `ThinkingMessage`.

- [ ] **Task 3.2: Add Code Block Syntax Highlighting**
    - [ ] Extend the `MarkdownText` composable or use a compatible library to detect and apply syntax highlighting for code blocks.
    - [ ] Ensure code blocks have a distinct background and a "copy" button for easy interaction.

---

## Phase 4: Advanced Features & Bug Fixes

**Goal:** Address known bugs and implement advanced features to improve usability.

- [ ] **Task 4.1: Implement In-Chat Search**
    - [ ] Add a search bar to the `ModernTopAppBar` that is only visible when viewing a chat screen.
    - [ ] Implement logic in the `MainViewModel` to filter the `messages` list based on the search query.
    - [ ] Add UI elements to highlight search results and navigate between them.

- [ ] **Task 4.2: Fix Cross-Chat Search (`NetworkOnMainThreadException`)**
    - [ ] Analyze the existing search implementation in `ChatListScreen` and its corresponding ViewModel/Repository methods.
    - [ ] Ensure all network and database operations related to search are performed off the main thread using the appropriate Coroutine Dispatcher (e.g., `Dispatchers.IO`).
    - [ ] Re-enable and test the search functionality.
