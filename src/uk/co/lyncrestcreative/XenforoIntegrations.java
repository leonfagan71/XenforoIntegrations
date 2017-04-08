package uk.co.lyncrestcreative;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;

import com.huskehhh.mysql.mysql.MySQL;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import uk.co.lyncrestcreative.rankManager;

public class XenforoIntegrations extends JavaPlugin implements Listener{
	static Connection mysqlConnection;
	public Map<String, Boolean> playersOnline = new HashMap<String, Boolean>();
	public static Boolean DEBUG=true;
	private Boolean runLooper=false;
	public static List<PermissionGroup> pexGroups;
	public static Map<String, Integer> xenforoGroups = new HashMap<String, Integer>();
		
	@Override
	public void onEnable(){
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("PermissionsEx")){
		    PermissionManager permissions = PermissionsEx.getPermissionManager();
		    pexGroups=permissions.getGroupList();
		} else {
		 disablePlugin("PermissionsEX NOT FOUND");
		}
		
		//setup config+
		//Plugin xenforoIntegrations = configManager.getPlugin(configManager.class);
		configManager.setupConfig();
		//getServer().getPluginManager().registerEvents(new configManager(this), this);
		openConnection();
		//setup rest
		
		getServer().getPluginManager().registerEvents(this, this);
		createTables();
		getXenforoGroups();
		//rankManager.getGroupColors();
		reloadPlayers();
		//restartLooper();
		
