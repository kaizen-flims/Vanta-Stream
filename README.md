<p align="center">
  <img src="app/src/main/res/drawable-nodpi/vanta_wordmark_speed.png" alt="Vanta" width="520" />
</p>

# Vanta Stream

**Vanta Stream** is an Android streaming interface project focused on a cinematic, modern experience with fluid motion, a dark visual system, and selective Liquid Glass interactions.

Vanta is being developed as a heavily redesigned fork of CloudStream, with the goal of preserving the flexible extension-driven foundation while rebuilding the product experience around a new identity and interface.

> Vanta Stream does not provide or host video sources by default. Functionality depends on user-installed extensions and sources.

## Vision

Vanta is designed around a few core principles:

- Cinematic, content-first browsing
- Black-first visual language with a red accent
- Liquid Glass used selectively for controls, navigation, sheets, and interactive surfaces
- Glass that visually reacts to the content beneath it rather than behaving like simple transparency
- Smooth, display-refresh-native animation with support for 60 Hz, 90 Hz, 120 Hz, and higher refresh-rate displays where available
- Responsive Android phone and TV experiences
- A clean separation between the streaming engine and the Vanta presentation layer

The aim is not to turn every surface into glass. Posters, rails, artwork, titles, and primary content should stay clean and readable while interactive UI receives the richer treatment.

## Current implementation

Vanta Stream has a Vanta-owned presentation foundation on top of the proven upstream engine. The current phone experience includes the cinematic Home hero, black/red design tokens, native-refresh motion, backdrop-reactive glass navigation and controls, Vanta search/library/result/download surfaces, player glass controls, the locked boot sequence, and Vanta launcher/TV/notification branding.

The extension/provider architecture, repositories, playback, history/data, downloads, Chromecast, package name, and legacy deep-link contracts remain compatible by design. Phone UI is the active presentation target; TV behavior is preserved while it receives measured Vanta updates.

See [docs/VANTA_ENGINEERING.md](docs/VANTA_ENGINEERING.md) for the locked design, motion, compatibility, and validation contracts.

## UI system

The reusable native presentation layer includes:

- Vanta Glass Surface
- Vanta Glass Button
- Vanta Glass Navigation
- Vanta Glass Sheets and dialogs
- Media-focused hero sections
- Cinematic horizontal content rails
- Vanta search, library, result, and download controls
- Vanta playback controls

The boot sequence is the sole intentional 60 FPS timeline. All other motion follows Android display vsync and can render at 60/90/120 Hz or higher. Glass uses hardware-backed backdrop capture and shader/color treatment; it does not perform per-frame software bitmap allocation.

## Extensions

Vanta inherits CloudStream's extension-oriented architecture.

Please respect copyright law and the terms of the services you access. Do not create or distribute extensions intended to unlawfully host or distribute copyrighted media.

## Credits

Vanta Stream is built on top of the excellent open-source work of the **CloudStream / reCloudStream contributors**.

Upstream project:
https://github.com/recloudstream/cloudstream

A huge thank you to everyone who has contributed to CloudStream and its ecosystem. Vanta would not exist without that foundation.

Vanta Stream is an independent fork and is not affiliated with or endorsed by the original CloudStream project.

## License

This project is derived from CloudStream and remains subject to the upstream project's **GNU General Public License v3.0 (GPL-3.0)**.

See the repository's license file for the complete terms.

---

### Vanta

**Engine inherited. Experience rebuilt.**
