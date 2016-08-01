import javax.swing.*;
import javax.swing.border.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;

import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.points.SizeablePointRenderer;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.graphics.Drawable;
import de.erichseifert.gral.data.comparators.Ascending;

class HeatmapPanel extends JPanel {
  private final CommunityStructure structure;
  private final int topicCount;
  private final int docCount;
  private SparseDoubleMatrix commThetas;
  private int numComms;
  private int layer;
  
  private final XYPlot plot;
  private final DrawablePanel panel;
  
  @SuppressWarnings("unchecked")
  public HeatmapPanel(CommunityStructure structure, int layer) {
    super(new BorderLayout());
    this.structure = structure;
    this.topicCount = structure.topicCount();
    this.docCount = structure.docCount();
    setLayer(layer);
    
		int count = -1;
		DataTable data = new DataTable(Double.class, Integer.class, Double.class);
		for (int comm = 0; comm < docCount; comm++) {
      for (int topic = 0; topic < topicCount; topic++) {
        if (structure.commSize(layer, comm) > GUI.MIN_SIZE 
        &&  structure.commSize(layer, comm) < GUI.MAX_SIZE) {
          count++;
          data.add(structure.entropy(layer, comm), topic, commThetas.get(topic, comm));
        }
      }
    }
    data.sort(new Ascending(0));

		plot = new XYPlot(data);
    plot.setInsets(new Insets2D.Double(GUI.BVAL, GUI.BVAL, GUI.BVAL, GUI.BVAL));

		plot.getAxis(XYPlot.AXIS_Y).setRange(-1, topicCount); 
    plot.getAxisRenderer(XYPlot.AXIS_X).setTickSpacing(0.1);
    plot.getAxisRenderer(XYPlot.AXIS_Y).setTickSpacing(1);
    
		SizeablePointRenderer pointRenderer = new SizeablePointRenderer();
		pointRenderer.setShape(new Ellipse2D.Double(-25d, -25d, 50d, 50d));
		pointRenderer.setColor(GUI.randomAlphaColour());
		pointRenderer.setColumn(2);
		plot.setPointRenderers(data, pointRenderer);
    
    panel = new DrawablePanel(plot);
    
    this.add(panel, BorderLayout.CENTER);
  }
  
  public void setLayer(int layer) {
    this.layer = layer;
    numComms = structure.numComms(layer);
    commThetas = structure.commThetas(layer);
  }
}