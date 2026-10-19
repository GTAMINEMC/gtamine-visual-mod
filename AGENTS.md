# Project rules

- Target Fabric and Minecraft 1.21.11, Java 21. Client-side only.
- Keep features in separate modules; use Fabric events before considering mixins.
- All features must stay inactive outside the approved server domain policy,
  including singleplayer. Clear session state on disconnect.
- Agree every new user-facing setting with the user before implementation.
- Put future keybindings in a dedicated GTAMINE Visual Controls category.
- Do not add placeholder gameplay features or settings to the base mod.
- HUD personalization must remain strictly client-side. Do not modify BetterHud,
  server plugins or resource packs, and do not introduce a HUD server bridge.
  The presence module only advertises the gtamine:visual plugin channel on allowed
  servers. Do not exchange versions or other data, or add a Bukkit library.
  NMinimap uses a native GUI texture composed from server map pixels and marker
  resources. Do not restore world core shader hooks or lightmap encoding: those
  conflict with Iris/OptiFine shader packs. Preserve normal rendering for
  unrecognized maps, unsupported resources and inactive sessions.
- BetterHud layout IDs are unavailable to the client: describe spatial grouping
  honestly, and preserve vanilla rendering for unsupported shader formats.
- Run `gradlew.bat build` after source changes.
