---
name: appCompras
description: Warm, inviting design system for calm, efficient meal planning
colors:
  primary-gold: "#ffb81c"
  accent-teal: "#1ca97a"
  accent-coral: "#ee3e4b"
  accent-purple: "#3f3ad9"
  accent-orange: "#f76a1c"
  accent-pink: "#ec5ea3"
  neutral-bg: "#ffffff"
  neutral-surface: "#f5f3ef"
  neutral-text: "#111111"
  neutral-muted: "#6e6b65"
  neutral-border: "#e8e4dc"
  semantic-warning: "#c87803"
  semantic-error: "#c82828"
rounded:
  sm: "8px"
  md: "12px"
  lg: "16px"
  full: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "18px"
  xxl: "28px"
typography:
  display:
    fontFamily: "'Bricolage Grotesque', sans-serif"
    fontSize: "40px"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "'Bricolage Grotesque', sans-serif"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: 1.2
  body:
    fontFamily: "'Inter', sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "'Inter', sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: 1.4
  caption:
    fontFamily: "'Inter', sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.4
components:
  button-primary:
    backgroundColor: "{colors.primary-gold}"
    textColor: "#ffffff"
    rounded: "{rounded.md}"
    padding: "11px 14px"
  button-primary-hover:
    backgroundColor: "#ffc640"
    textColor: "#ffffff"
  button-secondary:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.md}"
    padding: "11px 14px"
  button-secondary-hover:
    backgroundColor: "#ede9e1"
  button-pill:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.full}"
    padding: "8px 14px"
  button-pill-active:
    backgroundColor: "{colors.primary-gold}"
    textColor: "#ffffff"
  button-link:
    backgroundColor: "transparent"
    textColor: "{colors.primary-gold}"
  input-default:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.md}"
    padding: "11px 13px"
  card:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.lg}"
    padding: "16px"
---

# Design System: appCompras

## 1. Overview

**Creative North Star: "The Ingredient Palette"**

appCompras is a warm, inviting meal-planning tool built on a culinary foundation. The color palette—golden amber primary with vibrant accent colors (teal, coral, purple, orange, pink)—evokes both the practical efficiency of a kitchen and the diversity of ingredients. The design is intentionally minimal and calm: breathing room, generous whitespace, two complementary typefaces, and minimal shadows. Users plan meals without stress or corporate coldness. This is not a spreadsheet. It's a capable, thoughtful companion.

The system explicitly rejects SaaS marketing clichés (gradients, neon buttons, AI hype language), overly gamified or decorated apps, and corporate austerity. No side-stripe borders, no glassmorphism, no modal-first workflows. One accent color carries the primary action (golden primary); other colors remain subordinate, used strategically to signal state or category without visual noise.

**Key Characteristics:**
- Warm, approachable neutrals with one strong golden primary accent
- Two-font hierarchy: serif-leaning display type (Bricolage Grotesque), clean sans body (Inter)
- Minimal elevation (flat by default, single soft shadow on fixed nav)
- Mobile-first responsive; generous tap targets (44px minimum)
- Breathing space between sections; varied rhythm in padding and margins

## 2. Colors

