# Heaven's Collapse

A Paper plugin for **Minecraft Java 1.21.11** that adds a custom Mace,
**Heaven's Collapse**, with a charge-based one-shot lightning ability.

## Project structure

```
HeavensCollapse/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/heavenscollapse/
        │       ├── HeavensCollapsePlugin.java
        │       ├── HeavensCollapseItem.java
        │       ├── commands/
        │       │   └── HeavensCollapseCommand.java
        │       ├── listeners/
        │       │   ├── CombatListener.java
        │       │   ├── ItemSwitchListener.java
        │       │   ├── LightningObtainListener.java
        │       │   └── PlayerQuitListener.java
        │       └── util/
        │           ├── AbilityManager.java
        │           └── EffectsUtil.java
        └── resources/
            ├── plugin.yml
            └── config.yml
```

## Building via GitHub Actions (no local Maven needed)

This repo includes `.github/workflows/build.yml`, which builds the plugin
on GitHub's own servers every time you push. You never need Maven or a
JDK installed locally:

1. Create a new (empty) repository on GitHub.
2. Push this project to it:
   ```bash
   cd HeavensCollapse
   git init
   git add .
   git commit -m "Heaven's Collapse plugin"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
3. On GitHub, open the **Actions** tab. A "Build HeavensCollapse" run
   starts automatically. Once it finishes (green check), click into the
   run and download the **HeavensCollapse** artifact under "Artifacts" -
   that zip contains the compiled `.jar`, ready to drop into `plugins/`.
4. Optional - get it as a proper GitHub Release instead of an artifact
   zip: tag a commit and push the tag, e.g.
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   The workflow will attach `HeavensCollapse-1.0.0.jar` directly to a
   Release on the **Releases** page - a stable, permanent download link.

Any time you edit the code and push again, a fresh build (and artifact)
is produced automatically - no local build step at all.

## Building locally instead (optional)

If you'd rather build on your own machine: requires **JDK 21** and Maven.

```bash
cd HeavensCollapse
mvn clean package
```

The compiled plugin will be at:

```
target/HeavensCollapse-1.0.0.jar
```

## Installing

1. Stop your Paper 1.21.11 server.
2. Copy `HeavensCollapse-1.0.0.jar` into the server's `plugins/` folder.
3. Start the server. A default `config.yml` will be generated at
   `plugins/HeavensCollapse/config.yml`.

## Commands

| Command | Description |
|---|---|
| `/heavenscollapse` | Gives yourself the Heaven's Collapse mace. |
| `/heavenscollapse <player>` | Gives the mace to another online player. Supports tab-completion of online player names. |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `heavenscollapse.give` | `op` | Required to run `/heavenscollapse`. |

## How the three-hit system works

- Each player has their **own independent hit counter** (tracked by UUID),
  stored in `AbilityManager`. Hitting with Heaven's Collapse as Player A
  never affects Player B's counter.
- The three hits **must land on the same target, back to back**. If your
  mace connects with a different entity than the one your streak was
  building against, the streak resets to 1 against that new target -
  hits on different victims never add up toward each other. Landing all
  `hits-required` hits on one target in a row is what "charges" the
  attempt.
- On the Nth hit, the counter **resets to 0 immediately**, whether or not
  the special attack actually goes off. The Nth hit is a *checked attempt*:
  - If the **wielder** is airborne → Heaven's Collapse triggers:
    guaranteed kill, lightning, sounds and particles.
  - If the **wielder** is standing on the ground → the attempt fizzles and
    the hit is treated as a completely normal mace hit. The next cycle of
    `hits-required` hits starts fresh.
- **Ground detection** uses `Entity#isOnGround()`, which is Paper/Spigot's
  own server-side ground-collision flag (updated every tick from actual
  block collision), rather than checking Y velocity - so being at the top
  of a jump with zero vertical velocity, or falling slowly, still counts
  as airborne. **The check is on the wielder (the attacking player), not
  the target** - Heaven's Collapse activates only if the player
  themselves is airborne (jumping, falling, knocked back into the air,
  etc.) at the moment of the charged hit; if they're standing on solid
  ground, the attempt fizzles regardless of what the target is doing.
- **Idle reset:** if a player goes longer than `idle-reset-seconds`
  (default `30`) without landing a mace hit, their counter silently resets
  to 0 the next time they do hit something.
