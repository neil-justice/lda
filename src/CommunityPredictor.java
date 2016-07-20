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
  private final CommunityStructure structure;
  
  public CommunityPredictor(CommunityStructure structure) {
    this.structure = structure;
    theta = structure.theta();
    topicCount = structure.topicCount();
    docCount = structure.docCount();
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
    for (int i = 0; i < structure.layers(); i++) {
      LayerPredictor lp = new LayerPredictor(structure, i);
      lp.run();
    }
  }
  
  class LayerPredictor {
    private final int[] communities;     // communities[doc] == comm of that doc
    private final int[] bestTopicInComm; // most commonly ocurring topic in each comm
    private final int[] commScore;       // number of correct predictions of that community
    private final int[] commSizes;       // size of each community
    private final SparseDoubleMatrix commThetas; //aggregated theta values for comms
    private final int layer;
    private int correct = 0; // no. of correct predictions
    
    public LayerPredictor(CommunityStructure cs, int layer) {
      this.layer = layer;
      communities = cs.communities(layer);
      commSizes = cs.commSizes(layer);
      commThetas = cs.commThetas(layer);
      commScore = new int[docCount];
      bestTopicInComm = new int[docCount];
    }
    
    public void run() {
      getBestCommTopics();
      getBestFit();
      System.out.printf("Layer %d: %d/%d = %.01f%% predicted correctly%n", layer, 
                         correct, docCount, (correct / (double) docCount) * 100);
      // printScores();
    }
    
    private void printScores() {
      for (int comm = 0; comm < docCount; comm++) {
        if (commSizes[comm] != 0) {
          System.out.println("comm " + comm + ": " + 
                             "best topic: " + bestTopicInComm[comm] + " " +
                             commScore[comm] + "/" + commSizes[comm]);
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