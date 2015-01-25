package hopshackle.simulation.basic;

import hopshackle.simulation.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BasicHex extends Hex {

	private int carryingCapacity;
	private int maxCarryingCapacity;
	protected static long HEX_MAINTENANCE_CONSTANT = 5000;
	private static Font villageFont = new Font("Arial", Font.PLAIN, 9);
	private static HexFactory hexFactory = new BasicHexFactory();
	private static double REFORESTATION_RATE = SimProperties.getPropertyAsDouble("ReforestationRate", "0.0");
	private static double REFORESTATION_RATE_WITHOUT_ADJACENT_FOREST = SimProperties.getPropertyAsDouble("ForestationRate", "0.0");
	private long lastMaintenance = 0;

	public BasicHex(int row, int column) {
		super(row, column);
		maxCarryingCapacity = 10;
		carryingCapacity = 10;
	}

	public int getCarryingCapacity() {
		return carryingCapacity;
	}

	public int getMaxCarryingCapacity() {
		return maxCarryingCapacity;
	}

	public void changeCarryingCapacity(int i) {
		carryingCapacity+=i;
		if (carryingCapacity <= 0) {
			carryingCapacity = 0;
			if (terrain == TerrainType.FOREST) {
				setTerrain(TerrainType.PLAINS);
				changeCarryingCapacity(maxCarryingCapacity);
			}
		}
	}
	public void changeMaxCarryingCapacity(int i) {
		maxCarryingCapacity += i;
		if (maxCarryingCapacity <0) 
			maxCarryingCapacity = 0;
		if (carryingCapacity > maxCarryingCapacity) {
			changeCarryingCapacity(maxCarryingCapacity - carryingCapacity);
		}
	}

	@Override
	public void maintenance() {
		super.maintenance();
		long currentTime = world.getCurrentTime();
		if (currentTime - lastMaintenance > HEX_MAINTENANCE_CONSTANT) {
			if (carryingCapacity == maxCarryingCapacity) 
				switch (terrain) {
				case FOREST:
					forestExpansion();
					break;
				case PLAINS:
					reforestPlains();
				default:
					break;	
				}
			if (carryingCapacity < maxCarryingCapacity)
				changeCarryingCapacity(1);
			lastMaintenance = currentTime;
		}
	}

	private void forestExpansion() {
		if (Math.random() < REFORESTATION_RATE) {
			List<BasicHex> reforestOpportunities = new ArrayList<BasicHex>();
			for (Location adjacentLocation : accessibleLocations) {
				if (adjacentLocation instanceof BasicHex) {
					BasicHex adjacentHex = (BasicHex) adjacentLocation;
					if (adjacentHex.getTerrainType() == TerrainType.PLAINS && adjacentHex.getChildLocations().isEmpty()) {
						reforestOpportunities.add(adjacentHex);
					}
				}
			}
			if (!reforestOpportunities.isEmpty()) {
				BasicHex hexToReforest = reforestOpportunities.get(Dice.roll(1, reforestOpportunities.size())-1);
				hexToReforest.setTerrain(TerrainType.FOREST);
				hexToReforest.changeCarryingCapacity(-hexToReforest.getCarryingCapacity()+1);
			} 
		}
	}

	private void reforestPlains() {
		if (Math.random() < REFORESTATION_RATE_WITHOUT_ADJACENT_FOREST) {
			setTerrain(TerrainType.FOREST);
			changeCarryingCapacity(-getCarryingCapacity()+1);
		}
	}

	public static HexFactory getHexFactory() {
		return hexFactory;
	}

	public List<Hut> getHuts() {
		List<Hut> retValue = new ArrayList<Hut>();
		Village v = getVillage();
		if (v != null) {
			retValue.addAll(v.getHuts());
		}
		for (Location l : childLocations) {
			if (l instanceof Hut) {
				retValue.add((Hut)l);
			}
		}
		return retValue;
	}

	public Village getVillage() {
		for (Location l : childLocations) {
			if (l instanceof Village) {
				return (Village)l;
			}
		}
		return null;
	}

	@Override
	public void drawContent(Graphics g) {
		Village v = getVillage();
		if (v != null) {
			g.setFont(villageFont);
			g.drawString(v.toString(), (int)(getCentralX()-cellSize/4.0), (int)getCentralY());
		} else {
			super.drawContent(g);
		}
	}

	@Override
	public void fill(Graphics g) {
		super.fill(g);
		Color overrideShading = null;
		int numberOfHuts = getHuts().size();
		int alpha = (int)(255*((double)numberOfHuts/10.0));
		if (alpha > 255) alpha = 255;
		if (numberOfHuts > 0) {
			overrideShading = new Color(156, 102, 31, alpha);
		} 
		if (overrideShading != null) {
			g.setColor(overrideShading);
			Polygon hex = createPolygonFromHex();
			g.fillPolygon(hex);
		}
	}

	@Override
	public String additionalDescriptiveText() {
		String retString = terrain.toString();
		if (getHuts().size() > 0) {
			retString = retString + "; " + getHuts().size() + " Huts present";
		}
		return retString;
	}

	@Override
	public String toString() {
		String base = super.toString();
		if (getVillage() != null) {
			return getVillage().toString() + " (" + base + ")";
		}
		return base;
	}

}

class BasicHexFactory extends HexFactory {
	public BasicHex getHex(int i, int j) {
		return new BasicHex(i, j);
	}
}
