/* Can community membership predict topic usage?
 * one approach: given the theta for each community, and the theta for this node,
 * use KL divergence to find the closest community.  is it the actual community?
 */
import gnu.trove.list.array.TIntArrayList;
import java.util.*;

public class CommunityPredictor {
  private final int docCount;
  private final int topicCount;
  private final List<TIntArrayList> communities = null;
  
  public CommunityPredictor(int docCount, int topicCount) {
    this.docCount = docCount;
    this.topicCount = topicCount;
  }
  
  // aggregate theta values for documents to get community values
  private void aggregate() {
    
  }
  
  // finds the distance between each document
  private double[][] KLDivergence(double[][] theta) {
    double[][] KLDiv = new double[docCount][docCount];
    for (int doc1 = 0; doc1 < docCount; doc1++) {
      for (int doc2 = 0; doc2 < docCount; doc2++) {
        for (int topic = 0; topic < topicCount; topic++) {
          KLDiv[doc1][doc2] += theta[topic][doc1] * log2(theta[topic][doc1] / theta[topic][doc2]);
        }
      }
    }
    
    return KLDiv;
  }
  
  private double log2(double x) {
    return Math.log(x) / Math.log(2);
  }
}