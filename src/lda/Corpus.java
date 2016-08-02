import java.util.*;
import java.util.concurrent.*;

public class Corpus {
  
  private final Tokens tokens;
  private final Translator translator; // used to translate from ID to word/doc
  private final Random random = new Random();
  private final int wordCount;         // no. of unique words
  private final int docCount;          // no. of docs
  private final int tokenCount;        // total no. of tokens
  private final int topicCount;
  private final int[] tokensInTopic; 
  private final int[] tokensInDoc; 
  private final int[][] wordsInTopic;
  private final int[][] topicsInDoc;
  private final double[][] phiSum;     // multinomial dist. of words in topics
  private final double[][] thetaSum;   // multinomial dist. of topics in docs.
  private final double[] topicWeight;
  // high alpha: each document is likely to contain a mixture of most topics.
  // low alpha:  more likely that a document may contain just a few topics. 
  // high beta: each topic is likely to contain a mixture of most of words
  // low beta: each topic may contain a mixture of just a few of the words.
  private final double alpha; // hyperparameters
  private final double beta;  // hyperparameters
  private final double betaSum;  // hyperparameters
  private int cycles;
  private int samples; // no. of times the phi and theta sums have been added to
  private int burnLength; // length of burn-in phase to allow markov chain 
                          // to converge.
  private int sampleLag;  // cycles to skip samples from between samples, giving
                          // us decorrelated states of the markov chain
  private int moves = 0;  // no. of tokens who have changed topic this cycle.
  
  // DB stuff:
  private final SQLConnector c;
  private int prevCycles; // no. of cycles run in previous session
  private int prevTopics; // no. of topics from last session
  
  // multithreading stuff:
  private final int P = 3; // no. of processors.
  private final ExecutorService exec = Executors.newFixedThreadPool(P);
  private final List<GibbsSampler> gibbsSamplers = new ArrayList<>(P);
  private final CyclicBarrier barrier = new CyclicBarrier(P);
  private int docPartSize;
  private int wordPartSize;
  private int[] docPartStart  = new int[P + 1];
  private int[] wordPartStart = new int[P + 1];
  private int[] tokenPartStart= new int[P + 1];
                                      
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
    
    phiSum        = new double[wordCount][topicCount];
    thetaSum      = new double[topicCount][docCount];
    topicWeight   = new double[topicCount];
    
    alpha = 0.1; // 50 / (double) topicCount;
    beta  = 0.2; // 200 / (double) wordCount;
    betaSum = beta * wordCount;
    
    samples = 0;
    
    c = builder.connector();
    c.open();
    
    translator = new Translator(c);
    
    prevCycles = c.getCycles();
    prevTopics = c.getTopics();
    
    System.out.println(" V : " + wordCount + 
                       " D : " + docCount + 
                       " N : " + tokenCount);
                       
    System.out.println("" + prevCycles + " run so far, with Z = " + prevTopics);

