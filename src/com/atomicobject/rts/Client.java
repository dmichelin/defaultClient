package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Client {
	
	BufferedReader input;
	OutputStreamWriter out;
	LinkedBlockingQueue<Map<String, Object>> updates;
	Map<Long, Unit> units;
	Map<Long, Unit> enemyUnits;
	int numWorkers = 6;
	int numTanks = 0;
	int numScouts = 0;
	Coordinates baseCoords = null;
	Unit base = null;
	GameMap gameMap = new GameMap();

	public Client(Socket socket) {
		updates = new LinkedBlockingQueue<Map<String, Object>>();
		units = new HashMap<Long, Unit>();
		enemyUnits = new HashMap<>();
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new OutputStreamWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		System.out.println("Starting client threads ...");
		new Thread(() -> readUpdatesFromServer()).start();
		new Thread(() -> runClientLoop()).start();
	}
	
	public void readUpdatesFromServer() {
		String nextLine;
		try {
			while ((nextLine = input.readLine()) != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> update = (Map<String, Object>) JSONValue.parse(nextLine.trim());
				updates.add(update);
			}
		} catch (IOException e) {
			// exit thread
		}		
	}

	public void runClientLoop() {
		System.out.println("Starting client update/command processing ...");
		try {
			while (true) {
				processUpdateFromServer();
				respondWithCommands();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeStreams();
	}

	private void processUpdateFromServer() throws InterruptedException {
		Map<String, Object> update = updates.take();
		if (update != null) {
			@SuppressWarnings("unchecked")
			Collection<JSONObject> mapUpdates = (Collection<JSONObject>) update.get("tile_updates");
			addMapUpdate(mapUpdates);
			//System.out.println("Processing udpate: " + update);
			@SuppressWarnings("unchecked")
			Collection<JSONObject> unitUpdates = (Collection<JSONObject>) update.get("unit_updates");
			addUnitUpdate(unitUpdates);

		}
	}

	private void addUnitUpdate(Collection<JSONObject> unitUpdates) {
		unitUpdates.forEach((unitUpdate) -> {
			Long id = (Long) unitUpdate.get("id");
			String type = (String) unitUpdate.get("type");
			if (!type.equals("base")) {
				units.put(id, new Unit(unitUpdate));
			}
		});
		numWorkers = units.values().stream().filter(unit -> unit.type.equalsIgnoreCase(Unit.Types.WORKER.toString())).collect(Collectors.toList()).size();
		numScouts = units.values().stream().filter(unit -> unit.type.equalsIgnoreCase(Unit.Types.SCOUT.toString())).collect(Collectors.toList()).size();
		numTanks = units.values().stream().filter(unit -> unit.type.equalsIgnoreCase(Unit.Types.TANK.toString())).collect(Collectors.toList()).size();
	}
	private void addMapUpdate(Collection<JSONObject> mapUpdates) {
		mapUpdates.forEach((mapUpdate) -> {
			if((boolean)mapUpdate.get("visible")){
				Tile tile = new Tile(mapUpdate);
				Collection<JSONObject> units = (JSONArray) mapUpdate.get("units");
				units.forEach((unit) ->{
					Long id = (Long) unit.get("id");
					Unit enemyUnit = new Unit(unit);
					enemyUnit.type = (String) unit.get("type");
					enemyUnit.id = id;
					enemyUnit.x = (Long) mapUpdate.get("x");
					enemyUnit.y = (Long) mapUpdate.get("y");
					enemyUnits.put(id, enemyUnit);
					if(enemyUnit.type.equalsIgnoreCase(Unit.Types.BASE.toString())){
						baseCoords = enemyUnit.getCoordinates();
						base = enemyUnit;
					}
				});
				gameMap.placeVisible(tile);
			}
		});
	}

	private void respondWithCommands() throws IOException {
		if (units.size() == 0) return;
		
		JSONArray commands = buildCommandList();		
		sendCommandListToServer(commands);
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildCommandList() {
		JSONArray commands = new JSONArray();

		for(Unit unit: units.values()){
			if(unit.type.equalsIgnoreCase(Unit.Types.WORKER.toString())){
				issueWorkerCommand(unit,commands);
			}else if(unit.type.equalsIgnoreCase(Unit.Types.SCOUT.toString())){
				issueScoutCommand(unit,commands);
			}
			issueBaseCommand(commands);

		}
		return commands;
	}

	@SuppressWarnings("unchecked")
	private void sendCommandListToServer(JSONArray commands) throws IOException {
		JSONObject container = new JSONObject();
		container.put("commands", commands);
		//System.out.println("Sending commands: " + container.toJSONString());
		out.write(container.toJSONString());
		out.write("\n");
		out.flush();
	}

	private void closeStreams() {
		closeQuietly(input);
		closeQuietly(out);
	}

	private void closeQuietly(Closeable stream) {
		try {
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void issueWorkerCommand(Unit unit,JSONArray commands){
		List<Tile> tilesWithResources = gameMap.getGameMap().values().stream().filter(tile -> tile.hasResource()).collect(Collectors.toList());
		// if there are no resources, look for them
		if(baseCoords != null && numWorkers > 30){

			if(NavigationHelper.isNextTo(unit,gameMap.getTile(baseCoords))){
				melee(unit.id,NavigationHelper.getDirection(unit,gameMap.getTile(baseCoords),gameMap),commands);
			}
			try {
				moveUnit(unit.id,NavigationHelper.navigateTo(unit,gameMap.getTile(baseCoords),gameMap),commands);
			}catch (Exception e){
				moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
			}
		}
		else if(tilesWithResources.size()==0){
			if(unit.hasResources()){
				try {
					moveUnit(unit.id,NavigationHelper.navigateTo(unit,gameMap.getTile(new Coordinates(0,0)),gameMap),commands);
				}catch (Exception e){
					moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
				}

			} else{
				List<Tile> tilesNotSeen = gameMap.getGameMap().values().stream().filter(tile -> !tile.visible).collect(Collectors.toList());
				if(tilesNotSeen.size()>0){
					try{
						moveUnit(unit.id,NavigationHelper.navigateTo(unit,NavigationHelper.findClosestTile(unit,tilesNotSeen),gameMap),commands);
					}catch (Exception e){
						moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
					}
				}else{
					moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
				}
			}
			// randomly look around, maybe change this
		}
		else {
			Tile tile =NavigationHelper.findClosestTile(unit,tilesWithResources);
			// if the unit is next to a resource and has no resources, mine them
			if(NavigationHelper.isNextTo(unit,tile)&&!unit.hasResources()){
				mineResource(unit.id,NavigationHelper.getDirection(unit,tile,gameMap),commands);
			}
			// otherwise return to base
			else if(unit.hasResources()){
				try{
					moveUnit(unit.id,NavigationHelper.navigateTo(unit,gameMap.getTile(new Coordinates(0,0)),gameMap),commands);
				}catch (Exception e){
					moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
				}

			}
			// if the unit has no resources and is not next to one, navigate to one
			else{
				try{
					moveUnit(unit.id,NavigationHelper.navigateTo(unit,tile,gameMap),commands);
				}catch (Exception e){
					moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
				}
			}
		}
	}
	public void issueScoutCommand(Unit unit,JSONArray commands){
			List<Tile> tilesNotSeen = gameMap.getGameMap().values().stream().filter(tile -> !tile.isVisible()).collect(Collectors.toList());
			if(tilesNotSeen.size()>0){
				try{
					moveUnit(unit.id,NavigationHelper.navigateTo(unit,NavigationHelper.findClosestTile(unit,tilesNotSeen),gameMap),commands);
				}catch (Exception e){
					moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
				}

			}else{
				moveUnit(unit.id,Directions.values()[(int) Math.floor(Math.random() * 4)],commands);
			}
	}

	public void issueBaseCommand(JSONArray commands){
		if(numScouts<3){
			buildScout(commands);
		}else{
			buildWorker(commands);
		}
	}

	public void moveUnit(long unitId,Directions direction, JSONArray commands){
		JSONObject command = new JSONObject();
		command.put("command", "MOVE");
		command.put("dir", direction.getJsonDirection());
		command.put("unit", unitId);
		commands.add(command);
	}

	public void mineResource(long unitId,Directions direction, JSONArray commands){
		JSONObject command = new JSONObject();
		command.put("command", "GATHER");
		command.put("dir", direction.getJsonDirection());
		command.put("unit", unitId);
		commands.add(command);
	}
	private void buildWorker(JSONArray commands){
		buildUnit(Unit.Types.WORKER,commands);
	}
	private void buildTank(JSONArray commands){
		buildUnit(Unit.Types.TANK,commands);
	}
	private void buildScout(JSONArray commands){
		buildUnit(Unit.Types.SCOUT,commands);
	}
	private void buildUnit(Unit.Types unitType, JSONArray commands){
		JSONObject command = new JSONObject();
		command.put("command", "CREATE");
		command.put("type", unitType.toString().toLowerCase());
		commands.add(command);
	}
	public void melee(long unitId,Directions direction, JSONArray commands){
		JSONObject command = new JSONObject();
		command.put("command", "MELEE");
		command.put("target", base.id);
		command.put("unit", unitId);
		commands.add(command);
	}

}
