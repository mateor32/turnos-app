---
name: Virtual Queue Kinetic
colors:
  surface: '#fcf8fb'
  surface-dim: '#dcd9dc'
  surface-bright: '#fcf8fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f2f5'
  surface-container: '#f0edf0'
  surface-container-high: '#eae7ea'
  surface-container-highest: '#e5e1e4'
  on-surface: '#1c1b1d'
  on-surface-variant: '#4a4455'
  inverse-surface: '#313032'
  inverse-on-surface: '#f3f0f2'
  outline: '#7b7487'
  outline-variant: '#ccc3d8'
  surface-tint: '#732ee4'
  primary: '#630ed4'
  on-primary: '#ffffff'
  primary-container: '#7c3aed'
  on-primary-container: '#ede0ff'
  inverse-primary: '#d2bbff'
  secondary: '#5b598c'
  on-secondary: '#ffffff'
  secondary-container: '#c7c3fe'
  on-secondary-container: '#514f81'
  tertiary: '#005b3d'
  on-tertiary: '#ffffff'
  tertiary-container: '#007650'
  on-tertiary-container: '#76ffc2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#eaddff'
  primary-fixed-dim: '#d2bbff'
  on-primary-fixed: '#25005a'
  on-primary-fixed-variant: '#5a00c6'
  secondary-fixed: '#e3dfff'
  secondary-fixed-dim: '#c4c1fb'
  on-secondary-fixed: '#181445'
  on-secondary-fixed-variant: '#444173'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#fcf8fb'
  on-background: '#1c1b1d'
  surface-variant: '#e5e1e4'
typography:
  display-ticket:
    fontFamily: Plus Jakarta Sans
    fontSize: 72px
    fontWeight: '800'
    lineHeight: 76px
    letterSpacing: -0.04em
  display-ticket-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 52px
    letterSpacing: -0.03em
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system targets high-efficiency customer reception, virtual queue management, and service operations. The visual atmosphere balances corporate authority with extreme legibility and calm urgency. It rejects ornate decoration in favor of a crisp, minimalist, high-contrast aesthetic: pure white foundations, deep onyx typography, razor-sharp visual hierarchy, and an unmistakable electric purple accent that drives real-time action.

The primary emotional state evoked is certainty and relief. Users in physical or virtual waiting rooms require immediate confirmation of their ticket position, wait time, and counter status without cognitive friction. The identity is forward-leaning, direct, and structured, utilizing precision borders, clear data density, and massive numerical callouts.

## Colors

The palette establishes an ultra-clean corporate landscape with high-chroma directional cues:

- **Primary (`#7C3AED` - Electric Purple):** The signature driver for active tickets, primary actions, progress bars, and critical state anchors. It offers intense vibrancy against stark white surfaces.
- **Secondary (`#1E1B4B` - Deep Midnight Indigo):** Provides structural weight for navigation chrome, top banners, analytical dashboards, and active queue desk assignments.
- **Tertiary (`#10B981` - Emerald Feedback):** Communicates "Turno Listo / Ahora Atendiendo" (Ready / Now Serving), confirmation stamps, and positive status metrics.
- **Neutral (`#09090B` - Deep Onyx):** Grounding neutral for ultra-crisp typography, high-contrast borders, and high-visibility status tags.

### Functional Surfaces
- **Canvas Base:** `#FFFFFF` (Pure white for pristine, luminous clinical precision).
- **Subtle Surface Tint:** `#F8FAFC` to `#F1F5F9` for secondary metadata cards and inactive queue strips.
- **Border Neutral:** `#E2E8F0` for hairline dividers and container bounds.
- **Urgent Warning / Delayed:** `#F59E0B` (Amber) for extended wait alerts.
- **Critical / Missed Call:** `#EF4444` (Vibrant Red) for canceled or missed tokens.

## Typography

The type hierarchy prioritizes immediate scan-speed across varied viewing distances—from a mobile handset in a user's palm to a large wall-mounted monitor in an office reception area.

Plus Jakarta Sans brings a contemporary, technical, yet highly legible geometry. Numerical tracking is adjusted tight on display levels (`display-ticket`) to produce punchy, architectural ticket numbers (e.g., **A-42**, **M-108**). Tabular lining numbers should be forced on numeric strings to avoid shift jitter during live countdowns.

Uppercase transformations are strictly reserved for system labels (`label-sm`), counter designations, and badge states to provide authoritative contrast against standard conversational body text.

## Layout & Spacing

