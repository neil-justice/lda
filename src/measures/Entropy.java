import java.util.*;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.iterator.TIntObjectIterator;

public class Entropy {

  // measures the shannon entropy H of a prob. dist.
  public static double entropy(double[] dist) {
    return entropy(dist, 2);
  }
  
  public static double entropy(double[] dist, int base) {
    double H = 0d;
    for (int i = 0; i < dist.length; i++) {
      // if (dist[i] < 0d) throw new Error("Negative value in prob. dist.");
      if (dist[i] != 0d) H -= dist[i] * Math.log(dist[i]);
    }
    return H / Math.log(base);
  }
  
  public static double entropy(int comm, SparseDoubleMatrix commThetas) {
    return entropy(comm, commThetas, 2);
  }
  
  public static double entropy(int comm, SparseDoubleMatrix commThetas, int base) {
    double H = 0d;
    for (int i = 0; i < commThetas.xmax(); i++) {
      double d = commThetas.get(i, comm);
      if (d != 0d) H -= d * Math.log(d);
    }
    return H / Math.log(base);
  }
}