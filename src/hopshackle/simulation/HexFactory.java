package hopshackle.simulation;

public class HexFactory {
	public Hex getHex(int i, int j) {
		return new Hex(i, j);
	}
}
