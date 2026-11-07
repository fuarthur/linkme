---
name: "LinkMe"
description: "Native Android UI for interest discovery and one-to-one text chat."
colors:
  primary: "#5940A8"
  primary-dark: "#D0BCFF"
  on-primary: "#FFFFFF"
  on-primary-dark: "#30204F"
  primary-container: "#E9DFFF"
  primary-container-dark: "#43316A"
  on-primary-container: "#291352"
  on-primary-container-dark: "#E9DFFF"
  secondary: "#536752"
  secondary-dark: "#BBCDAD"
  secondary-container: "#D7E8CF"
  secondary-container-dark: "#3C4B39"
  surface: "#FCFAFF"
  surface-dark: "#17131C"
  surface-container: "#F1EDF7"
  surface-container-dark: "#26212D"
  on-surface: "#231F29"
  on-surface-dark: "#EAE2EF"
  on-surface-variant: "#625C6A"
  on-surface-variant-dark: "#CCC3D4"
  outline: "#7C7485"
  outline-dark: "#968D9E"
  outline-variant: "#D1C9DA"
  outline-variant-dark: "#4A4352"
  error: "#BA1A1A"
  error-dark: "#FFB4AB"
  on-error: "#FFFFFF"
  on-error-dark: "#690005"
typography:
  auth-title:
    fontFamily: "sans-serif-medium"
    fontSize: "34sp"
  screen-title:
    fontFamily: "sans-serif-medium"
    fontSize: "30sp"
  section-title:
    fontFamily: "sans-serif-medium"
    fontSize: "24sp"
  person-name:
    fontFamily: "sans-serif-medium"
    fontSize: "20sp"
  avatar-initials:
    fontFamily: "sans-serif-medium"
    fontSize: "19sp"
  conversation-name:
    fontFamily: "sans-serif-medium"
    fontSize: "18sp"
  intro:
    fontFamily: "sans"
    fontSize: "17sp"
  body:
    fontFamily: "sans"
    fontSize: "16sp"
  bio:
    fontFamily: "sans"
    fontSize: "15sp"
  body-small:
    fontFamily: "sans"
    fontSize: "14sp"
  label:
    fontFamily: "sans-serif-medium"
    fontSize: "14sp"
  status:
    fontFamily: "sans"
    fontSize: "13sp"
  preview:
    fontFamily: "sans"
    fontSize: "12sp"
  timestamp:
    fontFamily: "sans"
    fontSize: "11sp"
rounded:
  button: "16dp"
  field: "14dp"
  people-card: "16dp"
  message: "18dp"
spacing:
  space-4: "4dp"
  space-5: "5dp"
  space-6: "6dp"
  space-8: "8dp"
  space-12: "12dp"
  space-14: "14dp"
  space-16: "16dp"
  space-18: "18dp"
  space-20: "20dp"
  space-24: "24dp"
  space-28: "28dp"
  space-36: "36dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.button}"
  button-outline:
    textColor: "{colors.primary}"
    rounded: "{rounded.button}"
  button-text:
    textColor: "{colors.primary}"
    rounded: "{rounded.button}"
  field-outlined:
    textColor: "{colors.on-surface}"
    typography: "{typography.body}"
    rounded: "{rounded.field}"
    padding: "{spacing.space-16}"
  people-card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.people-card}"
    padding: "{spacing.space-20}"
  avatar:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    typography: "{typography.avatar-initials}"
    width: "52dp"
    height: "52dp"
  message-sent:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.message}"
    padding: "{spacing.space-14}"
  message-received:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body}"
    rounded: "{rounded.message}"
    padding: "{spacing.space-14}"
  preview-banner:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    typography: "{typography.preview}"
    padding: "4dp 12dp"
---

# Design System: LinkMe

## Overview

LinkMe keeps its original purple identity in a native Android interface built with Kotlin Views and Material 3. Its mode is Operate: people select shared interests, read profiles, send messages and maintain their own profile using familiar Android controls.

The implementation uses paired light/dark resource colors, system sans text, rounded controls and mostly flat content surfaces. This document captures the current source; it introduces no new brand metaphor, audience restriction or product claim. PRODUCT.md supplies the product and privacy constraints.

