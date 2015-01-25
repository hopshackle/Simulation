/**
 * Chart2D, a java library for drawing two dimensional charts.
 * Copyright (C) 2001 Jason J. Simas
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * The author of this library may be contacted at:
 * E-mail:  jjsimas@users.sourceforge.net
 * Street Address:  J J Simas, 887 Tico Road, Ojai, CA 93023-3555 USA
 */


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import net.sourceforge.chart2d.Chart2D;
import net.sourceforge.chart2d.Chart2DProperties;
import net.sourceforge.chart2d.Dataset;
import net.sourceforge.chart2d.GraphChart2DProperties;
import net.sourceforge.chart2d.GraphProperties;
import net.sourceforge.chart2d.LLChart2D;
import net.sourceforge.chart2d.LegendProperties;
import net.sourceforge.chart2d.MultiColorsProperties;
import net.sourceforge.chart2d.Object2DProperties;
import net.sourceforge.chart2d.WarningRegionProperties;


/**
 * A Chart2D demo demonstrating the LLChart2D object.
 * Container Class: JFrame<br>
 * Program Types:  Applet or Application<br>
 */
public class LLChart2DFrameDemo extends JApplet {


  private JFrame frame = null;
  private static boolean isApplet = true;


  /**
   * For running as an application.
   * Calls init() and start().
   * @param args An unused parameter.
   */
  public static void main (String[] args) {

    isApplet = false;
    LLChart2DFrameDemo demo = new LLChart2DFrameDemo();
    demo.init();
    demo.start();
    //exit on frame close event
  }


  /**
   * Configure the chart and frame, and open the frame.
   */
  public void init() {

    //Start configuring a JFrame GUI with a JTabbedPane for multiple chart panes
    JTabbedPane panes = new JTabbedPane();

    panes.addTab ("Bar", getChart2DDemoA());
    panes.addTab ("Bar with Regions", getChart2DDemoB());
    panes.addTab ("Bar with Trend Line", getChart2DDemoC());
    panes.addTab ("Bar with Cat Coloring", getChart2DDemoD());
    panes.addTab ("Bar True Stacked", getChart2DDemoE());
    panes.addTab ("Line", getChart2DDemoF());
    panes.addTab ("Line with Regions", getChart2DDemoG());
    panes.addTab ("Filled Line True Stacked", getChart2DDemoH());
    panes.addTab ("Line with Large Dataset", getChart2DDemoI());
    panes.addTab ("Scatter / Dot", getChart2DDemoJ());
    panes.addTab ("Line and Dot", getChart2DDemoK());
    panes.addTab ("Standard Overlay", getChart2DDemoL());

    //JTabbedPane specific GUI code
    //Chart2D by default magnifies itself on user resize, the magnification
    //raio is based on each chart's preferred size.  So that all the charts
    //have the same magnification ratio, we must make all the charts have the
    //same preferred size.  You can calculate each chart's preferred size
    //dynamically (slower), OR you can pick a size that is at least a big as
    //its dynamic preferred size and use this statically -- permanently.
    //I recommend using dynamic calc while writing your code, then for
    //production make sure to use the static code for the performance increase.
    //Add a System.out.println (size) with the dynamic code, and use that size
    //for your static code size.
    //Also, setting the panes preferred size with a static size pushes
    //calculation of charts in non-visible panes off, until they are visible.
    //This means that start up time with a static panes size is the same as if
    //you had only one Chart2D object.
    boolean dynamicSizeCalc = false;
    if (dynamicSizeCalc) {
      int maxWidth = 0;
      int maxHeight = 0;
      for (int i = 0; i < panes.getTabCount(); ++i) {
        Chart2D chart2D = (Chart2D)panes.getComponentAt (i);
        chart2D.pack();
        Dimension size = chart2D.getSize();
        maxWidth = maxWidth > size.width ? maxWidth : size.width;
        maxHeight = maxHeight > size.height ? maxHeight : size.height;
      }
      Dimension maxSize = new Dimension (maxWidth, maxHeight);
      System.out.println (maxSize);
      for (int i = 0; i < panes.getTabCount(); ++i) {
        Chart2D chart2D = (Chart2D)panes.getComponentAt (i);
        chart2D.setSize (maxSize);
        chart2D.setPreferredSize (maxSize);
      }
      System.out.println (panes.getPreferredSize());
    }
    else {
      Dimension maxSize = new Dimension (448, 293);
      for (int i = 0; i < panes.getTabCount(); ++i) {
        Chart2D chart2D = (Chart2D)panes.getComponentAt (i);
        chart2D.setSize (maxSize);
        chart2D.setPreferredSize (maxSize);
      }
      panes.setPreferredSize (new Dimension (453 + 5, 378 + 5)); //+ 5 slop
    }

    frame = new JFrame();
    frame.getContentPane().add (panes);
    frame.setTitle ("LLChart2DFrameDemo");
    frame.addWindowListener (
      new WindowAdapter() {
        public void windowClosing (WindowEvent e) {
          destroy();
    } } );
    frame.pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation (
      (screenSize.width - frame.getSize().width) / 2,
      (screenSize.height - frame.getSize().height) / 2);
  }