		//setup commands
		getCommand("iam").setExecutor(new commandExecutor(this));
		getCommand("xi").setExecutor(new commandExecutor(this));
		getCommand("rank").setExecutor(new commandExecutor(this));
	}
	@SuppressWarnings("unused")
	private void restartLooper(){
		Thread thread = new Thread(new Runnable() {
            public void run() {
            	while (true){
					runLooper=false;
					messageLooper.interrupt();
					try{
	            		Thread.currentThread();
						Thread.sleep(60000);
	            		runLooper=true;
	            		messageLooper.start();
	            		return;
	            	}catch(Exception e){
	            		if (DEBUG){
	            			logMessage(e.getMessage());
	            		}
	            	}
            	}
            }
        });
		thread.start();
	}
	public void disablePlugin (String Reason){
		logMessage("Disabling the plugin: "+Reason);
		Bukkit.getPluginManager().disablePlugin(this);
	}
	
	@Override 
	public void onDisable(){
		try {
			mysqlConnection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		logMessage("Goodbye!");
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerJoinEvent(PlayerJoinEvent e){
		//ensure db connection is open.
		openConnection();
		
		//a player has joined.
		
		Player player = e.getPlayer();
		player.sendMessage("You have joined the game.");
		String playerUUID= player.getUniqueId().toString();
		String playerName= player.getName();
		
		// if player has matching forum username
		Boolean userOnForums=false;
		int userOnForumsId=0;
		try {
			Statement fUser = mysqlConnection.createStatement();
			String SQLQuery="SELECT `user_id`,  LOWER(`username`)as username , COUNT(*), `user_group_id` FROM `xf_user` WHERE username='"+playerName.toLowerCase()+"'";
			ResultSet results=fUser.executeQuery(SQLQuery);
			//print stuff
			if (DEBUG){
				logMessage(SQLQuery);
				//logMessage(results.toString());
				logMessage(getFormattedResult(results).toString());//print json data
			}
			results.beforeFirst();
			while(results.next()){
				//do count check and shit
				try{
					if(results.getInt("COUNT(*)")==1){
						userOnForums=true;
						userOnForumsId=results.getInt("user_id");
					}
				}catch(Exception e3){
					if (DEBUG){
						logMessage(e3.getMessage());
						e3.printStackTrace();
					}
				}
			}
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		
		try {
			Statement statement = mysqlConnection.createStatement();
			String sqlStatement="INSERT INTO "+configManager.getConfigS("db.db_table_name") 
			+ " (minecraft_uuid, minecraft_username";
			if (userOnForums) sqlStatement+=", forum_id ";
			sqlStatement += ") VALUES "
			+ " ('"+playerUUID+"', '"+playerName+"'";
			if (userOnForums) sqlStatement+=", '"+userOnForumsId+"' ";
			sqlStatement+= ") ON DUPLICATE KEY UPDATE "
			+ " minecraft_uuid = VALUES(minecraft_uuid),"
			+ " minecraft_username = VALUES(minecraft_username)";
			if (userOnForums) sqlStatement+=", forum_id = VALUES(forum_id)";
			sqlStatement+=";";
			
			if (DEBUG){
				logMessage(sqlStatement);
			}
			statement.executeUpdate(sqlStatement);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
			logMessage("Failed to add user ("+playerName+") to database: "+e1.getMessage());
		}
		reloadPlayers();
	}
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onquit(PlayerQuitEvent e){
		//logMessage(e.getPlayer().getName()+" has quit");
		playersOnline.remove(e.getPlayer().getName());
		
		reloadPlayers();
		
	}
	
	public void createTables(){
		//check if table exists
		try{
			Statement statement = mysqlConnection.createStatement();
			statement.executeQuery("USE `"+configManager.getConfigS("db.db_name")+"`;");
		}catch(Exception e){
			//database doesn't exist.
			try {
				Statement statement = mysqlConnection.createStatement();
				statement.executeUpdate("CREATE DATABASE "+configManager.getConfigS("db.db_name")+";");
				logMessage("Created Database.");
			}catch(Exception g){
				//failed creating the database.
				logMessage("Error: "+g.getMessage());
				disablePlugin("Failed creating the database ("+configManager.getConfigS("db.db_name")+"), do you have permission?");
				return;
			}
		}
		//create userRealtionTable
		try{
			Statement statement = mysqlConnection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `"
					+ configManager.getConfigS("db.db_table_name")+"` ("
					+ "`minecraft_uuid` VARCHAR(100) NOT NULL,"
					+ "	`forum_id` INT NULL,"
					+ "	`minecraft_username` VARCHAR(50) NULL,"
					+ "	INDEX `uuid_key` (`minecraft_uuid`),"
					+ "	UNIQUE INDEX `uuid_unique` (`minecraft_uuid`)"
					+ ") COLLATE='latin1_swedish_ci' ENGINE=InnoDB ;");
			logMessage("Table 'userRelations' exists.");
		}catch(Exception e){
				logMessage("Error: "+e.getMessage());
				disablePlugin("Failed creating the table ("+configManager.getConfigS("db.db_table_name")+"), do you have permission?");
				return;
		}
	}
	public void reloadPlayers(){
		final Connection con = mysqlConnection;
		 Thread thread = new Thread(new Runnable() {
             public void run() {
            	 playersOnline.clear();
         		for(Player on : Bukkit.getOnlinePlayers()){ //loop thru all online players
         			String playerName=on.getName();
         			try {
						Statement checkExist = con.createStatement();
						ResultSet results=checkExist.executeQuery("SELECT COUNT(*), `minecraft_username`, `forum_id` as cnt FROM `"+configManager.getConfigS("db.db_table_name")+"` WHERE `minecraft_uuid`='"+on.getUniqueId()+"' LIMIT 1");
						if (DEBUG){
							logMessage("SELECT COUNT(*), `minecraft_username`, `forum_id` FROM `"+configManager.getConfigS("db.db_table_name")+"` WHERE `minecraft_uuid`='"+on.getUniqueId()+"' LIMIT 1");
							//logMessage(results.toString());
							logMessage(getFormattedResult(results).toString());
							java.sql.ResultSetMetaData rsmd = results.getMetaData();
							int columnCount = rsmd.getColumnCount();

							// The column count starts from 1
							for (int i = 1; i <= columnCount; i++ ) {
							  String name = rsmd.getColumnName(i);
							  logMessage("COL #"+i+" : "+name);
							  // Do stuff with name
							}
						}
						results.beforeFirst();
						List<Object[]> records=new ArrayList<Object[]>();
						while(results.next()){
						    int cols = results.getMetaData().getColumnCount();
						    Object[] arr = new Object[cols];
						    for(int i=0; i<cols; i++){
						      arr[i] = results.getObject(i+1);
						    }
						    records.add(arr);
						}
						results.beforeFirst();
						while(results.next()){
							if (DEBUG){logMessage("COUNT: "+results.getInt("COUNT(*)"));}
							
							if (results.getInt("COUNT(*)")==0){
								//doesn't exist already
								if (DEBUG){
									
									logMessage("SELECT COUNT(*) as cnt, `minecraft_username`, `forum_id` FROM `"+configManager.getConfigS("db.db_table_name")+"` WHERE `minecraft_uuid`='"+on.getUniqueId()+"' LIMIT 1");
									//logMessage(results.toString());
									logMessage(getFormattedResult(results).toString());
								}
								logMessage ("User " +playerName+" doesn't exist in the database.");
								playersOnline.put(playerName, false);//if player is in database it's true
							}else{
								//exists.
								try{
									if (DEBUG){
										logMessage ("User " +playerName+" exists in the database.");
										logMessage("forum_id: "+results.getInt(3));
									}
									if (results.getInt(3)==0){
										if (DEBUG) logMessage("User isn't assigned a forum user.");
										playersOnline.put(playerName, false);
									}else{
										if (DEBUG) logMessage("User "+playerName+" has a forum account with ID of "+results.getInt(3));
										playersOnline.put(playerName, true);
									}
									//playersOnline.put(playerName, true);//if player is in database it's true
								}catch(Exception e1){
									logMessage("ERROR: "+e1.getMessage());
								}

							}
						}
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logMessage("Error on reloadPlayers:"+e.getMessage());
					}
         			
         			
         		}
            }
		 });
         thread.start();
	}
	public static void logMessage(String msg){Logger.getLogger(XenforoIntegrations.getPlugin(XenforoIntegrations.class).getName()).info("[XenForoIntegrations] "+msg);}
	@SuppressWarnings("unchecked")
	public static List<JSONObject> getFormattedResult(ResultSet rs) {
	    List<JSONObject> resList = new ArrayList<JSONObject>();
	    try {
	        // get column names
	        java.sql.ResultSetMetaData rsMeta = rs.getMetaData();
	        int columnCnt = rsMeta.getColumnCount();
	        List<String> columnNames = new ArrayList<String>();
	        for(int i=1;i<=columnCnt;i++) {
	            columnNames.add(rsMeta.getColumnName(i));
	        }

	        while(rs.next()) { // convert each object to an human readable JSON object
	            JSONObject obj = new JSONObject();
	            for(int i=1;i<=columnCnt;i++) {
	                String key = columnNames.get(i - 1);
	                String value = rs.getString(i);
	                obj.put(key, value);
	            }
	            resList.add(obj);
	        }
	    } catch(Exception e) {
	        e.printStackTrace();
	    } finally {
	    }
	    return resList;
	}
	private Thread messageLooper = new Thread(new Runnable() {
            public void run() {
            	while (runLooper){
            		if (!runLooper){
            			break;
            		}
	            	//message users that aren't a forum member.
	            	for(Player on : Bukkit.getOnlinePlayers()){ //loop thru all online players
	         			String playerName=on.getName();
	         			try{
	         				if (playersOnline.containsKey(playerName)){
	         					logMessage(playersOnline.toString());
			         			if (playersOnline.get(playerName)){
			         				if (DEBUG){
			         					logMessage(playerName+" IS ON FORUMS");
			         				}
			         			}else{//is not on forums. going to send message
			         				if (DEBUG){
			         					logMessage(playerName+" IS NOT ON FORUMS");
			         				}
			         				//on.sendMessage(playerName+" You haven't signed up on the forums?");
			         				List<String> messages=configManager.getConfigSL("alert.message");
			         				for (String message : messages){
			         					on.sendMessage(colorize(message));
			         				}
			         			}
	         				}
	         			}catch(Exception e1){
	         				if (DEBUG){
		            			logMessage(e1.getMessage());
		            		}
	         			}
	         			
	            	}
	            	try{
	            		if (configManager.getConfigI("alert.messageTimeout")>5){
	            			Thread.currentThread();
							Thread.sleep(configManager.getConfigI("alert.messageTimeout")*1000);
	            		}else{
	            			Thread.currentThread();
							Thread.sleep(20000);
	            		}
	            		
	            	}catch(Exception e){
	            		if (DEBUG){
	            			logMessage(e.getMessage());
	            		}
	            	}
            	}
            }
		});
    private void getXenforoGroups(){
    	//XenforoGroups
    	try{
	    	Statement checkExist = mysqlConnection.createStatement();
			ResultSet results=checkExist.executeQuery("SELECT  `user_group_id`,  `title` FROM `test`.`xf_user_group` order by user_group_id LIMIT 50;");
			if (DEBUG){
				logMessage("SELECT  `user_group_id`,  `title` FROM `test`.`xf_user_group` order by user_group_id LIMIT 50;");
				logMessage(getFormattedResult(results).toString());
				java.sql.ResultSetMetaData rsmd = results.getMetaData();
				int columnCount = rsmd.getColumnCount();
	
				// The column count starts from 1
				for (int i = 1; i <= columnCount; i++ ) {
				  String name = rsmd.getColumnName(i);
				  logMessage("COL #"+i+" : "+name);
				  // Do stuff with name
				}
			}
			results.beforeFirst();
			while(results.next()){
				//if (DEBUG){logMessage("COUNT: "+results.getInt("COUNT(*)"));}
				logMessage(results.getInt("user_group_id")+":"+results.getString("title"));
				String groupTitle=results.getString("title");
				//groupTitle=groupTitle.replaceAll("[^a-zA-Z0-9]", "");
				groupTitle=groupTitle.toLowerCase();
				if (DEBUG)logMessage(groupTitle);
				try{
					xenforoGroups.put(groupTitle, results.getInt("user_group_id"));
					//configManager.setConfig("groups.xenforo."+results.getInt("user_group_id")+".title", groupTitle);
				}catch (Exception y){
					if (DEBUG) y.printStackTrace();
				}
			}
			configManager.saveConfig();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			if (DEBUG) e.printStackTrace();
			logMessage("Error on getXenforoGroups:"+e.getMessage());
		}
    }
	
	public static String colorize(String s){
        if(s == null) return null;
        return s.replaceAll("&([0-9a-f])", "\u00A7$1");
    }
	private void openConnection(){
		try{
			if (mysqlConnection == null || mysqlConnection.isClosed()){
				MySQL mysql= new MySQL (
			
					this.getConfig().getString("db.db_host"),
					this.getConfig().getString("db.db_port"),
					this.getConfig().getString("db.db_user"),
					this.getConfig().getString("db.db_pass")
					);
			
				//String hostname, String port, String database,String username, String password
				try {
					mysqlConnection=mysql.openConnection();
						
				} catch (Exception e) {
					logMessage("Connection to database failed: " +e.getMessage());
					disablePlugin("Connection Failed.");
				}
			}
		}catch(Exception e){
			disablePlugin("MYSQL ERROR - "+e.getMessage());
		}
		
	}
}