**Key Characteristics:**
- Original purple primary roles, with separate light and dark palettes.
- Native Material buttons, outlined fields, chips, toolbar and adaptive navigation.
- A dedicated interest draft editor with removable selected chips, optional local suggestions and an offline Wiki topic library.
- Initial avatars, visible shared-interest text and real conversation content.
- System font scaling, scrollable forms and system/keyboard inset handling.

Source of truth: `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`, `app/src/main/res/values/themes.xml`, `ui/Ui.kt`, the screen/activity implementations and adapters. Wiki behavior is implemented by `WikiTopicsActivity`, `WikiTopicsViewModel`, `WikiTopicsAdapter` and `WikiTopicRepository`; `InterestCatalog` and the generated `WikiTopicReferences` supply stored-label compatibility. Native dp/sp units in frontmatter are intentional. Component HTML/CSS in `.impeccable/design.json` is a labeled preview translation at density/font scale 1, not runtime code.

## Colors

The palette uses the original purple primary, muted green secondary roles and warm purple-tinted neutral surfaces. The frontmatter contains the actual resource values; every `-dark` entry is the corresponding `values-night` resource.

- **Primary:** `primary` and `on-primary` provide the primary action and sent-message pair; `primary-container` and `on-primary-container` provide initial avatars and the local-preview banner.
- **Secondary:** `secondary` and `secondary-container` are supplied to Material 3 controls through the theme. `colorOnSecondaryContainer` maps to `on-surface`; no separate custom token is defined.
- **Neutral:** `surface`, `surface-container`, `on-surface`, `on-surface-variant`, `outline` and `outline-variant` cover roots, received messages, text and boundaries.
- **Error:** `error` and `on-error` are the theme’s explicit error pair.

The theme uses these static day/night roles. No custom dynamic-color palette or synthesized tonal ramp is defined in the implementation.

## Typography

The theme’s font family is Android `sans`. The shared label helper selects `sans-serif-medium` for its `bold=true` variant; it does not select a separate display font. Text sizes are assigned in sp. Labels add 2dp of line spacing with a 1.0 line-spacing multiplier; line height otherwise follows native font metrics.

| Role | Implemented use |
| --- | --- |
| `auth-title` | Authentication heading |
| `screen-title` | Discover, Chats and Your profile headings |
| `section-title` | Empty-state headings, LinkMe name and chat welcome heading |
| `person-name` | Discovery card names, profile interest heading and interest-editor section headings |
| `avatar-initials` | Initial avatar text |
| `conversation-name` | Inbox names |
| `intro` | Authentication subtitle |
| `body` | General labels, message content and input text |
| `bio` | Discovery biography |
| `body-small`, `label` | Secondary content, form feedback and medium-weight labels |
| `status`, `preview`, `timestamp` | Metadata, demo banner and message timestamp |

Sizes are in the frontmatter. Native Material button, chip, toolbar and navigation typography remains inherited from the theme where the application does not override it.

## Layout

The main content is centered and capped at 720dp by `ContentFrame`. Compact widths use a bottom navigation bar with Discover, Chats and Profile. At `screenWidthDp >= 600`, the same destinations use an 88dp-wide navigation rail. There is no web breakpoint model in the Android implementation. Discover always uses one horizontally scrollable row for the selected interest mix, followed by an outlined Explore & filter interests button. When `screenHeightDp / fontScale < 600`, its header padding reduces from 24dp to 16dp and its helper subtitle is hidden to retain a usable people list. Profile keeps a wrapping interest mix and an outlined Edit interests button.

The dedicated interest editor keeps its Material toolbar and action footer outside the fill-viewport ScrollView. At normal effective heights, the search field and selected-count label also stay outside the scrolling body. When `screenHeightDp / fontScale < 600`, that search area moves into the ScrollView so the body remains reachable; the toolbar and footer stay in place. The search editor is a stable view while query results and other body content update. During the optional scene questions, search is hidden and the footer offers Skip this question.