  /**
   * Shows the JFrame GUI.
   */
  public void start() {
    frame.show();
  }


  /**
   * Ends the application or applet.
   */
  public void destroy() {

    if (frame != null) frame.dispose();
    if (!isApplet) System.exit (0);
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoA() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Amperage Used Per Appliance");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (-1);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"2002", "2001", "2000"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels = {"Computer", "Monitor", "AC", "Lighting", "Refrigerator"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Appliances");
    graphChart2DProps.setNumbersAxisTitleText ("Amps on 120 Volt Line");

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();

    //Configure dataset
    Dataset dataset = new Dataset (3, 5, 1);
    dataset.set (0, 0, 0, 3.5f);
    dataset.set (0, 1, 0, 2.5f);
    dataset.set (0, 2, 0, 10.0f);
    dataset.set (0, 3, 0, 0.5f);
    dataset.set (0, 4, 0, 2.0f);
    dataset.set (1, 0, 0, 3.0f);
    dataset.set (1, 1, 0, 2.0f);
    dataset.set (1, 2, 0, 10.0f);
    dataset.set (1, 3, 0, 1.0f);
    dataset.set (1, 4, 0, 2.0f);
    dataset.set (2, 0, 0, 2.5f);
    dataset.set (2, 1, 0, 2.0f);
    dataset.set (2, 2, 0, 10.5f);
    dataset.set (2, 3, 0, 1.0f);
    dataset.set (2, 4, 0, 2.5f);

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoB() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Amperage Used Per Appliance");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (-1);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"2002", "2001", "2000"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels = {"Computer", "Monitor", "AC", "Lighting", "Refrigerator"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Appliances");
    graphChart2DProps.setNumbersAxisTitleText ("Amps on 120 Volt Line");

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphOutlineComponentsExistence (true);

    //Configure dataset
    Dataset dataset = new Dataset (3, 5, 1);
    dataset.set (0, 0, 0, 3.5f);
    dataset.set (0, 1, 0, 2.5f);
    dataset.set (0, 2, 0, 10.0f);
    dataset.set (0, 3, 0, 0.5f);
    dataset.set (0, 4, 0, 2.0f);
    dataset.set (1, 0, 0, 3.0f);
    dataset.set (1, 1, 0, 2.0f);
    dataset.set (1, 2, 0, 10.0f);
    dataset.set (1, 3, 0, 1.0f);
    dataset.set (1, 4, 0, 2.0f);
    dataset.set (2, 0, 0, 2.5f);
    dataset.set (2, 1, 0, 2.0f);
    dataset.set (2, 2, 0, 10.5f);
    dataset.set (2, 3, 0, 1.0f);
    dataset.set (2, 4, 0, 2.5f);

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure warning regions for graph
    WarningRegionProperties warningRegionProps1 = new WarningRegionProperties();
    warningRegionProps1.setHigh (WarningRegionProperties.HIGH);
    warningRegionProps1.setLow (7f);

    //Configure warning regions for graph
    WarningRegionProperties warningRegionProps2 = new WarningRegionProperties();
    warningRegionProps2.setHigh (7f);
    warningRegionProps2.setLow (5f);
    warningRegionProps2.setComponentColor (new Color (146, 105, 0));
    warningRegionProps2.setBackgroundColor (new Color (222, 209, 176));

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);
    chart2D.addWarningRegionProperties (warningRegionProps1);
    chart2D.addWarningRegionProperties (warningRegionProps2);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoC() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Productivity by Age");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (-2);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"Mov. Avg.", "Raw Data"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"20-25", "25-30", "30-35", "35-40", "40-45", "45-50", "50-55", "55-60", "60-65"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Age Ranges");
    graphChart2DProps.setNumbersAxisTitleText ("Actual / Optimal");
    graphChart2DProps.setChartDatasetCustomizeGreatestValue (true);
    graphChart2DProps.setChartDatasetCustomGreatestValue (1f);
    graphChart2DProps.setChartDatasetCustomizeLeastValue (true);
    graphChart2DProps.setChartDatasetCustomLeastValue (.5f);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphComponentsAlphaComposite (graphProps.ALPHA_COMPOSITE_MILD);

    //Configure dataset
    Dataset dataset = new Dataset (1, 9, 1);
    dataset.set (0, 0, 0, .6f);
    dataset.set (0, 1, 0, .9f);
    dataset.set (0, 2, 0, .85f);
    dataset.set (0, 3, 0, .8f);
    dataset.set (0, 4, 0, .85f);
    dataset.set (0, 5, 0, .8f);
    dataset.set (0, 6, 0, .75f);
    dataset.set (0, 7, 0, .75f);
    dataset.set (0, 8, 0, .70f);

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure graph properties (trend)
    GraphProperties graphPropsTrend = new GraphProperties();
    graphPropsTrend.setGraphBarsExistence (false);
    graphPropsTrend.setGraphLinesExistence (true);

    //Configure dataset (trend)
    Dataset datasetTrend = new Dataset();
    datasetTrend.addMovingAverage (dataset, 3);

    //Configure graph component colors (trend)
    MultiColorsProperties multiColorsPropsTrend = new MultiColorsProperties();
    multiColorsPropsTrend.setColorsCustomize (true);
    multiColorsPropsTrend.setColorsCustom (new Color[] {new Color (193, 183, 0)});

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphPropsTrend);
    chart2D.addDataset (datasetTrend);
    chart2D.addMultiColorsProperties (multiColorsPropsTrend);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoD() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Productivity by Age");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (-2);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    legendProps.setLegendExistence (false);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"20-25", "25-30", "30-35", "35-40", "40-45", "45-50", "50-55", "55-60", "60-65"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Age Ranges");
    graphChart2DProps.setNumbersAxisTitleText ("Actual / Optimal");
    graphChart2DProps.setChartDatasetCustomizeGreatestValue (true);
    graphChart2DProps.setChartDatasetCustomGreatestValue (1f);
    graphChart2DProps.setChartDatasetCustomizeLeastValue (true);
    graphChart2DProps.setChartDatasetCustomLeastValue (.5f);
    graphChart2DProps.setGraphComponentsColoringByCat (true);
    graphChart2DProps.setGraphComponentsColorsByCat (new MultiColorsProperties());

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();

    //Configure dataset
    Dataset dataset = new Dataset (1, 9, 1);
    dataset.set (0, 0, 0, .6f);
    dataset.set (0, 1, 0, .9f);
    dataset.set (0, 2, 0, .85f);
    dataset.set (0, 3, 0, .8f);
    dataset.set (0, 4, 0, .85f);
    dataset.set (0, 5, 0, .8f);
    dataset.set (0, 6, 0, .75f);
    dataset.set (0, 7, 0, .75f);
    dataset.set (0, 8, 0, .70f);

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoE() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Bugs Per MS Product");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (2);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"Unfound", "Fixed", "Unfixed", "Permanent"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels = {"Windows", "Office", "Visual Dev", ".Net", "Explorer"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Products");
    graphChart2DProps.setNumbersAxisTitleText ("Bugs");

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphAllowComponentAlignment (true);
    graphProps.setGraphBarsRoundingRatio (0f);
    graphProps.setGraphOutlineComponentsExistence (true);

    //Configure dataset
    Dataset dataset = new Dataset (4, 5, 1);
    dataset.set (0, 0, 0, 110f);
    dataset.set (0, 1, 0, 700f);
    dataset.set (0, 2, 0, 300f);
    dataset.set (0, 3, 0, 250f);
    dataset.set (0, 4, 0, 800f);
    dataset.set (1, 0, 0, 400f);
    dataset.set (1, 1, 0, 200f);
    dataset.set (1, 2, 0, 100f);
    dataset.set (1, 3, 0, 650f);
    dataset.set (1, 4, 0, 450f);
    dataset.set (2, 0, 0, 500f);
    dataset.set (2, 1, 0, 200f);
    dataset.set (2, 2, 0, 100f);
    dataset.set (2, 3, 0, 550f);
    dataset.set (2, 4, 0, 350f);
    dataset.set (3, 0, 0, 250f);
    dataset.set (3, 1, 0, 150f);
    dataset.set (3, 2, 0, 750f);
    dataset.set (3, 3, 0, 100f);
    dataset.set (3, 4, 0, 150f);
    dataset.doConvertToStacked();

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoF() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Programmers By Language");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"C", "C++", "Java"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"1990", "1991", "1992", "1993", "1994", "1995",
       "1996", "1997", "1998", "1999", "2001", "2002"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Years");
    graphChart2DProps.setNumbersAxisTitleText ("Programmers");
    graphChart2DProps.setChartDatasetCustomizeGreatestValue (true);
    graphChart2DProps.setChartDatasetCustomGreatestValue (1000);
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphBarsExistence (false);
    graphProps.setGraphLinesExistence (true);
    graphProps.setGraphOutlineComponentsExistence (true);
    graphProps.setGraphAllowComponentAlignment (true);

    //Configure dataset
    Dataset dataset = new Dataset (3, 12, 1);
    dataset.set (0,  0, 0, 100f);  //1990
    dataset.set (0,  1, 0, 200f);  //1991
    dataset.set (0,  2, 0, 300f);  //1992
    dataset.set (0,  3, 0, 400f);  //1993
    dataset.set (0,  4, 0, 100f);  //1994
    dataset.set (0,  5, 0, 200f);  //1995
    dataset.set (0,  6, 0, 100f);  //1996
    dataset.set (0,  7, 0,   0f);  //1997
    dataset.set (0,  8, 0, 100f);  //1998
    dataset.set (0,  9, 0, 100f);  //1999
    dataset.set (0, 10, 0, 200f);  //2000
    dataset.set (0, 11, 0, 300f);  //2001
    dataset.set (1,  0, 0,   0f);  //1990
    dataset.set (1,  1, 0,   0f);  //1991
    dataset.set (1,  2, 0,   0f);  //1992
    dataset.set (1,  3, 0, 100f);  //1993
    dataset.set (1,  4, 0, 200f);  //1994
    dataset.set (1,  5, 0, 400f);  //1995
    dataset.set (1,  6, 0, 500f);  //1996
    dataset.set (1,  7, 0, 700f);  //1997
    dataset.set (1,  8, 0, 900f);  //1998
    dataset.set (1,  9, 0, 100f);  //1999
    dataset.set (1, 10, 0, 200f);  //2000
    dataset.set (1, 11, 0, 300f);  //2001
    dataset.set (2,  0, 0,   0f);  //1990
    dataset.set (2,  1, 0,   0f);  //1991
    dataset.set (2,  2, 0,   0f);  //1992
    dataset.set (2,  3, 0,   0f);  //1993
    dataset.set (2,  4, 0, 100f);  //1994
    dataset.set (2,  5, 0, 200f);  //1995
    dataset.set (2,  6, 0, 300f);  //1996
    dataset.set (2,  7, 0, 400f);  //1997
    dataset.set (2,  8, 0, 500f);  //1998
    dataset.set (2,  9, 0, 100f);  //1999
    dataset.set (2, 10, 0, 300f);  //2000
    dataset.set (2, 11, 0, 900f);  //2001

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoG() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Programmers By Language");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"C", "C++", "Java"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"1990", "1991", "1992", "1993", "1994", "1995",
       "1996", "1997", "1998", "1999", "2001", "2002"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Years");
    graphChart2DProps.setNumbersAxisTitleText ("Programmers");
    graphChart2DProps.setChartDatasetCustomizeGreatestValue (true);
    graphChart2DProps.setChartDatasetCustomGreatestValue (1000);
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphBarsExistence (false);
    graphProps.setGraphLinesExistence (true);
    graphProps.setGraphAllowComponentAlignment (true);

    //Configure dataset
    Dataset dataset = new Dataset (3, 12, 1);
    dataset.set (0,  0, 0, 100f);  //1990
    dataset.set (0,  1, 0, 200f);  //1991
    dataset.set (0,  2, 0, 300f);  //1992
    dataset.set (0,  3, 0, 400f);  //1993
    dataset.set (0,  4, 0, 100f);  //1994
    dataset.set (0,  5, 0, 200f);  //1995
    dataset.set (0,  6, 0, 100f);  //1996
    dataset.set (0,  7, 0,   0f);  //1997
    dataset.set (0,  8, 0, 100f);  //1998
    dataset.set (0,  9, 0, 100f);  //1999
    dataset.set (0, 10, 0, 200f);  //2000
    dataset.set (0, 11, 0, 300f);  //2001
    dataset.set (1,  0, 0,   0f);  //1990
    dataset.set (1,  1, 0,   0f);  //1991
    dataset.set (1,  2, 0,   0f);  //1992
    dataset.set (1,  3, 0, 100f);  //1993
    dataset.set (1,  4, 0, 200f);  //1994
    dataset.set (1,  5, 0, 400f);  //1995
    dataset.set (1,  6, 0, 500f);  //1996
    dataset.set (1,  7, 0, 700f);  //1997
    dataset.set (1,  8, 0, 900f);  //1998
    dataset.set (1,  9, 0, 100f);  //1999
    dataset.set (1, 10, 0, 200f);  //2000
    dataset.set (1, 11, 0, 300f);  //2001
    dataset.set (2,  0, 0,   0f);  //1990
    dataset.set (2,  1, 0,   0f);  //1991
    dataset.set (2,  2, 0,   0f);  //1992
    dataset.set (2,  3, 0,   0f);  //1993
    dataset.set (2,  4, 0, 100f);  //1994
    dataset.set (2,  5, 0, 200f);  //1995
    dataset.set (2,  6, 0, 300f);  //1996
    dataset.set (2,  7, 0, 400f);  //1997
    dataset.set (2,  8, 0, 500f);  //1998
    dataset.set (2,  9, 0, 100f);  //1999
    dataset.set (2, 10, 0, 300f);  //2000
    dataset.set (2, 11, 0, 900f);  //2001

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure warning regions for graph
    WarningRegionProperties warningRegionProps1 = new WarningRegionProperties();
    warningRegionProps1.setHigh (100);
    warningRegionProps1.setLow (WarningRegionProperties.LOW);

    //Configure warning regions for graph
    WarningRegionProperties warningRegionProps2 = new WarningRegionProperties();
    warningRegionProps2.setHigh (200);
    warningRegionProps2.setLow (100);
    warningRegionProps2.setComponentColor (new Color (146, 105, 0));
    warningRegionProps2.setBackgroundColor (new Color (222, 209, 176));

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);
    chart2D.addWarningRegionProperties (warningRegionProps1);
    chart2D.addWarningRegionProperties (warningRegionProps2);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoH() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Programmers By Language");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (2);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"C", "C++", "Java"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"1990", "1991", "1992", "1993", "1994", "1995",
       "1996", "1997", "1998", "1999", "2001", "2002"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Years");
    graphChart2DProps.setNumbersAxisTitleText ("Programmers");
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphBarsExistence (false);
    graphProps.setGraphLinesExistence (true);
    graphProps.setGraphLinesFillInterior (true);
    graphProps.setGraphLinesThicknessModel (1);
    graphProps.setGraphAllowComponentAlignment (true);
    graphProps.setGraphComponentsAlphaComposite (graphProps.ALPHA_COMPOSITE_MEDIUM);

    //Configure dataset
    Dataset dataset = new Dataset (3, 12, 1);
    dataset.set (0,  0, 0, 100f);  //1990
    dataset.set (0,  1, 0, 200f);  //1991
    dataset.set (0,  2, 0, 300f);  //1992
    dataset.set (0,  3, 0, 400f);  //1993
    dataset.set (0,  4, 0, 100f);  //1994
    dataset.set (0,  5, 0, 200f);  //1995
    dataset.set (0,  6, 0, 100f);  //1996
    dataset.set (0,  7, 0,   0f);  //1997
    dataset.set (0,  8, 0, 100f);  //1998
    dataset.set (0,  9, 0, 100f);  //1999
    dataset.set (0, 10, 0, 200f);  //2000
    dataset.set (0, 11, 0, 300f);  //2001
    dataset.set (1,  0, 0,   0f);  //1990
    dataset.set (1,  1, 0,   0f);  //1991
    dataset.set (1,  2, 0,   0f);  //1992
    dataset.set (1,  3, 0, 100f);  //1993
    dataset.set (1,  4, 0, 200f);  //1994
    dataset.set (1,  5, 0, 400f);  //1995
    dataset.set (1,  6, 0, 500f);  //1996
    dataset.set (1,  7, 0, 700f);  //1997
    dataset.set (1,  8, 0, 900f);  //1998
    dataset.set (1,  9, 0, 100f);  //1999
    dataset.set (1, 10, 0, 200f);  //2000
    dataset.set (1, 11, 0, 300f);  //2001
    dataset.set (2,  0, 0,   0f);  //1990
    dataset.set (2,  1, 0,   0f);  //1991
    dataset.set (2,  2, 0,   0f);  //1992
    dataset.set (2,  3, 0,   0f);  //1993
    dataset.set (2,  4, 0, 100f);  //1994
    dataset.set (2,  5, 0, 200f);  //1995
    dataset.set (2,  6, 0, 300f);  //1996
    dataset.set (2,  7, 0, 400f);  //1997
    dataset.set (2,  8, 0, 500f);  //1998
    dataset.set (2,  9, 0, 100f);  //1999
    dataset.set (2, 10, 0, 300f);  //2000
    dataset.set (2, 11, 0, 900f);  //2001
    dataset.doConvertToStacked();

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoI() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Weekly LOC Programmed");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (1);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"2001", "2000", "1999"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Weeks by Month");
    graphChart2DProps.setNumbersAxisTitleText ("LOC");
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphBarsExistence (false);
    graphProps.setGraphLinesExistence (true);
    graphProps.setGraphAllowComponentAlignment (true);
    graphProps.setGraphLinesWithinCategoryOverlapRatio (1f);

    //Configure dataset
    Random random = new Random();
    Dataset dataset = new Dataset (3, 12, 4);
    for (int i = 0; i < dataset.getNumSets(); ++i) {
      for (int j = 0; j < dataset.getNumCats(); ++j) {
        for (int k = 0; k < dataset.getNumItems(); ++k) {
          int increaseMetric = dataset.getNumSets() - i - 1;
          dataset.set (i, j, k,
            (increaseMetric + 1) * random.nextInt (5) + (increaseMetric + 1) * 30 + j * 3);
        }
      }
    }

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoJ() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Weekly LOC Programmed");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (1);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"2001", "2000", "1999"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Weeks by Month");
    graphChart2DProps.setNumbersAxisTitleText ("LOC");
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphBarsExistence (false);
    graphProps.setGraphDotsExistence (true);
    graphProps.setGraphAllowComponentAlignment (true);
    graphProps.setGraphDotsWithinCategoryOverlapRatio (1f);

    //Configure dataset
    Random random = new Random();
    Dataset dataset = new Dataset (3, 12, 4);
    for (int i = 0; i < dataset.getNumSets(); ++i) {
      for (int j = 0; j < dataset.getNumCats(); ++j) {
        for (int k = 0; k < dataset.getNumItems(); ++k) {
          int increaseMetric = dataset.getNumSets() - i - 1;
          dataset.set (i, j, k,
            (increaseMetric + 1) * random.nextInt (5) + (increaseMetric + 1) * 30 + j * 3);
        }
      }
    }

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoK() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Monthly LOC Programmed");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (1);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels = {"2001", "2000", "1999"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels =
      {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Months");
    graphChart2DProps.setNumbersAxisTitleText ("LOC");
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties
    GraphProperties graphProps = new GraphProperties();
    graphProps.setGraphBarsExistence (false);
    graphProps.setGraphLinesExistence (true);
    graphProps.setGraphLinesThicknessModel (2);
    graphProps.setGraphLinesWithinCategoryOverlapRatio (1f);
    graphProps.setGraphDotsExistence (true);
    graphProps.setGraphDotsThicknessModel (10);
    graphProps.setGraphDotsWithinCategoryOverlapRatio (1f);
    graphProps.setGraphAllowComponentAlignment (true);

    //Configure dataset
    Random random = new Random();
    Dataset dataset = new Dataset (3, 12, 1);
    for (int i = 0; i < dataset.getNumSets(); ++i) {
      for (int j = 0; j < dataset.getNumCats(); ++j) {
        for (int k = 0; k < dataset.getNumItems(); ++k) {
          int increaseMetric = dataset.getNumSets() - i - 1;
          dataset.set (i, j, k,
            (increaseMetric + 1) * random.nextInt (5) + (increaseMetric + 1) * 30 + j * 3);
        }
      }
    }

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphProps);
    chart2D.addDataset (dataset);
    chart2D.addMultiColorsProperties (multiColorsProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }


  /**
   * Builds the demo chart.
   * @return The demo chart.
   */
  private Chart2D getChart2DDemoL() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("Monthly LOC | Defects Programmed");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (1);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels =
      {"Defects Java", "Defects C++", "Defects C", "LOC Java", "LOC C++", "LOC C"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure graph chart properties
    GraphChart2DProperties graphChart2DProps = new GraphChart2DProperties();
    String[] labelsAxisLabels = {"2001", "2000", "1999", "1998"};
    graphChart2DProps.setLabelsAxisLabelsTexts (labelsAxisLabels);
    graphChart2DProps.setLabelsAxisTitleText ("Months");
    graphChart2DProps.setNumbersAxisTitleText ("LOC | Defects");
    graphChart2DProps.setLabelsAxisTicksAlignment (graphChart2DProps.CENTERED);

    //Configure graph properties for line overlay
    GraphProperties graphPropsLine = new GraphProperties();
    graphPropsLine.setGraphBarsExistence (false);
    graphPropsLine.setGraphLinesExistence (true);
    graphPropsLine.setGraphAllowComponentAlignment (true);

    //Configure dataset for line overlay
    Random random = new Random();
    Dataset datasetLine = new Dataset (3, 4, 1);
    for (int i = 0; i < datasetLine.getNumSets(); ++i) {
      for (int j = 0; j < datasetLine.getNumCats(); ++j) {
        for (int k = 0; k < datasetLine.getNumItems(); ++k) {
          datasetLine.set (
            datasetLine.getNumSets() - 1 - i, j, k,
            (i + 1) * random.nextInt (5) + (i + 1) * 30 + j * 3);
        }
      }
    }

    //Configure graph component colors for line overlay
    MultiColorsProperties multiColorsPropsLine = new MultiColorsProperties();
    multiColorsPropsLine.setColorsType (multiColorsPropsLine.PASTEL);

    //Configure graph properties for bar underlay
    GraphProperties graphPropsBars = new GraphProperties();
    graphPropsBars.setGraphOutlineComponentsExistence (true);
    graphPropsBars.setGraphComponentsAlphaComposite (graphPropsBars.ALPHA_COMPOSITE_MILD);

    //Configure dataset for bar underlay
    Dataset datasetBars = new Dataset (3, 4, 1);
    for (int i = 0; i < datasetBars.getNumSets(); ++i) {
      for (int j = 0; j < datasetBars.getNumCats(); ++j) {
        for (int k = 0; k < datasetBars.getNumItems(); ++k) {
          datasetBars.set (i, j, k, (i + 1) * random.nextInt (5) + (i + 1) * 30 + j * 3);
        }
      }
    }

    //Configure graph component colors for bar underlay
    MultiColorsProperties multiColorsPropsBars = new MultiColorsProperties();

    //Configure chart
    LLChart2D chart2D = new LLChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setGraphChart2DProperties (graphChart2DProps);
    chart2D.addGraphProperties (graphPropsLine);
    chart2D.addDataset (datasetLine);
    chart2D.addMultiColorsProperties (multiColorsPropsLine);
    chart2D.addGraphProperties (graphPropsBars);
    chart2D.addDataset (datasetBars);
    chart2D.addMultiColorsProperties (multiColorsPropsBars);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    return chart2D;
  }
}