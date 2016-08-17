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
import de.erichseifert.gral.graphics.Label;
import de.erichseifert.gral.data.comparators.Ascending;

import java.util.*;

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
    double[] entropy = structure.entropy(layer);
    IndexComparator comp = new IndexComparator(entropy);
    Integer[] indexes = comp.indexArray();
    Arrays.sort(indexes, comp); 
    
		DataTable data = new DataTable(Integer.class, Integer.class, Double.class);
		for (int i = 0; i < docCount; i++) {
      for (int topic = 0; topic < topicCount; topic++) {
        int comm = indexes[i];
        if (structure.commSize(layer, comm) > GUI.MIN_SIZE 
        &&  structure.commSize(layer, comm) < GUI.MAX_SIZE) {
          count++;
          data.add(count, topic, commThetas.get(topic, comm));
        }
      }
    }

		plot = new XYPlot(data);
    plot.setInsets(new Insets2D.Double(GUI.BVAL / 2d, GUI.BVAL * 1.5, GUI.BVAL * 1.5, GUI.BVAL));
    
    plot.getAxisRenderer(XYPlot.AXIS_X).setLabelDistance(0d);
    plot.getAxisRenderer(XYPlot.AXIS_X).setLabel(new Label("Entropy"));
    
    Label l = new Label("Topic");
    l.setRotation(90d);
    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabelDistance(1d);
    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabel(l);
    
    // plot.getAxisRenderer(XYPlot.AXIS_X).setTickSpacing(10);
    plot.getAxisRenderer(XYPlot.AXIS_X).setIntersection(-Double.MAX_VALUE);
    
    plot.getAxisRenderer(XYPlot.AXIS_Y).setTickSpacing(2);
		plot.getAxis(XYPlot.AXIS_Y).setRange(-1, topicCount); 

		SizeablePointRenderer pointRenderer = new SizeablePointRenderer();
		pointRenderer.setShape(new Ellipse2D.Double(-25d, -25d, 50d, 50d));
		pointRenderer.setColor(GUI.randomAlphaColour());
		pointRenderer.setColumn(2); // set size based on column 2
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