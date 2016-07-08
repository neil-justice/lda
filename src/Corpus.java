import java.util.*;

public class Corpus {
  
  private final Tokens tokens;
  private final Translator translator; // used to translate from ID to word/doc
  private final Random rand = new Random();
  
  private final int wordCount;         // no. of unique words
  private final int docCount;          // no. of docs
  private final int tokenCount;        // total no. of tokens
  private final int topicCount;
  private final int[] tokensInTopic; 
  private final int[][] wordsInTopic;
  private final int[][] topicsInDoc;
  private final int[][] phiSum;        // multinomial dist. of words in topics
  private final int[][] thetaSum;      // multinomial dist. of topics in docs.
  
  // high alpha: each document is likely to contain a mixture of most topics.
  // low alpha:  more likely that a document may contain just a few topics. 
  // high beta: each topic is likely to contain a mixture of most of words
  // low beta: each topic may contain a mixture of just a few of the words.
  private final double alpha; // hyperparameters
  private final double beta;  // hyperparameters
  private int cycles;
  private int samples; // no. of times the phi and theta sums have been added to
  private final int burnLength; // length of burn-in phase to allow markov chain 
                                // to converge.
  private final int sampleLag;  // cycles to skip samples from between samples-
                                // gives decorrelated states of the markov chain
  
  // DB stuff:
  private final SQLConnector c;
  private int prevCycles; // no. of cycles run in previous session
  private int prevTopics; // no. of topics from last session
  
  public Corpus(CorpusBuilder builder) {
    tokens     = builder.tokens();
    wordCount  = builder.wordCount();
    docCount   = builder.docCount();
    tokenCount = builder.tokenCount();
    topicCount = builder.topicCount();
    
    tokensInTopic = new int[topicCount];
    tokensInDoc   = new int[docCount];
    wordsInTopic  = new int[wordCount][topicCount];
    topicsInDoc   = new int[topicCount][docCount];
    
    phiSum        = new int[wordCount][topicCount];
    thetaSum      = new int[topicCount][docCount];
    
    alpha = 50 / (double) topicCount;
    beta  = 200 / (double) wordCount;
    samples = 0;
    
    c = new SQLConnector(builder.dir());
    c.open();
    
    translator = new Translator(c);
    
    prevCycles = c.getCycles();
    prevTopics = c.getTopics();
    
    System.out.println(" V : " + wordCount + 
                       " D : " + docCount + 
                       " N : " + tokenCount);
                       
    System.out.println("" + prevCycles + " run so far, with Z = " + prevTopics);
    
    if (prevTopics != topicCount || prevCycles == 0) {
      System.out.print("Uninitialised data or different Z-count detected.");
      System.out.println(" (Re)-initialising...");
      randomiseTopics();
      prevCycles = 0;
      c.setCycles(0);
    }
    
    initialiseMatrices();
  }
  
  // Initialises all tokens with a randomly selected topic.
  private void randomiseTopics() {
    Random rand = new Random();
    for (int i = 0; i < tokenCount; i++) {
      int topic = rand.nextInt(topicCount);
      tokens.setTopic(i, topic);
    }
  }
  
  // initialises the matrix of word occurrence count in topic,
  // and the matrix of topic occurrence count in document
  private void initialiseMatrices() {
    int count = 0;
    for (int i = 0; i < tokenCount; i++) {
      int word = tokens.word(i);
      int topic = tokens.topic(i);
      int doc = tokens.doc(i);
      wordsInTopic[word][topic]++;
      topicsInDoc[topic][doc]++;
      tokensInTopic[topic]++;
      tokensInDoc[doc]++;
      count++;
    }
    if (count != tokenCount) throw new IllegalStateException("incorrect token count");
  }  
  
  public void run(int cycles) {
    this.cycles = cycles;
    burnLength = cycles / 10;
    sampleLag = cycles / 50;
    cycles();
    write();
  }
  
  // write updated topics to db
  // TODO the strange thing here is that we only store one sample...
  public void write() {
    if (!c.isOpen()) c.open();
    
    c.updateTokens(tokens); 
    c.setTopics(topicCount);
    c.setCycles(prevCycles + cycles);
  }
  
  //close DB connection
  public void closeDB() {
    c.close();
  }
  
  public void print() {
    printWords();
    printDocs();
    termScore();
  }  
  
  private void cycles() {
    double avg = 0;
    for (int i = 0; i < cycles; i++) {
      long s = System.nanoTime();
      int moves = cycle();
      long e = System.nanoTime();
      double time = (e - s) / 1000000000d;
      avg += time;
      System.out.print("Cycle " + i);
      System.out.printf(", seconds taken: %.03f", time );
      System.out.println(", moves made: " + moves );
      if (i >= burnLength && i % sampleLag == 0) {
        updateParameters();
      }
    }
    avg /= cycles;
    System.out.printf("Avg. seconds taken: %.03f%n", avg );    
  }
  
  private int cycle() {
    int moves = 0;
    
    for (int i = 0; i < tokenCount; i++) {
      int word = tokens.word(i);
      int oldTopic = tokens.topic(i);
      int doc = tokens.doc(i);
      
      tokensInTopic[oldTopic]--;      
      wordsInTopic[word][oldTopic]--;
      topicsInDoc[oldTopic][doc]--;
      
      int newTopic = sample(word, oldTopic, doc);
      if (newTopic != oldTopic) moves++;
      
      tokensInTopic[newTopic]++;      
      wordsInTopic[word][newTopic]++;
      topicsInDoc[newTopic][doc]++;
      tokens.setTopic(i, newTopic);
    }
    
    return moves;
  }
  
