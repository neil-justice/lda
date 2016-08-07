/* generic hierarchical clusterer that runs the provided clusterer iteratively
 * on more and more coarse-grained versions of the graph. 
import java.util.*;

public class HierarchicalClusterer implements Clusterer {
  private int totalMoves = 0;
  private int layer = 0; // current community layer
  private final List<Graph> graphs = new ArrayList<Graph>();
  // maps between communities on L and nodes on L + 1:
  private final List<TIntIntHashMap> layerMaps = new ArrayList<>();
  private final List<int[]> communities = new ArrayList<int[]>();
  private final Maximiser maximiser;
  private final Random rnd;
  
  private HierarchicalClusterer() {
    rnd = new Random();
  }

  public HierarchicalClusterer(Graph g, Maximiser maximiser) {
    this();
    this.maximiser = maximiser;
    graphs.add(g);
    long seed = rnd.nextLong();
    rnd.setSeed(seed);
    System.out.println("Using seed " + seed);
  }
  
  public double modularity() {
    return graphs.get(layer).modularity();
  }
  
  @Override
  public List<int[]> run() { return run(9999); }
  
  public List<int[]> run(int maxLayers) {
    if (maxLayers <= 0) return null;
    System.out.printf("Detecting graph communities...");
    
    do {
      System.out.printf("Round %d:%n", layer);
      totalMoves = maximiser.run(graphs.get(layer));
      if (totalMoves > 0 && maxLayers >= layer) addNewLayer();
    }
    while (totalMoves > 0 && maxLayers >= layer);
    
    buildCommunityList();
    return communities;
  }

  private void addNewLayer() {
    Graph last = graphs.get(layer);
    TIntIntHashMap map = createLayerMap(last);
    layerMaps.add(map);
    layer++;
    Graph coarse = new GraphBuilder().coarseGrain(last, map).build();
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
}*/