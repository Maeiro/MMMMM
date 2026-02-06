Sync Client Server (SCS)
========================

NeoForge mod that keeps client mods in sync with a server-provided modpack.
The goal is to maintain server-client compatibility with a single in-game
Update button.

Fork notice
-----------
This project is a fork of `https://github.com/Place-Boy/MMMMM`.

Credits
-------
- Original project by Place-Boy: `https://github.com/Place-Boy`

Key features
------------
- In-game update button on the multiplayer server list.
- Confirmation screen (Yes/No) before starting the update, with quick access to cache cleanup.
- modId-based sync: avoids duplicates when the .jar filename changes.
- Optional /config update (enabled by default).
- Optional removal list in `mods.zip` to delete specific client jars.
- Optional mirror mode to make client `/mods` and `/config` 1:1 with the zips.
- Per-server cache isolation (each server has its own checksums and downloaded zips).
- Built-in file server to host `mods.zip` and `config.zip`.

Server usage
------------
1) Start the server with the mod installed.
2) Generate the packages:
   - `/scs save-mods` -> creates `SCS/shared-files/mods.zip`
   - `/scs save-config` -> creates `SCS/shared-files/config.zip`
   - (optional) Add `modsToRemoveFromTheClient.json` to `mods.zip` to remove client jars
3) The embedded file server runs on the `fileServerPort` value.

Notes:
- The commands bundle *all* mods/configs at once. You can also create `mods.zip`
  and `config.zip` manually if you want to ship only specific files.
- You can also include an optional `modsToRemoveFromTheClient.json` inside `mods.zip`
  to delete specific jars from the client.

Client usage
------------
1) Open the server list and edit the target server.
2) In **Download URL**, enter the server file host (IP or URL).
   - Example: `127.0.0.1:25566` or `http://myserver:25566`
3) Return to the list and click **Update**.
4) Confirm the update (Yes/No). Use **Clear cache** if you need to reset cached zips/checksums.

Mod configuration
-----------------
Config file (COMMON): `config/scs-common.toml`

- `fileServerPort` (int): file server port.
- `updateConfig` (bool): updates `/config` alongside `/mods` (default: true).
- `mirrorMods` (bool): mirrors `/mods` to `mods.zip` (removes files not in the zip).
- `mirrorConfig` (bool): mirrors `/config` to `config.zip` (removes files not in the zip).

How updates work
----------------
- The client downloads `mods.zip` and extracts it into `/mods`.
- For each .jar, the mod reads its `modId` and removes any older version of the same mod,
  even if the filename is different.
- If `modsToRemoveFromTheClient.json` exists in `mods.zip`, any jar listed there is
  removed from `/mods` during the update.
- If `updateConfig=true`, it also downloads `config.zip` and extracts it into `/config`.
- If `mirrorMods` or `mirrorConfig` is enabled, files not present in the zip are removed to keep the client 1:1.
- Update UI shows summary and details with scroll support for long change lists.

Removal list format
-------------------
Create `modsToRemoveFromTheClient.json` inside `mods.zip` with a plain JSON array
of jar file names:

```json
[
  "banana1.jar",
  "banana2.jar"
]
```

Tips / Troubleshooting
----------------------
- If `fileServerPort` changes, the file server restarts automatically.
- If the download has no progress/ETA, the server may be missing `Content-Length`.
- If a server URL is missing, the update flow will show a message instead of starting.
- Use **Clear cache** to remove cached zips/checksums if something gets stuck.

Cache layout
------------
Each server has its own cache folder:
- `SCS/servers/<server-id>/shared-files/`
- `SCS/servers/<server-id>/mods_checksums.json`
- `SCS/servers/<server-id>/config_checksums.json`
