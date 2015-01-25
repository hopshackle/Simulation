package hopshackle.simulation.dnd.genetics;

import hopshackle.simulation.*;
import hopshackle.simulation.dnd.*;
import hopshackle.simulation.dnd.Character;

public enum TradeGeneticEnum implements GeneticVariable {
	GOLD,
	FIVE,
	TEN,
	HUNDRED,
	MAX_PRICE,
	MIN_PRICE,
	AVG_PRICE,
	LAST_PRICE,
	PENULT_PRICE,
	OFFER_LIQUIDITY,
	BID_LIQUIDITY,
	SALES_VOLUME,
	VARIANCE;

	public String getDescriptor() {
		return "GEN3";
	}

	public double getValue(Object a, double var) {
		if (!(a instanceof DnDAgent)) return 0.00;
		return getValueOfItem((DnDAgent)a, null);
	}

	public double getValue(Object o1, Object o2) {
		if (!(o1 instanceof DnDAgent)) return 0.00;
		Artefact item = null;
		if (o2 == null) {
			//fine
		} else if (o2 instanceof Artefact) {
			item = (Artefact) o2;
		} else {
			return 0.00;
		}
		DnDAgent a1 = (DnDAgent) o1;
		return getValueOfItem(a1, item);
	}

	public double getValueOfItem(DnDAgent a, Artefact item) {
		if (!(a instanceof Character)) return 0;
		if (a.isDead()) return 0;
		Market m = a.getLocation().getMarket();

		switch (this) {
		case GOLD:
			return a.getGold();
		}

		return this.getValueAtMarket(m, item);
	}

	public double getValueAtMarket(Market m, Artefact item) {
		if (m==null) return 0.0;

		switch (this) {
		case FIVE:
			return 5.0;
		case TEN:
			return 10.0;
		case HUNDRED:
			return 100.0;
		case MAX_PRICE:
			if (m != null && item !=null) {
				return m.getMaxPrice(item, 5);
			} else return 0;
		case MIN_PRICE:
			if (m != null && item !=null) {
				return m.getMinPrice(item, 5);
			} else return 0;
		case AVG_PRICE:
			if (m != null && item !=null) {
				return m.getAveragePrice(item, 5);
			} else return 0;
		case LAST_PRICE:
			if (m != null && item !=null) {
				return m.getAveragePrice(item, 1);
			} else return 0;
		case PENULT_PRICE:
			if (m != null && item !=null) {
				double lastPrice = m.getAveragePrice(item, 1);
				double previousPrice = m.getAveragePrice(item, 2)*2-lastPrice;
				return previousPrice;
			} else return 0;
		case OFFER_LIQUIDITY:
			if (m != null && item !=null) {
				double bids = m.getBidVolume(item, 5);
				double sales = m.getSalesVolume(item, 5);
				double offers = m.getOfferVolume(item, 5);
				if (offers < 0.01 && bids > 0.01) return 1.0;
				if (offers < 0.01) return 0.0;
				return sales/offers;
			} else return 0;
		case BID_LIQUIDITY:
			if (m != null && item !=null) {
				double offers = m.getOfferVolume(item, 5);
				double sales = m.getSalesVolume(item, 5);
				double bids = m.getBidVolume(item, 5);
				if (bids < 0.01 && offers > 0.01) return 1.0;
				if (bids < 0.01) return 0.0;
				return sales/bids;
			} else return 0;
		case SALES_VOLUME:
			// if none actually sold, then default to 0.4 per period
			if (m != null && item !=null) {
				if (m.getTradingPeriods(item, 5) == 0) return 0.4;
				if (m.getSalesVolume(item, 5) == 0) return 0.4;
				return (double)m.getSalesVolume(item, 5) / (double)m.getTradingPeriods(item, 5);
			} else return 0;
		case VARIANCE:
			if (m != null && item !=null) {
				return m.getVariance(item, 5);
			} else return 0;
		}
		return 0.0;
	}

	public boolean unitaryRange() {
		return false;
	}

}
