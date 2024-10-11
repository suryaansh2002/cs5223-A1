/*
Purpose 
    - represents a coordinate on a grid with x and y values
    - implements Serializable, allowing its instances to be serialized and 
    deserialized, useful for storing and retrieving game state.
 */

import java.io.Serializable;

public class Position implements Serializable{

    private Integer x; // X-coordinate on the grid
    private Integer y; // Y-coordinate on the grid


    // Constructor to initialize Position with x and y coordinates
    public Position(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    // Getter for the x-coordinate
    public Integer getX() {
        return x;
    }

    // Setter for the x-coordinate
    public void setX(Integer x) {
        this.x = x;
    }

    // Getter for the y-coordinate
    public Integer getY() {
        return y;
    }

    // Setter for the y-coordinate
    public void setY(Integer y) {
        this.y = y;
    }

    // Checks if two Position objects are equal based on their x and y coordinates
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // If the objects are the same instance
        if (o == null || getClass() != o.getClass()) return false; // If the objects are different types

        Position position = (Position) o; // Cast the object to Position

        // Check if x and y coordinates are equal
        if (x != null ? !x.equals(position.x) : position.x != null) return false;
        return y != null ? y.equals(position.y) : position.y == null;
    }
}
