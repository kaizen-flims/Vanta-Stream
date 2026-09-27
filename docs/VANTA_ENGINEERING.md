# Vanta engineering contract

This document records product decisions that are treated as implementation constraints, not open design questions.

## Identity

- The product name is **Vanta Stream**. Compact UI labels use **Vanta**.
- The only approved full wordmark is `app/src/main/res/drawable-nodpi/vanta_wordmark_speed.png`, the selected 06 Speed artwork. Do not redraw, trace, regenerate, typeset, or reinterpret it.
- The visual system is black/near-black, `#E50914` red, off-white primary text, and neutral-gray secondary text.
- No upstream branding is shown as product identity. CloudStream/reCloudStream attribution and GPL credit belong in Settings → About and project documentation.

## Motion

The boot animation has one locked sequence:

1. Black screen.
2. Red speed streaks enter.
3. Streaks converge into the locked VANTA wordmark.
4. Once the artwork is uniform, excess streaks continue behind the completed wordmark and disappear.
5. The clean wordmark holds briefly, then the app opens.

The boot timeline is intentionally quantized to a logical 60 FPS clock. This exception must not become a global frame-rate cap.

Every other animation uses Android's frame scheduler and display vsync. Do not add 16.67 ms timers, fixed-frame loops, preferred-display-mode locks, or 60 Hz caps to the app UI. Phone interactions must remain capable of 60/90/120 Hz and higher where the device supports it. Respect system animation-disable and reduced-motion behavior.

## Liquid Glass

Glass is reserved for navigation, control groups, sheets/dialogs, chips, and player controls. Posters, artwork, rails, and other content surfaces remain clear and solid.

`VantaBackdropLayout` records the composed scene into a hardware render node. `VantaGlassPainter` samples that scene behind each glass consumer, then adds adaptive tint and a specular edge. The result therefore changes with the underlying content instead of behaving as a fixed translucent rectangle.

Performance rules:

- keep the path GPU-backed;
- do not allocate or read software bitmaps per frame;
- cache geometry, paths, gradients, and reusable paint state;
- exclude the glass consumer while its backdrop is captured;
- use platform cross-window blur for dialogs on supported Android versions;
- use the opaque high-contrast fallback when accessibility settings require it;
- retain an adaptive dark fallback when backdrop capture or blur is unavailable.

## Presentation and interaction

The hierarchy is content-first: a strong Home hero, followed by recognizable horizontal content rails and readable poster grids. Interaction follows native Android and iOS-like principles: immediate feedback, predictable navigation, 48 dp primary touch targets, restrained motion, and clear selected states.

Home, search, library, result, downloads, player, dialogs, and bottom navigation share Vanta tokens and components. New screens should reuse those primitives rather than introducing local near-duplicates.

Phone is the priority presentation target. TV focus behavior, navigation routes, playback, and leanback launch behavior must continue to work while TV visuals evolve.

## Compatibility boundary

The following legacy identifiers are intentionally retained until a separately tested migration exists:

| Contract | Reason retained |
| --- | --- |
| `com.lagradost.cloudstream3` application/package namespaces | Extension ABI, stored data, update paths, external integrations |
| `cloudstreamapp`, `cloudstreamrepo`, `cloudstreamplayer`, `cloudstreamsearch`, `cloudstreamcontinuewatching`, and `csshare` schemes | Installed repositories, browser links, sharing, player launch, TV resume |
| `https://cs.repo` repository links | Existing repository ecosystem |
| Provider and plugin API packages | Binary/source compatibility for extensions |
| Existing storage keys, database models, and notification channel IDs | User history, downloads, preferences, and upgrade continuity |

These identifiers are internal compatibility infrastructure, not visible branding. Do not rename them as a cosmetic cleanup. The safe migration order is dual-read/dual-route support, tested conversion, ecosystem transition, and only then removal.

Engine behavior to preserve includes extensions/providers, repositories, playback, history and local data, downloads, Chromecast, subtitles, sync services, and Android TV integration.

## Build and validation

Use a local JDK 17 and Android SDK. GitHub Actions is not the default validation path.

```bash
./gradlew --no-configuration-cache -Pkotlin.incremental=false \
  clean :app:assembleStableDebug :app:testStableDebugUnitTest :app:lintStableDebug
```

`-Pkotlin.incremental=false` is used for the final clean pass because stale incremental Kotlin state can otherwise produce misleading duplicate-declaration failures after presentation-layer changes.

Before publishing a build:

- confirm the merged manifest label is Vanta and its launcher is `VantaBootActivity`;
- confirm the TV banner uses the locked wordmark and all launcher variants use Vanta assets;
- confirm upstream names appear in user-visible resources only inside About attribution;
- confirm the compatibility schemes and package IDs above still exist;
- install and smoke-test Home, search, library, result, playback, downloads, Chromecast entry points, dialogs, and Settings → About;
- test at 60 Hz and at least one high-refresh display mode;
- test Android 12+ blur and an older-device fallback;
- test reduced motion/high contrast and a TV focus-navigation pass.

Automated checks cannot prove refresh-rate pacing, backdrop appearance, OEM blur behavior, or remote-control focus quality. Those remain hardware release gates.
