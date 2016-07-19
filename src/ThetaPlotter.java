import javax.swing.*;
import java.awt.Color;
import de.erichseifert.gral.data.*;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.graphics.Insets2D;
import java.util.*;

public class ThetaPlotter {
  private final int BVAL = 40; // border around graph
  private final int DEFAULT_DOC = 0; // doc loaded on startup;
  private final double[][] theta;
  private final int topicCount;
  private final int docCount;
  private final Random rnd = new Random();
  private JFrame frame;
  private XYPlot plot;
  
  public ThetaPlotter(double[][] theta) {
    this.theta = theta;
    topicCount = theta.length;
    docCount = theta[0].length;
    SwingUtilities.invokeLater(this::run);
  }
  
  public void run() {
    frame = new JFrame();
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
    frame.setTitle("CTUT");
    frame.add(initPlot());
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
    addData(1);
    removeData(1);
  }
  
  private DrawablePanel initPlot() {
    DataTable data = buildDataTable(DEFAULT_DOC);
    
    plot = new XYPlot(data);
    plot.getTitle().setText("Document " + DEFAULT_DOC);
    displayData(data, DEFAULT_DOC);
    
    plot.getAxis(XYPlot.AXIS_X).setRange(0, topicCount - 1);
    plot.getAxis(XYPlot.AXIS_Y).setRange(0d, 1d);
    plot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
    plot.getAxis(XYPlot.AXIS_X).setAutoscaled(false);
    plot.setInsets(new Insets2D.Double(BVAL, BVAL, BVAL, BVAL));
    return new DrawablePanel(plot);
  }
  
  private DataTable buildDataTable(int doc) {
    DataTable data = new DataTable(Integer.class, Double.class);
    
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, theta[topic][doc]);
    }
    
    return data;
  }
  
  // adds a dataset to the graph
  private void addData(int doc) {
    addData(buildDataTable(doc), doc);
  }
  
  private void addData(DataTable data, int doc) {
    plot.add(data);
    displayData(data, doc);
  }
  
  private void displayData(DataTable data, int doc) {
    Color colour = randomColour();
    LineRenderer lines = new DefaultLineRenderer2D();
    data.setName("" + doc);
    
    plot.setLineRenderers(data, lines);
    plot.getPointRenderers(data).get(0).setColor(colour);
    plot.getLineRenderers(data).get(0).setColor(colour);
  }
  
  private Color randomColour() {
    return new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
  }
  
  private void removeData(int doc) {
    DataSource toRemove = null;
    
    for (DataSource data: plot.getVisibleData()) {
      if (data.getName().equals("" + doc)) {
        toRemove = data;
      }
    }
    if (toRemove != null) plot.remove(toRemove);
  }
}

