import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import de.erichseifert.gral.data.*;
import de.erichseifert.gral.plots.BarPlot;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.BarPlot.BarRenderer;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.util.GraphicsUtils;

public class JSDivergencePanel extends JPanel {

  private final DrawablePanel JSPanel;
  private final BarPlot JSPlot;
  
  private final CommunityStructure structure;
  private final int docCount;  
  private int[] communities;
  private double[] docCommCloseness;
  private int layer;
  
  public JSDivergencePanel(CommunityStructure structure) {
    super(new BorderLayout());
    this.structure = structure;
    docCount    = structure.docCount();
    
    DataTable data = new DataTable(2, Double.class);
    data.add(1d, 1d);
    JSPlot = new BarPlot(data);
    JSPlot.setInsets(new Insets2D.Double(GUI.BVAL, GUI.BVAL, GUI.BVAL, GUI.BVAL));

    JSPlot.getAxis(XYPlot.AXIS_Y).setRange(0d, 1d);
    JSPlot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
    JSPanel = new DrawablePanel(JSPlot);
    
    add(JSPanel, BorderLayout.CENTER);
  }

  public void setLayer(int layer) {
    this.layer  = layer;
    communities = structure.communities(layer);
    docCommCloseness = structure.docCommCloseness(layer);
  }
  
  public void load(int comm) {
    JSPlot.clear();
    show(comm);
    JSPanel.repaint();
  }
  
  private void show(int comm) {
    DataTable data = new DataTable(2, Double.class);
    
    double count = 0.5d;
    
    for(int doc = 0; doc < docCount; doc++) {
      if (communities[doc] == comm) {
        data.add(count, docCommCloseness[doc]);
        count++;
      }
    }
    
    JSPlot.add(data);
    Color colour = GUI.randomColour();
    Color border = GUI.randomColour();

		BarRenderer pointRenderer = (BarRenderer) JSPlot.getPointRenderers(data).get(0);
		pointRenderer.setColor(colour);
		pointRenderer.setBorderStroke(new BasicStroke(1f));
		pointRenderer.setBorderColor(border);
    JSPlot.getAxis(XYPlot.AXIS_X).setRange(0,(int) count);
  }  
}