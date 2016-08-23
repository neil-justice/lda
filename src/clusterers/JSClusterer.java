/* clusters nodes based on their JS distance */
import java.util.*;

public class JSClusterer implements Clusterer {
  private final Graph g;
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final double[] commEntropy; // sum of entropy of all comm members
  private final int[] commSize;
  private final double threshold = 0.1;
  private final int topicCount;
  private int moves = 0;
  private int comms;
  
  public JSClusterer(Graph g, double[][] inverseTheta) {
    this.g = g;
    this.inverseTheta = inverseTheta;
    topicCount = inverseTheta[0].length;
    comms = g.order();
    
    communityProbSum = new double[g.order()][topicCount];
    commEntropy = new double[g.order()];
    commSize = new int[g.order()];
    initialise();
  }
  
  private void initialise() {
    for (int doc = 0; doc < g.order(); doc++) {
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[doc][topic] = inverseTheta[doc][topic];
      }
      commEntropy[doc] = entropy(inverseTheta[doc]);
      commSize[doc] = 1;
    }
  }
  
  @Override
  public List<int[]> run() {
    cluster();
    
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
      if (doc % 1000 == 0) System.out.println(doc);
    }
    
    for (int comm = 0; comm < g.order(); comm++) {
      if (commSize[comm] == 0) comms--;
    }
    
    System.out.println("Round finished. " + moves + " moves made.");
    System.out.println("" + comms + " communities, mod: " + g.modularity());
  }
  
  private void makeBestMove(int doc) {
    int comm;
    int oldComm = g.community(doc);
    int newComm = -1;
    double min;
    double dist = 1d;
    boolean found = false;

    if (commSize[oldComm] == 1) min = threshold;
    else min = JSD(doc, oldComm, commSize[oldComm]);
    
    for (comm = 0; comm < g.order() && found == false; comm++) {
      if (commSize[comm] != 0 && comm != oldComm) {
        dist = JSD(doc, comm, commSize[comm] + 1);
        if (dist < min) {
          min = dist;
          newComm = comm;
          found = true;
        }
      }
    }
    
    if (dist <= min) move(doc, newComm, oldComm);
  }
  
  private void move(int doc, int newComm, int oldComm) {
    double entropy = entropy(inverseTheta[doc]);
    
    commSize[newComm]++;
    commSize[oldComm]--;
    commEntropy[oldComm] -= entropy;
    commEntropy[newComm] += entropy;
    
    for (int topic = 0; topic < topicCount; topic++) {
      communityProbSum[oldComm][topic] -= inverseTheta[doc][topic];
      communityProbSum[newComm][topic] += inverseTheta[doc][topic];
    }
    
    g.moveToComm(doc, newComm);
    moves++;
  }

  private double JSD(int doc, int newComm, int size) {
    double weight = 1d / size;
    double entropy = entropy(inverseTheta[doc]);
    
    double[] sum = new double[topicCount];
    for (int topic = 0; topic < topicCount; topic++) {
      sum[topic] = (communityProbSum[newComm][topic] + inverseTheta[doc][topic])
                 * weight;
    }

    double esum = (commEntropy[newComm] + entropy) * weight;

    return entropy(sum) - esum;
  }
  
  private double entropy(double[] dist) {
    return DocumentSimilarityMeasurer.entropy(dist);
  }
}