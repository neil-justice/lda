import java.util.*;

public class CommunityStructure {
  private final double[][] theta;
  private final int topicCount;
  private final int docCount;
  private final int layers;
  private final List<int[]> communityLayers;
  private final List<SparseDoubleMatrix> commThetaLayers;
  private final List<int[]> commSizesLayers; // size of each community

  public CommunityStructure(List<int[]> communityLayers, double[][] theta) {
    this.communityLayers = communityLayers;
    this.theta = theta;
    topicCount = theta.length;
    docCount = communityLayers.get(0).length;
    commSizesLayers = new ArrayList<int[]>();   
    commThetaLayers = new ArrayList<SparseDoubleMatrix>();
    layers = communityLayers.size();
    
    for (int i = 0; i < layers; i++) {
      calculateCommThetas(i);
    }
  }
  
  private void calculateCommThetas(int layer) {
    int[] communities = communities(layer);
    int[] commSizes = new int[docCount];
    SparseDoubleMatrix commThetas = new SparseDoubleMatrix(topicCount, docCount);
    commSizesLayers.add(commSizes);
    commThetaLayers.add(commThetas);
    
    for (int doc = 0; doc < docCount; doc++) {
      int comm = communities[doc];
      commSizes[comm]++;
      for (int topic = 0; topic < topicCount; topic++) {
        commThetas.add(topic, comm, theta[topic][doc]);
      }
    }
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int comm = 0; comm < docCount; comm++) {
        if (commSizes[comm] != 0) {
          commThetas.div(topic, comm, commSizes[comm]);
        }
      }
    }    
  }
  
  public double[][] theta() { return theta; }
  public SparseDoubleMatrix commThetas(int layer) { return commThetaLayers.get(layer); }
  public int[] communities(int layer) { return communityLayers.get(layer); }
  public int[] commSizes(int layer) { return commSizesLayers.get(layer); }
  public int topicCount() { return topicCount; }
  public int docCount() { return docCount; }
  public int layers() { return layers; }
}