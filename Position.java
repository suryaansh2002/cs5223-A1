import java.io.Serializable;

public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
    
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Position)) {
            return false;
        }

        Position position = (Position) obj;
        return this.x == position.getX() && this.y == position.getY();
    }

    @Override
    public int hashCode() {
        return this.x * 31 + this.y;
    }
}