The layout model is a structured, responsive fluid grid built around an 8px base rhythmic cadence.

- **Breakpoints:**
  - `Mobile` (< 640px): 4 columns, `margin-mobile` (16px), `gutter-mobile` (16px). High vertical stacking, ticket tokens occupy full width. Sticky bottom drawer for action triggers.
  - `Tablet` (640px – 1024px): 8 columns, `margin` (32px), `gutter` (24px). Split view between ticket status and office occupancy telemetry.
  - `Desktop / Public Display` (> 1024px): 12 columns, max-width bounded at 1280px for consumer interfaces, or 100% viewport width for kiosk and wall-mounted screen configurations.

Spacing rhythm enforces generous breathing room around focal points (wait metrics, upcoming call) to prevent anxiety-inducing visual clutter.

## Elevation & Depth

Depth is articulated through high-precision containment, hairline borders, and selective ambient diffusion rather than layered skeuomorphism.

- **Level 0 (Flat / Canvas):** Pure `#FFFFFF` background with 1px `#E2E8F0` borders separating non-interactive content panes.
- **Level 1 (Card Rest):** Used for inactive tickets, queue lists, and settings. `#FFFFFF` background, border `1px solid #E2E8F0`, shadow `0 1px 3px 0 rgba(15, 23, 42, 0.04)`.
- **Level 2 (Active Focus / Live Ticket):** Applied to the active user ticket or live counter. Border `1px solid #C4B5FD` (soft electric violet tint), shadow `0 10px 25px -5px rgba(124, 58, 237, 0.12), 0 4px 6px -2px rgba(124, 58, 237, 0.04)`.
- **Level 3 (Modal, Floating Bar & Dialogues):** Pure white with shadow `0 20px 30px -10px rgba(15, 23, 42, 0.15)`, accompanied by a high-opacity dark overlay (`rgba(9, 9, 11, 0.60)`).

## Shapes

The interface balances sharp corporate geometry with purposeful micro-softness. 

- Interactive elements, queue ticket cards, and primary buttons use standard `roundedness: 2` (0.5rem / 8px).
- Status badges, ticket counters, and pill chips use fully rounded bounds (`9999px`) to create strong shape contrast against square-edged structural cards.
- Kiosk tactile targets leverage oversized corner radiuses (`1.5rem` / 24px) for prominent visual ergonomics.

## Components

### Buttons
- **Primary:** Background `#7C3AED`, text `#FFFFFF`, font `label-lg`, height `48px` (mobile touch-optimized: `52px`). Hover: `#6D28D9`. Active: `#5B21B6`. Focus ring: `2px solid #FFFFFF`, `2px solid #7C3AED`.
- **Secondary:** Surface `#FFFFFF`, border `1px solid #E2E8F0`, text `#09090B`. Hover: `#F8FAFC` and border `#CBD5E1`.
- **Destructive / Cancel Ticket:** Surface transparent, text `#EF4444`, hover background `#FEF2F2`.

### Ticket Card (Hero Element)
- The centerpiece of the interface. White surface container with a subtle interior header for the category (e.g., "Caja Rápida", "Atención Comercial").
- Houses the `display-ticket` number in bold black (`#09090B`) or active violet (`#7C3AED`).
- Features a bottom ribbon with dynamic status tags: "En Espera (3 personas antes)", "Es su turno - Módulo 4".

### Chips & Badges
- **Status Pills:** Height `24px` to `28px`, uppercase `label-sm`, padding `0.25rem 0.75rem`.
  - Active: Background `#EDE9FE`, text `#6D28D9`, dot indicator `#7C3AED`.
  - Success/Serving: Background `#D1FAE5`, text `#065F46`, dot indicator `#10B981`.
  - Queued: Background `#F1F5F9`, text `#475569`.

### Input Fields
- Height `48px`, font `body-lg`. Background `#FFFFFF`, border `1px solid #CBD5E1`, text `#09090B`.
- Focus state: Border `#7C3AED`, outline `2px solid rgba(124, 58, 237, 0.20)`.
- Label placed statically above in `label-md` with `#475569`.

### Lists & Queue Monitoring Strips
- Inactive waiting list items display in concise horizontal rows (`56px` height) with tabular ticket identifiers, separated by `1px solid #F1F5F9`.
- Current position indicator uses a left-edge purple highlight bar (4px width).

### Checkboxes & Radios
- Size `20px x 20px`. Border `2px solid #CBD5E1`. Selected state: fill `#7C3AED`, white check/dot icon.