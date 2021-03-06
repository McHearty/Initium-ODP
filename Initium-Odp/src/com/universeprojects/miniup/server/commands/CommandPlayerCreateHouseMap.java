package com.universeprojects.miniup.server.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.universeprojects.cacheddatastore.CachedDatastoreService;
import com.universeprojects.cacheddatastore.CachedEntity;
import com.universeprojects.cacheddatastore.QueryHelper;
import com.universeprojects.miniup.server.ODPDBAccess;
import com.universeprojects.miniup.server.commands.framework.Command;
import com.universeprojects.miniup.server.commands.framework.UserErrorMessage;
import com.universeprojects.miniup.server.services.MainPageUpdateService;
import com.universeprojects.miniup.server.services.PropertiesService;

public class CommandPlayerCreateHouseMap extends Command
{

	/**
	 * Creates map to learn path to a players house on use.
	 * 
	 * @author spfiredrake
	 */
	
	public CommandPlayerCreateHouseMap(ODPDBAccess db, HttpServletRequest request, HttpServletResponse response)
	{
		super(db, request, response);
	}

	@Override
	public void run(Map<String, String> parameters) throws UserErrorMessage
	{
		ODPDBAccess db = getDB();
		CachedDatastoreService ds = getDS();
		CachedEntity character = db.getCurrentCharacter();
		CachedEntity user = db.getCurrentUser();
		CachedEntity location = db.getEntity((Key)character.getProperty("locationKey"));

		Long gold = (Long)character.getProperty("dogecoins");
		
		// Validation
		if(location == null) throw new RuntimeException("Character location is null");
		PropertiesService ps = new PropertiesService(db);
		if(ps.doesUserOwnHouse(location, user) == false) throw new UserErrorMessage("It's not nice to share a house location that doesn't belong to you.");
		if(location.getProperty("parentLocationKey") == null) throw new RuntimeException("Parent location null for player owned house");
		if (gold<500) throw new UserErrorMessage("You cannot create a map right now. It costs 500 gold.");
		
		
		// Get town location and house owner to put in player owned HTML.
		CachedEntity townLocation = db.getEntity((Key)location.getProperty("parentLocationKey"));
		if(townLocation == null) throw new RuntimeException("Parent location refers to null entity");

		// Get the path from this location to the town.
		List<CachedEntity> pathList = db.getPathsByLocationAndType(location.getKey(), "PlayerHouse");
		if(pathList.isEmpty()) throw new RuntimeException("No town path exists from current location!");
		CachedEntity housePath = pathList.get(0);

		// Check that we don't already have too many maps out there...
		QueryHelper q = new QueryHelper(ds);
		Long count = q.getFilteredList_Count("Item", "keyCode", FilterOperator.EQUAL, housePath.getId());
		if (count>=10)
			throw new UserErrorMessage("10 maps already exist for this location. You cannot make more.");
		
		
		// Create the item, set all properties.
		boolean reusable = "true".equals(parameters.get("reusable"));
		long itemId = ds.getPreallocatedIdFor("Item");
		CachedEntity houseMap = new CachedEntity("Item", itemId);
		houseMap.setProperty("containerKey", character.getKey());
		houseMap.setProperty("icon", "images/small/Pixel_Art-Writing-I_Map.png");
		houseMap.setProperty("description", "Map showing the location of a player's house.");
		houseMap.setProperty("itemClass", "Map");
		houseMap.setProperty("name", "Map to player house: " + location.getProperty("name"));
		houseMap.setProperty("ownerOnlyHtml", 
				"<p>This map shows the path from " + townLocation.getProperty("name") + " to " + character.getProperty("name") + "'s property " + location.getProperty("name") + ". </p>" + 
				"<p><a onclick='playerReadMap(event, " +itemId + ", " + housePath.getId() + "," + !reusable + ")'>Read map to " + location.getProperty("name") + "</a></p>");
		houseMap.setProperty("keyCode", housePath.getId());
		// Allow user to destroy the script.
		List<CachedEntity> destroyScripts = db.getFilteredList("Script", "name", "DestroyPlayerMap");
		if(destroyScripts.isEmpty() == false)
		{
			List<Key> scriptKeys = new ArrayList<Key>();
			for(CachedEntity script:destroyScripts)
			{
				if(script != null) scriptKeys.add(script.getKey());
			}
			houseMap.setProperty("scripts", scriptKeys);
		}
		//setting durability
		if (reusable == false)
		{
			//here is giving one time use map to player
			houseMap.setProperty("durability", 1L);
			houseMap.setProperty("maxDurability", 1L);
		}
		
		// Now reduce the character gold by 500g
		character.setProperty("dogecoins", gold-500L);
		
		ds.put(character);
		ds.put(houseMap);
		
		// If inventory is open, we want to refresh the popup.
		setJavascriptResponse(JavascriptResponse.ReloadPagePopup);
		
		MainPageUpdateService mpus = new MainPageUpdateService(db, user, character, location, this);
		mpus.updateMoney();
		
		// Notify the user that an item has been created.
		throw new UserErrorMessage("You have scrawled a rough map and placed it in your inventory.", false);
	}
	
	
	
	
}
