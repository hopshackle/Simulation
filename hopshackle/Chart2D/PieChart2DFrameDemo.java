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


import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JApplet;
import javax.swing.JFrame;

import net.sourceforge.chart2d.Chart2DProperties;
import net.sourceforge.chart2d.Dataset;
import net.sourceforge.chart2d.LegendProperties;
import net.sourceforge.chart2d.MultiColorsProperties;
import net.sourceforge.chart2d.Object2DProperties;
import net.sourceforge.chart2d.PieChart2D;
import net.sourceforge.chart2d.PieChart2DProperties;


/**
 * A Chart2D demo demonstrating the PieChart2D object.
 * Container Class: JFrame<br>
 * Program Types:  Applet or Application<br>
 * Sizing:  Magnification<br>
 */
public class PieChart2DFrameDemo extends JApplet {


  private JFrame frame = null;
  private static boolean isApplet = true;


  /**
   * For running as an application.
   * Calls init() and start().
   * @param args An unused parameter.
   */
  public static void main (String[] args) {

    isApplet = false;
    PieChart2DFrameDemo demo = new PieChart2DFrameDemo();
    demo.init();
    demo.start();
  }


  /**
   * Configure the chart and frame, and open the frame.
   */
  public void init() {

    //<-- Begin Chart2D configuration -->

    //Configure object properties
    Object2DProperties object2DProps = new Object2DProperties();
    object2DProps.setObjectTitleText ("LOC per Class");

    //Configure chart properties
    Chart2DProperties chart2DProps = new Chart2DProperties();
    chart2DProps.setChartDataLabelsPrecision (-3);

    //Configure legend properties
    LegendProperties legendProps = new LegendProperties();
    String[] legendLabels =
      {"PieChart2D", "LBChart2D", "LLChart2D", "GraphChart2D", "Chart2D", "Object2D"};
    legendProps.setLegendLabelsTexts (legendLabels);

    //Configure dataset
    int numSets = 6, numCats = 1, numItems = 1;
    Dataset dataset = new Dataset (numSets, numCats, numItems);
    dataset.set (0, 0, 0, .419f);
    dataset.set (1, 0, 0, .284f);
    dataset.set (2, 0, 0, .284f);
    dataset.set (3, 0, 0, .714f);
    dataset.set (4, 0, 0, .193f);
    dataset.set (5, 0, 0, .241f);

    //Configure graph component colors
    MultiColorsProperties multiColorsProps = new MultiColorsProperties();

    //Configure pie area
    PieChart2DProperties pieChart2DProps = new PieChart2DProperties();

    //Configure chart
    PieChart2D chart2D = new PieChart2D();
    chart2D.setObject2DProperties (object2DProps);
    chart2D.setChart2DProperties (chart2DProps);
    chart2D.setLegendProperties (legendProps);
    chart2D.setDataset (dataset);
    chart2D.setMultiColorsProperties (multiColorsProps);
    chart2D.setPieChart2DProperties (pieChart2DProps);

    //Optional validation:  Prints debug messages if invalid only.
    if (!chart2D.validate (false)) chart2D.validate (true);

    //<-- End Chart2D configuration -->

    //Configure a JFrame GUI
    frame = new JFrame();
    frame.getContentPane().add (chart2D);  //Add Chart2D to GUI
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

    if (!isApplet) start();
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
}