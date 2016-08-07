/* clusters nodes based on their JS distance */
import java.util.*;

public class EntropyTemperer implements Clusterer {
  private final Graph g;
  private final CommunityStructure structure;
  private final int layer;
  private final double[][] theta;
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final int[] commSize;
  private final double minDiff = 0.05;
  private final double threshold = 0.3;
  private final int maxRuns = 5;
  private final int topicCount;
  private int moves = 0;
  private int comms;
  
  public EntropyTemperer(Graph g, CommunityStructure structure, int layer) {
    this.g = g;
    this.structure = structure;
    this.layer = layer;
    theta = structure.theta();
    topicCount = structure.topicCount();
    comms = g.order();
    
    inverseTheta = structure.inverseTheta();
    commSize = structure.commSizes(layer);
    communityProbSum = new double[g.order()][topicCount];
    initialise();
  }
  
  private void initialise() {
    for (int doc = 0; doc < g.order(); doc++) {
      int comm = structure.community(layer, doc);
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[comm][topic] += structure.theta(topic, doc);
      }
    }
  }
  
  @Override
  public List<int[]> run() {
    int runs = 0;
    
    do { 
      moves = 0;
      cluster();
      runs++;
    } while (moves > 1 && maxRuns > runs);
    
    List<int[]> list = new ArrayList<int[]>();
    list.add(g.communities());
    return list;
  }
  
  private void cluster() {
    long s1 = System.nanoTime();
    moveAll();
    long e1 = System.nanoTime();
    double time = (e1 - s1) / 1000000000d;
    System.out.println("seconds taken: " + time );    
  }
  
  private void moveAll() {
    comms = g.order();
    
    for (int doc = 0; doc < g.order(); doc++) {
      makeBestMove(doc);
    }
    
    for (int comm = 0; comm < g.order(); comm++) {
      if (commSize[comm] == 0) comms--;
    }
    
    System.out.println("Round finished. " + moves + " moves made.");
    System.out.println("" + comms + " communities, mod: " + g.modularity());
  }
  
  private void makeBestMove(int doc) {
    int oldComm = g.community(doc);
    int newComm = -1;
    double min;
    double dist;

    if (commSize[oldComm] == 1) min = threshold;
    else min = entropy(communityProbSum[oldComm]) - minDiff;
    
    for (int i = 0; i < g.neighbours(doc).size(); i++) {
      int comm = g.community(g.neighbours(doc).get(i));    
      if (commSize[comm] != 0 && comm != oldComm) {
        dist = newEntropy(communityProbSum[comm], inverseTheta[doc], commSize[comm] + 1);
        if (dist < min) {
          min = dist;
          newComm = comm;
        }
      }
    }
    
    if (newComm != -1) move(doc, newComm, oldComm);
  }
  
  private void move(int doc, int newComm, int oldComm) {
    
    commSize[newComm]++;
    commSize[oldComm]--;
    
    for (int topic = 0; topic < topicCount; topic++) {
      communityProbSum[oldComm][topic] -= theta[topic][doc];
      communityProbSum[newComm][topic] += theta[topic][doc];
    }
    
    g.moveToComm(doc, newComm);
    moves++;
  }

  private double newEntropy(double[] commDist, double[] docDist, int size) {
    double[] e = new double[topicCount];
    for (int topic = 0; topic < topicCount; topic++) {
      e[topic] = (commDist[topic] + docDist[topic]) / size;
    }
    return entropy(e);
  }
  
  private double entropy(double[] dist) {
    return DocumentSimilarityMeasurer.entropy(dist, topicCount);
  }
}