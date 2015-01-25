package hopshackle.simulation;

import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class Market implements Runnable {

	ArrayList<Bid> currentBids;
	ArrayList<Offer> currentItems;
	ArrayList<Artefact> currentItemsOnOffer;
	protected static Logger logger = Logger.getLogger("hopshackle.simulation");
	public static String newline = System.getProperty("line.separator");
	private File outSummary;
	private String dbTable;
	private int period;
	private World world;
	private Hashtable<Artefact, PriceSequence> priceHistory;
	private Object key;
	private boolean done = false;
	private ArrayList<ActionListener> listeners;
	private Connection dbConnection;
	private boolean hasDbConnection;
	private boolean maintainPriceHistory = true;

	public Market(File marketRecord, World world) {
		this.world = world;
		world.setMarket(this);
		currentBids = new ArrayList<Bid>();
		currentItems = new ArrayList<Offer>();
		currentItemsOnOffer = new ArrayList<Artefact>();
		hasDbConnection = false;
		if (marketRecord != null) {
			outSummary = marketRecord;
			dbTable = marketRecord.getName();
			dbTable = dbTable.substring(0, dbTable.indexOf('.'));
			setUpDatabaseConnection();
		}
		key = new Object();
		listeners = new ArrayList<ActionListener>();
		period = 0;
		priceHistory = new Hashtable<Artefact, PriceSequence>();

		world.addListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				synchronized (key) {
					done = true;
					key.notify();
				}
			}
		});
	}

	public synchronized void addBid(Bid newBid) {
		currentBids.add(newBid);
		updatePS(newBid.getItem());
	}

	public synchronized void addItem(Offer newItem) {
		currentItems.add(newItem);
		if (!currentItemsOnOffer.contains(newItem.getItem())) {
			currentItemsOnOffer.add(newItem.getItem());
			// Now sort currentSingleItems in ascending order of Avg Price
			Collections.sort(currentItemsOnOffer, new Comparator<Artefact>() {
				public int compare(Artefact a1, Artefact a2) {
					double retValue = getAveragePrice(a1, 10) - getAveragePrice(a2, 10);
					if (retValue > 0.0) return 1;
					if (retValue < 0.0) return -1;
					return 0;
				}
			});
			updatePS(newItem.getItem());
		}
	}

	private void updatePS(Artefact item) {
		if (maintainPriceHistory && !priceHistory.containsKey(item)) {
			// Add new priceSequence
			PriceSequence ps = new PriceSequence(item, this);
			priceHistory.put(item, ps);
		}
	}

	public synchronized List<Artefact> getItems() {
		return HopshackleUtilities.cloneList(currentItemsOnOffer);
	}
	public synchronized List<Artefact> getItemsBidFor() {
		List<Artefact> retValue = new ArrayList<Artefact>();
		for (Bid b : currentBids) 
			if (!retValue.contains(b.getItem()))
				retValue.add(b.getItem());

		return retValue;
	}

	public double getAveragePrice(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getAveragePrice(periods, true);
	}
	public double getVariance(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return (0.0);
		// if never sold before - then by definition no Variance
		PriceSequence ps = priceHistory.get(a);
		if (ps.getLastOnMarket() < period - 5)
			return (0.0);
		return ps.getVariance(periods, true);
	}
	public double getMaxPrice(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getMaxPrice(periods);
	}
	public double getMinPrice(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getMinPrice(periods);
	}
	public int getOfferVolume(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getOfferVolume(periods);
	}
	public int getBidVolume(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getBidVolume(periods);
	}
	public int getSalesVolume(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getSalesVolume(periods);
	}
	public int getTradingPeriods(Artefact a, int periods) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getTradingPeriods(periods);
	}
	public int getSequenceSize(Artefact a) {
		if (!priceHistory.containsKey(a)) return 0;
		PriceSequence ps = priceHistory.get(a);
		return ps.getSequenceSize();
	}

	public void run() {
		TimerTask newTask = new TimerTask() {
			public void run() {
				synchronized (key) {
					key.notify();
				}
			}
		};

		try {
			double throttle = SimProperties.getPropertyAsDouble("ActionThrottle", "1.0");
			double mktPeriod = SimProperties.getPropertyAsDouble("MarketPeriod", "3000");
			long freq = (long)(mktPeriod * throttle);

			world.setScheduledTask(newTask, freq, freq);

			do {
				synchronized (key) {
					try {
						key.wait();
					} catch (InterruptedException e) {}
				}

				resolveAllBids();

			} while (!done);
			if (hasDbConnection)
				dbConnection.close();

		} catch (Exception e) {
			logger.info("Market has crashed: " + e.toString());
			for ( StackTraceElement s : e.getStackTrace()) {
				logger.info(s.toString());
			}
		}
	}
	public void resolveAllBids() {
		long startTime = System.currentTimeMillis();
		//	logger.info("Starting market resolution");
		Hashtable<Artefact, PriceSequence> tempPriceHistory = new Hashtable<Artefact, PriceSequence>();
		synchronized (this) {
			period++;
			logger.info("Total bids = " + currentBids.size());

			ArrayList<Artefact> itemSet = new ArrayList<Artefact>();
			for (Artefact a : currentItemsOnOffer) 
				itemSet.add(a);

			for (Bid b : currentBids) 
				if (!itemSet.contains(b.getItem())) 
					itemSet.add(b.getItem());

			// Now iterate over items to find the market clearing price of each
			ArrayList<Offer> unsoldItems = new ArrayList<Offer>();
			for (Artefact a : itemSet) {
				ArrayList<Bid> allBids = new ArrayList<Bid>();
				for (Bid b : currentBids) {
					if (b.getItem().equals(a)) allBids.add(b);
				}
				Collections.sort(allBids, new Comparator<Bid>() {
					public int compare(Bid b1, Bid b2) {
						if (b2.getAmount() > b1.getAmount()) return 1;
						if (b2.getAmount() < b1.getAmount()) return -1;
						return 0;
					}
				});
				// the above has set up the array allBids to contain all Bids
				// for the current Artefact, sorted in descending order of bid amount

				ArrayList<Offer> allOffers = new ArrayList<Offer>();
				for (Offer o : currentItems) {
					if (o.getItem().equals(a)) allOffers.add(o);
				}
				Collections.sort(allOffers, new Comparator<Offer>() {
					public int compare(Offer o1, Offer o2) {
						if (o1.getReservePrice() > o2.getReservePrice()) return 1;
						if (o1.getReservePrice() < o2.getReservePrice()) return -1;
						return 0;
					}
				});
				// the above has set up the array allOffers to contain all Offers
				// for the current Artefact, sorted in ascending order of reservePrice

				// Now we run through the bids in descending order of amount

				double clearingPrice = 0;
				int clearingPoint = -1;
				boolean marketCleared = false;

				int nOffers = allOffers.size();
				int nBids = allBids.size();

				for (int loop=0; loop<nOffers && loop<nBids; loop++) {
					if (allBids.get(loop).getAmount() >= allOffers.get(loop).getReservePrice()) {
						// fine - keep going
					} else {
						// bidding is now under reserve price, so stop
						marketCleared = true;
						clearingPoint = loop-1;
						if (clearingPoint == -1) {
							// nothing actually sold - take largest Bid Price
							clearingPrice = allBids.get(0).getAmount();
						} else {
							clearingPrice = Math.max(allBids.get(clearingPoint+1).getAmount(), 
									allOffers.get(clearingPoint).getReservePrice());
							// market clears at highest of the highest unmet bid, 
							// or highest met reserve price
						}
						break;
					}
				}

				/* At this stage we have either found the clearing point in the market,
				 * or else we have fulfilled all of the Bids, or all of the Offer
				 */

				if (!marketCleared && !(nOffers == 0 || nBids == 0)) {
					if (nOffers > nBids) {
						// All bids have been fulfilled
						clearingPoint = nBids-1;
						double fulfilledReserve = allOffers.get(clearingPoint).getReservePrice();
						double filledBid = allBids.get(clearingPoint).getAmount();
						// clearingPrice is the lowest of the lowest bid amount 
						// and the lowest fulfilled reserve price
						clearingPrice = Math.min(fulfilledReserve, filledBid);
					} else {
						// all Offers have been met
						clearingPoint = nOffers-1;
						if (nOffers == nBids) {clearingPrice = allOffers.get(clearingPoint).getReservePrice();}
						else {
							double unfulfilledBid = allBids.get(nOffers).getAmount();
							double metReserve = allOffers.get(clearingPoint).getReservePrice();
							// clearingPrice is the highest of the highest unmet Bid price
							// and the highest met reserve
							clearingPrice = Math.max(unfulfilledBid, metReserve);
						}
					}
				}

				if (nOffers == 0) 
					clearingPrice = allBids.get(0).getAmount();
				// highest price Bid
				if (nBids == 0) 
					clearingPrice = 0;
				// lowest reserve price

				PriceSequence ps = priceHistory.get(a);

				ps.setLatestPrice(clearingPoint+1, clearingPrice, nOffers, nBids, period);

				int itemsNotTransferred = 0;
				for (int loop =0; loop<nBids; loop++) {
					if (loop <= clearingPoint) {
						boolean itemTransferred = allBids.get(loop).resolve(true, clearingPrice);
						if (!itemTransferred) itemsNotTransferred++;
					} else {
						allBids.get(loop).resolve(false, 0);
					}
				}
				for (int loop=0; loop<nOffers; loop++){
					if (loop<=(clearingPoint-itemsNotTransferred)) {
						allOffers.get(loop).resolve(clearingPrice);
					} else  {
						unsoldItems.add(allOffers.get(loop));
					}
				}
			}
			// We now just set up for the next Market round
			currentBids.clear();
			currentItems.clear();
			currentItemsOnOffer.clear();
			for (Offer o : unsoldItems){
				if (Math.random() < 0.20) {
					o.withdraw();
				} else {
					addItem(o);
				}
			}

			for (Artefact i : priceHistory.keySet()) {
				tempPriceHistory.put(i, priceHistory.get(i));
			} // temp copy made inside synchronized block to write outside


			ArrayList<ActionListener> temp = new ArrayList<ActionListener>();
			for (ActionListener al : listeners) 
				temp.add(al);
			// needs to be a copy, as usually the listeners remove themselves
			// as part of their action

			for (ActionListener al : temp) {
				al.actionPerformed(new ActionEvent(this, 1, "MarketCleared"));
			}
		}

		// we log the results outside of the synchronised block
		// as this is read only access to the price histories, which
		// are only updated in resolveAllBids()
		if (outSummary != null && hasDbConnection) {
			for (PriceSequence ps : tempPriceHistory.values()) 
				writeResultsToFile(ps);
		}	

	}

	private void setUpDatabaseConnection() {
		dbConnection = ConnectionFactory.getConnection();

		try {
			Statement st = dbConnection.createStatement();
			String sqlQuery = "DROP TABLE IF EXISTS " + dbTable + ";";
			st.executeUpdate(sqlQuery);
			sqlQuery = "CREATE TABLE IF NOT EXISTS " + dbTable +
					"(Period			INT			NOT NULL," +
					" Item				VARCHAR(50)	NOT NULL," +
					" Offers 			INT			NOT NULL," +
					" Bids				INT			NOT NULL," +
					" Sold				INT			NOT NULL," +
					" Price				DOUBLE			NOT NULL," +
					" AvgPrice			DOUBLE			NOT NULL );";
			st.executeUpdate(sqlQuery);
			st.close();

			hasDbConnection = true;
		} catch (SQLException e) {
			logger.severe("Error in dropping TABLE: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void writeResultsToFile(PriceSequence ps) {
		if (period != ps.getLastOnMarket()) return;
		// getLastSold gives system time that sale last took place
		// this check is to ensure we don;t keep picking up the last
		// sale when nothing has been seen in this market clearing phase
		int nOffers = ps.getOfferVolume(1);
		int nBids = ps.getBidVolume(1);
		double clearingPrice = ps.getMaxPrice(1);
		int sold = ps.getSalesVolume(1);
		Artefact a = ps.getArtefact();
		try {
			FileWriter summaryWriter = new FileWriter(outSummary, true);
			StringBuffer line = new StringBuffer();
			line.append(period);
			line.append(", " + nOffers + ", " +nBids);
			line.append(", " + (sold) +", " + a);
			line.append(", " + String.format("%.2f", clearingPrice));
			line.append(", " + String.format("%.2f", ps.getAveragePrice(5, true)));
			summaryWriter.write(line.toString() + newline);
			summaryWriter.close();
		} catch (IOException e) {
			logger.warning("Error in market record " + e.toString());
			e.printStackTrace();
		}

		try {
			Statement st = dbConnection.createStatement();
			String sqlQuery = String.format("INSERT INTO %s SET Period = %d, Item = '%s', Offers = %d, Bids = %d, " +
					"Sold = %d, Price = %.2f, AvgPrice = %.2f", dbTable, period, a.toString(),
					nOffers, nBids, sold, clearingPrice, ps.getAveragePrice(5, true));
			st.executeUpdate(sqlQuery);
			st.close();
		} catch (SQLException e) {
			logger.severe("Error in writing PriceSequence: " + e.getMessage());
			e.printStackTrace();
		}

	}

	public synchronized void addListener(ActionListener newListener) {
		listeners.add(newListener);
	}
	public synchronized void removeListener(ActionListener oldListener) {
		if (listeners.contains(oldListener))
			listeners.remove(oldListener);
	}
	public PriceSequence getFullPriceHistory(Artefact item, int periods) {
		PriceSequence retValue = new PriceSequence(item, this);

		int pricesToRead = Math.min(periods, this.getSequenceSize(item));
		PriceSequence masterCopy = priceHistory.get(item);

		for (int loop = 1; loop <= pricesToRead; loop++) {
			int volume = masterCopy.getSalesVolumeForPeriod(loop);
			double price = masterCopy.getSalesPriceForPeriod(loop);
			int offered = masterCopy.getOfferVolumeForPeriod(loop);
			int bids = masterCopy.getBidVolumeForPeriod(loop);
			retValue.setLatestPrice(volume, price, offered, bids, loop);
		}

		return retValue;
	}
	
	public void maintainPriceHistory(boolean maintain) {
		maintainPriceHistory = maintain;
	}
}
