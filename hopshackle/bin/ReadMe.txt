Description:
 Chart2D is a Java library for visualizing quantitative data using
 two-dimensional charts.  It supports pie charts, line charts, bar charts,
 dot (or scatter plot) charts, combination charts, and overlay charts.  The
 charts themselves can either be JComponents (ie for adding to content panes) or
 BufferedImages for image encoding and printing.

Contents of the Chart2D Directory:
  License.txt   file      The license under which you may use Chart2D.
  Changelog.txt file      A History of changes.
  *Demo.htm     files     HTML files for launching demos.
  *Demo.bat     files     MS Windows batch files for launching demos.
  *Demo.jpr     files     JBuilder project files for editing sources.
  Chart2D.jar   file      The jar of the "net" directory with only class files.
  net           directory Contains the sourcecode.
  javadocs      directory Contains HTML files documenting the sourcecode.
  Tutorial      directory Contains the Chart2D tutorial in HTML.

Note:  The included Chart2D.jar file only contains the core classes needed for
deployment with your application/applet.

How To Use Chart2D In Your Application:
-You probably downloaded a file with a name like Chart2D_X.Y.Z.jar.  Use jar
 to extract this file (ex. jar -xf Chart2D_X.Y.Z.jar).  The directory Chart2D
 should result.
-The Chart2D directory should have both the "net" directory and "Chart2D.jar"
 file inside of it.  One of these needs to be in your classpath, so you should
 copy/paste one of them to your working directory, or you should explicitly
 provide a path to them by various means (i.e. in your system's CLASSPATH
 environment variable).  (If you put the "net" directory in your path, then
 you'll need to compile the .java files into .class files.)
-For any java file in your application where you use Chart2D classes, place the
 line, "import net.sourceforge.chart2d.*;" near the top of it.
-LBChart2D, LLChart2D, and PieChart2D are Chart2D's main classes for creating
  different kinds of charts.
-Read the Chart2D tutorial, read the source of the demos, and/or read the
 javadocs for LBChart2D, LLChart2D, and PieChart2D.