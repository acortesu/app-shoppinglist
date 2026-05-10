# Product

## Register

product

## Users

Individual meal planners (Costa Rican context, but not geographically limited) who plan meals for a household over weekly or fortnightly periods. They think in terms of recipes they know, ingredients they need to buy, and want the system to handle unit conversions transparently so they don't have to. Their context is calm, deliberate—they're planning ahead, not in a rush.

## Product Purpose

appCompras removes friction from meal planning and shopping list generation. Users create recipes once, reuse them across meal plans, and generate shopping lists that automatically aggregate ingredients, normalize units, and convert to purchase quantities. Success means the user never has to think about unit conversion, never sees an invalid combination, and gets a shoppable list in minutes—not hours.

## Brand Personality

Warm, thoughtful, clear. The app is a capable tool, not a trendy service. It speaks in plain language (Spanish or English), respects the user's time, and gets out of the way. Three words: **clear, capable, unhurried.**

## Anti-references

- SaaS marketing sites with gradients, neon buttons, AI hype language
- Overly gamified or decorated productivity apps (badges, excessive animations, visual noise)
- Corporate coldness (austere, gray, intimidating form-heavy interfaces)
- Over-engineered UX (modals and popovers where inline alternatives exist)

## Design Principles

1. **Clarity without coldness** — Efficient workflows don't require corporate aesthetics. Warm, human design can be minimal.
2. **Mobile-first, breathing room** — Optimize for phones first, then scale up. Generous whitespace and legible typography prevent cognitive overload.
3. **System guides the user** — Invalid unit combinations are impossible before the user submits. The interface nudges toward valid states, reducing error handling friction.
4. **One job per screen** — Recipes, meal planning, shopping—each has its own focus. No nested modals or sidebar clutter.

## Accessibility & Inclusion

- **WCAG AA-adjacent** best-effort: color contrast (at least 4.5:1 for body text), keyboard navigation, screen reader support for semantic markup.
- **Reduced motion** respected: no forced animations, CSS transitions respect `prefers-reduced-motion`.
- **No color-only information** — status conveyed by text + color.
- **Touch-friendly** on mobile: minimum 44px tap targets.
