import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.data.*;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.graphics.Drawable;
import de.erichseifert.gral.util.GraphicsUtils;

public class ThetaPlotPanel extends JPanel {
  
  private final DrawablePanel thetaPanel;
  private final XYPlot thetaPlot;

  private final CommunityStructure structure;  
  private final double[][] theta;
  private final int topicCount;
  private final int docCount;  
  private int[] communities;
  private SparseDoubleMatrix commThetas;
  private int layer;

  public ThetaPlotPanel(CommunityStructure structure) {
    super(new BorderLayout());
    this.structure = structure;
    
    theta       = structure.theta();
    topicCount  = structure.topicCount();
    docCount    = structure.docCount();
    setLayer(1);
   
    DataTable data = newDataTable();
    data.add(0, 0d);
    
    thetaPlot = new XYPlot(data);
    
    thetaPlot.getAxis(XYPlot.AXIS_X).setRange(0, topicCount - 1);
    thetaPlot.getAxis(XYPlot.AXIS_Y).setRange(0d, 1d);
    thetaPlot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
    thetaPlot.getAxis(XYPlot.AXIS_X).setAutoscaled(false);
    thetaPlot.setInsets(new Insets2D.Double(GUI.BVAL, GUI.BVAL, GUI.BVAL, GUI.BVAL));
    thetaPanel = new DrawablePanel(thetaPlot);
    
    this.add(thetaPanel, BorderLayout.CENTER);
  }
  
  public Drawable drawable() { return thetaPlot; }
  
  public void setLayer(int layer) {
    this.layer  = layer;
    commThetas  = structure.commThetas(layer);
    communities = structure.communities(layer);
  }
  
  public void load(int comm) {
    thetaPlot.clear();
    show(comm);
    thetaPanel.repaint();
  }  
  
  // adds a doc's theta values to the graph
  private void showDoc(int doc) {
    DataTable data = newDataTable();
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, theta[topic][doc]);
    }
    thetaPlot.add(data);
    Color colour = GUI.randomColour();
    LineRenderer lines = new DefaultLineRenderer2D();
    thetaPlot.setLineRenderers(data, lines);
    
    thetaPlot.getPointRenderers(data).get(0).setColor(colour);
    thetaPlot.getLineRenderers(data).get(0).setColor(colour);
  }
  
  private void show(int comm) {
    DataTable data = newDataTable();
    
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, commThetas.get(topic, comm));
    }
    for (int doc = 0; doc < docCount; doc++) {
      if (communities[doc] == comm) showDoc(doc);
    }
    thetaPlot.add(data);
    Color colour = new Color(0f, 0f, 0f);
    LineRenderer lines = new DefaultLineRenderer2D();
    thetaPlot.setLineRenderers(data, lines);
    thetaPlot.getPointRenderers(data).get(0).setColor(colour);
    thetaPlot.getLineRenderers(data).get(0).setColor(colour);
    thetaPlot.getLineRenderers(data).get(0).setStroke(new BasicStroke(3f));
  }
  
  // varargs and generics do not mix well in java
  @SuppressWarnings("unchecked")
  private DataTable newDataTable() {
    return new DataTable(Integer.class, Double.class);
  }
}