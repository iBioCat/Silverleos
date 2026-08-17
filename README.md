# Silverleos

Ancient cave chameleon-like creature (**Чешуйник**) for **ModJam 2026 — Echoes of the Past**.

A Fabric mod for Minecraft **26.1.2**, animated with **GeckoLib**.

## Stack

| Component | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.155.2+26.1.2 |
| Fabric Loom | 1.17-SNAPSHOT |
| GeckoLib | 5.5.2 (`com.geckolib:geckolib-fabric-26.1.2`) |
| Gradle | 9.5.1 |
| Java | 25 |

Minecraft 26.x uses **official Mojang mappings** (Yarn is no longer published for this version), so the
code refers to Mojang names such as `PathfinderMob`, `Level`, and `Identifier`.

## Building and running

Java 25 does not need to be installed: the Gradle toolchain resolver downloads it automatically.

```bash
./gradlew build      # produces build/libs/silverleos-1.1.0.jar
./gradlew runClient  # launches a dev client with the mod loaded
./gradlew runServer  # launches a dev server
```

On Windows use `gradlew.bat` instead of `./gradlew`.

To see the creature in game, spawn it with:

```
/summon silverleos:silverleos
```

## Project layout

```
src/main/java/com/ibiocat/silverleos/
	Silverleos.java                    # common entrypoint
	entity/SilverleosEntity.java       # PathfinderMob + GeoEntity, wandering AI
	registry/ModEntities.java          # EntityType + attribute registration

src/client/java/com/ibiocat/silverleos/client/
	SilverleosClient.java              # client entrypoint, renderer binding
	render/SilverleosModel.java        # GeckoLib model (asset path resolution)
	render/SilverleosRenderer.java     # GeckoLib entity renderer

src/client/resources/assets/silverleos/
	geckolib/models/entity/silverleos.geo.json
	geckolib/animations/entity/silverleos.animation.json
	textures/entity/silverleos.png
	lang/{en_us,ru_ru}.json
```

> **Note:** GeckoLib 5.5 loads assets from `geckolib/models/` and `geckolib/animations/`,
> not the older `geo/` and `animations/` root directories.

## Replacing the placeholder assets

The bundled model, animations, and texture are functional placeholders. To swap in real art:

1. Build the model in [Blockbench](https://blockbench.net) using the **Bedrock Entity** format.
2. Export the geometry to `geckolib/models/entity/silverleos.geo.json`.
3. Export animations to `geckolib/animations/entity/silverleos.animation.json`, keeping the
   animation names `idle` and `walk` (or update the `RawAnimation` constants in `SilverleosEntity`).
4. Replace `textures/entity/silverleos.png`.

## License

MIT — see [LICENSE](LICENSE).
