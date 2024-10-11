/*
Purpose of this is for readability. throughout Game.java we do not want to deal
with raw numbers, easier to see Direction.W, Direction.S, etc.
Therefore:
    1. Readability
    2. Error handling (INVALID)
    3. Abstraction - easier to modify if want to add new directions in future (eg. diagonal)
*/

public enum Direction {

    W(1), // west
    S(2), // south 
    E(3), // east
    N(4), // north
    QUIT(9),
    NO_MOVE(0), // refresh
    INVALID(-1);

    private final Integer direction; // stores 1 for W, 2 for S, etc
    // constructor to initialise integer associated with each direction
    Direction(Integer direction) {
        this.direction = direction;
    }
    // returns the integer associated with each direction
    public Integer getDirection() {
        return direction;
    }
    // helper to take string input, convert to integer & return direction enum constant
    public static Direction getDirection(String inputString) {
        try {
            int input = Integer.parseInt(inputString);
            return input == 1 ? W :
                   input == 2 ? S :
                   input == 3 ? E :
                   input == 4 ? N :
                   input == 9 ? QUIT :
                   input == 0 ? NO_MOVE : INVALID;
        } catch (NumberFormatException e) {
            return INVALID;
        }
    }
    
}
