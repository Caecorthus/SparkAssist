# SparkAssist Architecture Constitution

This document is mandatory for future agents working in this repository. It
defines the target architecture and the governance rules for changing existing
Modules. It is not blanket authorization to refactor old code.

Read `CONTEXT.md` for domain vocabulary and `ARCHITECTURE_LOGS.md` for open or
closed architecture boards before changing architecture.

## Mandatory Rules

1. Preserve SparkAssist as a client-only mod.

   - `fabric.mod.json` must stay client-only unless the owner approves a board
     that adds server-side behavior.
   - Do not add server entrypoints, login handshakes, packets, server commands,
     or server authority to solve a client assist problem.
   - Do not turn SparkAssist into the enforcement point for the Spark mod stack.

2. Treat the local caller-facing contract as stable.

   SparkAssist exposes no downstream Java API. Its stable local and user-facing
   contract includes:

   - Mod id `sparkassist`.
   - Config keys `instinctKeyMode`, legacy `eventSoundVolume`, and
     `eventSoundVolumes`.
   - Serialized option names and `EventSoundGroup` serialized names.
   - Translation keys in `en_us.json` and `zh_cn.json`.
   - User-visible option behavior and default values.
   - Optional sound identifiers used by event sound volume routing.

   Default change mode is backward-compatible extension. Changing existing
   semantics, defaults, migration behavior, translation key meaning, or
   serialized names requires an owner-approved board with downstream and user
   impact called out before editing.

3. Keep optional sibling-mod support identifier-based by default.

   SparkAssist may recognize Wathe, NoellesRoles, SparkTraits, SparkWitch,
   SparkFactionAPI, or SparkStrength concepts by stable identifiers and local
   translation/config entries. Do not add compile-time dependencies, class
   references, reflection hooks, or runtime requirements for optional sibling
   mods unless the owner approves a board that needs them.

   Identifier support listed in the event sound catalog must also appear under
   `fabric.mod.json` `suggests`. A missing optional mod must leave ordinary client
   behavior unchanged.

4. Prove unrelated roles, talents, and sounds are not affected.

   A change for one event sound, role, trait, or client option must not broaden
   into ordinary Minecraft sounds, generic role audio, unrelated SparkTraits
   talents, unrelated SparkWitch roles, or unrelated Wathe behavior.

5. Existing Modules are not free to rewrite.

   Before deleting, moving, renaming, splitting, merging, or substantially
   changing any existing source Module, first provide:

   - Board: the named architecture area being changed.
   - Reason: the friction that makes the old Module shape unsafe or costly to
     keep.
   - Old code scope: exact packages, files, and methods that will be moved,
     deleted, renamed, or rewritten.
   - New Module shape: the proposed package/Module name and intended Interface,
     including allowed responsibilities.
   - Forbidden scope: files, methods, identifiers, option semantics, config
     migration behavior, sound groups, roles, talents, and optional-mod contracts
     that must not change.
   - Behavior invariants: null behavior, defaults, fallback behavior, ordering,
     refresh behavior, client-only behavior, optional-mod behavior, and safety
     exclusions that must be preserved.
   - Downstream impact: whether sibling mods, users, configs, translations,
     builds, or release expectations must change.
   - Verification plan: exact tests, build commands, static checks, and
     cross-repo searches/checks.

   Wait for explicit owner approval before making that change.

6. `client/mixin/` classes are thin Adapters at Minecraft or Fabric Seams.

   A mixin may:

   - Locate the injection point.
   - Read the minimum required context.
   - Delegate to a SparkAssist Module.

   A mixin must not own option semantics, event sound catalog data, volume
   policy, key-toggle state, config migration, translation decisions, or
   optional-mod support rules.

7. Keep pure rules separate from client Adapters.

   Pure rules that can be tested without a Minecraft client belong under
   `src/main/java/dev/caecorthus/sparkassist/`. Client-only file IO, Minecraft
   object access, screen construction, and mixin wiring belong under
   `src/client/java/dev/caecorthus/sparkassist/client/`.

