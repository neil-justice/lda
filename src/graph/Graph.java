/* An undirected, weighted graph data structure.  */
import java.util.*;

class Graph {
  private final int[][] matrix;
  private final int order; //no. of nodes
  private final int size;  //sum of edge weights / 2
  
  public Graph(GraphBuilder builder) {
    matrix = builder.matrix();
    order  = builder.order();
    size   = builder.size();
  }
  
  public size() { return size; }
}