  private int sample(int word, int oldTopic, int doc) {
    double[] probabilities = new double[topicCount];
    double sum = 0;

    for (int topic = 0; topic < topicCount; topic++) {
      probabilities[topic] = (wordsInTopic[word][topic] + beta)
                           * (topicsInDoc[topic][doc] + alpha)
                           / (tokensInTopic[topic] + docCount);
      sum += probabilities[topic];
    }
    
    int newTopic = -1;
    double sample = rand.nextDouble() * sum; // between 0 and sum of all probs
    
    while (sample > 0.0) {
      newTopic++;
      sample -= probabilities[newTopic];
    }
    
    if (newTopic == -1) {
      throw new Error("Sampling failure.  sample: " + sample + " sum: " + sum);
    }
    
    return newTopic;
  }
  
  // if the markov chain is out of burn-in phase, and the thinning interval
  // has passed (the thinning interval allows us to obtain decorrelated states
  // of the markov chain), then this round of sampling is added to the -sums.
  private void updateParameters() {
    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        thetaSum[topic][doc] += (topicsInDoc[topic][doc] + alpha)
                             /  (tokensInDoc[doc] + topicCount * alpha);
      }
    }
    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phiSum[word][topic] += (wordsInTopic[word][topic] + beta)
                            /  (tokensIntopic[topic] + wordCount * beta);
      }
    }
    samples++;
  }
  
  private double[][] phi() {
    double[][] phi = new double[topicCount][wordCount];

    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phi[topic][word] = phiSum[topic][word] / samples;
      }
    }
    
    return phi;
  }
  
  private double[][] theta() {
    double[][] theta = new double[docCount][topicCount];

    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        theta[topic][doc] = thetaSum[topic][doc] / samples;
      }
    }
    
    return theta;
  }
  
  // as laid out in Blei and Lafferty, 2009.  sorts words in topics by
  // prob. in topic * log (prob. in topic / geometric mean prob in all topics)
  // and defines topics by their top 10 words.
  private void termScore() {
    int top = 10;
    double[][] phi = phi();
    double[] geometricMean = geometricMean(phi);
    
    Integer[][] output = new Integer[topicCount][top];
    double[][] temp = new double[topicCount][wordCount]; //note the inverse dimensions
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = phi[word][topic] 
                          * Math.log(phi[word][topic] 
                          / geometricMean[word]);
      }
      IndexComparator comp = new IndexComparator(temp[topic]);
      Integer[] indexes = comp.indexArray();
      Arrays.sort(indexes, comp);
      output[topic] = Arrays.copyOf(indexes, top);
    }

    System.out.println("");
    for (int topic = 0; topic < topicCount; topic++) {
      System.out.print("" + topic + " : ");
      for (int i = 0; i < top; i++) {
        System.out.printf("%10s", translator.getWord(output[topic][i]));
      }
      System.out.println("");
    }
  }
  
  // for each (no. of times word W appears in topic T), divide by the total 
  // number of words in that topic.
  // private double[][] probabilityMatrix() {
  //   double[][] matrix = new double[wordCount][topicCount];
  //   
  //   for (int word = 0; word < wordCount; word++) {
  //     for (int topic = 0; topic < topicCount; topic++) {
  //       matrix[word][topic] = wordsInTopic[word][topic] + 1
  //                             / (double) tokensInTopic[topic] + 1d;
  //     }
  //   }
  // 
  //   return matrix;
  // }
  
  // finds the geometric mean of a matrix
  private double[] geometricMean(double[][] matrix) {
    int height = matrix.length;
    int width = matrix[0].length;
    if (height != wordCount) throw new Error("oops");
    if (width != topicCount) throw new Error("oops2");
    
    double[] geometricMean = new double[height];
    
    for (int i = 0; i < height; i++) {
      double sumlog = 0d;
      for (int j = 0; j < width; j++) {
        sumlog += Math.log(matrix[i][j]);
      }
      geometricMean[i] = Math.exp(sumlog/width);
    }
    
    return geometricMean;
  }  

  public void printWords() {
    System.out.println("");
    for (int word = 0; word < wordCount; word++) {
      System.out.printf("%15s", translator.getWord(word));
      for (int topic = 0; topic < topicCount; topic++) {
        System.out.printf("%4d", wordsInTopic[word][topic]);
      }
      System.out.println("");
    }
  }
  
  public void printDocs() {
    System.out.println("");
    for (int doc = 0; doc < docCount; doc++) {
      System.out.printf("%-10s", translator.getDoc(doc));
      double docTotal = 0;
      for (int topic = 0; topic < topicCount; topic++) {
        docTotal += topicsInDoc[topic][doc];
      }
      System.out.printf("tot: %5d", (int) docTotal);
      for (int topic = 0; topic < topicCount; topic++) {
        System.out.printf(" %.01f%%", (topicsInDoc[topic][doc] / docTotal) * 100d);
      }
      System.out.println("");
    }
  }
}