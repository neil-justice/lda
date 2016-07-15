import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class LouvainDetector {
  private int totalMoves = 0;
  private int layer = 0; // current community layer
  private final List<Graph> graphs = new ArrayList<Graph>();
  // maps between communities on L and nodes on L + 1:
  private final List<TIntIntHashMap> layerMaps = new ArrayList<>();
  private final List<int[]> communities = new ArrayList<int[]>();
  private final Maximiser m = new Maximiser();
  
  public LouvainDetector(Graph g) {
    graphs.add(g);
  }
  
  public void run() { run(9999); }
  
  public void run(int maxLayers) {
    if (maxLayers == 0) return;
    
    do {
      System.out.printf("Round %d:%n", layer);
      totalMoves = m.run(graphs.get(layer));
      if (totalMoves > 0 && maxLayers >= layer) addNewLayer();
    }
    while (totalMoves > 0 && maxLayers >= layer);
    mapToNextLayer(graphs.get(0), layerMaps.get(0), graphs.get(1).communities());
  }

  private void addNewLayer() {
    Graph last = graphs.get(layer);
    TIntIntHashMap map = createLayerMap(last);
    layerMaps.add(map);
    layer++;
    Graph coarse = new GraphBuilder().coarseGrain(last, map).build();
    if (last.m2() != coarse.m2()) {
      throw new Error("Coarse-grain error: " + last.size() + "!=" + coarse.size());
    }
    graphs.add(coarse);
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
      NL1toCL2[node] = commL2;
    }
    
    // for (int node = 0; node < g.order(); node++) {
    //   System.out.println("n: " + node + " c1: " + communities[node] +
    //                      " c2: " + layerAbove[node]);
    // }
    
    return NL1toCL2;
  }
  
  class Maximiser {
    private final Random rnd = new Random();
    private final double precision = 0.000001;
    private Graph g;
    private int[] shuffledNodes;
    
    private int run(Graph g) {
      this.g = g;
      buildShuffledList();
      totalMoves = 0;
      
      long s1 = System.nanoTime();
      reassignCommunities();
      long e1 = System.nanoTime();
      double time = (e1 - s1) / 1000000000d;
      System.out.println("seconds taken: " + time );
      
      return totalMoves;
    }
    
    private void reassignCommunities() {
      double mod = g.modularity();
      double oldMod;
      int moves;
      boolean hasChanged;
      
      do {
        hasChanged = true;
        oldMod = mod;
        moves = maximiseLocalModularity();
        totalMoves += moves;
        mod = g.modularity();
        if (mod - oldMod <= precision) hasChanged = false;
        if (moves == 0) hasChanged = false;
      } while (hasChanged);
      System.out.printf("Mod: %5f  Comms: %d Moves:  %d%n", 
                          mod , g.numComms(), totalMoves);
    } 
    
    private int maximiseLocalModularity() {
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
      int best = -1;
      
      for (int i = 0; i < g.neighbours(node).size(); i++) {
        int community = g.community(g.neighbours(node).get(i));
        double inc = deltaModularity(node, community);
        if (inc > max) {
          max = inc;
          best = community;
        }
      }
      
      if (best > 0 && best != g.community(node)) {
        g.moveToComm(node, best);
        return true;
      }
      else return false;
    }

    // change in modularity if node is moved to community
    private double deltaModularity(int node, int community) {
      double dnodecomm = (double) g.dnodecomm(node, community);
      double ctot      = (double) g.totDegree(community);
      double wdeg      = (double) g.degree(node);
      return dnodecomm - ((ctot * wdeg) / g.m2());
    }
  }
}