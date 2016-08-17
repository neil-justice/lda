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
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.graphics.Drawable;
import de.erichseifert.gral.graphics.Label;
import de.erichseifert.gral.data.comparators.Ascending;

import java.util.*;

class TopicPanel extends JPanel {
  private final int topicCount;
  private final int wordCount;
  private final double[][] phi;
  
  private final XYPlot plot;
  private final InteractivePanel panel;
  
  @SuppressWarnings("unchecked")
  public TopicPanel(double[][] phi) {
    super(new BorderLayout());
    this.wordCount = phi.length;
    this.topicCount = phi[0].length;
    this.phi = phi;
    
		DataTable data = new DataTable(Integer.class, Integer.class, Double.class);
		for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        data.add(topic, word, phi[word][topic]);
      }
    }

		plot = new XYPlot(data);
    plot.setInsets(new Insets2D.Double(GUI.BVAL / 2d, GUI.BVAL * 1.5, GUI.BVAL * 1.5, GUI.BVAL));
    
    plot.getAxisRenderer(XYPlot.AXIS_X).setLabelDistance(0d);
    plot.getAxisRenderer(XYPlot.AXIS_X).setLabel(new Label("Topic"));
    
    Label l = new Label("Word");
    l.setRotation(90d);
    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabelDistance(1d);
    plot.getAxisRenderer(XYPlot.AXIS_Y).setLabel(l);
    
    plot.getAxisRenderer(XYPlot.AXIS_X).setTickSpacing(1);
		plot.getAxis(XYPlot.AXIS_X).setRange(-1, topicCount); 
    plot.getAxisRenderer(XYPlot.AXIS_Y).setIntersection(-Double.MAX_VALUE);
    
    plot.getAxisRenderer(XYPlot.AXIS_Y).setTickSpacing(500);

		SizeablePointRenderer pointRenderer = new SizeablePointRenderer();
		pointRenderer.setShape(new Ellipse2D.Double(-100d, -100d, 200d, 200d));
		pointRenderer.setColor(GUI.randomAlphaColour());
		pointRenderer.setColumn(2); // set size based on column 2
		plot.setPointRenderers(data, pointRenderer);
    
    panel = new InteractivePanel(plot);
    
    this.add(panel, BorderLayout.CENTER);
  }
}