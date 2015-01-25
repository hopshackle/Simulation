package hopshackle.simulation.dnd;

import hopshackle.simulation.*;

public final class DNDArtefactUtilities {

	private DNDArtefactUtilities() {}

	public static double costToMake(Artefact i, Agent a) {

		Character c;
		if (a!= null && a instanceof Character) {
			c = (Character) a;
		} else {
			return 0.0;
		}
		double retValue = 0.0;
		Recipe r = i.getRecipe();
		for (Artefact ingredient : r.getIngredients().keySet()) {
			double number = r.getIngredients().get(ingredient);
			retValue += number * c.getValue(ingredient);
		}
		retValue += r.getGold();
		return retValue;

	}

	public static long timeToMake(Artefact i, Agent a) {
		
		Character c = (Character) a;
		Skill s =  c.getSkill(Skill.skills.CRAFT);
		if (s==null) return Long.MAX_VALUE;
		Craft craftSkill = (Craft) s;

		return craftSkill.timeToCreate(i);
	}
}
