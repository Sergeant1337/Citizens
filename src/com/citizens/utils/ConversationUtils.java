package com.citizens.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;

public class ConversationUtils {
	private static Map<String, Converser> conversations = new ConcurrentHashMap<String, Converser>();

	public static void addConverser(Player player, Converser converser) {
		if (conversations.get(player.getName()) != null) {
			player.sendMessage(ChatColor.GRAY
					+ "You can only have one chat editor open at a time.");
			return;
		}
		conversations.put(player.getName(), converser);
		converser.begin(player);
	}

	public static void onChat(PlayerChatEvent event) {
		String name = event.getPlayer().getName();
		if (conversations.get(name) != null) {
			event.setCancelled(true);
			boolean finished;
			if (ChatType.get(event.getMessage()) != null) {
				finished = conversations.get(name).special(event.getPlayer(),
						ChatType.get(event.getMessage()));
			} else {
				finished = conversations.get(name).converse(
						new ConversationMessage(event.getPlayer(), event
								.getMessage()));
			}
			if (finished)
				remove(event.getPlayer());
		}
	}

	public static void remove(Player player) {
		conversations.remove(player.getName());
	}

	public static void verify() {
		for (String name : conversations.keySet()) {
			if (Bukkit.getServer().getPlayer(name) == null) {
				conversations.remove(name);
			}
		}
	}

	public enum ChatType {
		EXIT("exit", "finish", "end"),
		UNDO("undo"),
		RESTART("restart");
		private final String[] possibilities;

		ChatType(String... string) {
			this.possibilities = string;
		}

		public static ChatType get(String message) {
			if (message.split(" ").length != 1)
				return null;
			else
				for (ChatType type : ChatType.values()) {
					if (type.possible(message.toLowerCase()))
						return type;
				}
			return null;
		}

		public boolean possible(String string) {
			for (String possibility : possibilities) {
				if (possibility.equals(string))
					return true;
			}
			return false;
		}
	}

	public static abstract class Converser {
		protected int step = 0;
		protected boolean attemptedExit = false;
		protected static final String endMessage = "Type in "
				+ StringUtils.wrap("finish") + " to end or "
				+ StringUtils.wrap("restart") + " to begin again.";

		public abstract void begin(Player player);

		public abstract boolean converse(ConversationMessage message);

		public abstract boolean allowExit();

		protected abstract void onExit();

		public boolean special(Player player, ChatType type) {
			if (type == ChatType.UNDO) {
				resetExit();
				if (step == 0)
					player.sendMessage(ChatColor.GRAY + "Nothing to undo.");
				else
					step = getUndoStep();
			} else if (type == ChatType.EXIT) {
				if (!allowExit()) {
					if (attemptedExit) {
						player.sendMessage(ChatColor.GRAY + "Exiting.");
						onExit();
						return true;
					} else if (!attemptedExit) {
						player.sendMessage(ChatColor.GRAY
								+ "Not finished yet. Do you really want to exit?");
						attemptedExit = true;
					}
				} else {
					player.sendMessage(ChatColor.GREEN + "Finished.");
					onExit();
				}
			} else if (type == ChatType.RESTART) {
				player.sendMessage(ChatColor.GRAY + "Restarted.");
				step = 0;
			}
			return false;
		}

		protected int getUndoStep() {
			return step - 1;
		}

		protected void resetExit() {
			if (this.attemptedExit)
				this.attemptedExit = false;
		}

		protected void exit(Player player) {
			special(player, ChatType.EXIT);
		}

		public String getMessage(String type, Object value) {
			return ChatColor.GREEN + "Set " + type + " to "
					+ StringUtils.wrap(value) + ".";
		}
	}

	public static class ConversationMessage {
		private final Player player;
		private final String message;
		private final String[] split;

		public ConversationMessage(Player player, String message) {
			this.player = player;
			this.message = message;
			this.split = message.split(" ");
		}

		public String getString(int index) {
			return split[index];
		}

		public String getJoinedStrings(int initialIndex) {
			StringBuilder buffer = new StringBuilder(split[initialIndex]);
			for (int i = initialIndex + 1; i < split.length; ++i) {
				buffer.append(" ").append(split[i]);
			}
			return buffer.toString();
		}

		public int getInteger(int index) throws NumberFormatException {
			return Integer.parseInt(split[index]);
		}

		public double getDouble(int index) throws NumberFormatException {
			return Double.parseDouble(split[index]);
		}

		public Player getPlayer() {
			return player;
		}

		public String getMessage() {
			return message;
		}
	}
}
