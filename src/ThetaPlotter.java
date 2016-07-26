import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import de.erichseifert.gral.data.*;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.BarPlot;
import de.erichseifert.gral.plots.BarPlot.BarRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.io.plots.*;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.plots.colors.QuasiRandomColors;
import java.util.*;
import java.io.*;
import java.io.FileOutputStream;

import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.graphics.Location;

public class ThetaPlotter {
  private final int BVAL = 40; // border around graph
  private final int DEFAULT_DOC = 0; // doc loaded on startup;
  
  private final CommunityStructure structure;
  private final double[][] theta;
  private final int topicCount;
  private final int docCount;
  private double[] docCommCloseness;
  private int[] communities;
  private int[] commSizes;
  private SparseDoubleMatrix commThetas;
  private int numComms;
  
  private JFrame frame;
  private JList<Integer> list;
  private DrawablePanel thetaPanel;
  private DrawablePanel JSPanel;
  private XYPlot thetaPlot;
  private BarPlot JSPlot;
  private final Random rnd = new Random();
  private int layer;
  
  public ThetaPlotter(CommunityStructure structure) {
    this.structure = structure;
    
    theta       = structure.theta();
    topicCount  = structure.topicCount();
    docCount    = structure.docCount();
    setLayer(1);

    SwingUtilities.invokeLater(this::run);
  }
  
  public void run() {
    frame = new JFrame();
    frame.setSize(800, 400);
    frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
    frame.setTitle("CTUT");
    frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
    frame.add(thetaPanel());
    frame.add(JSPanel());
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private JPanel JSPanel() {
    DataTable data = new DataTable(Integer.class, Double.class);
    data.add(1, 1d);
    JSPlot = new BarPlot(data);
    JSPlot.setInsets(new Insets2D.Double(BVAL, BVAL, BVAL, BVAL));

    JSPlot.getAxis(XYPlot.AXIS_Y).setRange(0d, 1d);
    JSPlot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
    JSPanel = new DrawablePanel(JSPlot);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(JSPanel, BorderLayout.CENTER);
    return panel;
  }
  
  private JPanel thetaPanel() {
    DataTable data = new DataTable(Integer.class, Double.class);
    data.add(0, 0d);
    
    thetaPlot = new XYPlot(data);
    
    thetaPlot.getAxis(XYPlot.AXIS_X).setRange(0, topicCount - 1);
    thetaPlot.getAxis(XYPlot.AXIS_Y).setRange(0d, 1d);
    thetaPlot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
    thetaPlot.getAxis(XYPlot.AXIS_X).setAutoscaled(false);
    thetaPlot.setInsets(new Insets2D.Double(BVAL, BVAL, BVAL, BVAL));
    thetaPanel = new DrawablePanel(thetaPlot);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(thetaPanel, BorderLayout.CENTER);
    panel.add(listPanel(), BorderLayout.EAST);
    return panel;
  }

  private JPanel listPanel() {
    DefaultListModel listModel = new DefaultListModel();
    for (int comm = 0; comm < docCount; comm++) {
      if (commSizes[comm] > 0) listModel.addElement(comm);
    }
    list = new JList<Integer>(listModel);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(0);
    list.addListSelectionListener(this::listenOnList);
    
    JScrollPane listScrollPane = new JScrollPane(list);
    JButton png = new JButton("Save as PNG");
    png.addActionListener(this::savePNG);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(listScrollPane, BorderLayout.CENTER);
    panel.add(png, BorderLayout.SOUTH);
    panel.setBorder(new EmptyBorder(BVAL, 0, BVAL, BVAL / 2));
    return panel;    
  }
  
  private void setLayer(int layer) {
    this.layer  = layer;
    commThetas  = structure.commThetas(layer);
    communities = structure.communities(layer);
    commSizes   = structure.commSizes(layer);
    numComms    = structure.numComms(layer);
    docCommCloseness = structure.docCommCloseness(layer);
  }
  
  private void listenOnList(ListSelectionEvent e) {
    if (e.getValueIsAdjusting() == false) {
      int index = list.getSelectedIndex();
      if (index != -1) loadComm();
    }
  }
  
  private void loadComm() {
    int comm = list.getSelectedValue();
    thetaPlot.clear();
    JSPlot.clear();
    addCommToThetaPlot(comm);
    addCommToJSPlot(comm);
    thetaPanel.repaint();
    JSPanel.repaint();
    System.out.println("distance from U: " + structure.commVariance(layer)[comm]);
  }

  // adds a doc's theta values to the graph
  private void addDoc(int doc) {
    DataTable data = new DataTable(Integer.class, Double.class);
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, theta[topic][doc]);
    }
    thetaPlot.add(data);
    Color colour = randomColour();
    LineRenderer lines = new DefaultLineRenderer2D();
    thetaPlot.setLineRenderers(data, lines);
    
    thetaPlot.getPointRenderers(data).get(0).setColor(colour);
    thetaPlot.getLineRenderers(data).get(0).setColor(colour);
  }
  
  private void addCommToThetaPlot(int comm) {
    DataTable data = new DataTable(Integer.class, Double.class);
    
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, commThetas.get(topic, comm));
    }
    for (int doc = 0; doc < docCount; doc++) {
      if (communities[doc] == comm) addDoc(doc);
    }
    thetaPlot.add(data);
    Color colour = new Color(0f, 0f, 0f);
    LineRenderer lines = new DefaultLineRenderer2D();
    thetaPlot.setLineRenderers(data, lines);
    thetaPlot.getPointRenderers(data).get(0).setColor(colour);
    thetaPlot.getLineRenderers(data).get(0).setColor(colour);
    thetaPlot.getLineRenderers(data).get(0).setStroke(new BasicStroke(3f));
  }
  
  private void addCommToJSPlot(int comm) {
    DataTable data = new DataTable(Double.class, Double.class);
    
    double count = 0.5d;
    for(int doc = 0; doc < docCount; doc++) {
      if (communities[doc] == comm) {
        data.add(count, docCommCloseness[doc]);
        count++;
      }
    }
    JSPlot.add(data);

    Color colour = randomColour();
    Color border = randomColour();

		BarRenderer pointRenderer = (BarRenderer) JSPlot.getPointRenderers(data).get(0);
		pointRenderer.setColor(colour);
		pointRenderer.setBorderStroke(new BasicStroke(1f));
		pointRenderer.setBorderColor(border);
    JSPlot.getAxis(XYPlot.AXIS_X).setRange(0,(int) count);
  }
  
  private void savePNG(ActionEvent ae) {
    try {
      int comm = list.getSelectedValue();
      File f = new File("charts/" + comm + ".png");
      DrawableWriter writer = DrawableWriterFactory.getInstance().get("image/png");
      writer.write(thetaPlot, new FileOutputStream(f), 600, 400);      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private Color randomColour() {
    return new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
  }  
}

