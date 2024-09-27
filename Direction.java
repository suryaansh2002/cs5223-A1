public enum Direction{
    WEST(1),
    SOUTH(2),
    EAST(3),
    NORTH(4),
    QUIT(9),
    REFRESH(0),
    INVALID(-1);

    private Integer value;

    Direction(int value){
        this.value = value;
    }

    public Integer getDirection(){
        return this.value;
    }

    public static Direction getDirection(String dString){
        if (dString == null) {
            return INVALID;
        }
        try {
            Integer d = Integer.parseInt(dString);
            for(Direction direction : Direction.values()){
                if(direction.getDirection().equals(d)){
                    return direction;
                }
            }
        } catch (NumberFormatException e) {
            // Handle the case where dString is not a valid integer
            return INVALID;
        }
        return INVALID;
    }
}