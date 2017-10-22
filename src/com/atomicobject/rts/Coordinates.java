package com.atomicobject.rts;

public class Coordinates {
    public final long x;
    public final long y;
    public Coordinates(long x, long y) {
        this.x = x;
        this.y = y;
    }
    public String toString(){
        return ""+x+","+y;
    }
    public boolean equals(Coordinates c){
        return (this.x== c.x && this.y == c.y);
    }
}
