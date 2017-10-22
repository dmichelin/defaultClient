package com.atomicobject.rts;

import org.json.simple.JSONObject;

public class TileResource {
    int id; //Unique identifier for this resource.
    String type; //Time of resource (small or large)
    int total; //Total amount of value left in this resource.
    int value; //Value of a single harvested load.

    public TileResource(JSONObject json){
        id = ((Long) json.get("id")).intValue();
        type = (String) json.get("type");
        total = ((Long) json.get("total")).intValue();
        value = ((Long) json.get("value")).intValue();

    }
}
