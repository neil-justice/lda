/* Can community membership predict topic usage?
 * one approach: given the theta for each community, and the theta for this node,
 * use KL divergence to find the closest community.  is it the actual community?
 */
import java.util.*;

public class CommunityPredictor {
  private final int docCount;
  private final int topicCount;
  private final double[][] theta;
  private final int[] bestTopicInDoc;  // most commonly ocurring topic in each doc
  private final int layers;
  
  private final List<int[]> communityLayers;
  private final List<int[]> commSizesLayers;  // size of each community
  private final List<SparseDoubleMatrix> commThetaLayers;
  private final List<int[]> bestTopicInCommLayers = new ArrayList<int[]>();
  private final List<double[]> commJSAvg;
  
  public CommunityPredictor(CommunityStructure structure) {
    theta      = structure.theta();
    topicCount = structure.topicCount();
    docCount   = structure.docCount();
    layers     = structure.layers();
    commJSAvg  = structure.commJSAvg();
    
    communityLayers = structure.communityLayers();
    commSizesLayers = structure.commSizesLayers();
    commThetaLayers = structure.commThetaLayers();
    
    bestTopicInDoc = new int[docCount];

    for (int doc = 0; doc < docCount; doc++) {
      double max = 0d;
      for (int topic = 0; topic < topicCount; topic++) {
        if (theta[topic][doc] > max) {
          max = theta[topic][doc];
          bestTopicInDoc[doc] = topic;
        }
      }
    }   
  }
  
  public void run() {
    for (int i = 0; i < layers; i++) {
      LayerPredictor lp = new LayerPredictor(i);
      bestTopicInCommLayers.add(lp.run());
    }
  }
  
  public int[] bestTopicInDoc() { return bestTopicInDoc; }
  public int bestTopicInDoc(int doc) { return bestTopicInDoc[doc]; }
  
  public int[] bestTopicInComm(int layer) { return bestTopicInCommLayers.get(layer); }
  public int bestTopicInComm(int layer, int comm) { return bestTopicInCommLayers.get(layer)[comm]; }
  
  class LayerPredictor {
    private final int[] communities;     // communities[doc] == comm of that doc
    private final int[] bestTopicInComm; // most commonly ocurring topic in each comm
    private final int[] commScore;       // number of correct predictions of that community
    private final int[] commSizes;       // size of each community
    private final SparseDoubleMatrix commThetas; //aggregated theta values for comms
    private final int layer;
    private int correct = 0; // no. of correct predictions
    
    public LayerPredictor(int layer) {
      this.layer = layer;
      communities = communityLayers.get(layer);
      commSizes   = commSizesLayers.get(layer);
      commThetas  = commThetaLayers.get(layer);
      commScore       = new int[docCount];
      bestTopicInComm = new int[docCount];
    }
    
    public int[] run() {
      getBestCommTopics();
      getBestFit();
      System.out.printf("Layer %d: %d/%d = %.01f%% predicted correctly%n", layer, 
                         correct, docCount, (correct / (double) docCount) * 100);
      printScores();
      return bestTopicInComm;
    }
    
    private void printScores() {
      for (int comm = 0; comm < docCount; comm++) {
        if (commSizes[comm] != 0) {
          System.out.println("comm: " + comm +
                             " best: " + bestTopicInComm[comm] + " " +
                             commScore[comm] + "/" + commSizes[comm] 
                              + " JS " + commJSAvg.get(layer)[comm]);
        }
      }
    }
    
    private void getBestCommTopics() {
      
      for (int comm = 0; comm < docCount; comm++) {
        double max = 0d;
        for (int topic = 0; topic < topicCount; topic++) {
          if (commThetas.get(topic, comm) > max) {
            max = commThetas.get(topic, comm);
            bestTopicInComm[comm] = topic;
          }
        }
      }    
    }
    
    // finds the closest community to each doc based on KL-divergence of the theta
    // values of the doc and the community(aggregate of docs)
    private void getBestFit() {
      int checked = 0;
      for (int doc = 0; doc < docCount; doc++) {
        int comm = communities[doc];
        if (bestTopicInDoc[doc] == bestTopicInComm[comm]) {
          correct++;
          commScore[comm]++;
        }
        checked++;
        // System.out.println(correct + "/" + checked + " predicted correctly.");
      }
    }
    
  }
}