package com.atomicobject.rts;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tile {
    Long x; //Tile coord (positive to the right “E”)
    Long y; // Tile coord (positive is down “S”)
    boolean visible; //Can currently be seen by one of your units
    boolean blocked; //Tile can be walked on by units.
    TileResource resource; //Description of the resource (if any) on this tile.
    List<Unit> enemyUnits; //Enemies found on this tile.
    Tile up;
    Tile down;
    Tile left;
    Tile right;

    public Tile getUp() {
        return up;
    }

    public void setUp(Tile up) {
        this.up = up;
    }

    public Tile getDown() {
        return down;
    }

    public void setDown(Tile down) {
        this.down = down;
    }

    public Tile getLeft() {
        return left;
    }

    public void setLeft(Tile left) {
        this.left = left;
    }

    public Tile getRight() {
        return right;
    }

    public void setRight(Tile right) {
        this.right = right;
    }

    // known tile constructor
    public Tile(JSONObject json){
        if((JSONObject) json.get("resources")!=null){
            resource = (TileResource) new TileResource((JSONObject) json.get("resources"));
        }

        x = (Long) json.get("x");
        y = (Long) json.get("y");
        visible = (boolean) json.get("visible");
        blocked = json.get("blocked")!= null ?(boolean) json.get("blocked") : false ;
         List<Unit> tentativeList = (List<Unit>) json.get("units");
        enemyUnits = tentativeList == null ? new ArrayList<>() : tentativeList;
    }

    public Tile(Long x, Long y){
        this.x = x;
        this.y = y;
        blocked = false;
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public Coordinates getCoordinates(){
        return new Coordinates(x,y);
    }

    public boolean hasResource(){
        return resource!=null;
    }

    public List<Tile> getNeighbors(){
        List<Tile> neighbors = new ArrayList<>();
        neighbors.add(right);
        neighbors.add(left);
        neighbors.add(up);
        neighbors.add(down);
        return neighbors;
    }

    public boolean equals(Tile t){
        return t.getCoordinates().equals(getCoordinates());
    }
}
