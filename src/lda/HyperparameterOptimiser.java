/* implements algorithm 2.2 / eq. 2.58 from Wallach's phd thesis */

public class HyperparameterOptimiser {
  private final int[] docLength; // docLength[l] == no. of docs of length l
  private int[][] docTopicCount; // docTopicCount[k][cnt] = no. of docs with
                                 // cnt no. of topic k tokens
                                 
  public HyperparameterOptimiser() {
    
  }
}