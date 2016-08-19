package hopshackle.simulation;

import java.util.HashMap;

public class Recipe {

	private HashMap<Artefact, Double> ingredients;
	private double gold;
	private Artefact product;
	
	public Recipe(Artefact a, double gold) {
		product = a;
		this.gold = gold;
		ingredients = new HashMap<Artefact, Double>();
	}
	
	public void addIngredient(Artefact ingredient, double number) {
		if (ingredients.containsKey(ingredient)) {
			double currentNumber = ingredients.get(ingredient);
			ingredients.put(ingredient, number + currentNumber);
		} else {
			ingredients.put(ingredient, number);
		}
	}
	
	public HashMap<Artefact, Double> getIngredients() {
		return ingredients;
	}
	
	public double getGold() {return gold;}
	
	public String toString() {return "Recipe for " + product.toString();}

	public Artefact getProduct() {
		return product;
	}
	
}