The Wiki picker shares the centered 720dp content frame, native toolbar and keyboard insets. Its outlined search field and text-only Fields action share one row; the header uses 16dp horizontal padding. A status line reports the current field, result count and selected count. At `screenHeightDp / fontScale < 600`, that line is end-ellipsized to one line and row descriptions reduce from two lines to one. The toolbar, search/header and filled action footer remain outside the weighted RecyclerView. During loading, the list remains as a weighted blank region below the progress indicator, keeping the footer at the bottom. Empty and load-error content use a scrollable weighted region in its place. The footer uses 16dp horizontal padding and remains reachable above the keyboard.

Main screen headers and profile content use 24dp padding; the authentication form uses 28dp; discovery card interiors use 20dp. The spacing entries above are the actual repeated values, including the smaller 5dp message spacing and 6dp card margins. Cards have 20dp horizontal and 6dp vertical outer margins. Inbox rows use 24dp horizontal and 18dp vertical padding and a 96dp minimum height.

Forms use fill-viewport scrolling. `mount` applies system-bar and display-cutout insets; IME-aware roots use the larger of keyboard and bottom-system insets. Text follows Android font scaling. Message text is capped at 72% of the smaller of display width and 720dp; the surrounding bubble adds its own padding.

## Elevation & Depth

Discovery `MaterialCardView` instances explicitly set elevation to zero and use a 1dp `outline-variant` stroke. Message bubbles distinguish direction through color and alignment. Other Material controls retain their inherited native elevation/state behavior. The application defines no custom shadow ramp, animation duration or easing token.

## Shapes

The frontmatter records the actual button, field, card and message radii. Initial avatars use an oval drawable, with a default square size of 52dp; the authentication avatar is 56dp. Native chips and navigation indicators retain their Material 3 shapes rather than an application-defined radius.

## Components

### Buttons

The shared helper creates filled, outlined or text-only `MaterialButton` variants, with sentence-case text, a 52dp minimum height, the documented button radius and 4dp top/bottom insets. Width fills the parent by default. Typography, ripple, disabled, pressed and focus rendering remain native Material behavior.

### Inputs / Fields

Fields use `TextInputLayout.BOX_BACKGROUND_OUTLINE` and `TextInputEditText`. The shared helper sets the documented field radius, 56dp editor minimum height, body text size and 16dp editor padding. Multiline fields show 3–5 lines. Password fields use the native visibility toggle and disable widget-state saving; email fields use the email keyboard/autofill hint. Length counters are enabled only when a maximum is provided.

### Chips

Interest chips use native `Chip` controls with a 48dp minimum height and an ensured minimum touch target. Chip groups use 8dp horizontal and 4dp vertical spacing. Discover and Profile show the current mix as chips that open the editor; they are not inline checkboxes. Discover keeps its mix in one horizontal scroll row, while Profile wraps it. Profile resolves compact Wiki references to their full labels with the small generated lookup, without loading the full Wiki index. Its chips cap width at the smaller of the display width and 720dp, less 48dp, and end-ellipsize long text.

In the editor, search and browse topics are checkable chips. Your mix uses removable chips with an explicit close action, `primary-container` background and `on-primary-container` text and close icon. Long selected labels are end-ellipsized within the content width, and each close action has a Remove [interest] description. Suggestion chips start unselected and add an interest only when tapped. Other chip colors, corner shape and state layers remain inherited from Material 3.

### Interest editor

The outlined Search or create an interest field supports the 148 starter topics in 12 groups and custom labels, with a 40-UTF-16-unit input limit. A visible selected counter supports a mix of up to 16 interests. Search results and custom creation share the same field; the filled Use [count] interests action is disabled for an empty mix. The UI is English; the original Chinese starter aliases remain available. The separate Wiki snapshot uses English titles and does not provide a translated Chinese Wiki library.

With an empty search query, the outlined Help me find my interests action follows Your mix, then Browse Wiki topics appears before Ideas to explore. With a typed query, Search the Wiki topic library appears after starter results and before custom creation; it passes that query and the current mix into the separate picker. Local suggestions are grouped by their source: introduction mentions, scene choices or a related catalog category. The optional flow presents three scene questions, a numbered toolbar subtitle, outlined choice buttons and Skip this question. Choosing or skipping questions shapes suggestions without adding them to the mix. Browse interests uses a horizontal category rail, wrapping topic chips and Show more topics.

