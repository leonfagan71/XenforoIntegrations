package uk.co.lyncrestcreative;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class rankManager {
	private static XenforoIntegrations plugin; // pointer to your main class, unrequired if you don't need methods from the main class
	private static boolean DEBUG=XenforoIntegrations.DEBUG;
	private static void logMessage(String s){XenforoIntegrations.logMessage(s);}
	public rankManager(XenforoIntegrations plugin) {
		this.plugin = plugin;
	}
	private static Map<String, String> colors= new HashMap<String, String>();
	
	public static void getGroupColors(){
		if (true)return;
		
		
		//loop through ranks and get css properties
		try {
			Statement fUser = XenforoIntegrations.mysqlConnection.createStatement();
			String sqlQuery="SELECT  `user_group_id`,  `title`,  `display_style_priority`,  `username_css` FROM `xf_user_group`";
			ResultSet results=fUser.executeQuery(sqlQuery);
			//print stuff
			if (DEBUG){
				logMessage(sqlQuery);
				//logMessage(results.toString());
				logMessage(XenforoIntegrations.getFormattedResult(results).toString());//print json data
			}
			results.beforeFirst();
			while(results.next()){
				//do count check and shit
				Map<String, String> groupCSS=parseCSS(results.getString("username_css"));
				if (DEBUG) logMessage(groupCSS.toString());
				
				if (!(groupCSS.get("color")==null)){
					//not null
					String color=groupCSS.get("color");
					color=color.replaceAll(" ", "");
					colors.put(results.getString("user_group_id"), color);
					//configManager.setConfig("groups.xenforo."+results.getInt("user_group_id")+".color", color);
				}
				//
			}
			if (DEBUG) logMessage(colors.toString());
			configManager.saveConfig();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	
	public static boolean setRank(Player player, String rank){
		if (checkRank(rank)){
			//rank exists.
			return true;
		}
		else{
			return false;
		}
		
	}
	public static boolean checkRank(String rank){
		Statement rcheck;
		try {
			rcheck = XenforoIntegrations.mysqlConnection.createStatement();
		
			String sqlQuery="SELECT COUNT(*) FROM xf_user_group WHERE `title` like '"+rank+"'";
			ResultSet results=rcheck.executeQuery(sqlQuery);
			//print stuff
			if (DEBUG){
				logMessage(sqlQuery);
				//logMessage(results.toString());
				logMessage(XenforoIntegrations.getFormattedResult(results).toString());//print json data
			}
			results.beforeFirst();
			while(results.next()){
				int exist=results.getInt("COUNT(*)");
				if (exist==1){
					return true;
				}
				//
			}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			if (DEBUG){e.printStackTrace();}
			return false;
		}
		
		return false;
	}
	//public boolean 
	
	
	//parseCSS
	private static Map <String, String> parseCSS(String css){
	    Map <String, String> outCSS= new HashMap<String, String>();
	    if (css.length() < 6){
	    	return outCSS;
	    }
	    css=css.replace("\r", "");
	    css=css.replace("\n", "");
	    css=css.replace("\t", "");
	    String[] parts = css.split(";");
	    for (String element : parts) {
	      	//System.out.println(element);
	      	String[] cpart = element.split(":");
	      	//System.out.println(cpart[0]);
	      	//System.out.println(cpart[1]);
	      	outCSS.put(cpart[0], cpart[1]);
		}
	    return outCSS;
	  }
}
