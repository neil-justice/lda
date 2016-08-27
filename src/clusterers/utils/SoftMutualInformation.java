/* calculates the mutual information of 2 partition sets, one of which is a 
 * soft / fuzzy / probablistic clustering */
import gnu.trove.list.array.TIntArrayList;

public class SoftMutualInformation {
  private final int N; // no. of nodes
  private final int layer;
  private final double[][] jointDistribution;
  
  // soft clustering info:
  private final double[][] theta;
  private final double[] softClusteringDistribution;
  private final int topicCount;
  
  // hard clustering info:
  private final CommunityStructure structure;
  private final int[] commSize;
  private final double[] hardClusteringDistribution;
  private final int numComms;
  
  public SoftMutualInformation(CommunityStructure structure, int layer) {
    this.structure = structure;
    N = structure.docCount();
    theta = structure.theta();
    topicCount = structure.topicCount();
    numComms = structure.numComms(layer);
    commSize = structure.commSizes(layer);
    this.layer = layer;
    
    softClusteringDistribution = softClusteringDistribution();
    hardClusteringDistribution = hardClusteringDistribution();
    jointDistribution = jointDistribution();
    System.out.println("Distributions calculated");
  }
  
  public double run() {
    long s = System.nanoTime();
    double NMI = NMI();
    long e = System.nanoTime();
    double time = (e - s) / 1000000000d;
    System.out.println("seconds taken: " + time );
    return NMI;
  }
    
  public double NMI() {
    double MI = 0d;
    for (int topic = 0; topic < topicCount; topic++) {
      double soft = softClusteringDistribution[topic];
      for (int i = 0; i < numComms; i++) {
        double joint = jointDistribution[topic][i];
        double hard = hardClusteringDistribution[i];
        MI += joint * log(joint / (hard * soft));
      }
    }
    // return normalise(MI / Math.log(2));
    return MI / Math.log(2);
  }
  
  private double normalise(double MI) {
    double e1 = DocumentSimilarityMeasurer.entropy(softClusteringDistribution);
    double e2 = DocumentSimilarityMeasurer.entropy(hardClusteringDistribution);
    return MI / ((e1 + e2) * 0.5);
  }  
  
  private double[][] jointDistribution() {
    double[][] dist = new double[topicCount][numComms];
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int i = 0; i < numComms; i++) {
        int comm = structure.commIndex(layer, i);
        dist[topic][i] = joint(topic, comm);
      }
    }
    return dist;
  }
  
  private double joint(int topic, int comm) {
    double joint = 0;
    for (int doc = 0; doc < N; doc++) {
      if (structure.community(layer, doc) == comm) joint += theta[topic][doc];
    }
    return joint;
  }
  
  private double[] hardClusteringDistribution() {
    double[] dist = new double[numComms];
    
    for (int i = 0; i < numComms; i++) {
      int comm = structure.commIndex(layer, i);
      dist[i] = (double) commSize[comm] / N;
    }
    
    return dist;
  }
  
  private double[] softClusteringDistribution() {
    double[] dist = new double[topicCount];
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < N; doc++) {
        dist[topic] += theta[topic][doc];
      }
      dist[topic] /= N;
    }
    return dist;
  }
  
  // in information theory, 0 log(0) == 0.
  // this happens because often comm1 and comm2 have no nodes in common
  private double log(double val) {
    if (val == 0) return 0;
    else return Math.log(val);
  }
}