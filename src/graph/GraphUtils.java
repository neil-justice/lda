/* loads a partition set into a Graph object */

public class GraphUtils {
  
  public static Graph loadPartitionSet(Graph g, int[] community) {
    if (community.length != g.order()) throw new Error("community array length-graph size mismatch");
    for (int node = 0; node < g.order(); node++) {
      g.moveToComm(node, community[node]);
    }
    return g;
  }
}  