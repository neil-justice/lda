import javax.swing.*;
import javax.swing.border.*;
import java.text.DecimalFormat;

class CommInfoPanel extends JPanel {
  private final CommunityStructure structure;
  private int layer;
  private final int topicCount;
  private final int docCount;
  private int commSize;
  private int numComms;
  private double JSDiv;
  private double JSImp;
  private double entropy;
  private int score;
  private int comm;
  private JLabel[] labels = new JLabel[7]; //no. of fields to display
  
  public CommInfoPanel(CommunityStructure structure) {
    super();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EtchedBorder());
    this.structure = structure;
    docCount = structure.docCount();
    topicCount = structure.topicCount();
    
    for (int label = 0; label < labels.length; label++) {
      labels[label] = new JLabel();
      add(labels[label]);
    }
    updateLabels();
  }
  
  public void setLayer(int layer) {
    this.layer = layer;
    numComms = structure.numComms(layer);
    clearLabels();
  }
  
  public void load(int comm) {
    this.comm = comm;
    JSDiv = structure.JSDiv(layer, comm);
    JSImp = structure.JSImp(layer, comm);
    commSize = structure.commSize(layer, comm);
    score = structure.commScore(layer, comm);
    entropy = structure.entropy(layer, comm);
    updateLabels();
  }
  
  private void updateLabels() {
    DecimalFormat f = new DecimalFormat("#.##");
    double perc = (score / (double) commSize) * 100;
    
    labels[0].setText("Layer: " + layer);
    labels[1].setText("Comms: " + numComms);
    labels[2].setText("score: " + score + "/" + commSize);
    labels[3].setText("percentage: " + f.format(perc) + "%");
    labels[4].setText("JSDiv: " + f.format(JSDiv));
    labels[5].setText("JSImp: " + f.format(JSImp));
    labels[6].setText("Entropy: " + f.format(entropy));
  }
  
  private void clearLabels() {
    for (int label = 0; label < labels.length; label++) {
      labels[label].setText("");
    }
  }
}