package hopshackle.simulation.test;
import static org.junit.Assert.*;
import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;

import org.junit.Test;

public class RecipeTest {


	@Test
	public void testCreation() {
		Recipe r = new Recipe(Weapon.SHORT_SWORD, 1);
		r.addIngredient(Resource.METAL, 1);
		r.addIngredient(Shield.SMALL_SHIELD, 1);
		
		assertEquals(r.getGold(), 1, 0);
		assertTrue(r.getIngredients().containsKey(Resource.METAL));
		assertEquals(r.getIngredients().get(Resource.METAL), 1, 0);
		assertFalse(r.getIngredients().containsKey(Resource.WOOD));

		r.addIngredient(Resource.METAL, 1);
		r.addIngredient(Resource.WOOD, 1);
		assertEquals(r.getGold(), 1, 0);
		assertEquals(r.getIngredients().get(Resource.METAL), 2, 0);
		assertTrue(r.getIngredients().containsKey(Resource.WOOD));
	}
}
