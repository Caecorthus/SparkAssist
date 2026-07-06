# SparkAssist Architecture Logs

This file records SparkAssist architecture board history and candidate boards.

Read `ARCHITECTURE.md` first. These notes constrain future work, but they do not
authorize edits. Approval, behavior-invariant, downstream-impact, and
verification rules remain in `ARCHITECTURE.md`.

## Initial Architecture Review

SparkAssist starts from a small client-only shape:

- Pure config, input, and sound Modules live under `src/main/java`.
- Minecraft client Adapters live under `src/client/java`.
- Mixin Modules are already mostly thin Adapters.
- `EventSoundVolumeRules` was the main architecture friction because one Module
  owned cross-mod sound-event knowledge, slash resource identifiers, grouping
  policy, and volume helpers.

## Board Register

The initial boards are closed. Future boards are open candidates only and must
use the approval template in `ARCHITECTURE.md` before code changes.

### Event Sound Catalog

Status: Closed on 2026-07-06.

Closure: `EventSoundCatalog` now owns the package-private catalog
Implementation. `EventSoundVolumeRules` remains the stable lookup and volume
Interface.

Reason: `EventSoundVolumeRules` has a small caller Interface but a dense
Implementation containing Wathe, NoellesRoles, SparkTraits, and SparkWitch
sound knowledge. This weakens Locality when adding or correcting event sound
groups.

Old code scope:

- `src/main/java/dev/caecorthus/sparkassist/sound/EventSoundVolumeRules.java`
- `src/main/java/dev/caecorthus/sparkassist/sound/EventSoundGroup.java`
- `src/main/java/dev/caecorthus/sparkassist/config/SparkAssistConfig.java`
- `src/client/resources/assets/sparkassist/lang/en_us.json`
- `src/client/resources/assets/sparkassist/lang/zh_cn.json`
- `src/test/java/dev/caecorthus/sparkassist/sound/EventSoundVolumeRulesTest.java`
- `src/test/java/dev/caecorthus/sparkassist/config/SparkAssistConfigTest.java`

Possible new Module shape:

- Keep `EventSoundVolumeRules` as the stable lookup Interface unless the owner
  approves a rename.
- Move catalog data into a sound-domain Implementation shape that makes each
  group readable as event ids plus resource ids.
- Preserve enum-driven config and UI behavior unless the owner approves a
  different Interface.

Forbidden scope:

- Do not change existing serialized group names.
- Do not change existing translation keys.
- Do not remove legacy `eventSoundVolume` migration.
- Do not route ordinary Minecraft sounds, Wathe gun sounds, generic SparkTraits
  item sounds, generic SparkWitch ambience, voice, blocks, or items into event
  volume.
- Do not add class dependencies on optional sibling mods.

Behavior invariants:

- `groupFor(eventId, selectedSoundId)` prefers the event id and falls back to the
  selected sound resource id.
- Unknown ids return `null`.
- Missing config entries default to `1.0`.
- Volume values clamp to `0.0..1.0`, with `NaN` treated as `1.0`.
- Existing event groups keep their exact ids and slider behavior.

Verification plan:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean test --no-daemon --no-watch-fs --console=plain`
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build --no-daemon --no-watch-fs --console=plain`
- `git diff --check`
- Cross-repo `rg` checks against SparkTraits, SparkWitch, Wathe, and NoellesRoles
  sound registries or `sounds.json` when sound ids change.

### Client Sound Runtime

Status: Closed on 2026-07-06.

Closure: `EventSoundRefreshCategories` now owns refresh category metadata, and
the client sound runtime delegates to it instead of embedding category choices.

Reason: sound scaling crosses two Minecraft `SoundSystem` Seams plus config
lookup and refresh behavior. The current tests include a source-string guard for
refresh categories, which is a shallow test Seam.

Old code scope:

- `src/client/java/dev/caecorthus/sparkassist/client/mixin/SoundSystemMixin.java`
- `src/client/java/dev/caecorthus/sparkassist/client/sound/EventSoundVolumeController.java`
- `src/test/java/dev/caecorthus/sparkassist/sound/EventSoundVolumeRulesTest.java`

Possible new Module shape:

- Keep `SoundSystemMixin` as a thin Adapter.
- Concentrate refresh category decisions and sound-runtime routing behind a
  client sound Module Interface that tests can inspect without scanning source
  text when practical.

Forbidden scope:

- Do not change event sound group membership while working only on runtime.
- Do not change vanilla category volume semantics.
- Do not add server-side behavior.

Behavior invariants:

- Initial sound playback and already-playing volume recalculation are both
  scaled.
- Non-event sounds keep their original volume.
- Currently playing ambient, music, and player-category event sounds can refresh
  when sliders change.

Verification plan:

- Plain JUnit tests for any pure refresh metadata Interface.
- `clean test`, `clean build`, and `git diff --check`.

### Client Settings Access

Status: Closed on 2026-07-06.

Closure: `SparkAssistClientSettings` now owns settings-screen reads, writes,
saves, and option side effects. Runtime Adapters still read config where they
need Minecraft context.

Reason: multiple client Modules currently reach through the global config
manager, mutate config, save, and trigger side effects. This can become shallow
if more settings are added.

Old code scope:

- `src/client/java/dev/caecorthus/sparkassist/client/SparkAssistClient.java`
- `src/client/java/dev/caecorthus/sparkassist/client/config/SparkAssistConfigManager.java`
- `src/client/java/dev/caecorthus/sparkassist/client/screen/SparkAssistOptions.java`
- `src/client/java/dev/caecorthus/sparkassist/client/screen/EventSoundOptionsScreen.java`
- `src/client/java/dev/caecorthus/sparkassist/client/input/InstinctKeyController.java`
- `src/client/java/dev/caecorthus/sparkassist/client/sound/EventSoundVolumeController.java`

Possible new Module shape:

- Keep `SparkAssistConfig` as the pure config model.
- Add or deepen a client settings Adapter only if repeated settings make save
  and side-effect Locality worse.

Forbidden scope:

- Do not change JSON keys, defaults, migration behavior, or option labels as a
  side effect of this board.
- Do not move pure config semantics into client-only classes.

Behavior invariants:

- Config load failures fall back to defaults.
- Save failures do not crash the client settings screen.
- Changing instinct mode resets toggle state.
- Changing event sound volume refreshes currently playing event sounds.

Verification plan:

- Existing config tests plus new tests for any pure settings rules.
- `clean test`, `clean build`, and `git diff --check`.

### Wathe Instinct Toggle

Status: Closed on 2026-07-06.

Closure: `InstinctKeyRules` now owns the pure Wathe translation-key routing
rule. `InstinctKeyController` remains the client Adapter for config lookup and
toggle lifecycle.

Reason: the pure toggle Module is small and tested, but the full feature spans a
Minecraft key Seam, a hard-coded Wathe translation key, config lookup, option UI,
and disconnect reset.

Old code scope:

- `src/main/java/dev/caecorthus/sparkassist/input/InstinctToggleState.java`
- `src/client/java/dev/caecorthus/sparkassist/client/input/InstinctKeyController.java`
- `src/client/java/dev/caecorthus/sparkassist/client/mixin/KeyBindingMixin.java`
- `src/client/java/dev/caecorthus/sparkassist/client/screen/SparkAssistOptions.java`
- `src/client/java/dev/caecorthus/sparkassist/client/SparkAssistClient.java`
- `src/test/java/dev/caecorthus/sparkassist/input/InstinctToggleStateTest.java`

Possible new Module shape:

- Keep `InstinctToggleState` as the pure behavior Interface.
- Deepen only the client Adapter if lifecycle or key-binding behavior starts to
  drift.

Forbidden scope:

- Do not alter non-Wathe key bindings.
- Do not require Wathe classes at compile time.
- Do not change `HOLD` default behavior without owner approval.

Behavior invariants:

- Hold mode mirrors the physical key and clears toggle state.
- Toggle mode changes only on rising edge.
- Disconnect or relevant lifecycle reset clears toggle state.
- The feature applies only to Wathe's instinct key translation key.

Verification plan:

- Existing `InstinctToggleStateTest`.
- Add focused tests for any new pure lifecycle/key routing rules.
- `clean test`, `clean build`, and `git diff --check`.

## Closed Boards

- Event Sound Catalog.
- Client Sound Runtime.
- Client Settings Access.
- Wathe Instinct Toggle.

## Legacy Register

No retired source Modules are registered yet. The old inline catalog data inside
`EventSoundVolumeRules` is migrated to `EventSoundCatalog`; the source-string
refresh guard is migrated to `EventSoundRefreshCategoriesTest`.

## Non-Source Output Register

`bin/` and `build/` are local output trees. They are not source Modules and must
not be used as evidence of duplicate code without checking `src/` first.
