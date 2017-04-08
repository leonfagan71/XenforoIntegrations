package uk.co.lyncrestcreative;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionGroup;
import java.io.File;
import java.util.List;

public  class configManager {
	private static FileConfiguration MainConfig;
	private static Plugin xenforoIntegrations;
	public static  void setupConfig(){
		xenforoIntegrations = XenforoIntegrations.getPlugin(XenforoIntegrations.class);
		//Inside your onEnable
		//MainConfig = YamlConfiguration.loadConfiguration(new File(xenforoIntegrations.getDataFolder(), "config.yml"));
		MainConfig=xenforoIntegrations.getConfig();
		if (!new File(MainConfig.getCurrentPath()).exists()){
			getDefaultConfig();
			xenforoIntegrations.getConfig().options().copyDefaults(true);
			xenforoIntegrations.saveConfig();
			XenforoIntegrations.logMessage("Saved Default config");
			if (XenforoIntegrations.DEBUG){
				XenforoIntegrations.logMessage("CONFIG PATH: "+MainConfig.getCurrentPath());
			}
		}
		xenforoIntegrations.getConfig();
	}
	
	private static void getDefaultConfig(){
		
		//database
		MainConfig.addDefault("#d1", "These are database credentials. plugin will not start without these.");
		MainConfig.addDefault("db.db_host", "localhost");
		MainConfig.addDefault("db.db_port", 3306);
		MainConfig.addDefault("db.db_user", "root");
		MainConfig.addDefault("db.db_pass", "password");
		MainConfig.addDefault("db.db_name", "myDatabase");
		MainConfig.addDefault("db.db_table_name", "userRelations");
		
		//timeoutalert settings
		MainConfig.addDefault("alert.messageTimeout", 20);
		String[] alertMessages={
				"You have not signed up to the forums",
				"&bPlease sign up at https://sarcasticMC.net",
				"When this is done, please type /iam {username}"
		};
		MainConfig.addDefault("alert.message", alertMessages);
		
		//ranktable
		//MainConfig.addDefault("#c1", "Please list the ranks in the order: lowest to highest");
		String[] ranks={
			"default",
			"admin",
			"owner"
		};
		//MainConfig.addDefault("config.ranks", ranks);
		
		//UserRelations
		MainConfig.addDefault("#c2", "This only works with PEX currently :/");
		MainConfig.addDefault("config.permissionsManager", "permissionsEx");
		
		//MainConfig.addDefault("groups", );
		MainConfig.addDefault("groups.#g1", "These are pex groups, please set values to forum group ids");
		MainConfig.addDefault("groups.#g2", "any groups that you don't want to be accessable, set group number to 0");
		
		for (PermissionGroup pg : XenforoIntegrations.pexGroups){
			MainConfig.addDefault("groups.pexgroups."+pg.getName(), 0);
	    }
		//MainConfig.addDefault("groups.#g3", "These are Xenforo Ranks, this will update automatically, please use as reference only.");
		//MainConfig.addDefault("groups.xenforo", "null");


		//enablers
		MainConfig.addDefault("abilities.enabled.iam", true);
		MainConfig.addDefault("abilities.enabled.looper", true);
		MainConfig.addDefault("abilities.enabled.xi", true);
		
		//messages
		//iam
		MainConfig.addDefault("messages.iam.noperms", "&4You do not have permission to perform this command");
		MainConfig.addDefault("messages.iam.disabled", "&4This command has been disabled.");
		//noperms
		MainConfig.addDefault("messages.xi.noperms", "&4You do not have permission to perform this command");
		//ranks
		MainConfig.addDefault("messages.rank.noperms", "&4You do not have permission to perform this command");
		MainConfig.addDefault("messages.rank.hrank", "&4You're trying to set this players rank to something higher than yours.");
		MainConfig.addDefault("messages.rank.success", "&2The rank has been set.");
		MainConfig.addDefault("messages.rank.noexist", "&eThe rank you've specified does not exist.");
		
		
	}
	public static String getConfigS(String Key){
		String ret=null;
		ret=MainConfig.getString(Key);
		return ret;
	}

	public static List<String> getConfigSL(String Key) {
		// TODO Auto-generated method stub
		List<String> ret=null;
		ret=MainConfig.getStringList(Key);
		return ret;
	}
	public static Boolean getConfigB(String Key) {
		// TODO Auto-generated method stub
		boolean ret=false;
		ret=MainConfig.getBoolean(Key);
		return ret;
	}
	public static int getConfigI(String Key) {
		// TODO Auto-generated method stub
		int ret=0;
		ret=MainConfig.getInt(Key);
		return ret;
	}
	public static void setConfig(String Key, Object object) {
		MainConfig.set(Key, object);
	}

	public static void saveConfig() {
		//MainConfig.saveConfig();
		xenforoIntegrations.saveConfig();
	}
}
