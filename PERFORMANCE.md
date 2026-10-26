# Performance findings

From a code read on 2026-10-25, not measured. Confirm with the spark profiler if needed.
Items 1–4 were fixed on 2026-10-25; 5–7 are small and left open on purpose (pick up if they ever show
up in a profile).

- [x] **1. PlacedBlocks gets slower the more you build** (`skill/PlacedBlocks.java`)
  Every place/break copies the whole per-chunk set twice (`new HashSet` + `Set.copyOf`, boxed Longs).
  Entries for blocks removed by other means (explosions, water, pistons, endermen, falling sand,
  fire, trampling) are never removed and get saved with the chunk forever.
  Fix: mutable `LongOpenHashSet` + `chunk.markUnsaved()` (not synced, so the immutable rule doesn't apply).
  DONE: mutable `LongLinkedOpenHashSet`, only logs/ores/farm blocks, max 10k per chunk (oldest forgotten).
- [x] **2. Full PlayerProgress re-sent on every bit of XP** (`Skills.addXp`, `PlayerProgress`)
  Each xp gain copies the record and `setAttached` → Fabric sends the whole record (kills, foods,
  biomes, perks, crops, bred) via NBT immediately. Timber with 64 logs = 64 full packets in one tick.
  Fix: own small attachment for skill xp, or collect changes and `setAttached` once at end of tick.
  DONE: pending map in `PamAttachments`, flushed at end of tick and on save/respawn (`PlayerListMixin`).
- [x] **3. Big lag spikes from Timber, Clear Cut and Vein Miner** (`PerkEffects`)
  Every extra block runs the full break pipeline. Clear Cut scans the tree box inflated by 7 and
  destroys possibly hundreds of leaves in one tick.
  Fix: queue the extra breaks and do 8–16 blocks per tick. #2 also makes this cheaper.
  DONE: only Clear Cut, 32 leaves per tick. Timber/Vein Miner stay instant (decided).
- [x] **4. Extra work on every monster spawn attempt** (`MobUnlocks`, `Elites.onNaturalSpawn`)
  `requiredRank` builds a registry-key string and searches lists by string per spawn group;
  locked mobs repeat vanilla's `generator.getMobsAt` structure lookup.
  Fix: `IdentityHashMap<EntityType, Integer>` of required ranks built at server start; check it first.
  DONE: rank table built in `MobUnlocks.loadConfig` (server start). Structure lookup for locked mobs stays (needed).
- [ ] **5. Shadow Step II escape scan** (`MovementPerks.shadowStepEscape`)
  Queries mobs within 64 blocks every 10 ticks per sneaking player. Fix: ~32 blocks.
- [ ] **6. Treasure Sense client scan** (`client/BlockSense`)
  Radius 24 = up to ~118k block checks every 2.5 s; possible small frame hitch deep down.
  Fix: sections nearest-first with early stop, or spread over ticks.
- [ ] **7. Small things**
  - `Progression.onAdvancementCompleted` computes `status()` twice (all ~1000+ advancements each time).
  - ~~`PerkEffects.SECOND_WIND_COOLDOWNS` never removes anyone (tiny leak).~~ DONE: `perk/Cooldown` drops
    finished cooldowns on logout and clears all on server stop.
- [ ] **8. Bestiary page requests** (`bestiary/BestiaryInfo.onRequest`)
  Every request creates a throwaway entity, even when the drops are already cached; any entity type id is
  accepted and nothing limits how often a client asks. A modified client could spam it (some modded entities do
  work in their constructor). The server also sends drop chances whatever the player's familiarity (only the
  client hides them; fine for now).
  Fix: cache the whole reply per type (traits and `canBeElite` only depend on type and config), create the entity
  only on a cache miss, ignore types without a spawn egg.
- [ ] **9. Collections checked on every kill** (`Bestiary.checkCollections`)
  For the killer and every assisting player: goes through all kill counts and tries up to 7 advancement grants.
  Cheap today; with kill chambers, skip milestones the player already has.
