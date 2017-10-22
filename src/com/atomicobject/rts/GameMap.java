package com.atomicobject.rts;

import java.util.HashMap;

public class GameMap {
    HashMap<String,Tile> gameMap;

    public GameMap() {
        gameMap = new HashMap<>();
    }

    public HashMap<String, Tile> getGameMap() {
        return gameMap;
    }

    public void setGameMap(HashMap<String, Tile> gameMap) {
        this.gameMap = gameMap;
    }

    public boolean placeTile(Tile tile){
        boolean allNull = true;
        // get left, right, up, and down
        Tile up = gameMap.get(new Coordinates(tile.x,tile.y+1).toString());
        Tile down = gameMap.get(new Coordinates(tile.x,tile.y-1).toString());
        Tile left = gameMap.get(new Coordinates(tile.x-1,tile.y).toString());
        Tile right = gameMap.get(new Coordinates(tile.x+1,tile.y).toString());

        // update up, down, left, and right
        if(up!=null){
            allNull = false;
            up.setDown(tile);
        }
        if(down!=null){
            allNull = false;
            down.setUp(tile);
        }
        if(left!=null){
            allNull = false;
            left.setRight(tile);
        }
        if(right!=null){
            allNull = false;
            right.setLeft(tile);
        }

        gameMap.put(new Coordinates(tile.x,tile.y).toString(),tile);

        return !allNull;
    }
    public void placeVisible(Tile t){
        t.visible = true;
        gameMap.put(new Coordinates(t.x,t.y).toString(),t);

    }

    public Tile getTile(Coordinates coordinates){
        Tile t =gameMap.get(coordinates.toString());
        if(t== null){
            gameMap.put(coordinates.toString(),new Tile(coordinates.x,coordinates.y));
        }
        return gameMap.get(coordinates.toString());
    }
}