The palette is warm and intentional. The golden primary (#ffb81c) carries ≤10% of any screen—it's reserved for primary actions and active states. Accent colors (teal, coral, purple, orange, pink) are available but used only when semantically meaningful (e.g., recipe categories, status signals). The neutral foundation is warm: an off-white background (#ffffff), light warm surface (#f5f3ef), and soft muted text (#6e6b65 for secondary copy).

### Primary
- **Golden Amber** (#ffb81c): Primary accent for buttons, active states, and high-priority actions. The unifying color of the system.

### Secondary (Supporting accents)
- **Inviting Teal** (#1ca97a): Positive, growth-oriented; category or success signal.
- **Vibrant Coral** (#ee3e4b): Alert or warning without aggression.
- **Elegant Purple** (#3f3ad9): Secondary category or distinctive highlight.
- **Energetic Orange** (#f76a1c): Tertiary category or warm accent.
- **Soft Pink** (#ec5ea3): Tertiary category or soft highlight.

### Neutral
- **Clean White** (#ffffff): Primary background (page, shell).
- **Warm Surface** (#f5f3ef): Secondary surface for cards, inputs, buttons in non-primary states.
- **Near Black** (#111111): Primary text and headers.
- **Muted Gray** (#6e6b65): Secondary text, disabled states, labels.
- **Soft Border** (#e8e4dc): Borders, dividers, subtle structure.

### Semantic
- **Warning Amber** (#c87803): Inline notice and non-critical alerts.
- **Error Red** (#c82828): Form validation errors, destructive actions.

### Named Rules

**The One Accent Rule.** The golden primary (#ffb81c) is used on ≤10% of any screen. It is never combined with other accent colors on a single screen. When multiple categories need color distinction, use the secondary accents; reserve golden amber for the single most important action.

**The Warm Foundation Rule.** Every neutral should tint slightly toward the palette's warm hue, never pure gray. The off-white background (#ffffff) and soft surface (#f5f3ef) carry this warmth. Cool grays make the interface feel sterile; warmth invites.

## 3. Typography

**Display Font:** Bricolage Grotesque (sans-serif, web-safe fallback: Georgia, serif)  
**Body Font:** Inter (sans-serif, web-safe fallback: system sans)

**Character:** A two-family pairing that balances personality with clarity. Bricolage Grotesque is geometric and friendly, used sparingly for h1 headers and card headings; it signals warmth without whimsy. Inter is the workhorse—clean, legible at small sizes, efficient. The contrast between the two creates visual rhythm without complexity.

### Hierarchy
- **Display** (Bricolage Grotesque, 40px, 600 weight, 1 line-height): Page headers only. Responsive scaling down to 32px on mobile (currently in styles.css).
- **Headline** (Bricolage Grotesque, 20px, 600 weight): Card headings, major section titles.
- **Body** (Inter, 16px, 400 weight, 1.5 line-height): Paragraph text, primary content. Capped at 65–75 characters per line for optimal readability.
- **Label** (Inter, 14px, 500 weight, 1.4 line-height): Form labels, secondary content, button text.
- **Caption** (Inter, 13px, 400 weight, 1.4 line-height): Footnotes, helper text, metadata.

### Named Rules

**The Weight Contrast Rule.** Hierarchy is achieved through weight (400 → 600) and size (16px → 40px), never through color alone. When two text elements differ in size, they must also differ in weight by at least 100 points (400 → 600).

**The Line-Length Rule.** Body text is capped at 65–75 characters per line. On screens wider than 600px, add left/right padding or max-width constraints so reading remains comfortable. Never justify type; use left-align.

## 4. Elevation

The system is **flat by default**. There is no shadow vocabulary beyond a single, minimal shadow on the fixed bottom navigation (0 4px 16px rgba(0, 0, 0, 0.12)). This shadow signals that the nav is floating above content, not embedded. All other surfaces (cards, buttons, inputs) are flat. Depth is conveyed through:

- **Tonal layering:** Cards use a slightly darker surface color (#f5f3ef) against the white background to create subtle separation.
- **Borders:** 1px borders (using the neutral-border color) define edges without shadow.
- **State changes:** Hover, focus, and active states change background color or opacity, not shadow depth.

This minimalism keeps the interface calm and mobile-friendly (shadows can tax rendering on low-end devices).

### Shadow Vocabulary
- **Navigation Soft Glow** (`box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12)`): Floating navigation bar only. Indicates above-content elevation.

### Named Rules

**The Flat-By-Default Rule.** Surfaces are flat at rest. Shadows are forbidden except on the fixed bottom navigation. Use tonal layering (surface colors) and borders to define structure. Motion and state change convey interactivity, not depth.

## 5. Components

### Buttons

**Style:** Geometric, confident, warm. Primary buttons feel inviting; secondary buttons fade into the surface.

- **Primary** (Golden Amber background, white text, 12px radius, 11px 14px padding): High-confidence actions (save, generate, next). Uses `opacity: 0.9` on hover, no background shift. Font-weight: 600.
- **Secondary** (Neutral surface background, dark text, 12px radius, 11px 14px padding): Lower-priority actions. Hover darkens background to #ede9e1. Font-weight: 500.
- **Pill Buttons** (Full-width or contained, rounded to 999px, 8px 14px padding, 14px font size): Tab-like selection. Inactive state matches secondary button; active state uses primary gold. Active carries 600 weight.
- **Link Button** (Transparent background, golden text): Inline actions within text flow. No button styling; operates as a link. Hover reduces opacity to 0.8.

### Inputs & Fields

- **Style:** Light surface background (#f5f3ef), 1px border (neutral-border), 12px radius, 11px 13px padding.
- **Placeholder:** Muted gray (#6e6b65).
- **Focus:** Border color shifts to neutral-border (already visible), background remains unchanged. No colored outline ring; rely on border contrast. Will benefit from focus-visible refinement in Block 7.
- **Error:** Error message appears below in error-red (#c82828), 13px font size, 6px margin-top.

### Cards

- **Background:** Neutral surface (#f5f3ef).
- **Border:** 1px solid neutral-border (#e8e4dc).
- **Radius:** 16px.
- **Padding:** 16px.
- **Internal Heading (h3):** Bricolage Grotesque, 20px, 600 weight, 0 margin-bottom 8px.
- **Internal Text (p):** Inter, 14px, muted gray, 6px margin.
- **Shadow:** None (flat by default).

### Pills / Tags

- **Inline badges** for metadata, categories, or state labels.
- **Style:** Neutral surface background, 1px border, 8px radius, 3px 10px padding, 12px font size, muted text.
- **Color:** Inherit neutral palette unless semantically distinct (e.g., category color).

### Bottom Navigation

- **Position:** Fixed bottom, centered, 16px margin from edge.
- **Width:** Min 100vw, max 424px (responsive).
- **Background:** White (#ffffff).
- **Border:** 1px solid neutral-border, fully rounded (999px radius).
- **Shadow:** 0 4px 16px rgba(0, 0, 0, 0.12) — the only shadow in the system.
- **Padding:** 10px 18px.
- **Gap:** 16px between nav items.
- **Z-index:** 20 (above content).

### Day Chips (Planner context)

- **Inactive:** Same as pill buttons (neutral surface, border, dark text).
- **Active:** Golden primary background, white text, no border, 600 weight.
- **Behavior:** Horizontal scroll on mobile; grid layout on desktop.
- **Padding:** 8px 12px, 14px font size, 999px radius.

## 6. Do's and Don'ts

### Do:
- **Do** use the golden primary (#ffb81c) for the single most important action on any screen. ≤10% coverage.
- **Do** rely on tonal layering (surface colors) and 1px borders to define structure. No shadows except the bottom nav.
- **Do** maintain 44px minimum tap target size on mobile for all interactive elements (buttons, chips, inputs).
- **Do** respect `prefers-reduced-motion` — no automatic animations, state changes only.
- **Do** ensure color contrast meets WCAG AA minimum (4.5:1 for body text, 3:1 for large text). The muted gray (#6e6b65) on white is 7.2:1; primaries are 7+.
- **Do** use Bricolage Grotesque sparingly (h1, card headings). The weight contrast between Bricolage and Inter is the visual rhythm.
- **Do** cap body text at 65–75 characters per line and use left-align.
- **Do** use the semantic colors (warning amber, error red) for form validation and inline alerts.

### Don't:
- **Don't** combine multiple accent colors on a single screen. Golden primary + teal + coral is visual chaos.
- **Don't** use `#000000` or `#ffffff` as pure black/white. Tint toward warmth.
- **Don't** add shadows to cards, buttons, or inputs. The system is flat by default; only the nav floats.
- **Don't** use side-stripe borders (`border-left` > 1px) as a colored accent. Use full borders or background tints instead.
- **Don't** animate CSS layout properties (left, width, height, margin). Transitions are allowed on background-color, opacity, color only.
- **Don't** apply gradient text (`background-clip: text`) for emphasis. Use weight or size instead.
- **Don't** use glassmorphism, blur effects, or decorative overlays. This is not a trendy app.
- **Don't** use the accent colors (teal, coral, purple, orange, pink) for dominant UI elements. They are subordinate; reserve dominance for the golden primary or neutrals.
- **Don't** assume dark mode is an improvement. The warm, light palette is the complete design vision.
- **Don't** avoid SaaS marketing clichés with your eyes closed. Specifically: no gradient backgrounds, no neon accents, no AI-hype language in copy, no hero-metric templates.
