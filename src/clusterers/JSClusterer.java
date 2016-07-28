/* clusters nodes based on their JS distance */
import java.util.*;
import gnu.trove.set.hash.TIntHashSet;

public class JSClusterer implements Clusterer {
  private final Graph g;
  private final TIntHashSet[] members;  // community members
  private final double[][] theta;
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final double threshold = 0.1;
  private final int topicCount;
  private int moves = 0;
  private int comms;
  
  public JSClusterer(Graph g, double[][] theta) {
    this.g = g;
    this.theta = theta;
    topicCount = theta.length;
    members = new TIntHashSet[g.order()];
    comms = g.order();
    
    inverseTheta = new double[g.order()][topicCount];
    communityProbSum = new double[g.order()][topicCount];
    initialiseProbMatrices();
    
    for (int comm = 0; comm < g.order(); comm++) {
      members[comm] = new TIntHashSet();
      members[comm].add(comm);
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
    for (int doc = 0; doc < g.order(); doc++) {
      makeBestMove(doc);
      if (doc % 1000 == 0) System.out.println(doc);
    }
    comms = g.order();
    for (int comm = 0; comm < g.order(); comm++) {
      if (members[comm].size() == 0) comms--;
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
    if (members[oldComm].size() == 1) min = threshold;
    else min = DocumentSimilarityMeasurer.JSDivergence(inverseTheta[doc], getCommTheta(oldComm));
    
    for (comm = 0; comm < g.order() && found == false; comm++) {
      if (members[comm].size() != 0 &&  comm != oldComm) {
        dist = DocumentSimilarityMeasurer.JSDivergence(inverseTheta[doc], getCommTheta(comm));
        if (dist < min) {
          min = dist;
          newComm = comm;
          found = true;
        }
      }
    }
    
    if (dist > min) return;
    else move(doc, newComm, oldComm);
  }
  
  private void move(int doc, int newComm, int oldComm) {
    recalculateCommProbSums(doc, newComm, oldComm);
    g.moveToComm(doc, newComm);
    members[newComm].add(doc);
    members[oldComm].remove(doc);
    moves++;
  }
  
  // avg JS distance between a doc and each member of a community
  // public double JSAvgDistance(int comm, int doc) {
  //   Procedure p = new Procedure(doc);
  //   members[comm].forEach(p);
  //   return p.dist();
  // }
  // 
  // class Procedure implements TIntProcedure {
  //   private final int doc;
  //   private double js = 0;
  //   private int count = 0;
  //   
  //   public Procedure(int doc) {
  //     this.doc = doc;
  //   }
  //   
  //   @Override
  //   public boolean execute(int value) {
  //     if (doc == value) return true;
  //     js += simRanker.JSDistance(doc, value);
  //     count++;
  //     return true;
  //   }
  //   
  //   public double dist() { return js / count; }
  // }
  
  private void initialiseProbMatrices() {
    for (int doc = 0; doc < g.order(); doc++) {
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[doc][topic] = theta[topic][doc];
        inverseTheta[doc][topic] = theta[topic][doc];
      }
    }
  }
  
  private void recalculateCommProbSums(int doc, int newComm, int oldComm) {
    for (int topic = 0; topic < topicCount; topic++) {
      communityProbSum[newComm][topic] += theta[topic][doc];
      communityProbSum[oldComm][topic] -= theta[topic][doc];
    }
  }
  
  private double[] getCommTheta(int comm) {
    double[] dist = new double[topicCount];
    int size = members[comm].size();
    if (size == 1) return communityProbSum[comm];
    
    for (int topic = 0; topic < topicCount; topic++) {
      dist[topic] = communityProbSum[comm][topic] / size;
    }
    return dist;
  }
}