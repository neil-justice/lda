/* seeks to maximise modularity and minimise entropy simultaneously */
import java.util.*;

public class HybridClusterer implements Clusterer {
  private final double precision = 0.000001;
  private final Graph g;
  private int[] shuffledNodes;
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final int[] commSize;
  private final int topicCount;
  private int totalMoves = 0;
  private final Random rnd = new Random();
  
  public HybridClusterer(Graph g, double[][] inverseTheta) {
    this.g = g;
    this.inverseTheta = inverseTheta;
    topicCount = inverseTheta[0].length;
    communityProbSum = new double[g.order()][topicCount];
    commSize = new int[g.order()];
    
    for (int node = 0; node < g.order(); node++) {
      int comm = g.community(node);
      commSize[comm] = 1;
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[comm][topic] = inverseTheta[node][topic];
      }
    }
  }  
  
  @Override
  public List<int[]> run() {
    buildShuffledList();
    
    long s1 = System.nanoTime();
    reassignCommunities();
    long e1 = System.nanoTime();
    double time = (e1 - s1) / 1000000000d;
    System.out.println("seconds taken: " + time );
    
    List<int[]> list = new ArrayList<int[]>();
    list.add(g.communities());
    return list;
  }
  
  private void reassignCommunities() {
    double mod = g.modularity();
    double oldMod;
    int moves;
    boolean hasChanged;
    
    do {
      hasChanged = true;
      oldMod = mod;
      moves = maximise();
      totalMoves += moves;      
      mod = g.modularity();
      if (mod - oldMod <= precision) hasChanged = false;
      if (moves == 0) hasChanged = false;
      System.out.printf("Mod: %5f  Comms: %d Moves:  %d%n", 
                          mod , g.numComms(), moves);
    } while (hasChanged);
  } 
  
  private int maximise() {
    int moves = 0;
    for (int i = 0; i < g.order(); i++) {
      int node = shuffledNodes[i];
      if (makeBestMove(node)) moves++;
    }
    return moves;
  }
  
  public void buildShuffledList() {
    int count = g.order();
    shuffledNodes = new int[count];
    
    for (int i = 0; i < count; i++) {
      shuffledNodes[i] = i;
    }
    
    for (int i = count; i > 1; i--) {
      int r = rnd.nextInt(i);
      swap(shuffledNodes , i - 1, r);
    }
  }

  private void swap(int[] a, int i, int j) {
    int temp = a[i];
    a[i] = a[j];
    a[j] = temp;
  }    
  
  private boolean makeBestMove(int node) {
    double max = 0d;
    
    int bestComm = -1;
    int oldComm = g.community(node);
    
    for (int i = 0; i < g.neighbours(node).size(); i++) {
      int comm = g.community(g.neighbours(node).get(i));
      double mod = deltaModularity(node, comm);
      double ent = newEntropy(communityProbSum[comm], inverseTheta[node], 
                              commSize[comm] + 1);
      // System.out.println(mod + " " + ent);
      // maximise mod, minimise ent:
      double inc = mod * ((1d - ent) * 2900d);
      if (inc > max) {
        max = inc;
        bestComm = comm;
      }
    }
    
    if (bestComm > 0 && bestComm != oldComm) {
      move(node, bestComm, oldComm);
      return true;
    }
    else return false;
  }
  
  private void move(int doc, int newComm, int oldComm) {
    commSize[newComm]++;
    commSize[oldComm]--;
    
    for (int topic = 0; topic < topicCount; topic++) {
      communityProbSum[oldComm][topic] -= inverseTheta[doc][topic];
      communityProbSum[newComm][topic] += inverseTheta[doc][topic];
    }
    
    g.moveToComm(doc, newComm);
  }

  // change in modularity if node is moved to community
  private double deltaModularity(int node, int community) {
    double dnodecomm = (double) g.dnodecomm(node, community);
    double ctot      = (double) g.totDegree(community);
    double wdeg      = (double) g.degree(node);
    return dnodecomm - ((ctot * wdeg) / g.m2());
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