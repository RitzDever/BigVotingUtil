
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteManager<T extends Nameable> implements CommandExecutor {
	private static final String VOTE_PREFIX = ChatColor.WHITE + "["
			+ ChatColor.YELLOW + "Voter" + ChatColor.WHITE + "] "
			+ ChatColor.AQUA;
	private Map<Player, Integer> playerVotes = new HashMap<Player, Integer>();
	private List<T> arenas;
	private VoteAPI connection;

	public enum VotePermission {
		DENIED_NOT_IN_LOBBY,
		DENIED_NO_TIME,
		ALLOW
	}

	public static interface VoteAPI {
		public VotePermission getPermission(Player p);

		public float getVoteWeight(Player p);
	}

	public VoteManager(VoteAPI connection, List<T> arenas) {
		this.arenas = arenas;
		this.connection = connection;
	}

	public int getVotes(int id) {
		int count = 0;
		for (Entry<Player, Integer> vote : playerVotes.entrySet()) {
			if (vote.getValue() != null && vote.getValue().intValue() == id) {
				count += connection.getVoteWeight(vote.getKey());
			}
		}
		return count;
	}

	public void reset(Player p) {
		if (p == null) {
			playerVotes.clear();
		} else {
			playerVotes.remove(p);
		}
	}

	public T getWinningVote() {
		List<Integer> bestIDs = new ArrayList<Integer>();
		int bestCount = 0;
		for (int id = 0; id < arenas.size(); id++) {
			int count = getVotes(id);
			if (count > bestCount) {
				bestIDs.clear();
				bestIDs.add(id);
				bestCount = count;
			} else if (count == bestCount) {
				bestIDs.add(id);
			}
		}
		int id = (int) (Math.random() * arenas.size());
		if (bestIDs.size() != 0) {
			id = bestIDs.get((int) (Math.random() * bestIDs.size()));
		}
		return arenas.get(id);
	}

	public void broadcastVotes() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (connection.getPermission(p) != VotePermission.DENIED_NOT_IN_LOBBY) {
				p.sendMessage(VOTE_PREFIX + "Available arenas: ");
				Integer myVote = playerVotes.get(p);
				for (int i = 0; i < arenas.size(); i++) {
					p.sendMessage(VOTE_PREFIX
							+ ChatColor.YELLOW
							+ (i + 1)
							+ ChatColor.GOLD
							+ ": "
							+ ChatColor.YELLOW
							+ arenas.get(i).getName()
							+ ChatColor.GOLD
							+ "  (Votes: "
							+ ChatColor.YELLOW
							+ getVotes(i)
							+ ChatColor.GOLD
							+ ")"
							+ (myVote != null && myVote.intValue() == i ? " **"
									: ""));
				}
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		Player p = (Player) sender;
		VotePermission perm = connection.getPermission(p);

		if (perm == VotePermission.DENIED_NOT_IN_LOBBY) {
			p.sendMessage(VOTE_PREFIX + ChatColor.RED
					+ "You can only vote in the lobby!");
			return true;
		}
		if (args.length == 0) {
			sender.sendMessage(VOTE_PREFIX + "Available arenas: ");
			Integer myVote = playerVotes.get(p);
			for (int i = 0; i < arenas.size(); i++) {
				p.sendMessage(VOTE_PREFIX
						+ (i + 1)
						+ ": "
						+ arenas.get(i).getName()
						+ "  (Votes: "
						+ getVotes(i)
						+ ")"
						+ (myVote != null && myVote.intValue() == i ? " **"
								: ""));
			}
			return true;
		} else if (perm != VotePermission.DENIED_NO_TIME) {
			try {
				int id = Integer.valueOf(args[0]) - 1;
				if (id >= 0 && id < arenas.size()) {
					playerVotes.put(p, id);
					broadcastVotes(); // TODO Optional
					return true;
				} else {
					p.sendMessage(VOTE_PREFIX + "There is no arena #" + id
							+ ".  Type \"/vote\" to get a list of arenas.");
					return true;
				}
			} catch (Exception e) {
				StringBuilder name = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					if (i != 0) {
						name.append(" ");
					}
					name.append(args[i]);
				}
				String res = name.toString().toLowerCase();
				for (int id = 0; id < arenas.size(); id++) {
					T cfg = arenas.get(id);
					if (cfg.getName().trim().equalsIgnoreCase(res.trim())) {
						// Vote
						playerVotes.put(p, id);
						p.sendMessage(VOTE_PREFIX + "You voted for Arena #"
								+ (id + 1) + ", \"" + ChatColor.YELLOW
								+ arenas.get(id).getName() + ChatColor.GOLD
								+ "\".");
						return true;
					}
				}
				p.sendMessage(VOTE_PREFIX
						+ "There is no arena with the name \""
						+ name.toString().trim()
						+ "\".  Type \"/vote\" to get a list of arenas.");
				return true;
			}
		} else {
			p.sendMessage(VOTE_PREFIX + ChatColor.RED
					+ "You can't vote in the last 10 seconds.");
			return true;
		}
	}

}