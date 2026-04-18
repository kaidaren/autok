# Design System: Editorial Engineering

## 1. Overview & Creative North Star: "The Kinetic Architect"
This design system moves away from the "standard dashboard" aesthetic into a high-end editorial space tailored specifically for developers. The North Star is **The Kinetic Architect**: a philosophy where technical precision meets fluid, organic movement.

Instead of rigid grids and heavy borders that clutter a developer's cognitive load, we utilize **intentional asymmetry, extreme typographic contrast, and tonal layering**. The goal is to make a mobile automation console feel less like a "utility tool" and more like a high-performance flight deck. We break the "template" look by using substantial white space (the "Breathing Room" principle) and overlapping technical metadata over clean, minimalist surfaces.

---

## 2. Colors: Tonal Depth & The "No-Line" Rule
We utilize a sophisticated palette of vibrant blues and cool neutrals. The primary goal is to guide the eye through luminescence rather than structural scaffolding.

### The "No-Line" Rule
**Explicit Instruction:** Junior designers are prohibited from using 1px solid borders for sectioning or layout containment. Structural boundaries must be defined solely through:
- **Background Color Shifts:** A `surface-container-low` section sitting on a `surface` background.
- **Tonal Transitions:** Using the gradient of `surface-container-lowest` to define interactive zones.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of semi-translucent materials.
- **Base Level:** `surface` (#f5f7f9) - The canvas.
- **Sectioning:** `surface-container-low` (#eef1f3) - For large secondary content areas.
- **Actionable Containers:** `surface-container-lowest` (#ffffff) - For cards and input fields.
- **Nested Detail:** Use `surface-container-high` (#dfe3e6) inside a white card to highlight technical log snippets.

### The Glass & Signature Texture
- **Glassmorphism:** Floating action buttons and navigation overlays must use a `surface` color at 80% opacity with a `20px` backdrop blur. This allows the kinetic motion of code logs to bleed through the UI, maintaining a sense of depth.
- **Signature Gradients:** For primary CTAs (e.g., "Run Automation"), use a linear gradient: `primary` (#00E7FF) to `primary-container` (#B5F5FF) at a 135-degree angle. This adds a "jewel" quality to the vibrant blue accent.

---

## 3. Typography: Technical Authority
We pair **Space Grotesk** (Display/Headline) with **Inter** (Body) to create a "Technical Editorial" vibe.

- **Display & Headlines (Space Grotesk):** These are your "Architect" elements. Use `display-lg` and `headline-md` with tight letter-spacing (-0.02em) to create an authoritative, modern feel.
- **Body & Labels (Inter):** These are your "Precision" elements. `body-md` is the workhorse for code descriptions.
- **Technical Logs:** For code and logs, use Inter with `letter-spacing: 0.05em` or a monospaced alternative, ensuring `on-surface-variant` (#595c5e) provides enough contrast against the vibrant blue accents.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are banned. We use light and tone to imply physics.

- **The Layering Principle:** Place a `surface-container-lowest` card on a `surface-container-low` section. The change from #eef1f3 to #ffffff creates a "soft lift" that feels premium and native to the screen.
- **Ambient Shadows:** If a card must "float" (e.g., a modal or a floating controller), use an extra-diffused shadow:
    - `box-shadow: 0 12px 40px rgba(30, 41, 59, 0.06);` (using a 6% opacity of the `on-surface` Slate color).
- **The "Ghost Border" Fallback:** Where accessibility requires a container edge, use a `1px` stroke of `outline-variant` (#abadaf) at **15% opacity**. It should be felt, not seen.

---

## 5. Components: Precision Primitives

### Buttons & CTAs
- **Primary:** Gradient fill (Vibrant Blue to Light Blue). **Moderate roundedness.** No border.
- **Secondary:** `surface-container-highest` background with `on-surface` text.
- **Tertiary:** Pure text using `primary` (#00E7FF) with a `label-md` weight.

### Input Fields & Technical Logs
- **Inputs:** `surface-container-lowest` (White) background. On focus, the "Ghost Border" increases to 40% opacity in `primary` (#00E7FF).
- **Code Blocks:** Use `surface-container-high` containers. Forbid dividers between log lines; use `8px` vertical spacing (from the scale) to separate lines of data.

### Chips (Status Indicators)
- **Active State:** `primary-container` background with `on-primary-container` text.
- **Idle State:** `secondary-container` background.
- **Style:** Use `maximum` (pill-shaped) roundedness for status chips to contrast against the `moderate` roundedness of cards.

### Cards & Lists
- **Forbid dividers.** Use `surface-container-low` backgrounds to separate "Header" areas from "Content" areas within a card.
- **Asymmetric Metadata:** Place technical timestamps or IDs in `label-sm` in the top-right corner, slightly overlapping the card edge to break the "boxed-in" feel.

---

## 6. Do's and Don'ts

### Do:
- **Do** use `surface-container` tiers to create hierarchy instead of grey lines.
- **Do** embrace extreme white space between automation blocks (min 32px).
- **Do** use `primary` (Vibrant Blue) sparingly as a "laser pointer" to guide the user's focus to the most important action.
- **Do** use Glassmorphism on the mobile bottom navigation bar to maintain the "Kinetic" feel.

### Don't:
- **Don't** use 100% opaque, high-contrast borders (`#E2E8F0` at 100% is too heavy; use it at 20% or rely on surface shifts).
- **Don't** use standard "Material Design" blue for links; stay within the Vibrant Blue/Neutral family.
- **Don't** crowd the interface. If the logs are dense, the surrounding UI must be invisible.
- **Don't** use generic drop shadows. If it doesn't look like ambient light, it's too heavy.