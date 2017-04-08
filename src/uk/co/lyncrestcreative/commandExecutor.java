package uk.co.lyncrestcreative;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class commandExecutor implements CommandExecutor{
	@SuppressWarnings("unused")
	private XenforoIntegrations plugin; // pointer to your main class, unrequired if you don't need methods from the main class

	public commandExecutor(XenforoIntegrations plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	// implementation exactly as before...
		//sender.sendMessage(cmd.getName());
		if (cmd.getName().equalsIgnoreCase("iam")){
			if (!configManager.getConfigB("abilities.enabled.iam")){//command is not enabled
				sender.sendMessage(XenforoIntegrations.colorize(configManager.getConfigS("messages.iam.disabled")));
				return true;
			}
			if (!sender.hasPermission("xi.iam")){
				sender.sendMessage(XenforoIntegrations.colorize(configManager.getConfigS("messages.iam.noperms")));
				return true;
			}
			
			
			sender.sendMessage(XenforoIntegrations.colorize("&2all is good"));
		}else if(cmd.getName().equalsIgnoreCase("xi")){
			
		}else if (cmd.getName().equalsIgnoreCase("rank")){
			if (args.length==2){
				if (rankManager.checkRank(args[1])){
					sender.sendMessage("rank exist");
					if (rankManager.setRank(Bukkit.getPlayer(args[0]), args[1])){
						sender.sendMessage("set rank for player");
						return true;
					}else{
						sender.sendMessage("Failed to set player rank");
						return true;
					}
				}else{
					sender.sendMessage("rank doesn't exist");
					return true;
				}
			}else{
				sender.sendMessage("Invalid amount of args: "+args.length);
				return true;
			}
		}
		sender.sendMessage(XenforoIntegrations.colorize("&bUnknown command"));
		return true;
	}
}
