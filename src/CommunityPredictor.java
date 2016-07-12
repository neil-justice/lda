/* Can community membership predict topic usage?
 * one approach: given the theta for each community, and the theta for this node,
 * use KL divergence to find the closest community.  is it the actual community?
 */
import gnu.trove.list.array.TIntArrayList;
import java.util.*;

public class CommunityPredictor {
  private final int docCount;
  private final int topicCount;
  private final List<TIntArrayList> communities;
  private final int numComms;
  private final double[][] theta;
  private final double[][] commThetas; //aggregated theta values for comms
  private final double[][] KLDivergence; // K-L distance between each doc and each comm
  
  public CommunityPredictor(int docCount, int topicCount,
                            List<TIntArrayList> communities, double[][] theta) {
    this.docCount = docCount;
    this.topicCount = topicCount;
    this.communities = communities;
    this.theta = theta;
    this.numComms = communities.size();
    commThetas = new double[topicCount][numComms];
    KLDivergence = new double[docCount][numComms];
  }
  
  public void run() {
    aggregateAll();
    KLDivergence();
  }
  
  // aggregate theta values for documents to get community values
  private void aggregateAll() {
    for (int comm = 0; comm < numComms; comm++) {
      aggregate(communities.get(comm), comm);
    }
  }
  
  private void aggregate(TIntArrayList community, int comm) {
    int size = community.size();
    for (int i = 0; i < size; i++) {
      int doc = community.get(i);
      for (int topic = 0; topic < topicCount; topic++) {
        commThetas[topic][comm] += theta[topic][doc];
      }
    }
    for (int topic = 0; topic < topicCount; topic++) {
      commThetas[topic][comm] /= size;
    }
  }
  
  // finds the distance between each document and each community
  private void KLDivergence() {
    for (int doc = 0; doc < docCount; doc++) {
      for (int comm = 0; comm < numComms; comm++) {
        for (int topic = 0; topic < topicCount; topic++) {
          KLDivergence[doc][comm] += theta[topic][doc] 
                                  *  log2(theta[topic][doc] 
                                  / commThetas[topic][comm]);
        }
      }
    }
  }
  
  private double log2(double x) {
    return Math.log(x) / Math.log(2);
  }
}