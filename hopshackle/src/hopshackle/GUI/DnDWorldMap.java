package hopshackle.GUI;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class DnDWorldMap extends SimpleAnimationPanel {

	private static final long serialVersionUID = 1L;
	private World w;
	private SquareMap worldMap;
	private int cellSize;	// Pixels per Square
	
	public DnDWorldMap(World w) {
		super();
		this.w = w;
		LocationMap tempMap = w.getLocationMap();
		if (tempMap instanceof SquareMap) {
			worldMap = (SquareMap)tempMap;
		} else {
			throw new AssertionError("Must have a World with a SquareMap");
		}
		cellSize = 50;
		setToolTipText("Initial");
		
		this.setDoubleBuffered(true);
		
		this.setMillisecondsPerFrame(2000);
		this.start();
	}

	@Override
	public void drawFrame(Graphics g) {

		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.drawRect(0,0, getWidth(), getHeight());

		List<Location> allLocations = w.getChildLocations();
		for (Location l : allLocations) {
			if (l instanceof Square) {
				Square s = (Square) l;
				g.setColor(new Color(((s.getX() + s.getY())*10), 0, 0, (s.getX() + s.getY())*10));
				// Color is red in proportion to the challenge rating

				g.fillRect(s.getX()*cellSize, s.getY()*cellSize, cellSize, cellSize);

				List<Agent> arrAg = s.getAgents();
				int totalPop = 0;
				int parties = 0;
				for (Agent a : arrAg) {
					if (a instanceof Party) {
						parties++;
						totalPop = totalPop + ((Party)a).getSize();
					} else {
						if (((Character)a).getParty() == null)
							totalPop++; // else already included in totalPop
					}
				}
				if (totalPop > 0) {
					g.setColor(Color.BLACK);
					String text = Integer.toString(totalPop)+ "/" + Integer.toString(parties);
					char[] cA = text.toCharArray();
					g.drawChars(cA, 0, cA.length, s.getX()*cellSize, s.getY()*cellSize+cellSize/2);
				}
			}
		}

	}

	public int getCellSize() {
		return cellSize;
	}
	public void setCellSize(int cellSize) {
		this.cellSize = cellSize;
	}

	private Square getSquare(int x, int y) {
		int s_x = x / cellSize;
		int s_y = y / cellSize;
		return worldMap.getSquareAt(s_x, s_y);
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		int newX = e.getX();
		int newY = e.getY();
		Square s = getSquare(newX, newY);
		if (s != null) {
			String retStr;
			retStr = "<html> " + s.toString() + "<br>CR: " + (s.getX() + s.getY());
			List<Agent> arrAg = s.getAgents();
			for (Agent a : arrAg) {
				if (a instanceof Party) {
					retStr = retStr + "<br>" + a.toString();
					for (Character member : ((Party)a).getMembers()) {
						retStr= retStr + "<br> -  " + member.toString();
					}
				}
				if (a instanceof Character) {
					if (((Character)a).getParty() == null) {
						retStr = retStr + "<br>" + a.toString();
					}
				}
			}			
			return retStr + "</html>";

		} else return "Null";
	}
}