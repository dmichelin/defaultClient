package com.atomicobject.rts;

public enum Directions {
        NORTH("N"),
        SOUTH("S"),
        EAST("E"),
        WEST("W"),
        NONE("");
        private String jsonDirection;
        Directions(String jsonDirection){
            this.jsonDirection = jsonDirection;
        }

    public String getJsonDirection() {
        return jsonDirection;
    }
}
