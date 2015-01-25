package hopshackle.GUI;

import java.awt.Graphics;

import javax.swing.JPanel;

import net.sourceforge.chart2d.*;
public class RealTimeLBChart2D extends SimpleAnimationPanel{

	private Dataset dataSet;
	private double[][] parentData;
	private int lastItemInParentDataConverted = -1;
	private int numberOfCategories, numberOfItems;
	private LBChart2D graph;
	private String nameOfGraph;
	private String[] nameOfVariable;

	/*
	 * The sourceArray fed into the constructor is the backing array for the graph
	 * 
	 * It must start the data series at index zero, and indicate how far the data extends by populating any undetermined
	 * index with a value of Double.NaN - the RealTimeLBChart2D will then draw up to the first Double.NaN that it encounters
	 */
	public RealTimeLBChart2D(String nameOfGraph, String[] nameOfVariable, double[][] sourceArray, int datapointsPerTimeUnit, int dataDurationInTimeUnits) {
		parentData = sourceArray;
		this.nameOfVariable = nameOfVariable;
		this.nameOfGraph = nameOfGraph;
		numberOfItems = datapointsPerTimeUnit;
		numberOfCategories = dataDurationInTimeUnits;
		dataSet = new Dataset(nameOfVariable.length, numberOfCategories, numberOfItems);
		convertNewValuesToDataset();
		graph = GraphHelper.makeStandardGraph(this.nameOfGraph, "Time", dataSet, nameOfVariable);
		JPanel graphHolder = new JPanel();
		graphHolder.add(graph);
		this.add(graphHolder);
	}

	public void drawFrame(Graphics g) {
		if (newValuesExist()) {
			convertNewValuesToDataset();
			graph.repaint();
		}
	}

	private void convertNewValuesToDataset() {
		int maxLoop = numberOfCategories * numberOfItems;
		for (int loop = lastItemInParentDataConverted + 1; loop < maxLoop ; loop++) {
			int category = loop/numberOfItems;
			int item = loop%numberOfItems;
			for (int varLoop = 0; varLoop < nameOfVariable.length; varLoop++) {
				if (!(new Double(parentData[varLoop][loop]).equals(Double.NaN))) {
					dataSet.add(varLoop+1, category, item, (float)parentData[varLoop][loop]);
				} else {
					lastItemInParentDataConverted = loop - 1;
					return;
				}
			}
		}
	}

	public boolean newValuesExist() {
		if (!(new Double(parentData[0][lastItemInParentDataConverted+1]).equals(Double.NaN)))
			return true;
		else 
			return false;
	}

}
