/* Handles one layer of modularity maximisation / entropy minimisation as part
 * of the hierarchical hybrid cluster */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class Temperer implements Clusterer {
  private double threshold = 0.85; // comm with entropy over this == weak
  private double nodesToTemper = 0.5; // fraction of nodes in weak comms to move
  
  private final Graph g;
  private final Random rnd = new Random();
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final double[] commEntropySum; // sum of entropy of all members
  private final double[] nodeEntropy; // entropy of each node
  private final boolean[] isWeak; // marks high-entropy communities.
  private final int[] commSize;
  private final int topicCount;
  private int[] shuffledNodes;
  private int weakComms; // no. of comms with entropy over threshold
  private int totalMoves = 0;
  
  public Temperer(Graph g, double[][] inverseTheta) {
    this.g = g;
    this.inverseTheta = inverseTheta;
    topicCount = inverseTheta[0].length;
    if (inverseTheta.length != g.order()) throw new Error("graph-theta size mismatch");
    
    communityProbSum = new double[g.order()][topicCount];
    commEntropySum = new double[g.order()];
    nodeEntropy = new double[g.order()];
    commSize = new int[g.order()];
    isWeak = new boolean[g.order()];
    
    fillArrays();
  }
  
  // initial values for various arrays
  private void fillArrays() {
    for (int node = 0; node < g.order(); node++) {
      if (g.degree(node) == 0) throw new Error("0-deg node at " + node);
      nodeEntropy[node] = entropy(inverseTheta[node]);
      int comm = g.community(node);
      commSize[comm]++;
      commEntropySum[comm] += nodeEntropy[node];
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[comm][topic] += inverseTheta[node][topic];
      }
    }
  }
  
  @Override
  public List<int[]> run() {
    buildShuffledList();
    
    long s1 = System.nanoTime();
    minimiseEntropy();
    long e1 = System.nanoTime();
    double time = (e1 - s1) / 1000000000d;
    System.out.println("seconds taken: " + time );
    
    List<int[]> list = new ArrayList<int[]>();
    list.add(g.communities());
    return list;
  }
  
  public int totalMoves() { return totalMoves; }
  
  public double threshold() { return threshold; }
  
  public void setThreshold(double threshold) {this.threshold = threshold; }
  
  public double nodesToTemper() { return nodesToTemper; }
  
  public void setNodesToTemper(double nodesToTemper) { 
    this.nodesToTemper = nodesToTemper;
  }

  // moves a randomly selected (nodesToTemper * 100)% of the nodes in comms
  // of entropy > threshold to the communities where their presence lowers the
  // most.
  private void minimiseEntropy() {
    System.out.println("% of nodes in weak comms to move : " + (nodesToTemper * 100) + "%");
    System.out.println("Entropy threshold for a weak comm: " + threshold);
    int success = 0;
    int total = 0;
    double avgH = avgEntropy();
    buildShuffledList(); // reshuffle nodes
    findWeakComms();
    int max = (int) (weakComms * nodesToTemper);
    
    for (int i = 0; i < g.order() && total < max; i++) {
      int node = shuffledNodes[i];
      int comm = g.community(node);
      if (isWeak[comm]) {
        if (minimiseNodeJSD(node)) success++;
        total++;
      }
      if (i % 500 == 0) System.out.println(total + "/" + max + " moved");
    }
    
    System.out.printf("Mod: %5f  Comms: %d Moves:  %d%n", 
                      g.modularity() , g.numComms(), success);        
    System.out.println(success + "/" + total + " moved");
    System.out.println("delta H: " + avgH + " -> " + avgEntropy());
  }
  
  // marks communities as weak if their entropy exceeds the threshold value.
  private void findWeakComms() {
    for (int i = 0; i < g.order(); i++) {
      int node = shuffledNodes[i];
      int comm = g.community(node);
      double H = commEntropy(communityProbSum[comm], commSize[comm]);
      if (H > threshold) {
        isWeak[comm] = true;
        weakComms++;
      }
    }
  }
  
  // finds the community where moving the node there would lower JSD the
  // most.  Is not restricted only to neighbouring communities, but it can only
  // move nodes to other weak communities (to avoid diluting strong ones)
  private boolean minimiseNodeJSD(int node) {
    int oldComm = g.community(node);
    int newComm = -1;
    double min = threshold;
  
    for (int comm = 0; comm < g.order(); comm++) {
      if (isWeak[comm] && commSize[comm] != 0 && comm != oldComm) {
        double jsd = JSD(node, comm);
        if (jsd < min) {
          min = jsd;
          newComm = comm;
        }
      }
    }
    
    if (newComm != -1) {
      move(node, newComm, oldComm);
      return true;
    }
    else return false;
    
  }
  
  // moves the node and updates the relevant arrays.
  private void move(int node, int newComm, int oldComm) {
    double entropy = nodeEntropy[node];
    
    commSize[newComm]++;
    commSize[oldComm]--;
    commEntropySum[oldComm] -= entropy;
    commEntropySum[newComm] += entropy;
    
    for (int topic = 0; topic < topicCount; topic++) {
      communityProbSum[oldComm][topic] -= inverseTheta[node][topic];
      communityProbSum[newComm][topic] += inverseTheta[node][topic];
    }
    
    g.moveToComm(node, newComm);
  }

  // new entropy if node were to be moved to comm
  private double newEntropy(double[] commDist, double[] nodeDist, int size) {
    double[] e = new double[topicCount];
    for (int topic = 0; topic < topicCount; topic++) {
      e[topic] = (commDist[topic] + nodeDist[topic]) / size;
    }
    return entropy(e);
  }
  
  // finds entropy of comm
  private double commEntropy(double[] commDist, int size) {
    double[] e = new double[topicCount];
    for (int topic = 0; topic < topicCount; topic++) {
      e[topic] = commDist[topic] / size;
      if (e[topic] < 0d) System.out.println("commEntropy fail: " + size + " " + e[topic] + " " + topic);
    }
    return entropy(e);
  }

  private double entropy(double[] dist) {
    return DocumentSimilarityMeasurer.entropy(dist, topicCount);
  }
  
  private double avgEntropy() {
    double sum = 0d;
    for (int comm = 0; comm < g.order(); comm++) {
      if (commSize[comm] > 0) {
        sum += commEntropy(communityProbSum[comm], commSize[comm]);
      }
    }
    
    return sum / g.numComms();
  }
  
  private double JSD(int node, int newComm) {
    double weight = 1d / (commSize[newComm] + 1);
    
    double[] sum = new double[topicCount];
    for (int topic = 0; topic < topicCount; topic++) {
      sum[topic] = (communityProbSum[newComm][topic] + inverseTheta[node][topic])
                 * weight;
    }
  
    double esum = (commEntropySum[newComm] + nodeEntropy[node]) * weight;
    return entropy(sum) - esum;
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
}