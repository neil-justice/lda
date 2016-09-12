/* Tempers a partition set, attempting to increase the extent to which it
 * captures text similarities without decreasing modularity too much. */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class Temperer implements Clusterer {
  // comm with entropy over this count as weak:
  private double threshold = 0.5; 
  // fraction of nodes in weak comms to move:
  private double nodesToTemper = 1;
  // modularity may not drop below this fraction of the original modularity:
  private double modThresh = 0.75; 
  // new ent must be this much better:
  private double precision = 0.001;
  // maximum fraction of nodes to move per round:
  private double maxNodes = 0.25;
  // bias < 1: biased towards JSD/ent.  bias > 1: biased towards preserving
  // modularity.
  private double bias = 0.2;
  
  private final Graph g;
  private final Random rnd = new Random();
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final double[] commEntropySum; // sum of entropy of all members
  private final double[] nodeEntropy; // entropy of each node
  private final boolean[] isWeak; // marks high-entropy communities.
  private final int[] commSize;
  private final int topicCount;
  private final double originalMod; // mod at start of tempering
  private int[] shuffledNodes;
  private int weakComms; // no. of comms with entropy over threshold
  private int totalMoves = 0;
  
  public Temperer(Graph g, double[][] inverseTheta) {
    this.g = g;
    this.inverseTheta = inverseTheta;
    topicCount = inverseTheta[0].length;
    originalMod = g.modularity();
    if (inverseTheta.length != g.order()) throw new Error("graph-theta size mismatch");
    
    communityProbSum = new double[g.order()][topicCount];
    commEntropySum = new double[g.order()];
    nodeEntropy = new double[g.order()];
    commSize = new int[g.order()];
    isWeak = new boolean[g.order()];
    
    long seed = rnd.nextLong();
    rnd.setSeed(seed);
    System.out.println("Using seed " + seed);
    
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
    boolean finished = false;
    double avgH;
    double newH;
    double newMod;
    int round = 0;
    int moves;
    buildShuffledList();
    
    long s1 = System.nanoTime();
    
    do {
      moves = 0;
      System.out.println("Tempering, round " + round);
      avgH = avgEntropy();
      moves = moveNodes();
      totalMoves += moves;
      newH = avgEntropy();
      newMod = g.modularity();
      round++;
      System.out.println("delta H: " + avgH + " -> " + newH);
      if (avgH - newH <= precision) finished = true;
      if (newMod < (originalMod * modThresh)) finished = true;
      if (moves == 0) finished = true;
    } while (!finished);
    
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
  
  public double[][] communityProbSum() { return communityProbSum; }
  public int[] commSize() { return commSize; }

  // moves a randomly selected (nodesToTemper * 100)% of the nodes in comms
  // of entropy > threshold to the communities where their presence lowers the
  // most.
  private int moveNodes() {
    System.out.println("% of nodes in weak comms to move : " + (nodesToTemper * 100) + "%");
    System.out.println("Entropy threshold for a weak comm: " + threshold);
    int success = 0;
    int total = 0;
    buildShuffledList(); // reshuffle nodes
    findWeakComms();
    int max = (int) (weakComms * nodesToTemper);
    if (max > g.order() * maxNodes) max = (int) (g.order() * maxNodes);
    
    for (int i = 0; i < g.order() && total < max; i++) {
      int node = shuffledNodes[i];
      int comm = g.community(node);
      if (isWeak[comm]) {
        if (minimiseNodeJSD(node)) success++;
        total++;
      }
      if (i % 5000 == 0) System.out.println(total + "/" + max + " moved");
    }
    
    resetWeakComms();
    System.out.printf("Mod: %5f  Comms: %d Moves:  %d%n", 
                      g.modularity() , g.numComms(), success); 
    return success;
  }
  
  // marks communities as weak if their entropy exceeds the threshold value.
  private void findWeakComms() {
    double avgModCont = g.modularity() / (g.numComms() * bias);
    
    for (int node = 0; node < g.order(); node++) {
      int comm = g.community(node);
      double H = commEntropy(communityProbSum[comm], commSize[comm]);
      if (H > threshold && g.modularityContribution(comm) < avgModCont) {
        isWeak[comm] = true;
        weakComms++;
      }
    }
  }
  
  private void resetWeakComms() {
    Arrays.fill(isWeak, false);
    weakComms = 0;    
  }
  
  // finds the community where moving the node there would lower JSD the
  // most.  Is not restricted only to neighbouring communities, but it can only
  // move nodes to other weak communities (to avoid diluting strong ones)
  private boolean minimiseNodeJSD(int node) {
    int oldComm = g.community(node);
    int newComm = -1;
    double max = 0d;
  
    for (int comm = 0; comm < g.order(); comm++) {
      if (isWeak[comm] && commSize[comm] != 0 && comm != oldComm) {
        double imp = JSD(comm) - JSD(node, comm);
        if (imp > max) {
          max = imp;
          newComm = comm;
        }
      }
    }
    
    if (newComm != -1  && newComm != oldComm) {
      move(node, newComm);
      return true;
    }
    else return false;
  }
  
  // moves the node and updates the relevant arrays.
  private void move(int node, int newComm) {
    double entropy = nodeEntropy[node];
    int oldComm = g.community(node);
    
    commSize[newComm]++;
    commSize[oldComm]--;
    commEntropySum[oldComm] -= entropy;
    commEntropySum[newComm] += entropy;
    // for fixing floating-point rounding errors:
    if (commSize[oldComm] == 0 ) commEntropySum[oldComm] = 0d;
    
    for (int topic = 0; topic < topicCount; topic++) {
      communityProbSum[oldComm][topic] -= inverseTheta[node][topic];
      communityProbSum[newComm][topic] += inverseTheta[node][topic];
      // for fixing floating-point rounding errors:
      if (commSize[oldComm] == 0 ) communityProbSum[oldComm][topic] = 0d;
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
    }
    return entropy(e);
  }

  private double entropy(double[] dist) {
    return Entropy.entropy(dist, topicCount);
  }
  
  private double avgEntropy() {
    checkSize();
    double sum = 0d;
    for (int node = 0; node < g.order(); node++) {
      int comm = g.community(node);
      sum += commEntropy(communityProbSum[comm], commSize[comm]);
    }
    
    return sum / g.order();
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
  
  private double JSD(int comm) {
    double weight = 1d / (commSize[comm]);
    
    double[] sum = new double[topicCount];
    for (int topic = 0; topic < topicCount; topic++) {
      sum[topic] = communityProbSum[comm][topic] * weight;
    }
  
    double esum = commEntropySum[comm] * weight;
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
  
  // for debugging
  private void checkSize() {
    int sum = 0;
    for (int comm = 0; comm < g.order(); comm++) {
      sum += commSize[comm];
    }
    if (sum != g.order()) throw new Error("size off: " + sum);
  }  
}