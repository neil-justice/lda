/* seeks to maximise modularity and minimise entropy simultaneously */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class HybridClusterer implements Clusterer {
  private final double precision = 0.000001;
  private final Random rnd = new Random();
  private final int topicCount;
  private final List<double[][]> inverseThetas = new ArrayList<>();
  private final List<Graph> graphs = new ArrayList<Graph>();
  // maps between communities on L and nodes on L + 1:
  private final List<TIntIntHashMap> layerMaps = new ArrayList<>();
  private final List<int[]> communities = new ArrayList<int[]>();  
  private int totalMoves = 0;
  private int layer = 0; // current community layer  
  
  public HybridClusterer(Graph g, double[][] inverseTheta) {
    graphs.add(g);
    inverseThetas.add(inverseTheta);
    topicCount = inverseTheta[0].length;
  }

  @Override
  public List<int[]> run() { return run(9999); }
  
  public List<int[]> run(int maxLayers) {
    if (maxLayers <= 0) return null;
    System.out.printf("Detecting graph communities...");
    
    do {
      totalMoves = 0;
      System.out.printf("Round %d:%n", layer);
      Maximiser m = new Maximiser(graphs.get(layer), inverseThetas.get(layer));
      SparseDoubleMatrix newTheta = m.run();
      if (totalMoves > 0 && maxLayers >= layer) addNewLayer(newTheta);
    }
    while (totalMoves > 0 && maxLayers >= layer);
    
    buildCommunityList();
    return communities;
  }

  private void addNewLayer(SparseDoubleMatrix newTheta) {
    Graph last = graphs.get(layer);
    TIntIntHashMap map = createLayerMap(last);
    layerMaps.add(map);
    layer++;
    Graph coarse = new GraphBuilder().coarseGrain(last, map).build();
    graphs.add(coarse);
    inverseThetas.add(mapTheta(newTheta, coarse.order(), map));
  }
  
  // maps the commTheta from L to the inv. theta on L + 1
  private double[][] mapTheta(SparseDoubleMatrix newTheta, int order,
                              TIntIntHashMap map) {
    double[][] inverseTheta = new double[order][topicCount];
    
    for (int comm = 0; comm < order; comm++) {
      for (int topic = 0; topic < topicCount; topic++) {
        int node = map.get(comm);
        inverseTheta[node][topic] = newTheta.get(topic, comm);
      }
    }
    return inverseTheta;
  }
  
  // map from community -> node on layer above
  private TIntIntHashMap createLayerMap(Graph g) {
    int count = 0;
    int[] communities = g.communities();
    boolean[] isFound = new boolean[g.order()];
    TIntIntHashMap map = new TIntIntHashMap();
    Arrays.sort(communities);
    
    for (int i = 0; i < g.order(); i++) {
      if (!isFound[communities[i]]) {
        map.put(communities[i], count);
        isFound[communities[i]] = true;
        count++;
      }
    }
    if (map.size() != g.numComms()) throw new Error("Map creation failed.");
    
    return map;
  }
  
  // uses the layer maps to assign a community from each layer to the base layer
  // graph.
  private void buildCommunityList() {
    List<int[]> rawComms = new ArrayList<int[]>();
    communities.add(graphs.get(0).communities());
    
    for (int i = 0; i < layer; i++) {
      rawComms.add(graphs.get(i).communities());
    }
    
    for (int i = 0; i < layer - 1; i++) {
      communities.add(mapToBaseLayer(i , rawComms));
    }
  }
  
  // maps layers to each other until the specified layer has been mapped to the
  // base layer
  private int[] mapToBaseLayer(int layer, List<int[]> rawComms) {
    int[] a = mapToNextLayer(graphs.get(layer), layerMaps.get(layer), 
                             rawComms.get(layer + 1));
    layer--;
    
    while (layer >= 0) {
      a = mapToNextLayer(graphs.get(layer), layerMaps.get(layer), a);
      layer--;
    }
    
    return a;
  }
  
  // maps each node in a layer to its community on the layer above it
  private int[] mapToNextLayer(Graph g, TIntIntHashMap map, int[] commsL2) {
    int[] commsL1 = g.communities();
    int[] NL1toCL2 = new int[g.order()];

    for (int nodeL1 = 0; nodeL1 < g.order(); nodeL1++) {
      int commL1 = commsL1[nodeL1];
      int nodeL2 = map.get(commL1);
      int commL2 = commsL2[nodeL2];
      NL1toCL2[nodeL1] = commL2;
    }
    
    return NL1toCL2;
  }
  
  public class Maximiser {
    private final Graph g;
    private final double[][] subTheta;
    private final double[][] communityProbSum; // sum of theta of all comm members
    private final double[] commEntropySum; // sum of entropy of all members
    private final int[] commSize;
    private int[] shuffledNodes;
    
    public Maximiser(Graph g, double[][] subTheta) {
      this.g = g;
      this.subTheta = subTheta;
      communityProbSum = new double[g.order()][topicCount];
      commSize = new int[g.order()];
      commEntropySum = new double[g.order()];
      
      for (int node = 0; node < g.order(); node++) {
        int comm = g.community(node);
        if (comm != node) throw new Error("Needs a fresh graph.");
        commSize[comm] = 1;
        commEntropySum[comm] = entropy(subTheta[comm]);
        for (int topic = 0; topic < topicCount; topic++) {
          communityProbSum[comm][topic] = subTheta[comm][topic];
        }
      }
    }  
    
    public SparseDoubleMatrix run() {
      buildShuffledList();
      
      long s1 = System.nanoTime();
      reassignCommunities();
      long e1 = System.nanoTime();
      double time = (e1 - s1) / 1000000000d;
      System.out.println("seconds taken: " + time );
      return getCommThetas();
    }
    
    // todo add avg entropy
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
        // double jsd = JSD(node, comm, commSize[comm] + 1);
        double ent = newEntropy(communityProbSum[comm], subTheta[node], 
                                commSize[comm] + 1);
        // maximise mod, minimise ent:
        double inc = mod * (1d - ent);
        // System.out.println(inc + " " + mod + " " + jsd + " " + ent);
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
      double entropy = entropy(subTheta[doc]);
      
      commSize[newComm]++;
      commSize[oldComm]--;
      commEntropySum[oldComm] -= entropy;
      commEntropySum[newComm] += entropy;
      
      for (int topic = 0; topic < topicCount; topic++) {
        communityProbSum[oldComm][topic] -= subTheta[doc][topic];
        communityProbSum[newComm][topic] += subTheta[doc][topic];
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
    
    private double JSD(int node, int newComm, int size) {
      double weight = 1d / size;
      double entropy = entropy(subTheta[node]);
      
      double[] sum = new double[topicCount];
      for (int topic = 0; topic < topicCount; topic++) {
        sum[topic] = (communityProbSum[newComm][topic] + subTheta[node][topic])
                   * weight;
      }

      double esum = (commEntropySum[newComm] + entropy) * weight;

      return entropy(sum) - esum;
    }
    
    private SparseDoubleMatrix getCommThetas() {
      SparseDoubleMatrix commThetas = new SparseDoubleMatrix(topicCount, g.order());
      
      for (int node = 0; node < g.order(); node++) {
        int comm = g.community(node);    
        for (int topic = 0; topic < topicCount; topic++) {
          commThetas.add(topic, comm, subTheta[node][topic]);       
        }
      }
      
      int commCnt = 0;
      for (int comm = 0; comm < g.order(); comm++) {
        if (commSize[comm] != 0) {
          for (int topic = 0; topic < topicCount; topic++) {
            commThetas.div(topic, comm, commSize[comm]); 
          }
        }
      }
      return commThetas;
    }
  }
}