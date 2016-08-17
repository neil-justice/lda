/* seeks to maximise modularity and minimise entropy simultaneously */
import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class HybridClusterer implements Clusterer {
  private final List<Graph> graphs = new ArrayList<Graph>();
  private final List<double[][]> inverseThetas = new ArrayList<>();
  private final LayerMapper mapper = new LayerMapper(graphs);
  private final int topicCount;
  private Integer totalMoves = 0;
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
      boolean tempering = false; // controls whether the layer is tempered
      if (layer == 0) tempering = true;
      if (layer == 1) tempering = true;
      LocalMaximiser m = new LocalMaximiser(graphs.get(layer), 
                                            inverseThetas.get(layer),
                                            tempering);
      m.run();
      totalMoves = m.totalMoves();
      if (totalMoves > 0 && maxLayers >= layer) addNewLayer(m.inverseTheta());
    }
    while (totalMoves > 0 && maxLayers >= layer);
    
    return mapper.mapAll();
  }

  private void addNewLayer(SparseDoubleMatrix newTheta) {
    Graph last = graphs.get(layer);
    TIntIntHashMap map = mapper.map(last);
    layer++;
    Graph coarse = new GraphBuilder().coarseGrain(last, map).build();
    graphs.add(coarse);
    inverseThetas.add(mapTheta(newTheta, coarse.order(), map));
  }
  
  // maps the commTheta from L-1 to the inv. theta on L.
  private double[][] mapTheta(SparseDoubleMatrix newTheta, int order,
                              TIntIntHashMap map) {
    double[][] inverseTheta = new double[order][topicCount];
    
    for ( SparseDoubleMatrix.Iterator it = newTheta.iterator(); it.hasNext(); ) {
      it.advance();
      int topic = it.x();
      int comm = it.y();
      int node = map.get(comm);
      inverseTheta[node][topic] = it.value();
    }

    return inverseTheta;
  }
}