- **Switching away:** by default (`reset-on-item-switch: false`),
  switching your held hotbar slot away from Heaven's Collapse does
  **not** reset the streak - only the idle timeout above does. This
  matters for playstyles like mace-elytra combat, where you naturally
  touch other hotbar slots (equipping an elytra, grabbing rockets or a
  pearl) between swings without meaning to abandon your streak. Set
  `reset-on-item-switch: true` for the stricter behaviour where merely
  touching another slot resets the counter immediately.
- **Disconnects:** a player's counter is removed entirely when they leave
  the server, so no stale state accumulates over time.

## How the special attack guarantees a kill (and respects Totem of Undying)

The kill goes through Minecraft's **normal damage pipeline** instead of
bypassing it, specifically so Totem of Undying keeps working correctly.
`CombatListener` does two things on the triggering hit:

1. Caps the target's current health at **one heart** (`setHealth`,
   never healing them if they're already lower).
2. Sets that same hit's damage to **20 hearts** (`event.setDamage(40.0)`).

Even after armor, enchantment protection and resistance reduce that 40
damage, the remainder still vastly exceeds the one heart of health left,
so the kill is effectively guaranteed - but because it's still a genuine
`EntityDamageByEntityEvent` (not a direct health override), a totem in
the target's off-hand pops exactly as it would for any other lethal hit:
it cancels the death, consumes itself, and leaves the target at 1 HP with
the usual totem effects and sound.

- Armor, resistance, and absorption hearts do **not** prevent the kill
  (they reduce the 40 damage, but nowhere near enough to matter).
- Totem of Undying **is respected** - a target holding one survives, as
  it should.
- Death messages and kill credit still correctly attribute to the
  attacker, since this is an ordinary player-caused damage event.
- The ability **never triggers against the attacker themselves** and only
  ever targets `LivingEntity` instances.

## How the lightning-obtaining mechanic works

The original request describing this mechanic was ambiguous ("if a player
is holding the mace / is in the required 'mace' state when struck by
lightning"). The implemented interpretation:

> If a player is struck by lightning while holding a **plain, vanilla
> Mace** in either hand, that specific Mace transforms in place into
> Heaven's Collapse.

This is enabled/disabled via `lightning-obtain.enabled` in `config.yml`.
If `lightning-obtain.negate-damage` is `true` (default), the lightning
damage that triggered the transformation is cancelled, so the player
isn't punished for being "chosen."

**Duplication safety:** the mechanic *transforms* an existing plain Mace
rather than adding a new item. Once transformed, the item is no longer a
"plain Mace" (`HeavensCollapseItem#isPlainMace` returns `false` for it),
so even if the lightning event were somehow processed twice for the same
strike, the second pass finds nothing left to transform and does nothing.

If a player isn't holding a plain Mace when struck by lightning, nothing
special happens - lightning behaves completely normally.

## Configuration reference (`config.yml`)

```yaml
hits-required: 3          # Hits needed to trigger a charged attempt.
idle-reset-seconds: 30    # Seconds of inactivity before the counter resets.
reset-on-item-switch: false  # true = touching another hotbar slot also resets the streak.

lightning:
  enabled: true            # Master switch for the lightning visual/strike.
  damage: false            # true = a real lightning bolt (can hurt/ignite nearby blocks/entities).
                            # false = visual-only bolt (strikeLightningEffect), no damage or fire.
  fire: false               # Only used when damage: true - clears fire left near the strike.

effects:
  particles: true
  sounds: true

lightning-obtain:
  enabled: true
  negate-damage: true       # Cancel the lightning damage that grants the weapon.

messages:
  # ... color-coded ('&') message strings, see the shipped config.yml.
```

The plugin uses safe defaults for every value (`getInt`/`getBoolean` with
fallbacks), so a malformed or partially-edited `config.yml` will never
crash the plugin - missing or invalid keys just fall back to their
default.

## Known nuances

- Bukkit's `ArmorStand` technically implements `LivingEntity`, so a
  charged hit against an armor stand that is airborne (e.g. mid-fall)
  will trigger the special. This matches vanilla's own type hierarchy and
  is left as-is rather than special-cased.
- The item is marked unbreakable and uses a Paper-only visual enchant
  glint override (`ItemMeta#setEnchantmentGlintOverride`) instead of a
  real enchantment, so nothing about its appearance can interfere with
  damage calculation.
- Heaven's Collapse always comes with a **max-level Density enchant**
  baked in the moment it's created - via `/heavenscollapse` or by
  transforming a plain Mace with lightning - so it's never a "weak"
  version of the weapon.
- **Anvil protection**: combining an enchanted book, repairing, or
  renaming Heaven's Collapse in an anvil is monitored by
  `AnvilProtectionListener`. If the anvil's result item ever comes out
  without the Heaven's Collapse identity (name/lore/glint/unbreakable/PDC
  tag) - which can happen if another plugin on the server rebuilds the
  result item as part of its own enchanting or attribute system - this
  listener re-stamps the identity onto the result while leaving whatever
  the anvil actually changed (new enchant, repaired durability, new name)
  intact. This covers the standard vanilla/Paper anvil case completely.
  If a server has a fully custom enchanting/attribute-swap GUI that
  bypasses the anvil inventory entirely (not a `PrepareAnvilEvent`), that
  plugin would need its own compatibility handling for custom items -
  outside what this plugin can control from the outside.

