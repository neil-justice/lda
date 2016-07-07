import com.amd.aparapi.Kernel;

public class SamplerKernel extends Kernel {
  
  @Local private final int[] tokensInTopicLocal; // need returning
  @Constant private final int[] words;
  @Constant private final int[] docs;
  private final int[] topics;
  private final int epoch;
  private final double[] probabilities;
  @Constant private final int[] tokenPartStart;
  @Constant private final int[] wordPartStart;
  private final int[][] wordsInTopic;
  private final int[][] topicsInDoc;
  @Constant private final double beta;
  @Constant private final double alpha;
  @Constant private final int docCount;
  @Constant private final int P;
  
  public SamplerKernel(int[] tokensInTopic, 
                       int[] words, 
                       int[] docs, 
                       int[] topics, 
                       int epoch, 
                       double[] probabilities,
                       int[] tokenPartStart,
                       int[] wordPartStart,
                       int[][] wordsInTopic,
                       int[][] topicsInDoc,
                       double beta,
                       double alpha,
                       int docCount,
                       int P) {
                         
    this.tokensInTopicLocal = tokensInTopic;
    this.words = words;
    this.docs = docs;
    this.topics = topics;
    this.epoch = epoch;
    this.probabilities = probabilities;
    this.tokenPartStart = tokenPartStart;
    this.wordPartStart = wordPartStart;
    this.wordsInTopic = wordsInTopic;
    this.topicsInDoc = topicsInDoc;
    this.beta = beta;
    this.alpha = alpha;
    this.docCount = docCount;
    this.P = P;
  }
  
  @Override
  public void run() {
    int id = getGlobalId();
    
    for (int i = tokenPartStart[id]; i < tokenPartStart[id + 1]; i++) {
      int word = words[i];
      int wps = (epoch + id) % P;
      // checks if the word is in the word partition
      if (word >= wordPartStart[wps] && word < (wordPartStart[wps + 1])) {
        int oldTopic = topics[i];
        int doc = docs[i];
        tokensInTopicLocal[oldTopic]--;      
        wordsInTopic[word][oldTopic]--;
        topicsInDoc[oldTopic][doc]--;
        
        int newTopic = sample(word, oldTopic, doc);
        // if (newTopic != oldTopic) moves++;
        
        tokensInTopicLocal[newTopic]++;      
        wordsInTopic[word][newTopic]++;
        topicsInDoc[newTopic][doc]++;
        topics[i] = newTopic;
      }
    }    
  }
  
  private int sample(int word, int oldTopic, int doc) {
    double sum = 0;

    for (int topic = 0; topic < probabilities.length; topic++) {
      probabilities[topic] = (wordsInTopic[word][topic] + beta)
                           * (topicsInDoc[topic][doc] + alpha)
                           / (tokensInTopicLocal[topic] + docCount);
      sum += probabilities[topic];
    }
    
    int newTopic = -1;
    double sample = Math.random() * sum; // between 0 and sum of all probs
    
    while (sample > 0.0) {
      newTopic++;
      sample -= probabilities[newTopic];
    }
    
    return newTopic;
  } 
}