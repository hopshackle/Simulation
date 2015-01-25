package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

import java.util.*;

public class TradeDecider {

	public static ArrayList<GeneticVariable> variables = new ArrayList<GeneticVariable>(EnumSet.allOf(TradeGeneticEnumAdventurer.class));

	public double getValue (Artefact item, Agent buyer, Genome g) {
		if (!(item instanceof DnDArtefact)) 
			return 0.0;
		DnDArtefact dndItem = (DnDArtefact) item;
		
		double retValue = 0;
		double functionalValue = 0;
		double tradeValue = 0;

		double temp = dndItem.getACChange(buyer);
		functionalValue += temp * g.getValue(TradeValuationsAdventurer.AC, buyer, temp, variables);
		temp = dndItem.getAvgDmgChange(buyer);
		functionalValue += temp * g.getValue(TradeValuationsAdventurer.AVG_DAMAGE, buyer, temp, variables);
		temp = dndItem.getOneOffHeal();
		functionalValue += temp * g.getValue(TradeValuationsAdventurer.HEAL, buyer, temp, variables);

		temp = dndItem.getMagicAttack();
		Character buyerAsCharacter = (Character) buyer;
		Weapon currentWeapon = buyerAsCharacter.getWeapon();
		if (currentWeapon != null)
			temp -= buyerAsCharacter.getWeapon().getMagicAttack();
		functionalValue += temp * g.getValue(TradeValuationsAdventurer.MAGIC, buyer, temp, variables);

		retValue = Math.max(functionalValue, tradeValue);

		return retValue;
	}

	public double getValueBid (Artefact item, Agent buyer, Genome g) {
		return getValue(item, buyer, g);
	}

	public double getValueOffer (Artefact item, Agent buyer, Genome g) {
		return getValue(item, buyer, g);
	}

	public void trade(Character c, Recipe recipe, int number) {
		// function unique to a trade decider
		// default function is for a Fighter
		Location l = c.getLocation();
		Market m = null;
		if (l != null) 
			m = l.getMarket();

		CombatAgent ca = c.getCombatAgent();
		ca.setCurrentCM(new CombatModifier(ca, null, null));  // for valuing items
		if (m != null) {
			for (Artefact i : m.getItems()) {
				double bidValue;
				bidValue = getValueBid(i, c, c.getGenome());

				if (bidValue > c.getGold())
					bidValue = c.getGold();
				if (bidValue > 0) {
					Bid newBid = new Bid(c, bidValue, i);
					m.addBid(newBid);
					if (c.getGold()< 1) break;
				}
			}
			// Now determine what to sell
			// Basically - keep current equipped items, plus spare weapon
			// in case of breakage
			// As a special case, if gold < 0, then sell spares
			// spare is weapon with highest dmg per round value
			boolean debugLevel = c.getDebugLocal();
			c.setDebugLocal(false);

			boolean armourFound = false, shieldFound = false, weaponFound = false;
			Weapon spareWeapon = null;
			double spareWpnDmg = 0;
			ArrayList<Artefact> itemsToSell = new ArrayList<Artefact>();
			for (Artefact i : c.getInventory()) {
				if (i instanceof Armour) {
					if (i.equals(c.getArmour())) {
						if (armourFound) {
							itemsToSell.add(i);
						}
						armourFound = true;
					} else itemsToSell.add(i);
					continue;
				}
				if (i instanceof Shield) {
					if (i.equals(c.getShield())) {
						if (shieldFound) {
							itemsToSell.add(i);
						}
						shieldFound = true;
					} else itemsToSell.add(i);
					continue;
				}
				if (i instanceof Weapon) {
					if (i.equals(c.getWeapon())) {
						if (!weaponFound) {
							weaponFound = true;
							continue;
						}
						itemsToSell.add(i);
					} else itemsToSell.add(i);

					double temp = ca.getAvgDmgPerRound(ca, (Weapon)i);
					if (temp > spareWpnDmg) {
						spareWpnDmg = temp;
						spareWeapon = (Weapon)i;
					}
					continue;
				}

				if (i instanceof Component) {
					itemsToSell.add(i);
					break;
				}

				if (i instanceof Potion) {
					// do not sell
				}
			}

			c.setDebugLocal(debugLevel);

			for (Artefact i : itemsToSell) {
				if (spareWeapon != null && i.equals(spareWeapon) && c.getGold()>0) {
					spareWeapon = null;
					continue;
				}
				// don't sell spare weapon unless destitute

				Offer o = new Offer(c, i, (m.getAveragePrice(i,5)/5.0));
				m.addItem(o);
			}

			// then we may pick a random Shield, Weapon, and Armour, and put in a bid for them
			// this is to create demand to which the Experts will respond
			Artefact randomItem = null;
			for (int loop=0; loop<4 && c.getGold() > 0; loop++) {
				switch (loop) {
				case 0:
					randomItem = Shield.values()[Dice.roll(1,Shield.values().length)-1];
					break;
				case 1:
					randomItem = Weapon.values()[Dice.roll(1,Weapon.values().length)-1];
					while (randomItem == Weapon.FIST) {
						randomItem = Weapon.values()[Dice.roll(1,Weapon.values().length)-1];
					}
					break;
				case 2:
					randomItem = Armour.values()[Dice.roll(1,Armour.values().length)-1];
					break;
				case 3: 
					randomItem = Potion.values()[Dice.roll(1, Potion.values().length)-1];
					break;
				}

				if (m.getItems().contains(randomItem))
					continue;
				// only stick in bids for non-available stuff

				double bidValue = getValueBid(randomItem, c, c.getGenome());

				if (bidValue > c.getGold())
					bidValue = c.getGold();
				if (bidValue > 0.0) {
					Bid newBid = new Bid(c, bidValue, randomItem);
					m.addBid(newBid);
				}
			}
			ca.setCurrentCM(null);
		}
	}
}