8. Code shape should stay small and single-purpose.

   - In all cases, code aesthetics, human readability, and correct functionality
     are the highest priorities. Do not game the limits by making code cramped,
     obscure, or harder to maintain.
   - A Module should have one cohesive responsibility. Split it only when the
     current Interface leaks multiple independent reasons to change.
   - Five parameters, 30-70 method lines, 100 method lines, and 200-300 class
     lines are review signals, not mechanical limits. Minecraft overrides and
     mixin signatures may be externally imposed.
   - Blank lines and comments do not count toward method, function, or class line
     limits.
   - Use Interface depth, the deletion test, leverage, and Locality to decide
     whether a split helps. Existing size alone is never permission to refactor.

9. Comments must be English and Chinese when they explain:

   - Interface semantics.
   - Minecraft or Fabric Seam behavior.
   - Mixin injection reasons.
   - Cross-mod compatibility rules.
   - Legacy retention or migration reasons.

   Do not add noise comments to self-explanatory code.

10. Tests should cross the same Interface as callers.

   Prefer testing Modules through their real Interface. Do not test private
   helper details when a caller-facing Interface can express the behavior. String
   scans of source files are allowed only as a last-resort guard for mixin/client
   behavior that cannot be exercised in plain JUnit.

11. Treat generated output as non-source.

    `build/` and `bin/` are local output. Do not use them as evidence of source
    duplication, do not stage them for architecture changes, and clean before
    treating stale `*Test 2.class` failures as logic failures.

## External Contract Matrix

| Provider | Coupling | Consumed contract | Missing/incompatible behavior |
| --- | --- | --- | --- |
| Minecraft/Fabric | Required | client lifecycle, sound, screen, and mixin Seams | Loader rejects the mod |
| Wathe | Optional identifier-only | `key.wathe.instinct` and cataloged sound ids | Assist leaves input/sound behavior unchanged |
| NoellesRoles | Optional identifier-only | cataloged sound ids | Assist leaves sound behavior unchanged |
| SparkWitch | Optional identifier-only | cataloged sound ids | Assist leaves sound behavior unchanged |
| SparkTraits | Optional identifier-only | cataloged sound ids | Assist leaves sound behavior unchanged |
| SparkStrength | Optional identifier-only | cataloged sound ids | Assist leaves sound behavior unchanged |

Do not add reflection or sibling Java imports to implement an identifier-only
contract. `verifyArchitecture` protects the client-only metadata and required
governance/test files.

## Target Architecture

Only the local Interfaces listed in this document are stable. SparkAssist does
not currently expose a downstream Java package Interface for sibling mods.

```text
src/main/java/dev/caecorthus/sparkassist/
  SparkAssist.java
  config/
    SparkAssistConfig.java
  input/
    InstinctKeyRules.java
    InstinctToggleState.java
  sound/
    EventSoundCatalog.java
    EventSoundGroup.java
    EventSoundRefreshCategories.java
    EventSoundVolumeRules.java

src/client/java/dev/caecorthus/sparkassist/client/
  SparkAssistClient.java
  config/
    SparkAssistClientSettings.java
    SparkAssistConfigManager.java
  input/
    InstinctKeyController.java
  sound/
    EventSoundVolumeController.java
  screen/
    SparkAssistOptions.java
    EventSoundOptionsScreen.java
  mixin/
    thin Minecraft/Fabric Adapters only

src/client/resources/
  sparkassist.client.mixins.json
  assets/sparkassist/lang/

src/main/resources/
  fabric.mod.json

src/test/java/dev/caecorthus/sparkassist/
  tests for config, input, and sound Interfaces
```

### `config/`

`SparkAssistConfig` owns the pure config model: defaults, JSON migration,
serialized option names, value clamping, and per-option getters/setters.

It must not know Minecraft client file paths, screens, key bindings, sound
system objects, or Fabric loader state.

### `input/`

`InstinctKeyRules` owns the exact identifier rule for Wathe's instinct key and
config-availability gating. It must not broaden to other key bindings.

`InstinctToggleState` owns pure rising-edge toggle behavior. It must stay
testable without Minecraft classes.

Client lifecycle, Minecraft key binding state, and config lookup belong in the
client input Adapter Module.

### `sound/`

`EventSoundGroup` owns the user-facing event sound group names, serialized config
names, and translation keys.

