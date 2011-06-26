package com.temp.NPCTypes.Pirates;

import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.iConomy.util.Messaging;
import com.temp.Constants;
import com.temp.Misc.ActionManager;
import com.temp.Misc.CachedAction;
import com.temp.NPCs.NPCManager;
import com.temp.Utils.LocationUtils;
import com.temp.Utils.MessageUtils;
import com.temp.resources.redecouverte.NPClib.HumanNPC;

public class PirateTask implements Runnable {
	@Override
	public void run() {
		HumanNPC npc;
		int UID;
		Player[] online = Bukkit.getServer().getOnlinePlayers();
		for (Entry<Integer, HumanNPC> entry : NPCManager.getList().entrySet()) {
			{
				npc = entry.getValue();
				npc.updateMovement();
				UID = entry.getKey();
				for (Player p : online) {
					String name = p.getName();
					if (LocationUtils.checkLocation(npc.getLocation(),
							p.getLocation(), 0)) {
						cacheActions(p, npc, UID, name);
					} else {
						resetActions(UID, name, npc);
					}
				}
			}
		}
	}

	private void resetActions(int entityID, String name, HumanNPC npc) {
		ActionManager
				.resetAction(entityID, name, "takenItem", npc.isType("pirate"));
	}

	private void cacheActions(Player player, HumanNPC npc, int entityID,
			String name) {
		CachedAction cached = ActionManager.getAction(entityID, name);
		if (!cached.has("takenItem") && npc.isType("pirate")) {
			steal(player, npc);
			cached.set("takenItem");
		}
		ActionManager.putAction(entityID, name, cached);
	}

	/**
	 * Steal something from a player's inventory, economy-plugin account, or
	 * chest
	 * 
	 * @param player
	 * @param npc
	 */
	private void steal(Player player, HumanNPC npc) {
		Random random = new Random();
		int randomSlot;
		int count = 0;
		ItemStack item = null;
		if (npc.isType("pirate")) {
			int limit = player.getInventory().getSize();
			while (true) {
				randomSlot = random.nextInt(limit);
				item = player.getInventory().getItem(randomSlot);
				if (item != null) {
					player.getInventory().setItem(randomSlot, null);
					Messaging
							.send(player,
									ChatColor.RED
											+ "["
											+ npc.getStrippedName()
											+ "] "
											+ ChatColor.WHITE
											+ MessageUtils
													.getRandomMessage(Constants.pirateStealMessages));
					// may want to check if this returns a non-empty
					// hashmap (bandit didn't have enough room).
					npc.getInventory().addItem(item);
					break;
				} else {
					if (count >= limit) {
						break;
					}
					count += 1;
				}
			}
		}
	}
}