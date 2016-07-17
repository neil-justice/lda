/* Can community membership predict topic usage?
 * one approach: given the theta for each community, and the theta for this node,
 * use KL divergence to find the closest community.  is it the actual community?
 */
import java.util.*;

public class CommunityPredictor {
  private final int docCount;
  private final int topicCount;
  private final int[] communities; // communities[doc] == comm of that doc
  private final int[] bestFit; // bestFit[doc] == predicted comm of that doc
  private final int[] commScore; // number of correct predictions of that community
  private final int[] commSizes; // size of each community
  private final int numComms;
  private final double[][] theta;
  private final SparseDoubleMatrix commThetas; //aggregated theta values for comms
  private int correct = 0; // no. of correct predictions
  
  public CommunityPredictor(int[] communities, double[][] theta) {
    this.communities = communities;
    this.theta = theta;
    topicCount = theta.length;
    docCount = communities.length;
    numComms = docCount; // because community numbers are not consecutive
    commSizes = new int[numComms];
    commScore = new int[numComms];
    bestFit = new int[docCount];
    commThetas = new SparseDoubleMatrix(topicCount, numComms);
  }
  
  public void run() {
    aggregate();
    getBestFit();
    System.out.println(correct + "/" + docCount + " predicted correctly.");
    printScores();
  }
  
  private void printScores() {
    for (int comm = 0; comm < numComms; comm++) {
      if (commSizes[comm] != 0) {
        System.out.println("comm " + comm + ": " + commScore[comm] + "/" + commSizes[comm]);
      }
    }
  }
  
  private void aggregate() {
    System.out.println("Aggregating...");
    for (int doc = 0; doc < docCount; doc++) {
      int comm = communities[doc];
      commSizes[comm]++;
      for (int topic = 0; topic < topicCount; topic++) {
        commThetas.add(topic, comm, theta[topic][doc]);
      }
    }
    for (int topic = 0; topic < topicCount; topic++) {
      for (int comm = 0; comm < numComms; comm++) {
        if (commSizes[comm] != 0) {
          commThetas.div(topic, comm, commSizes[comm]);
        }
      }
    }
  }
  
  // finds the closest community to each doc based on KL-divergence of the theta
  // values of the doc and the community(aggregate of docs)
  private void getBestFit() {
    int checked = 0;
    System.out.println("Calculating closest community...");
    for (int doc = 0; doc < docCount; doc++) {
      bestFit[doc] = getClosestComm(doc);
      if (bestFit[doc] == communities[doc]) {
        correct++;
        commScore[communities[doc]]++;
      }
      checked++;
      System.out.println(correct + "/" + checked + " predicted correctly.");
    }
  }
  
  private int getClosestComm(int doc) {
    double min = Double.MAX_VALUE;
    int closestComm = -1;
    
    for (int comm = 0; comm < numComms; comm++) {
      if (commSizes[comm] != 0) {
        double KLDivergence = getKLDivergence(doc, comm);
        if (KLDivergence < min) {
          min = KLDivergence;
          closestComm = comm;
        }
      }
    }
    return closestComm;
  }
  
  private double getKLDivergence(int doc, int comm) {
    double KLDivergence = 0d;
    for (int topic = 0; topic < topicCount; topic++) {
      KLDivergence += theta[topic][doc] * 
                      log2(theta[topic][doc] /  commThetas.get(topic, comm));
                      
    }
    return KLDivergence;
  }
  
  private double log2(double x) {
    return Math.log(x) / Math.log(2);
  }
}