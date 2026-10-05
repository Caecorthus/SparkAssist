# SparkAssist Domain Context

SparkAssist is a client-only assist mod for Spark Wathe sessions. This glossary
keeps architecture discussion tied to assist behavior instead of sibling role or
talent logic.

## Language

**Client-side assist**:
A local quality-of-life behavior that changes only the player's client experience.
_Avoid_: role feature, talent behavior, server rule

**SparkAssist client settings**:
The persisted local JSON settings for assist choices.
_Avoid_: server config, game settings

**Wathe instinct toggle**:
A client interpretation of Wathe's `key.wathe.instinct` as hold or rising-edge toggle behavior.
_Avoid_: instinct ability, highlight rule

**Event sound volume**:
A user-facing `0.0..1.0` multiplier applied only to cataloged event sounds after vanilla sound category volume.
_Avoid_: master volume, music volume, global sound volume

**Event sound group**:
A named settings row that maps one assist volume slider to one family of event sounds.
_Avoid_: sound category, channel

**Event sound catalog**:
The explicit whitelist of sound-event identifiers and selected resource identifiers that SparkAssist may scale.
_Avoid_: all mod sounds, auto-discovered sounds

**Client sound runtime**:
The client playback path where SparkAssist recognizes cataloged sounds, applies
event sound volume, and refreshes active sound categories.
_Avoid_: server sound playback, sound registration

**Optional sibling mod support**:
Identifier-based support for known Wathe, NoellesRoles, SparkWitch, and
SparkTraits sounds without owning those mods' role or talent logic.
_Avoid_: required sibling mod, shared role system

**Guidebook decoration**:
Faction-keyed ornaments drawn on and around the guide's panels and the owner info card: chapter plate,
watermark sigil, ribbon bookmark, wax seal, steam curls, rim foliage, frame plates and the directory wallpaper.
Generated as pixels from the **Round seed**, cached as dynamic textures, and never changing any geometry or hit
area. Controlled by the `guidebookDecor` (off / light / medium / full) and `guidebookFoliage` settings.
_Avoid_: theme, skin, texture pack, resource pack

**Decoration set**:
The plate, sigil, foliage, accent colour, seal mark and charm a page or a player gets. Resolved from the entry's
tab and faction group (the directory's own grouping) and overridden per member by the entry's optional JSON
`decor` object; a player's set follows their role's faction.
_Avoid_: theme, style preset

**Chapter plate**:
The 56 px duotone scene at the top of a page, above its header, that scrolls with the page; six scenes, one per
decoration set.
_Avoid_: banner, header image, cover

**No-go zone**:
A rectangle decorations may never paint over: band text, buttons, the text column, the scrollbar gutter,
directory rows, the HUD band, panel interiors and the small ornaments themselves. Enforced per pixel.
_Avoid_: hit box, obstacle (the directory's placement term)

**Round seed**:
A value fixed when a round starts that picks this round's decoration variants (stars, trees, curls, vines) and
is kept after the round ends so the lobby and title screen show the last round's look.
_Avoid_: world seed, random

**Card decoration bridge**:
SparkWitch's and SparkTraits' reflective call into SparkAssist's public `GuidebookDecorApi` at two points of
their card draw; a no-op without SparkAssist.
_Avoid_: dependency, mixin

**Safety rule for unrelated roles/talents/sounds**:
The rule that anything outside the named assist case must keep previous behavior.
_Avoid_: broad audio suppression, role rebalance, talent patch

## Relationships

- **SparkAssist client settings** store one **Wathe instinct toggle** mode and
  one **Event sound volume** per **Event sound group**.
- Each **Event sound group** owns one or more identifiers in the
  **Event sound catalog**.
- The **Client sound runtime** consults the **Event sound catalog** before
  applying any **Event sound volume**.
- **Optional sibling mod support** contributes known identifiers to the
  **Event sound catalog**; it does not make SparkAssist responsible for sibling
  role or talent state.
- The **Safety rule for unrelated roles/talents/sounds** overrides convenience:
  no catalog match means no volume change.

## Example Dialogue

> **Dev:** "Can we lower Corrupt Cop Moment music with SparkAssist?"
> **Domain expert:** "Yes, because it is an **Event sound group** backed by
> identifiers in the **Event sound catalog**. Do not lower unrelated
> NoellesRoles sounds unless they are explicitly added to that catalog."

## Flagged Ambiguities

- "Sound category" can mean vanilla categories such as `AMBIENT`, `MUSIC`, or
  `PLAYERS`; SparkAssist's user-facing concept is **Event sound group**.
- SparkTraits sounds are cataloged as **Optional sibling mod support** and are
  mirrored in `fabric.mod.json` `suggests`; this remains identifier-only and does
  not create a runtime dependency.
- "Instinct" can mean Wathe highlight behavior or SparkAssist's
  **Wathe instinct toggle**; this context uses it only for the local key mode
  assist.
