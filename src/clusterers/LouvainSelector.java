/* Runs the louvain detector the set number of times and returns the random
 * seed of the best run-through. */
import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.IOException;

public class LouvainSelector {
  private final SQLConnector c;
  private final Random rnd = new Random();
  private final String dir;
  
  public LouvainSelector(String dir, SQLConnector c) {
    this.c = c;
    this.dir = dir;
  }
  
  public long run(int times) {
    long[] seeds = new long[times];
    double[] mods  = new double[times];
    
    System.out.println("Running " + times + " times:");
    for (int i = 0; i < times; i++) {
      seeds[i] = rnd.nextLong();
      System.out.println("Run " + i + " seed " + seeds[i]);
      Graph g = new GraphBuilder().fromFileAndDB(dir + CTUT.GRAPH, c).build();
      LouvainDetector detector = new LouvainDetector(g, seeds[i]);
      detector.run();
      mods[i] = detector.modularity();
    }
    long best = seeds[max(mods)];
    write(best);
    return best;
  }
  
  private int max(double[] mods) {
    int index = -1;
    double max = 0d;
    for (int i = 0; i < mods.length; i++) {
      if (mods[i] > max) {
        index = i;
        max = mods[i];
      }
    }
    return index;
  }
  
  private void write(long mod) {
    Path filepath = Paths.get(dir + CTUT.LOUVAIN_SEED);
    List<String> data = new ArrayList<String>();
    data.add("" + mod);
    
    try {
      Files.write(filepath, data, Charset.forName("UTF-8"));
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }
}