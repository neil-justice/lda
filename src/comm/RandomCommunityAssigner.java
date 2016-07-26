/* Assigns each node to a randomly generated set. */
import java.util.*;

public class RandomCommunityAssigner {
  private final List<int[]> communities = new ArrayList<int[]>();
  private final Random rnd = new Random();
  private final int layers = 5;
  private final int order;
  
  public RandomCommunityAssigner(int order) {
    this.order = order;
    for (int layer = 0; layer < layers; layer++) {
      int[] comm = new int[order];
      communities.add(comm);
    }
  }
  
  public List<int[]> run() {
    generate(20000, 0);
    generate(3000, 1);
    generate(1000, 2);
    generate(600, 3);
    generate(400, 4);
    
    return communities;
  }
  
  // generates max communities for the layer specified
  private void generate(int max, int layer) {
    if (layer >= layers) throw new Error("layer too high");
    
    for (int node = 0; node < order; node++) {
      communities.get(layer)[node] = rnd.nextInt(max);
    }
  }
}