import java.util.*;
import gnu.trove.list.array.TIntArrayList;

public class CommunityStructure {
  private final double[][] theta;
  private final int topicCount;
  private final int docCount;
  private final int layers;
  private final DocumentSimilarityMeasurer simRanker;
  private final List<int[]> communityLayers;
  private final List<SparseDoubleMatrix> commThetaLayers = new ArrayList<SparseDoubleMatrix>();
  private final List<int[]> commSizesLayers = new ArrayList<int[]>();  // size of each community
  private final List<double[]> docCommCloseness = new ArrayList<double[]>(); // JS distance between doc and its comm
  private final TIntArrayList numComms = new TIntArrayList();

  public CommunityStructure(List<int[]> communityLayers, double[][] theta) {
    this.communityLayers = communityLayers;
    this.theta = theta;
    topicCount = theta.length;
    docCount = communityLayers.get(0).length;
    layers = communityLayers.size();
    
    for (int i = 0; i < layers; i++) {
      initialiseLayers(i);
    }

    simRanker = new DocumentSimilarityMeasurer(this);
    
    for (int i = 0; i < layers; i++) {
      initialiseDists(i);
    }
  }
  
  private void initialiseDists(int layer) {
    double[] dc = new double[docCount];
    docCommCloseness.add(dc);
    
    for (int doc = 0; doc < docCount; doc++) {
      int comm = communities(layer)[doc];
      dc[doc] = simRanker.JSCommDistance(comm, doc, layer);
    }
  }
  
  private void initialiseLayers(int layer) {
    int[] communities = communities(layer);
    int[] commSizes = new int[docCount];
    SparseDoubleMatrix commThetas = new SparseDoubleMatrix(topicCount, docCount);
    commSizesLayers.add(commSizes);
    commThetaLayers.add(commThetas);
    int commCnt = 0;
    
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
          commCnt++;
        }
      }
    }
    numComms.add(commCnt);
  }
  
  public double[][] theta() { return theta; }
  public SparseDoubleMatrix commThetas(int layer) { return commThetaLayers.get(layer); }
  public int[] communities(int layer) { return communityLayers.get(layer); }
  public int[] commSizes(int layer) { return commSizesLayers.get(layer); }
  public int topicCount() { return topicCount; }
  public int docCount() { return docCount; }
  public int layers() { return layers; }
  public int numComms(int layer) { return numComms.get(layer); }
  public double[] docCommCloseness(int layer) { return docCommCloseness.get(layer); }
}