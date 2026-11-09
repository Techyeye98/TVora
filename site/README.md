# TVora website v2

The "alive" redesign of the TVora landing page. The original stays untouched in `..\Website`.
One static HTML page, no build step, no libraries. Open `index.html`, or run a local server:

```powershell
cd "C:\Users\namya\OneDrive\Desktop\Techyeye98\TVora\Website-v2"
python -m http.server 8080
```

Then open http://localhost:8080

## What's new compared with v1

- **Live launcher hero.** TVora's home screen is rebuilt in code instead of a screenshot: a real clock
  and greeting from the visitor's time, Inputs / Wi-Fi / headphones / Settings pills and a dock of apps.
- **Glow follows focus.** Hovering an app, or pressing the ← → arrow keys like a TV remote, moves
  focus and turns the whole hero to that app's colour, the way the launcher's context glow does.
  When nobody touches it, focus drifts across the dock on its own.
- **Clean-up moment.** The page opens on a cluttered TV; the phone taps Make it clean, a scan line
  sweeps and the launcher appears. "Show the before again" replays it.
- **Channel OSD.** Each section is a channel (CH 01 to CH 09); a TV-style badge flashes the channel
  name as it comes on screen.
- **Working switches** in What changes (CH 02): they flip off one by one to 23 on arrival, then visitors can
  tap any of them. The counter follows. Live TV/HDMI and the remote are protected: they shake and stay on.
- **Scroll reveal** like the portfolio: content rises 36px and fades in (0.8s, 80ms stagger) as it enters.
- **Live GitHub stats** in the hero: total APK downloads across all releases and the star count, read
  from GitHub's public API on each visit (cached for an hour per visitor). Falls back to the last known
  numbers (130 downloads, 5 stars on 7 Nov 2026) if GitHub can't be reached.
- **Star buttons** with the live count in the nav and the final section.
- **Direct download:** every Download button now starts the latest release's APK straight away
  (found through the API, so new versions need no website change), then shows a pop-up asking for a
  GitHub star. If the API is unreachable, the button opens the release page as before.
- **Sticky phone** in How it works changes screen as each step scrolls past (stacked on phones).
- **Channel flip** with scan-line noise when switching home screen screenshots.
- **Restore proof counts to 112/112** with a filling bar.
- **CRT power-on** reveal for the TV and phone visuals.
- Everything respects reduced motion: the page shows its finished state with no animation.

## Folder layout

```
Website-v2/
├── index.html              Whole page: markup, styles, script and the TV model list
├── README.md               This file
├── assets/images/          Same images as v1 (logo, 4 TV screenshots, 4 phone steps)
└── source-data/
    └── tv-models.json      The 248 TV models in the searchable list (already inlined)
```

## Updating

- **New release: nothing to change.** On every visit the page reads the latest GitHub release and
  fills in the version, APK size, release date, the release-notes headline (first line of the
  release body) and the direct APK link. Write a good first line in each release's notes; it shows
  in the hero pill.
- Values written in the HTML (v0.1.5, 13.8 MB, 145 downloads, 8 stars) are only the fallback when
  GitHub can't be reached. Refresh them now and then, along with `softwareVersion` / `datePublished`
  in the JSON-LD block (search engines read that one, they don't run the script).
- Dock apps in the hero: edit the five `.l-app` buttons (`data-rgb` is the glow colour).

## Publishing on GitHub Pages

Copy this folder's contents into a `site/` or `docs/` folder of the TVora repo (or a separate repo),
then GitHub → Settings → Pages → deploy from that folder.
