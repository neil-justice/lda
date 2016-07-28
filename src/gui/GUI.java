import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;

import de.erichseifert.gral.io.plots.*;
import de.erichseifert.gral.graphics.Insets2D;
import java.util.*;
import java.io.*;
import java.io.FileOutputStream;

import gnu.trove.list.array.TIntArrayList;

import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.graphics.Insets2D;
import de.erichseifert.gral.graphics.Location;

public class GUI {
  public static final int BVAL = 40; // border around graph
  
  private final CommunityStructure structure;
  private static final Random rnd = new Random();
  private final int docCount;
  private int[] commSizes;
  private int layer;
  
  private JFrame frame;
  private JList<Integer> list;
  private final ThetaPlotPanel thetaPanel;
  private final JSDivergencePanel JSPanel;
  private final CommInfoPanel infoPanel;
  private final HeatmapPanel heatmap;
  
  public GUI(CommunityStructure structure) {
    this.structure = structure;
    
    thetaPanel = new ThetaPlotPanel(structure);
    JSPanel = new JSDivergencePanel(structure);
    infoPanel = new CommInfoPanel(structure);
    heatmap = new HeatmapPanel(structure, 1);
    docCount = structure.docCount();
    setLayer(1);

    SwingUtilities.invokeLater(this::run);
  }
  
  private void setLayer(int layer) {
    this.layer  = layer;
    thetaPanel.setLayer(layer);
    JSPanel.setLayer(layer);
    infoPanel.setLayer(layer);
    heatmap.setLayer(layer);
    
    commSizes = structure.commSizes(layer);
  }
  
  public void run() {
    frame = new JFrame();
    frame.setSize(800, 400);
    frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
    frame.setTitle("CTUT - Community Topic Usage Tracker");
    frame.setLayout(new BorderLayout());
    frame.add(chartPanel(), BorderLayout.CENTER);
    frame.add(listPanel(), BorderLayout.EAST);
    frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    frame.setVisible(true);
  }
  
  private JPanel chartPanel() {
    JPanel charts = new JPanel(new GridLayout(1,2));
    charts.add(thetaPanel);
    charts.add(JSPanel);
    JPanel all = new JPanel(new GridLayout(2,1));
    all.add(charts);
    all.add(heatmap);
    return all;
  }
  
  private void load() {
    int comm = list.getSelectedValue();
    thetaPanel.load(comm);
    JSPanel.load(comm);
    infoPanel.load(comm);
  }  
    
  private JPanel listPanel() {
    DefaultListModel<Integer> listModel = new DefaultListModel<Integer>();
    
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
    panel.add(infoPanel, BorderLayout.NORTH);
    panel.setBorder(new EmptyBorder(BVAL, 0, BVAL, BVAL / 2));
    
    return panel;    
  }

  private void listenOnList(ListSelectionEvent e) {
    if (e.getValueIsAdjusting() == false) {
      int index = list.getSelectedIndex();
      if (index != -1) load();
    }
  }
  
  private void savePNG(ActionEvent ae) {
    try {
      int comm = list.getSelectedValue();
      File f = new File("charts/" + comm + ".png");
      DrawableWriter writer = DrawableWriterFactory.getInstance().get("image/png");
      writer.write(thetaPanel.drawable(), new FileOutputStream(f), 600, 400);      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static Color randomColour() {
    return new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
  }
  
  public static Color randomAlphaColour() {
    return new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat(), 0.8f);
  }
}

