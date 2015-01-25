package hopshackle.GUI;

import hopshackle.simulation.*;

import java.awt.event.*;

import javax.swing.*;

public class MarketViewer extends JTabbedPane implements ActionListener {

	private Market market;
	private Artefact item;
	private double[] priceData, volumeData, bidLiquidityData, offerLiquidityData;
	private int lastIndexPopulated = -1;

	public MarketViewer(Artefact a, Market market) {
		item = a;
		this.market = market;
		setUpDisplay();
		market.addListener(this);
	}

	private void setUpDisplay() {
		PriceSequence ps = market.getFullPriceHistory(item, 300);

		priceData = makeDataset(new priceVisitor(ps));
		volumeData = makeDataset(new volumeVisitor(ps));
		bidLiquidityData = makeDataset(new bidLiquidityVisitor(ps));
		offerLiquidityData = makeDataset(new offerLiquidityVisitor(ps));
		String[] itemName = new String[] {item.toString()};

		double[][] prices = new double[1][];
		prices[0] = priceData;
		double[][] volumes = new double[1][];
		volumes[0] = volumeData;
		double[][] liquidity = new double[2][];
		liquidity[0] = bidLiquidityData;
		liquidity[1] = offerLiquidityData;
		String[] liquidityLabels = new String[] {"Bid Liquidity", "Offer Liquidity"};

		RealTimeLBChart2D priceGraph = new RealTimeLBChart2D("Sales Price", itemName, prices, 20, 60);
		RealTimeLBChart2D volumeGraph = new RealTimeLBChart2D("Volume Sold", itemName, volumes, 20, 60);
		RealTimeLBChart2D liquidityGraph = new RealTimeLBChart2D(item.toString(), liquidityLabels, liquidity, 20, 60);

		JScrollPane pricePane = new JScrollPane(priceGraph);
		JScrollPane volumePane = new JScrollPane(volumeGraph);
		JScrollPane liquidityPane = new JScrollPane(liquidityGraph);
		this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.addTab("Volume", volumePane);
		this.addTab("Price", pricePane);
		this.addTab("Liquidity", liquidityPane);

		volumeGraph.setMillisecondsPerFrame(5000);
		priceGraph.setMillisecondsPerFrame(5000);
		liquidityGraph.setMillisecondsPerFrame(5000);
		volumeGraph.start();
		priceGraph.start();
		liquidityGraph.start();
	}

	private double[] makeDataset(DoubleValueVisitor valueVisitor) {
		double[] retArray = new double[1200];
		for (int loop = 0; loop < 300; loop++) {
			retArray[loop] = valueVisitor.getValue(loop);
			if (!(new Double(retArray[loop]).equals(Double.NaN)))
				lastIndexPopulated = loop;
		}	
		for (int loop = 300; loop < 1200; loop++) {
			retArray[loop] = Double.NaN;
		}
		return retArray;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("MarketCleared")) {
			getLastPriceAndVolumeSold();
			if (!(new Double(priceData[1199]).equals(Double.NaN))) {
				market.removeListener(this);
			}
		}
	}

	private void getLastPriceAndVolumeSold() {
		priceData[lastIndexPopulated+1] = market.getAveragePrice(item, 1);
		volumeData[lastIndexPopulated+1] = market.getSalesVolume(item, 1);
		if (market.getBidVolume(item, 1)==0) 
			bidLiquidityData[lastIndexPopulated+1] = 0.0;
		else
			bidLiquidityData[lastIndexPopulated+1] = 100.0 * (double)market.getSalesVolume(item, 1) / (double)market.getBidVolume(item, 1);

		if (market.getOfferVolume(item, 1)==0)
			offerLiquidityData[lastIndexPopulated+1] = 0.0;
		else
			offerLiquidityData[lastIndexPopulated+1] = 100.0 * (double)market.getSalesVolume(item, 1) / (double)market.getOfferVolume(item, 1);
		
		lastIndexPopulated++;
	}

	abstract class DoubleValueVisitor {
		PriceSequence ps;

		public DoubleValueVisitor(PriceSequence ps) {
			this.ps = ps;
		}
		abstract double getValue(int period);
	}
	class volumeVisitor extends DoubleValueVisitor {
		public volumeVisitor(PriceSequence ps) {
			super(ps);
		}

		@Override
		public double getValue(int period) {
			if (period > ps.getSequenceSize()) return Double.NaN;
			return ps.getSalesVolumeForPeriod(period);
		}
	}
	class priceVisitor extends DoubleValueVisitor {
		public priceVisitor(PriceSequence ps) {
			super(ps);
		}

		@Override
		public double getValue(int period) {
			if (period > ps.getSequenceSize()) return Double.NaN;
			return ps.getSalesPriceForPeriod(period);
		}
	}

	class bidLiquidityVisitor extends DoubleValueVisitor {

		public bidLiquidityVisitor(PriceSequence ps) {
			super(ps);
		}

		@Override
		public double getValue(int period) {
			if (period > ps.getSequenceSize()) return Double.NaN;
			if (ps.getBidVolumeForPeriod(period) == 0) return 0.0;
			double sales = ps.getSalesVolumeForPeriod(period);
			double bids = ps.getBidVolumeForPeriod(period);
			return 100.0 * sales / bids;
		}
	}
	class offerLiquidityVisitor extends DoubleValueVisitor {

		public offerLiquidityVisitor(PriceSequence ps) {
			super(ps);
		}

		@Override
		public double getValue(int period) {
			if (period > ps.getSequenceSize()) return Double.NaN;
			if (ps.getOfferVolumeForPeriod(period) == 0) return 0.0;
			return 100.0 * (double)ps.getSalesVolumeForPeriod(period) / (double)ps.getOfferVolumeForPeriod(period);
		}

	}

}
