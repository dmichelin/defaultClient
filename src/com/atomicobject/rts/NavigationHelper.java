package com.atomicobject.rts;

import com.sun.istack.internal.Nullable;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class NavigationHelper {
    /**
     * WILL GET STUCK IN THE CORNERS
     * @param unit
     * @param tile
     * @param map
     * @return
     */
    public static Directions getDirection(Unit unit, Tile tile, GameMap map){
        List<Directions> listOfDirections = new ArrayList<>();
        Coordinates origin = new Coordinates(unit.x.intValue(),unit.y.intValue());
        Coordinates destination = new Coordinates(tile.x.intValue(),tile.y.intValue());
        Coordinates navVector = new Coordinates(destination.x-origin.x,destination.y-origin.y);
        // if the navigation vector in the y direction is positive, go north
        Tile targetTile;
        if(navVector.y<0){
            targetTile = map.getTile(new Coordinates(origin.x,origin.y-1));
            if(targetTile!=null){
                listOfDirections.add(Directions.NORTH);
            }
        }
        //if the navigation vector in the y direction is negative, go south
        else if(navVector.y>0){
            targetTile = map.getTile(new Coordinates(origin.x,origin.y+1));
            if(targetTile!=null){
                listOfDirections.add(Directions.SOUTH);
            }
        }
        else if(navVector.x>0){
            targetTile = map.getTile(new Coordinates(origin.x+1,origin.y));
            if(targetTile!=null){
                listOfDirections.add(Directions.EAST);
            }
        }
        else if(navVector.x<0){
            targetTile = map.getTile(new Coordinates(origin.x-1,origin.y));
            if(targetTile!=null){
                listOfDirections.add(Directions.WEST);
            }
        }
        return listOfDirections.size()> 0 ? listOfDirections.get(0): Directions.NONE ;
    }
    @Nullable
    public static Tile findClosestTile(Unit unit, Collection<Tile> tiles){
        long shortest = Long.MAX_VALUE;
        Tile shortestTile = null;
        for(Tile tile: tiles){
            long disty = Math.abs(tile.y-unit.y);
            long distx = Math.abs(tile.x-unit.x);
            if(shortest >= distx+disty){
                shortest= distx+disty;
                shortestTile = tile;
            }
        }
        return shortestTile;
    }

    public static boolean isNextTo(Unit unit, Tile tile){
        long disty = Math.abs(tile.y-unit.y);
        long distx = Math.abs(tile.x-unit.x);

        return distx+disty == 1 ||distx+disty == 1 ;
    }


    public static Directions navigateTo(Unit unit, Tile tile, GameMap map) throws Exception{
        Queue<Tile> frontier = new ConcurrentLinkedDeque<>();
        HashMap<String, Tile> cameFrom = new HashMap<>();
        frontier.add(map.getTile(unit.getCoordinates()));
        while(!frontier.isEmpty()){
            Tile current = frontier.remove();

            if(current.equals(tile)){
                break;
            }

            for(Tile neighbor : getNeighbors(current,map)){
                if(!cameFrom.containsKey(neighbor.getCoordinates().toString())){
                        if(!neighbor.blocked||neighbor.equals(tile)) frontier.add(neighbor);
                    cameFrom.put(neighbor.getCoordinates().toString(),current);
                }
            }
        }
        return getDirection(unit, reconstruct(cameFrom,map.getTile(unit.getCoordinates()),tile),map);
    }

    public static List<Tile> getNeighbors(Tile tile,GameMap map){
        List<Tile> neighbors = new ArrayList<>();
        neighbors.add(map.getTile(new Coordinates(tile.x-1,tile.y)));
        neighbors.add(map.getTile(new Coordinates(tile.x+1,tile.y)));
        neighbors.add(map.getTile(new Coordinates(tile.x,tile.y-1)));
        neighbors.add(map.getTile(new Coordinates(tile.x,tile.y+1)));

        return neighbors;
    }

    private static Tile reconstruct(HashMap<String,Tile> cameFrom,Tile tile, Tile end) throws Exception{
        List<Tile> directions = new ArrayList<>();
        Tile currentTile = end;
        while(currentTile!=tile){
            if(currentTile==null){
                throw new Exception();
            }
                directions.add(cameFrom.get(currentTile.getCoordinates().toString()));
                currentTile=cameFrom.get(currentTile.getCoordinates().toString());
        }
        return directions.get(directions.size()-2);
    }

    public static List<Unit> enemiesInRange(Unit unit, Collection<Unit> enemies){
        List<Unit> unitsInRange = enemies.stream().filter(unit1 -> (Math.abs(unit.x-unit1.x)<3&&(Math.abs(unit.y-unit1.y)<3))&&(Math.abs(unit.x-unit1.x)>0&&(Math.abs(unit.y-unit1.y)>0))).collect(Collectors.toList());
        return unitsInRange;
    }
}
