/* 'clusters' nodes such that each community contains only 1 node, 1 node
 * per community, unless entropy is over a certain threshold - all of these
 * nodes are put in 1 community together. */ 
import java.util.*;

public class TrivialEntropyMinimiser implements Clusterer {
  private final int order;
  private final int topicCount;
  private final int[] community;
  private final double threshold = 0.6;
  private final double[] entropy;
  private final double[][] inverseTheta;
  private int highComm = -1; // the community which holds all high-entropy nodes
  private int count = 0;
  
  public TrivialEntropyMinimiser(int order, double[][] theta) {
    this.order = order;
    community = new int[order];
    entropy = new double[order];
    inverseTheta = MatrixTransposer.transpose(theta);
    topicCount = theta.length;
  }
  
  @Override
  public List<int[]> run() { 
    List<int[]> list = new ArrayList<>();
    list.add(community);
    
    for (int node = 0; node < order; node++) {
      entropy[node] = DocumentSimilarityMeasurer.entropy(inverseTheta[node], 
                                                         topicCount);
      if (entropy[node] <= threshold) community[node] = node;
      else {
        if (highComm == -1) highComm = node;
        community[node] = highComm;
        count++;
      }
    }
    
    System.out.println("" + count + " above threshold");
    return list;
  }
}