    randomiseTopics();
    initialiseMatrices();
    initMulti();
  }
  
  private void initMulti() {
    docPartSize  = docCount / P; 
    wordPartSize = wordCount / P;
    
    for (int i = 0; i < P; i++) {
      docPartStart[i]   = i * docPartSize;
      wordPartStart[i]  = i * wordPartSize;
      tokenPartStart[i] = tokens.getDocStartPoint(docPartStart[i]);
    }
    docPartStart[P]   = docCount;
    wordPartStart[P]  = wordCount;
    tokenPartStart[P] = tokenCount;
    
    for (int proc = 0; proc < P; proc++) {
      gibbsSamplers.add(new GibbsSampler(proc));
    }    
  }
  
  // Initialises all tokens with a randomly selected topic.
  private void randomiseTopics() {
    for (int i = 0; i < tokenCount; i++) {
      int topic = random.nextInt(topicCount);
      tokens.setTopic(i, topic);
    }
  }
  
  // initialises the matrix of word occurrence count in topic,
  // and the matrix of topic occurrence count in document
  private void initialiseMatrices() {
    for (int i = 0; i < tokenCount; i++) {
      int word = tokens.word(i);
      int topic = tokens.topic(i);
      int doc = tokens.doc(i);
      wordsInTopic[word][topic]++;
      topicsInDoc[topic][doc]++;
      tokensInTopic[topic]++;
      tokensInDoc[doc]++;
    }
  }  
  
  public void run(int cycles) {
    this.cycles = cycles;
    burnLength = cycles / 10;
    sampleLag = cycles / 50;
    cycles();
    write();
  }
  
  // write phi and theta to db, as well as no. of cycles run and the topicCount
  // used.
  public void write() {
    if (!c.isOpen()) c.open();
    
    c.writeTheta(theta());
    c.writePhi(phi());
    c.setTopics(topicCount);
    c.setCycles(cycles);
  }
  
  //close DB connection and shutdown thread pool
  public void quit() {
    exec.shutdown();
    c.close();
  }
  
  public void print() {
    // printWords();
    // printDocs();
    // LDAUtils.mostCommon(phi(), translator);
    
    LDAUtils.termScore(phi(), translator);
  }  
  
  private void cycles() {
    
    double avg = 0;
    for (int i = 0; i < cycles; i++) {
      long s = System.nanoTime();
      cycle();
      if (i >= burnLength && i % sampleLag == 0) updateParameters();
      long e = System.nanoTime();
      double time = (e - s) / 1000000000d;
      avg += time;
      System.out.print("Cycle " + i);
      System.out.printf(", seconds taken: %.03f", time );
      System.out.print(", moves made: " + moves );
      if (i < burnLength) System.out.print(" (burn-in)");
      else if (i % sampleLag != 0) System.out.print(" (sample-lagging)");
      System.out.println();
      moves = 0;
    }
    avg /= cycles;
    System.out.printf("Avg. seconds taken: %.03f%n", avg );    
  }
  
  private void cycle() {
    try {
      exec.invokeAll(gibbsSamplers);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }
    // check if all tokens have been through the gibbs sampler
    // int ch = tokens.check();
    // if (ch != 0) System.out.println("" + ch + "/" + tokenCount + " unchecked!");
  }
  
  // implements the multithreading collapsed gibbs sampling algorithm put
  // forward by Yan et. al. (2009)
  class GibbsSampler implements Callable<Object> {
    private int localMoves = 0;
    private final int proc;           // thread id
    private int[] localTokensInTopic; // local version to avoid race conditions
    // private int count = 0;            // tokens processed by this thread
    
    public GibbsSampler(int proc) {
      this.proc = proc;
      localTokensInTopic = Arrays.copyOf(tokensInTopic, tokensInTopic.length);
    }

    @Override
    public Object call() {
      for (int epoch = 0; epoch < P; epoch++) {
        for (int i = tokenPartStart[proc]; i < tokenPartStart[proc + 1]; i++) {
          int word = tokens.word(i);
          int wps = (epoch + proc) % P;
          // checks if the word is in the word partition
          if (word >= wordPartStart[wps] && word < (wordPartStart[wps + 1])) {
            int oldTopic = tokens.topic(i);
            int doc = tokens.doc(i);
            localTokensInTopic[oldTopic]--;      
            wordsInTopic[word][oldTopic]--;
            topicsInDoc[oldTopic][doc]--;
            
            int newTopic = sample(word, oldTopic, doc);
            if (newTopic != oldTopic) localMoves++;
            
            localTokensInTopic[newTopic]++;      
            wordsInTopic[word][newTopic]++;
            topicsInDoc[newTopic][doc]++;
            tokens.setTopic(i, newTopic);
            // count++;
          }
        } 
        synchronise();
      }
      // System.out.println("proc " + proc + " count: " + count + "/" + tokenCount);
      return null;
    }
    
    private int sample(int word, int oldTopic, int doc) {
      double[] probabilities = new double[topicCount];
      double sum = 0;

      for (int topic = 0; topic < topicCount; topic++) {
        probabilities[topic] = (topicsInDoc[topic][doc] + alpha)
                             * ((wordsInTopic[word][topic] + beta)
                             /  (localTokensInTopic[topic] + betaSum));
        sum += probabilities[topic];
      }
      
      int newTopic = -1;
      double sample = random.nextDouble() * sum; // between 0 and sum of all probs
      
      while (sample > 0.0) {
        newTopic++;
        sample -= probabilities[newTopic];
      }
      
      if (newTopic == -1) {
        throw new Error("Sampling failure. Sample: " + sample + " sum: " + sum);
      }
      
      return newTopic;
    }

    //synchronise local tokensInTopic arrays here and reload them
    private void synchronise() {
      for (int i = 0; i < tokensInTopic.length; i++) {
        localTokensInTopic[i] -= tokensInTopic[i];
      }
      await();
      write();
      await();
      localTokensInTopic = Arrays.copyOf(tokensInTopic, tokensInTopic.length);
    }

    private synchronized void write() {
      for (int i = 0; i < tokensInTopic.length; i++) {
        tokensInTopic[i] += localTokensInTopic[i];
      }
      moves += localMoves;
      localMoves = 0;
    }

    private void await() {
      try {
        barrier.await();
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
        System.exit(1);
      } catch (BrokenBarrierException e) {
        System.out.println(e.getMessage());
        System.exit(1);
      }
    }
  }
  
  // if the markov chain is out of burn-in phase, and the thinning interval
  // has passed (the thinning interval allows us to obtain decorrelated states
  // of the markov chain), then this round of sampling is added to the -sums.
  private void updateParameters() {
    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        thetaSum[topic][doc] += (topicsInDoc[topic][doc] + alpha)
                             /  (double)(tokensInDoc[doc] + topicCount * alpha);
      }
    }
    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phiSum[word][topic] += (wordsInTopic[word][topic] + beta)
                            /  (double)(tokensInTopic[topic] + wordCount * beta);
      }
    }
    samples++;
  }
  
  private double[][] phi() {
    double[][] phi = new double[wordCount][topicCount];

    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phi[word][topic] = phiSum[word][topic] / samples;
      }
    }
    
    return phi;
  }
  
  private double[][] theta() {
    double[][] theta = new double[topicCount][docCount];

    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        theta[topic][doc] = thetaSum[topic][doc] / samples;
        topicWeight[topic] += theta[topic][doc];
      }
      topicWeight[topic] /= docCount;
    }
    
    return theta;
  }

  // public void printWords() {
  //   System.out.println("");
  //   for (int word = 0; word < wordCount; word++) {
  //     System.out.printf("%15s", translator.getWord(word));
  //     for (int topic = 0; topic < topicCount; topic++) {
  //       System.out.printf("%4d", wordsInTopic[word][topic]);
  //     }
  //     System.out.println("");
  //   }
  // }
  
  // public void printDocs() {
  //   System.out.println("");
  //   for (int doc = 0; doc < docCount; doc++) {
  //     System.out.printf("%-10s", translator.getDoc(doc));
  //     double docTotal = 0;
  //     for (int topic = 0; topic < topicCount; topic++) {
  //       docTotal += topicsInDoc[topic][doc];
  //     }
  //     System.out.printf("tot: %5d", (int) docTotal);
  //     for (int topic = 0; topic < topicCount; topic++) {
  //       System.out.printf(" %.01f%%", (topicsInDoc[topic][doc] / docTotal) * 100d);
  //     }
  //     System.out.println("");
  //   }
  // }
}