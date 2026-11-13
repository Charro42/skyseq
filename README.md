# SkySeq Macros (Fabric, Minecraft 26.3)

Custom keybinds that run a sequence of commands, key presses, mouse clicks and waits. Supports cycling
(e.g. Mouse 5 -> loadout 1, then 2, then 3, then back to 1). Everything is set up in-game; settings are
saved to `config/skyseq.json`.

Needs: Minecraft 26.3, Fabric Loader 0.19.5+, Fabric API, Java 25. Mod Menu is optional but gives you the config button.
Hypixel SkyBlock currently allows 26.2 and 26.3.

## Getting the .jar (no software needed, about 10 minutes)

1. Make a free account at github.com and sign in.
2. Click **+** (top right) -> **New repository**. Name it `skyseq`, keep it **Public**, click **Create repository**.
3. Unzip the file you were given. On the new repository page click **uploading an existing file**,
   then drag in **everything inside the unzipped folder**
   (`build.gradle`, `settings.gradle`, `gradle.properties`, `src`, `.github`, ...).
   Click **Commit changes**.
   - If the `.github` folder doesn't upload (some computers hide folders that start with a dot): click
     **Add file -> Create new file**, type `.github/workflows/build.yml` as the name (typing the slashes
     makes the folders), paste in the contents of that file, and commit.
4. Click the **Actions** tab. A run called "Build mod" starts by itself. Wait about 3-5 minutes for the green tick.
   - If it goes red, click the run, click the red step, copy the error text and send it to Claude.
5. Click the finished run, scroll to **Artifacts**, download **skyseq-jar**. Unzip it: inside is `skyseq-1.0.0+26.3.jar`.

## Installing

1. Install Fabric for 26.3 (Modrinth App / Prism / the official Fabric installer).
2. Put these three in the instance's `mods` folder: `skyseq-1.0.0+26.3.jar`, **Fabric API** (26.3) and (optional) **Mod Menu** (26.3).
3. Start the game. Open **Mod Menu -> SkySeq Macros -> the config button**.

## Using it

- The included "Loadout cycle" example (Mouse 5: `/loadout`, wait for menu, press the cycle number, Escape)
  is **switched off** until you press its OFF button. Cycle values `1,2,3` mean it uses 1, then 2, then 3, then 1 again.
  The position is saved between sessions.
- **+ New keybind** makes a new one. Click **Trigger** then press any key or Mouse 3/4/5 (left and right click are not allowed).
- Step types: Command (`/cmd` or chat text), Press key, Click, Escape (closes the open menu), Wait, Wait open (waits for any menu, or one whose title contains your text), Wait close.
- `{n}` in any text = the current cycle value (or the repeat number in Repeat mode).
- Mode: Run once / Cycle / Repeat (runs the whole sequence N times).
- Each step has a delay in ms. Everything is editable with the ^ v x buttons.
- Safeguards: only one sequence runs at a time, triggers are ignored while a menu/chat is open, there is a cooldown per keybind,
  and every "wait" times out (default 4 s).
- **Strict mode** (main screen): each keypress runs only the first action. Use it if you want to stay closer to Hypixel's one-action-per-keypress rule.

## Limits worth knowing

- Triggers are read by polling the keyboard/mouse 20 times a second, so very quick taps can be missed, and the trigger key still
  does its normal Minecraft job too. Mouse 4/5 or an unused key avoids that.
- "Click" clicks wherever the mouse cursor is. "Press key" inside a menu sends that key to the menu.
- Check Hypixel's rules on modifications before using multi-step macros on SkyBlock.

## Tweaking

Everything is in `src/main/java/dev/skyseq/`. Edit a file on GitHub (pencil icon), commit, and the Actions tab builds a new jar.
Default values: `Config.java` (`defaults()`), `Step.java`, `Macro.java`.