`EventSoundVolumeRules` is the stable caller-facing lookup and volume-rule
Interface inside SparkAssist. It is not a downstream Java API.
It must not own a dense inline catalog, depend on optional sibling-mod classes,
Minecraft client state, config file IO, or UI classes.

`EventSoundCatalog` owns the package-private catalog Implementation. It may map
both dotted sound-event identifiers and slash resource identifiers, but it must
keep those identifier families readable and explicit. Every identifier has
exactly one owning group; duplicate ownership must fail during catalog creation.

`EventSoundRefreshCategories` owns the pure refresh metadata for vanilla sound
categories that can contain cataloged event sounds.

Adding a new event sound group should normally mean:

- A narrowly named `EventSoundGroup` entry.
- English and Chinese translation keys.
- Exact event/resource identifiers in the event sound catalog.
- Config migration/default coverage.
- Positive tests for the new event sounds.
- Negative tests proving unrelated roles, talents, and ordinary sounds are not
  affected.

### `client/config/`

`SparkAssistConfigManager` is the file Adapter for loading and saving local
client settings. It may use Fabric loader paths and Gson. It must not own option
semantics or feature-specific side effects.

`SparkAssistClientSettings` is the client settings Adapter for screen reads and
writes. It owns save plus feature-specific reset/refresh side effects for option
changes, so screen Modules stay shallow.

### `client/input/`

`InstinctKeyController` adapts Minecraft key state to the pure Wathe instinct
rules and toggle Module. It may know client lifecycle reset points. It must not
own config serialization or unrelated key behavior.

### `client/sound/`

`EventSoundVolumeController` adapts Minecraft sound instances to event sound
rules and local config. It may know Minecraft `SoundSystem` refresh mechanics.
It must not own the catalog of optional-mod sound identifiers or the refresh
category list.

### `client/screen/`

Screen Modules build vanilla options and route option changes into
`SparkAssistClientSettings`. They must not own persistence formats, save logic,
feature-specific side effects, or event sound identifier lists.

### `client/mixin/`

Mixin Modules are Adapters. Deleting a mixin should move injection knowledge, not
SparkAssist rules. If a mixin starts needing a second unrelated behavior, stop
and request an architecture board instead of adding more logic inline.

### Resources

`sparkassist.client.mixins.json` owns mixin registration only.

Language files own user-facing strings. Every new option or event sound group
must have both English and Chinese entries unless the owner explicitly approves a
temporary missing translation.

## Verification Expectations

For documentation-only changes:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew verifyArchitecture --no-daemon --no-watch-fs --console=plain
git diff --check
```

For internal code movement with behavior preserved:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean test --no-daemon --no-watch-fs --console=plain
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build --no-daemon --no-watch-fs --console=plain
git diff --check
```

For client-only metadata, mixin Adapter, optional sibling-mod id, or cross-mod
sound/input behavior changes, also verify the relevant `fabric.mod.json`, mixin
config, language keys, config serialization, and whitelist tests. Confirm no new
hard dependencies or server handshakes were introduced without approval.

If public Interface behavior, metadata contracts, or optional sibling-mod
behavior changes, search affected sibling repos and run the smallest meaningful
downstream compile/test/build checks before claiming no downstream impact.

## Architecture Closure

There is no active structural refactor in this repository. The current target
shape is intentionally small: most changes should be local, tested extensions
rather than package moves.

### Stop Line

Do not split, move, rename, or delete Modules just because a finer shape is
possible. Future architecture work requires an owner-approved board and at least
one concrete trigger:

- A bug, crash, or compatibility issue proves the current Module shape caused
  drift.
- A new feature needs behavior at an existing Seam and would otherwise add logic
  to the wrong Module.
- Tests, build output, or this document show the Implementation has diverged
  from the rules above.

Absent one of those triggers, keep the architecture stable and make local,
task-scoped changes only.

### Architecture Logs

Open board candidates, closed-board history, and migrated, retired, or
watch-only Module details live in `ARCHITECTURE_LOGS.md`.

Read that file before touching a board or former legacy area. It records
constraints and history only; all approval and verification gates remain in this
file.

## Approval Template For Architecture Changes

Use this before modifying old architecture:

```text
Board:
Reason:
Old code scope:
New Module shape:
Forbidden scope:
Behavior invariants:
Downstream impact:
Verification plan:
Downstream notes needed:
```
