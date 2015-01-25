package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public class ChrData  {
	CharacterClass chrClass;
	long xp;
	int str, dex, con, intl, wis, cha;

	public ChrData(Character c) {
		chrClass = 	c.getChrClass();
		xp = c.getXp();
		if (xp<0) xp = 0;
		str = c.getStrength().getMod();
		dex = c.getDexterity().getMod();
		con = c.getConstitution().getMod();
		intl = c.getIntelligence().getMod();
		wis = c.getWisdom().getMod();
		cha = c.getCharisma().getMod();
	}
	public ChrData (CharacterClass chrClass, long xp, int str, int dex, int con, int intl, int wis, int cha) {
		this.chrClass = chrClass;
		if (xp < 0) xp = 0;
		this.xp = xp;
		this.str = str;
		this.dex = dex;
		this.con = con;
		this.intl = intl;
		this.wis = wis;
		this.cha = cha;
	}

	public double[] toInputArray() {
		double[] retValue = new double[6];
		retValue[0] = str*0.20;
		retValue[1] = dex*0.20;
		retValue[2] = con*0.20;
		retValue[3] = intl*0.20;
		retValue[4] = wis*0.20;
		retValue[5] = cha*0.20;

		return retValue;
	}
}