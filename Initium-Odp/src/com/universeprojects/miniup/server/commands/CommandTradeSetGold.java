package com.universeprojects.miniup.server.commands;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.universeprojects.cacheddatastore.CachedDatastoreService;
import com.universeprojects.cacheddatastore.CachedEntity;
import com.universeprojects.miniup.server.NotificationType;
import com.universeprojects.miniup.server.ODPDBAccess;
import com.universeprojects.miniup.server.TradeObject;
import com.universeprojects.miniup.server.commands.framework.Command;
import com.universeprojects.miniup.server.commands.framework.UserErrorMessage;

public class CommandTradeSetGold extends Command {
	
	public CommandTradeSetGold(ODPDBAccess db, HttpServletRequest request, HttpServletResponse response)
	{
		super(db, request, response);
	}
	
	public void run(Map<String,String> parameters) throws UserErrorMessage {
		
		ODPDBAccess db = getDB();
		CachedDatastoreService ds = getDS();
		TradeObject tradeObject = TradeObject.getTradeObjectFor(ds, db.getCurrentCharacter(request));
		String dogecoinStr = parameters.get("amount");
		CachedEntity character = db.getCurrentCharacter(request);
		Key otherCharacter = (Key) character.getProperty("combatant");
        
        Long dogecoin = null;
        try {
        	dogecoinStr = dogecoinStr.replace(",", "");
            dogecoin = Long.parseLong(dogecoinStr);
        	}
        	catch (Exception e){
        		new UserErrorMessage("Please type a valid gold amount.");
        	}
        	
        
        db.setTradeDogecoin(ds, db.getCurrentCharacter(request), dogecoin);
        db.sendNotification(ds,otherCharacter,NotificationType.tradeChanged);
        
        Integer tradeVersion = tradeObject.getVersion();
        addCallbackData("tradeVersion",tradeVersion);
	}
}