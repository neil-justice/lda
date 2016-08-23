/* Handles one layer of modularity maximisation / entropy minimisation as part
 * of the hierarchical hybrid cluster */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class LocalMaximiser {
  private final double precision = 0.000001;
  private double threshold = 0.55; // comm with entropy over this == weak
  private double nodesToTemper = 0.2; // fraction of nodes in weak comms to move
  
  private final Graph g;
  private final Random rnd = new Random();
  private final int layer; // layer in the hierarchy this maximiser works on
  private final double[][] inverseTheta;
  private final double[][] communityProbSum; // sum of theta of all comm members
  private final double[] commEntropySum; // sum of entropy of all members
  private final double[] nodeEntropy; // entropy of each node
  private final boolean[] isWeak; // marks high-entropy communities.
  private final int[] commSize;
  private final int topicCount;
  private final boolean minimiseEnt; // turns on entropy minimisation
  private final boolean maximiseMod; // turns on modularity maximisation
  private int[] shuffledNodes;
  private int weakComms; // no. of comms with entropy over threshold
  private int totalMoves = 0;
  
  public LocalMaximiser(Graph g, double[][] inverseTheta, 
                        boolean minimiseEnt, boolean maximiseMod, int layer) {
    this.g = g;
    this.inverseTheta = inverseTheta;
    this.minimiseEnt = minimiseEnt;
    this.maximiseMod = maximiseMod;
    this.layer = layer;
    topicCount = inverseTheta[0].length;
    if (inverseTheta.length != g.order()) throw new Error("graph-theta size mismatch");
    // nodesToTemper -= layer / 10d;
    
    communityProbSum = new double[g.order()][topicCount];
    commEntropySum = new double[g.order()];
    nodeEntropy = new double[g.order()];
    commSize = new int[g.order()];
    isWeak = new boolean[g.order()];
    // check();
    fillArrays();
  }
  
  // initial values for various arrays
  private void fillArrays() {
    for (int node = 0; node < g.order(); node++) {
      // if (g.degree(node) == 0) throw new Error("0-deg node at " + node);
      int comm = g.community(node);
      if (comm != node) throw new Error("Needs a fresh graph.");
      commSize[comm] = 1;
      commEntropySum[comm] = entropy(inverseTheta[comm]);
      nodeEntropy[node] = entropy(inverseTheta[node]);
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[comm][topic] = inverseTheta[comm][topic];
      }
    }
  }
  
  // checks integrity of inverseTheta.  will not work if hyperparameters have
  // been optimised, since theta won't sum to 1.
  private void check() {
    for (int node = 0; node < g.order(); node++) {
      double sum = 0d;
      for (int topic = 0; topic < topicCount; topic++) {
        sum += inverseTheta[node][topic];
      }
      // System.out.println(sum);
      if (sum > 1.001 || sum < 0.99) {
        throw new Error("theta integrity fail: " + sum);
      }
    }
  }
  
  public void run() {
    buildShuffledList();
    
    long s1 = System.nanoTime();
    reassignCommunities();
    long e1 = System.nanoTime();
    double time = (e1 - s1) / 1000000000d;
    System.out.println("seconds taken: " + time );
  }
  
  public int totalMoves() { return totalMoves; }
  
  public SparseDoubleMatrix inverseTheta() { return getCommThetas(); }
  
  public double threshold() { return threshold; }
  
  public void setThreshold(double threshold) {this.threshold = threshold; }
  
  public double nodesToTemper() { return nodesToTemper; }
  
  public void setNodesToTemper(double nodesToTemper) { 
    this.nodesToTemper = nodesToTemper;
  }
  
  private void reassignCommunities() {
    double mod = g.modularity();
    double oldMod;
    int moves;
    boolean hasChanged;
    
    if (maximiseMod) {
      do {
        hasChanged = true;
        oldMod = mod;
        moves = maximiseModularity();
        totalMoves += moves;      
        mod = g.modularity();
        if (mod - oldMod <= precision) hasChanged = false;
        if (moves == 0) hasChanged = false;
        System.out.printf("Mod: %5f  Comms: %d Moves:  %d%n", 
                            mod , g.numComms(), moves);
      } while (hasChanged);
    }
    else totalMoves = 1; // stops the hybridclusterer ending prematurely
    
    if (minimiseEnt) minimiseEntropy();
  } 
  
  private int maximiseModularity() {
    int moves = 0;
    for (int i = 0; i < g.order(); i++) {
      int node = shuffledNodes[i];
      if (makeBestMove(node)) moves++;
    }
    return moves;
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
      if (isWeak[comm] 
      &&  commSize[comm] != 0
      &&  comm != oldComm) {
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
  
  // finds the best neighbouring community to move the node to.  The increase
  // in modularity is tempered by the decrease in entropy.
  private boolean makeBestMove(int node) {
    double max = 0d;
    
    int bestComm = -1;
    int oldComm = g.community(node);
    // double oldH = commEntropy(communityProbSum[oldComm], commSize[oldComm]) * commSize[oldComm];
    
    for (int i = 0; i < g.neighbours(node).size(); i++) {
      int comm = g.community(g.neighbours(node).get(i));
      double mod = deltaModularity(node, comm);
      double H = newEntropy(communityProbSum[comm], inverseTheta[node], 
                            commSize[comm] + 1);
      // double deltaH = oldH - H;
      // maximise mod, minimise ent:
      double inc = mod * (1d - H);
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

  // change in modularity if node is moved to community
  private double deltaModularity(int node, int community) {
    double dnodecomm = (double) g.dnodecomm(node, community);
    double ctot      = (double) g.totDegree(community);
    double wdeg      = (double) g.degree(node);
    return dnodecomm - ((ctot * wdeg) / g.m2());
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

  private SparseDoubleMatrix getCommThetas() {
    SparseDoubleMatrix commThetas = new SparseDoubleMatrix(topicCount, g.order());
    
    for (int node = 0; node < g.order(); node++) {
      int comm = g.community(node);
      for (int topic = 0; topic < topicCount; topic++) {
        commThetas.add(topic, comm, inverseTheta[node][topic]);       
      }
    }
    
    for (int comm = 0; comm < g.order(); comm++) {
      if (commSize[comm] != 0) {
        for (int topic = 0; topic < topicCount; topic++) {
          commThetas.div(topic, comm, commSize[comm]); 
        }
      }
    }
    return commThetas;
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