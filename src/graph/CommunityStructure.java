/* class for community layer info */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class CommunityStructure {
  private int layer = 0;                  // current community layer
  private final List<Graph> coarseGraphs; // coarse-grained community graphs
  private final TIntArrayList numComms;   // total no. of communities per layer
  private final List<int[]> commLayers;   // list of community layers
  private final List<int[]> totDegrees;   // total degree of community in layer
  private final List<int[]> intDegrees;   // int. degree of community in layer  
  // maps between communities on L and nodes on L + 1:
  private final List<TIntIntHashMap> layerMaps;
  
  public CommunityStructure(Graph g) {
    commLayers   = new ArrayList<int[]>();
    totDegrees   = new ArrayList<int[]>();
    intDegrees   = new ArrayList<int[]>();
    numComms     = new TIntArrayList();
    layerMaps    = new ArrayList<TIntIntHashMap>();
    coarseGraphs = new ArrayList<Graph>();
    
    commLayers.add(new int[g.order()]);
    totDegrees.add(new int[g.order()]);
    intDegrees.add(new int[g.order()]);
    numComms.add(order);
    coarseGraphs.add(g);
  }

  public void newLayer() {
    layerMaps.add(createLayerMap());
    layer++;
    Graph coarse = new GraphBuilder().coarseGrain(coarseGraphs.get(layer - 1), this).build();
    int order = coarse.order();
    
    commLayers.add(new int[order]);
    totDegrees.add(new int[order]);
    intDegrees.add(new int[order]);
    numComms.add(order);
    coarseGraphs.add(coarse);
  }
  
  // map from community -> node on layer above
  private TIntIntHashMap createLayerMap() {
    int[] communities = Arrays.copyOf(communities(layer), communities(layer).length);
    boolean[] isFound = new boolean[order];
    TIntIntHashMap map = new TIntIntHashMap();
    int count = 0;
    Arrays.sort(communities);
    
    for (int i = 0; i < order; i++) {
      if (!isFound[communities[i]]) {
        map.put(communities[i], count);
        isFound[communities[i]] = true;
        count++;
      }
    }
    
    return map;
  }  
  
  public TIntIntHashMap layerMap() { return layerMaps.get(layer); }
  
  public int layer() { return layer; }
  
  public int[] communities(int layer) { return commLayers.get(layer); }
  public int[] totDegrees(int layer) { return totDegrees.get(layer); }
  public int[] intDegrees(int layer) { return intDegrees.get(layer); }
  
  public int numComms(int layer) { return numComms.get(layer); }
  
  public void decNumComms(int layer) { numComms.set(layer, numComms.get(layer) - 1); }
}