The Wiki picker returns only on its explicit Use action; the parent editor replaces its local mix and clears its search. Back from Wiki cancels that picker draft and leaves the parent mix unchanged. The parent editor’s own Use interests action returns a local draft to its caller. From Profile, the returned mix stays in the profile form until Save profile publishes it; from Discover, it changes only the separate discovery filters. The editor explains the caller-specific next step. Back from a question returns to editing without applying the unfinished question run; Back from editing cancels the activity draft.

### Wiki topic picker

The offline library contains 30,919 Wikidata-sourced topics in 23 fields from OtherWise’s public snapshot. It excludes the 718 separately authored OtherWise records. It loads and searches off the main thread, suppresses stale search results and retains query, field and selected mix across recreation. The search input is limited to 120 UTF-16 units, separately from the existing 40-unit stored-interest limit. The full unfiltered library is available through a RecyclerView that binds visible rows rather than constructing a control for every topic.

Rows use native state layers and dividers, a 96dp minimum height and 16dp horizontal/12dp vertical padding. Each presents a Material checkbox, a medium-weight title of up to two lines, a short description, the field and a text-only Info action. Compact descriptions use one line; other descriptions use up to two. The tappable row exposes the title, description, field and checked state as one checkbox role; the visual checkbox and text do not create duplicate accessibility nodes. Info, or a long press, opens full topic details, including the complete title, description, field and a Read source action to that Wikidata entity.

Fields opens a single-choice native dialog with All fields and the 23 imported fields. Sources in the toolbar opens a credit dialog with linked licensing and source information: Wikidata names/descriptions under CC0, and Wikipedia/Wikimedia selection and fields via OtherWise under CC BY-SA 4.0. These credits and the bundled provenance describe an offline data import; no OtherWise service is started.

The status reports result and selected counts with singular/plural wording. A full mix shows an inline error with a next action instead of adding a seventeenth interest. Loading shows progress without displacing the bottom action; load failure offers Try again and guidance to return to the mix. Nothing matched suggests another English title or field, or returning to create a custom interest. The filled Use [count] interest(s) footer is disabled for an empty mix and returns a local selection only.

Stored interests keep the existing length, selection and lookup-key policy. Ordinary Wiki titles use literal values. A generated lookup restores complete labels for 152 compact references: 147 long titles and five titles whose meanings would otherwise be narrowed by starter aliases. Titles are not truncated into stored identities, and distinct meanings remain separate. These labels do not imply inferred interests, match scores or automatic profile publication.

### Cards / Containers

People cards use the `people-card` token assignment, zero elevation and the outlined boundary described above. They show initials, name, biography and an outlined Say hello action. Exact common topics use a shared-interest count and You both enjoy text; category-only connections use A related interest and Connected through text. Additional interests appear as Also into text. The card does not show private contact details or a fabricated match score.

### Navigation

The three destinations use native bottom navigation or a rail according to the layout condition above. The chat screen uses a native Material toolbar and Android Back dispatcher. Selection, focus and ripple states remain theme behavior.

### Avatars, messages and preview identity

Avatars derive up to two initials and use `L` for an empty name; they are decorative for accessibility. Sent messages align to the end with the `message-sent` colors; received messages align to the start with `message-received`. Bubbles use 14dp padding, selectable body text and a smaller timestamp or Waiting to send… status. The debug emulator build alone displays Local preview · sample data, using the `preview-banner` assignment. No photo-storage or online-status indicator is implied.

## Do's and Don'ts

### Do

- Do use Theme.LinkMe and the paired resource color roles for light and dark UI.
- Do keep geometry in dp, text in sp and the existing system/keyboard inset handling.
- Do reuse the native Ui.kt helpers and Material controls for new screens.
- Do show saved or sent success after backend acknowledgement, with an explicit failure or retry state.
- Do label local sample data with the implemented Local preview · sample data banner.

### Don’t

- Don’t expose private email or phone details in discovery cards.
- Don’t invent match percentages, proximity, verification, online status or activity counts.
- Don’t replace the original purple identity or system font without an updated brief.
- Don’t treat the sidecar’s HTML/CSS previews as Android production source.
