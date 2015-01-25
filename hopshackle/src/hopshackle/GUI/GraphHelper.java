package hopshackle.GUI;

import net.sourceforge.chart2d.*;


public class GraphHelper {

	public static LBChart2D makeStandardGraph(String graphTitle, String xAxisLabel, Dataset dataset, String[] categoryLabels) {
		//Configure object properties
		Object2DProperties object2DProps = new Object2DProperties();
		object2DProps.setObjectTitleText (graphTitle);

		//Configure chart properties
		Chart2DProperties chart2DProps = new Chart2DProperties();
		chart2DProps.setChartDataLabelsPrecision (1);

		//Configure graph chart properties
		GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();

		graphChart2DProps.setLabelsAxisTitleText ("Time");
		graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);
		graphChart2DProps.setNumbersAxisTitleText (xAxisLabel);

		//Configure graph properties
		GraphProperties graphProps = new GraphProperties();
		graphProps.setGraphBarsExistence (false);
		graphProps.setGraphLinesExistence (true);
		graphProps.setGraphAllowComponentAlignment (true);
		graphProps.setGraphLinesWithinCategoryOverlapRatio (1f);
		graphProps.setGraphLinesThicknessModel(2);

		int minutes = dataset.getNumCats();
		String [] labelsAxisLabels = new String[minutes];
		for (int loop = 1; loop <= minutes; loop++) 
			labelsAxisLabels[loop-1] = String.format("%02d", loop);
	
		graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);

		//Configure graph component colors
		MultiColorsProperties multiColorsProps = new MultiColorsProperties();
		LegendProperties legendProps = new LegendProperties();
		legendProps.setLegendLabelsTexts (categoryLabels);

		//Configure chart
		LBChart2D chart2D = new LBChart2D();
		chart2D.setObject2DProperties (object2DProps);
		chart2D.setChart2DProperties (chart2DProps);
		chart2D.setLegendProperties (legendProps);
		chart2D.setGraphChart2DProperties (graphChart2DProps);
		chart2D.addGraphProperties (graphProps);
		chart2D.addMultiColorsProperties (multiColorsProps);
		chart2D.addDataset (dataset);

		//Optional validation:  Prints debug messages if invalid only.
		if (!chart2D.validate (false)) chart2D.validate (true);

		return chart2D;
	}
}
