import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.event.*;
import de.erichseifert.gral.data.*;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.io.plots.*;
import de.erichseifert.gral.graphics.Insets2D;
import java.util.*;
import java.io.*;
import java.io.FileOutputStream;

public class ThetaPlotter {
  private final int BVAL = 40; // border around graph
  private final int DEFAULT_DOC = 0; // doc loaded on startup;
  
  private final CommunityStructure structure;
  private final double[][] theta;
  private final int topicCount;
  private final int docCount;
  private int[] communities;
  private int[] commSizes;
  private SparseDoubleMatrix commThetas;
  
  private JFrame frame;
  private JList<Integer> list;
  private DrawablePanel plotPanel;
  private XYPlot plot;
  private final Random rnd = new Random();
  
  public ThetaPlotter(CommunityStructure structure) {
    this.structure = structure;
    
    theta       = structure.theta();
    topicCount  = structure.topicCount();
    docCount    = structure.docCount();
    commThetas  = structure.commThetas(1);
    communities = structure.communities(1);
    commSizes   = structure.commSizes(1);
    
    SwingUtilities.invokeLater(this::run);
  }
  
  public void run() {
    frame = new JFrame();
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
    frame.setTitle("CTUT");
    frame.add(plotPanel(), BorderLayout.CENTER);
    frame.add(listPanel(), BorderLayout.EAST);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
    addComm(communities[0]);
  }
  
  private DrawablePanel plotPanel() {
    DataTable data = new DataTable(Integer.class, Double.class);
    data.add(0, 0d);
    
    plot = new XYPlot(data);
    
    plot.getAxis(XYPlot.AXIS_X).setRange(0, topicCount - 1);
    plot.getAxis(XYPlot.AXIS_Y).setRange(0d, 1d);
    plot.getAxis(XYPlot.AXIS_Y).setAutoscaled(false);
    plot.getAxis(XYPlot.AXIS_X).setAutoscaled(false);
    plot.setInsets(new Insets2D.Double(BVAL, BVAL, BVAL, BVAL));
    plotPanel = new DrawablePanel(plot);
    return plotPanel;
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
  
  private void listenOnList(ListSelectionEvent e) {
    if (e.getValueIsAdjusting() == false) {
      int index = list.getSelectedIndex();
      if (index != -1) {
        int comm = list.getSelectedValue();
        plot.clear();
        addComm(comm);
        plotPanel.repaint();
      }
    }
  }
  
  private void savePNG(ActionEvent ae) {
    try {
      int comm = list.getSelectedValue();
      File f = new File("charts/" + comm + ".png");
      DrawableWriter writer = DrawableWriterFactory.getInstance().get("image/png");
      writer.write(plot, new FileOutputStream(f), 600, 400);      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  // adds a doc's theta values to the graph
  private void addDoc(int doc) {
    DataTable data = new DataTable(Integer.class, Double.class);
    
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, theta[topic][doc]);
    }
    
    plot.add(data);

    Color colour = randomColour();
    LineRenderer lines = new DefaultLineRenderer2D();
    data.setName("d" + doc);
    
    plot.setLineRenderers(data, lines);
    plot.getPointRenderers(data).get(0).setColor(colour);
    plot.getLineRenderers(data).get(0).setColor(colour);
  }
  
  private Color randomColour() {
    return new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
  }
  
  private void removeDoc(int doc) {
    DataSource toRemove = null;
    
    for (DataSource data: plot.getVisibleData()) {
      if (data.getName().equals("d" + doc)) {
        toRemove = data;
      }
    }
    if (toRemove != null) plot.remove(toRemove);
  }
  
  private void addComm(int comm) {
    DataTable data = new DataTable(Integer.class, Double.class);
    
    for (int topic = 0; topic < topicCount; topic++) {
      data.add(topic, commThetas.get(topic, comm));
    }
    
    int count = 0;
    for (int doc = 0; doc < docCount; doc++) {
      if (communities[doc] == comm) {
        addDoc(doc);
        count++;
      }
    }
    
    plot.add(data);
    Color colour = new Color(0f, 0f, 0f);
    LineRenderer lines = new DefaultLineRenderer2D();
    data.setName("c" + comm);
    
    plot.setLineRenderers(data, lines);
    plot.getPointRenderers(data).get(0).setColor(colour);
    plot.getLineRenderers(data).get(0).setColor(colour);
    plot.getLineRenderers(data).get(0).setStroke(new BasicStroke(3f));
  }  
}

