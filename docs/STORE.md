# Connecting a store

Donor ranks and donor cosmetics are never earned in game. They are handed out with one console command, so any store that can run a console command when someone pays can sell them. Tebex is the usual one for Minecraft and is what this guide uses.

## What the plugin already does

```
a cosmetic <player> give <id>
a cosmetic <player> take <id>
```

`<id>` is a donor rank, a cosmetic or the second suffix slot. It works from the console, the player has to be online, and giving something twice does nothing. `take` is for refunds and chargebacks.

## What can be sold

| Kind | Ids |
|---|---|
| Donor ranks | `luminary`, `celestial`, `nova`, `zenith` |
| Second suffix slot | `suffix_slot_2` |
| Donor suffixes, by package | Luminary: `suffix_supporter`, `suffix_donor`, `suffix_investor`, `suffix_vip`. Celestial: `suffix_rich`, `suffix_patron`, `suffix_vip_plus`, `suffix_mvp`. Aurora: `suffix_mvp_plus`, `suffix_maestro` |
| Bold suffixes | `suffix_immortal`, `suffix_supreme`, `suffix_ultimate`, `suffix_apex`, `suffix_nexus`, `suffix_radiant`, `suffix_paragon`, `suffix_divine`, `suffix_eternal`, `suffix_ascendant`, and the others marked "From a donor package" in `/customise` |
| Chat and name colours | `chat_<colour>` and `name_<colour>`, every colour but the default |
| Join messages | `join_legend`, `join_royal`, `join_storm`, `join_star`, `join_aurora` |
| Trails | `trail_soul`, `trail_electric`, `trail_glow`, `trail_totem`, `trail_end` |
| Death messages | `death_reign`, `death_aurora` |

In game, `/a cosmetic <player> give ` then Tab lists every id exactly as the plugin spells it. Check an id there before putting it in a package.

## Setting up Tebex

1. Make an account at tebex.io and create a store for a Minecraft: Java Edition server.
2. In the Tebex panel, under Game Servers, add a server and copy its secret key.
3. Download the Tebex plugin for Paper from Tebex and put the jar in the host's `plugins/` folder, then restart.
4. In the server console run `tebex secret <key>` with the key from step 2. Do this in the console, never in chat.
5. In the panel, create a package for each thing you sell. Under its commands add one line per item, with "require the player to be online" switched on:

   ```
   a cosmetic {username} give nova
   a cosmetic {username} give suffix_slot_2
   a cosmetic {username} give suffix_mvp
   ```

6. For refunds and chargebacks, add the same lines with `take` under the package's refund and chargeback commands.
7. Buy a package yourself with a 100% off coupon and watch the console: each command should answer `Gave ... to <name>`.

Players open the store with Tebex's own `/buy`. It is already on the list of commands non-staff see when they press Tab.

## Rules to know before selling

- Minecraft's commercial usage guidelines allow selling cosmetics and ranks that do not change how the game plays. Everything in the table above is cosmetic. Do not sell Essence, resources, tool levels or anything that gives an advantage.
- What goes in which package, and the prices, are yours to decide. Nothing in the plugin depends on it.
- `/a reset <player> all` wipes donor ranks and cosmetics too. After a wipe, Tebex can resend a player's purchases from the panel.
