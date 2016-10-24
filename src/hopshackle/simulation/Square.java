package hopshackle.simulation;

public class Square extends Location {

	private int x;
	private int y;
	
	public Square(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public double distanceFrom(Square s) {
		return Math.sqrt(Math.pow((this.x - s.x), 2.0) + Math.pow((this.y - s.y), 2.0));
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}

	@Override
	public void maintenance() {
		super.maintenance();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "x:"+getX() + " y:" + getY();
	}
}
