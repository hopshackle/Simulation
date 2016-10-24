package hopshackle.simulation.test;

import static org.junit.Assert.assertEquals;
import hopshackle.simulation.*;
import hopshackle.simulation.basic.BasicAgent;

import java.util.*;

import org.junit.*;
public class GiftPolicyTest {

	GiftPolicy dummyGiftPolicy;
	Agent giver, recipient1, recipient2, recipient3;
	World w;

	@Before
	public void setup() {
		w = new World();
		giver = new BasicAgent(w);
		recipient1 = new BasicAgent(w);
		recipient2 = new BasicAgent(w);
		recipient3 = new BasicAgent(w);
		constructGiftPolicy();
	}
	
	private void constructGiftPolicy() {
		for (int n = 0; n < 5; n++) {
			giver.addItem(Resource.FOOD);
			giver.addItem(Resource.METAL);
		}

		dummyGiftPolicy = new GiftPolicy() {

			@Override
			public <T extends Agent> List<Gift> getListOfItemsGiven(T giver) {
				List<Gift> giftList = new ArrayList<Gift>();
				giftList.add(new Gift(recipient1, Resource.FOOD));
				giftList.add(new Gift(recipient1, Resource.FOOD));
				giftList.add(new Gift(recipient2, Resource.FOOD));
				giftList.add(new Gift(recipient2, Resource.METAL));
				return giftList;
			}
		};
	}

	@Test
	public void getListOfItemsDoesNotGiveItems() {
		assertEquals(giver.getNumberInInventoryOf(Resource.FOOD), 5);
		assertEquals(recipient1.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(recipient2.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(recipient3.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(giver.getNumberInInventoryOf(Resource.METAL), 5);
		assertEquals(recipient1.getNumberInInventoryOf(Resource.METAL), 0);
		assertEquals(recipient2.getNumberInInventoryOf(Resource.METAL), 0);
		assertEquals(recipient3.getNumberInInventoryOf(Resource.METAL), 0);

		dummyGiftPolicy.getListOfItemsGiven(giver);

		assertEquals(giver.getNumberInInventoryOf(Resource.FOOD), 5);
		assertEquals(recipient1.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(recipient2.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(recipient3.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(giver.getNumberInInventoryOf(Resource.METAL), 5);
		assertEquals(recipient1.getNumberInInventoryOf(Resource.METAL), 0);
		assertEquals(recipient2.getNumberInInventoryOf(Resource.METAL), 0);
		assertEquals(recipient3.getNumberInInventoryOf(Resource.METAL), 0);
	}

	@Test
	public void giveItemsToRecipients() {
		dummyGiftPolicy.giveItems(giver);
		assertEquals(giver.getNumberInInventoryOf(Resource.FOOD), 2);
		assertEquals(recipient1.getNumberInInventoryOf(Resource.FOOD), 2);
		assertEquals(recipient2.getNumberInInventoryOf(Resource.FOOD), 1);
		assertEquals(recipient3.getNumberInInventoryOf(Resource.FOOD), 0);
		assertEquals(giver.getNumberInInventoryOf(Resource.METAL), 4);
		assertEquals(recipient1.getNumberInInventoryOf(Resource.METAL), 0);
		assertEquals(recipient2.getNumberInInventoryOf(Resource.METAL), 1);
		assertEquals(recipient3.getNumberInInventoryOf(Resource.METAL), 0);
	}
}