## Verifying the ability is working

With `debug.actionbar: true` (the default), the attacker sees a live
action-bar readout on every mace hit:

- `Heaven's Collapse: 1/3`, `2/3`, `3/3`
- On the 3rd hit: either **"HEAVEN'S COLLAPSE!"** (it fired) or
  **"Heaven's Collapse fizzled - you must be airborne to trigger it."**

That last message is the most common source of "it's not doing anything"
reports: **the special only checks on the Nth hit, and only fires if the
WIELDER is airborne at that exact moment** - jump, fall, or get knocked
into the air yourself right as you land the charged hit. The target's own
position doesn't matter. Once you've confirmed it works, set
`debug.actionbar: false` for normal play.

## Cinematic effects

`EffectsUtil` plays the full "divine strike" presentation when Heaven's
Collapse activates (each layer independently toggleable via
`effects.sounds` / `effects.particles`):

- **Sounds**: lightning thunder + impact, a heavy mace smash, a beacon
  chime, the Trident's Channeling thunder for a second storm rumble, and
  the Warden's sonic boom layered in as a distinct signature sound - all
  vanilla, so no resource pack is required.
- **Particles**: a flash, END_ROD light rays, electric sparks, cloud
  burst, a dense Sculk Charge Pop flash right at the impact point, and a
  Sonic Boom particle (with its own built-in expanding-ring animation),
  plus a spark trail connecting the attacker to the target.
- **Warden burst**: rings alternating between Sculk Soul wisps and Sculk
  Charge Pop motes animate outward from the impact point over eight
  ticks, like a shockwave, followed by a second, smaller Sonic Boom pulse
  a few ticks later for a "one-two punch" feel. Implemented as short
  repeating/delayed tasks rather than single spawns, so the particles
  visibly travel outward instead of just appearing already spread out.

## Troubleshooting

**`mvn clean package` fails to resolve `io.papermc.paper:paper-api`**
Make sure you have an internet connection and that
`https://repo.papermc.io/repository/maven-public/` isn't blocked by a
firewall/proxy. This repository is declared in `pom.xml` and is required
to download the Paper API.

**Compilation errors about `release 21`**
You need JDK 21 specifically (`java -version`). Paper 1.21.x requires
Java 21; older JDKs will fail to compile or run the plugin.

**Plugin loads but `/heavenscollapse` says "Unknown command"**
Check the server log on startup for a warning from HeavensCollapse about
failing to register the command - this almost always means `plugin.yml`
wasn't packaged correctly. Verify `plugin.yml` is present in the built jar
under its root (unzip the jar and check).

**The special attack never triggers**
- Confirm you're hitting with the mace in your **main hand** specifically.
- Confirm **you (the wielder)** are actually airborne at the moment of the
  charged hit - jump and swing mid-air, or hit while falling. The
  target's position doesn't matter, only yours. A wielder standing on
  even a thin block, slab, or fence post still counts as "on the ground."
- Confirm `hits-required` in the config matches what you expect, and that
  you haven't gone idle past `idle-reset-seconds` between hits (switching
  hotbar slots no longer resets it by default - see
  `reset-on-item-switch`).
- Turn on `debug.actionbar: true` and watch the action bar - it tells you
  exactly whether each hit is being counted and why the Nth hit did or
  didn't fire.

**Real lightning damages/burns things I didn't want it to**
Set `lightning.damage: false` (the default) to use the purely visual
`strikeLightningEffect` instead of a real lightning bolt.

**Getting duplicate items from the lightning-obtain mechanic**
This shouldn't happen given the transform-in-place design described
above; if you suspect another plugin is also listening to
`EntityDamageEvent` with cause `LIGHTNING` and interacting with the same
item, please report the exact reproduction steps.
