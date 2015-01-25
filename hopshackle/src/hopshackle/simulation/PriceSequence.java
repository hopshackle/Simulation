package hopshackle.simulation;

import java.util.ArrayList;

public class PriceSequence {

	private Artefact item;
	private Market market;
	private ArrayList<Double> recentPrices;
	private ArrayList<Integer> recentTurnover;
	private ArrayList<Integer> recentVolumeOffered;
	private ArrayList<Integer> recentVolumeBid;
	private ArrayList<Integer> dataPointTimes;
	private int lastSold;
	private int lastOnMarket;
	private int sequenceSize;
	private int dataPointsToKeepMax, dataPointsToKeepMin;

	public PriceSequence(Artefact i, Market m) {
		item = i;
		market = m;
		recentPrices = new ArrayList<Double>();
		recentTurnover = new ArrayList<Integer>();
		recentVolumeOffered = new ArrayList<Integer>();
		recentVolumeBid = new ArrayList<Integer>();
		dataPointTimes = new ArrayList<Integer>();
		sequenceSize = 0;
		lastSold = 0;
		lastOnMarket = 0;
		dataPointsToKeepMax = 500;
		dataPointsToKeepMin = 300;
	}

	public synchronized void setLatestPrice(int volume, double price, int volumeOffered, int volumeBids, int clearingPeriod) {
		recentPrices.add(price);
		recentTurnover.add(volume);
		recentVolumeOffered.add(volumeOffered);
		recentVolumeBid.add(volumeBids);
		sequenceSize++;
		if (volume > 0) lastSold = clearingPeriod;
		lastOnMarket = clearingPeriod;
		dataPointTimes.add(clearingPeriod);

		if (sequenceSize > dataPointsToKeepMax) {
			//housekeeping
			for (int n = 0; n<(dataPointsToKeepMax - dataPointsToKeepMin); n++) {
				recentPrices.remove(0);
				recentTurnover.remove(0);
				recentVolumeOffered.remove(0);
				recentVolumeBid.remove(0);
				dataPointTimes.remove(0);
			}
			sequenceSize = recentPrices.size();
		}
	}

	public int getLastOnMarket() {
		return lastOnMarket;
	}
	public synchronized double getAveragePrice(int periods, boolean weightedByVolume) {
		double workingTotal = 0;
		double workingTotalNonClearing = 0;
		int totalSold = 0;
		if (sequenceSize == 0) return 0.0;	// no history at this stage
		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			if (weightedByVolume) {
				workingTotal += recentPrices.get(loop-1)*recentTurnover.get(loop-1);
				workingTotalNonClearing += recentPrices.get(loop-1);
				totalSold += recentTurnover.get(loop-1);
			} else {
				workingTotal += recentPrices.get(loop-1);
				workingTotalNonClearing = workingTotal;
				totalSold++;
			}
		}
		if (totalSold == 0)	
			return workingTotalNonClearing / Math.min(sequenceSize, periods);

		// to cope with non-clearing prices, where there is
		// indicative interest, but no actual trades
		// These periods never count towards avgPrice
		// UNLESS there have been no sales

		return (workingTotal/(double)totalSold);
	}

	public synchronized double getSalesPriceForPeriod(int period) {
		if (historyAvailable(period))
			return recentPrices.get(sequenceSize - period);
		return 0.0;
	}
	public synchronized int getSalesVolumeForPeriod(int period) {
		if (historyAvailable(period))
			return recentTurnover.get(sequenceSize - period);
		return 0;
	}
	public synchronized int getBidVolumeForPeriod(int period) {
		if (historyAvailable(period))
			return recentVolumeBid.get(sequenceSize - period);
		return 0;
	}
	public synchronized int getOfferVolumeForPeriod(int period) {
		if (historyAvailable(period))
			return recentVolumeOffered.get(sequenceSize - period);
		return 0;
	}

	private boolean historyAvailable(int period) {
		if (period == 0) return false;
		return period < sequenceSize;
	}

	public synchronized double getMaxPrice(int periods) {
		double maxPrice = 0;
		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			if (recentPrices.get(loop-1) > maxPrice) maxPrice = recentPrices.get(loop-1);
		}
		return maxPrice;
	}
	public synchronized double getMinPrice(int periods) {
		double minPrice = Double.MAX_VALUE;
		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			if (recentPrices.get(loop-1) < minPrice) minPrice = recentPrices.get(loop-1);
		}
		return minPrice;
	}
	public synchronized int getBidVolume(int periods) {
		int bidVolume = 0;
		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			bidVolume += recentVolumeBid.get(loop-1);
		}
		return bidVolume;
	}
	public synchronized int getOfferVolume(int periods) {
		int offerVolume = 0;
		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			offerVolume += recentVolumeOffered.get(loop-1);
		}
		return offerVolume;
	}
	public synchronized int getSalesVolume(int periods) {
		int salesVolume = 0;
		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			salesVolume += recentTurnover.get(loop-1);
		}
		return salesVolume;
	}
	public synchronized int getTradingPeriods(int periods) {
		if (lastOnMarket < periods)
			return lastOnMarket;
		return periods;

	}
	public Market getMarket() {
		return market;
	}
	public Artefact getArtefact() {
		return item;
	}
	public int getSequenceSize() {
		return sequenceSize;
	}

	public synchronized double getVariance(int periods, boolean weightedByVolume) {
		double total = 0;
		double squareSum = 0;
		int count = 0;

		for (int loop = sequenceSize; loop>0 && loop>(sequenceSize-periods); loop--) {
			if (weightedByVolume) {
				squareSum += recentPrices.get(loop-1) * recentPrices.get(loop-1) * recentTurnover.get(loop-1);
				count += recentTurnover.get(loop-1);
				total += recentPrices.get(loop-1) * recentTurnover.get(loop-1);
			} else {
				squareSum += recentPrices.get(loop-1) * recentPrices.get(loop-1);
				count = count+1;
				total += recentPrices.get(loop-1);
			}
		}
		if (count == 0)	count = 1;

		double mean = total / (double)count;

		return squareSum /(double)count - mean*mean;
	}

}
