import java.util.*;
import java.util.concurrent.*;

public class Corpus {
  
  private final Tokens tokens;
  private final Translator translator; // used to translate from ID to word/doc
  private final int wordCount;              // no. of unique words
  private final int docCount;               // no. of docs
  private final int tokenCount;             // total no. of tokens
  private final int topicCount;
  private final int[] tokensInTopic; 
  private final int[][] wordsInTopic;
  private final int[][] topicsInDoc;
  // high alpha: each document is likely to contain a mixture of most topics.
  // low alpha:  more likely that a document may contain just a few topics. 
  // high beta: each topic is likely to contain a mixture of most of words
  // low beta: each topic may contain a mixture of just a few of the words.
  private final double alpha; // hyperparameters
  private final double beta; // hyperparameters
  private final SQLConnector c;
  private int cycles;
  private int prevCycles; // no. of cycles run in previous session
  private int prevTopics; // no. of topics from last session
  private int moves = 0;  // no. of tokens who have changed topic this cycle.
  
  // multithreading stuff:
  private final int P = 3; // no. of processors.
  private final GibbsSampler[] gibbsSamplers = new GibbsSampler[P];
  private final CyclicBarrier barrier = new CyclicBarrier(P);
  private int docPartSize;
  private int wordPartSize;
  private int[] docPartStart  = new int[P + 1];
  private int[] wordPartStart = new int[P + 1];
  private int[] tokenPartStart= new int[P + 1];
                                      
  public Corpus(CorpusBuilder builder) {
    tokens = builder.tokens();
    wordCount = builder.wordCount();
    docCount = builder.docCount();
    tokenCount = builder.tokenCount();
    topicCount = builder.topicCount();
    tokensInTopic  = new int[topicCount];
    wordsInTopic = new int[wordCount][topicCount];
    topicsInDoc  = new int[topicCount][docCount];
    alpha = 50 / (double) topicCount;
    beta  = 200 / (double) wordCount;
    
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
    initMulti();
  }
  
  public void initMulti() {
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
  }
  
  public void run(int cycles) {
    this.cycles = cycles;
    cycles();
    write();
  }
  
  // write updated topics to db
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
      cycle();
      long e = System.nanoTime();
      double time = (e - s) / 1000000000d;
      avg += time;
      System.out.print("Cycle " + i);
      System.out.printf(", seconds taken: %.03f", time );
      System.out.println(", moves made: " + moves );
      moves = 0;
    }
    avg /= cycles;
    System.out.printf("Avg. seconds taken: %.03f%n", avg );    
  }
  
  //TODO thread pooling, executor etc
  private void cycle() {
    Thread[] threads = new Thread[P];
  
    for (int proc = 0; proc < P; proc++) {
      gibbsSamplers[proc] = new GibbsSampler(proc);
    }
    
    for (int i = 0; i < P; i++) {
      threads[i] = new Thread(gibbsSamplers[i]);
      threads[i].start();
    }
    
    for (int i = 0; i < P; i++){
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        System.out.print(e);
      }
    }
    
    // check if all tokens have been through the gibbs sampler
    // int ch = tokens.check();
    // if (ch != 0) System.out.println("" + ch + "/" + tokenCount + " unchecked!");
  }
  
  // implements the multithreading collapsed gibbs sampling algorithm put
  // forward by Yan et. al. (2009)
  class GibbsSampler implements Runnable {
    private int localMoves = 0;
    private final int proc;           // thread id
    private int[] localTokensInTopic; // local version to avoid race conditions
    private int count = 0;            // tokens processed by this thread
    
    public GibbsSampler(int proc) {
      this.proc = proc;
      localTokensInTopic = Arrays.copyOf(tokensInTopic, tokensInTopic.length);
    }

    @Override
    public void run() {
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
            count++;
          }
        } 
        synchronise();
      }
      // System.out.println("proc " + proc + " count: " + count + "/" + tokenCount);
    }
    
    
    private int sample(int word, int oldTopic, int doc) {
      double[] probabilities = new double[topicCount];
      double sum = 0;

      for (int topic = 0; topic < topicCount; topic++) {
        probabilities[topic] = (wordsInTopic[word][topic] + beta)
                             * (topicsInDoc[topic][doc] + alpha)
                             / (localTokensInTopic[topic] + docCount);
        sum += probabilities[topic];
      }
      
      int newTopic = -1;
      double sample = Math.random() * sum; // between 0 and sum of all probs
      
      while (sample > 0.0) {
        newTopic++;
        sample -= probabilities[newTopic];
      }
      
      if (newTopic == -1) {
        throw new IllegalStateException ("New topic not sampled.  sample: " + sample + " sum: " + sum);
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
      count++;
    }
    if (count != tokenCount) throw new IllegalStateException("incorrect token count");
  }
  
  // as laid out in Blei and Lafferty, 2009.  sorts words in topics by
  // prob. in topic * log (prob. in topic / geometric mean prob in all topics)
  // and defines topics by their top 10 words.
  private void termScore() {
    int top = 10;
    double[][] probabilityMatrix = probabilityMatrix();
    double[] geometricMean = geometricMean(probabilityMatrix);
    
    Integer[][] output = new Integer[topicCount][top];
    double[][] temp = new double[topicCount][wordCount]; //note the inverse dimensions
    
    for (int topic = 0; topic < topicCount; topic++) {
      for (int word = 0; word < wordCount; word++) {
        temp[topic][word] = probabilityMatrix[word][topic] 
                          * Math.log(probabilityMatrix[word][topic] 
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
  private double[][] probabilityMatrix() {
    double[][] matrix = new double[wordCount][topicCount];
    
    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        matrix[word][topic] = wordsInTopic[word][topic] + 1
                              / (double) tokensInTopic[topic] + 1d;
      }
    }

    return matrix;
  }
  
  // finds the geometric mean of each row (word) in the probability matrix
  // taken from the wordsInTopic matrix.
  private double[] geometricMean(double[][] probabilityMatrix) {
    double[] geometricMean = new double[wordCount];
    
    for (int word = 0; word < wordCount; word++) {
      double sumlog = 0d;
      for (int topic = 0; topic < topicCount; topic++) {
        sumlog += Math.log(probabilityMatrix[word][topic]);
      }
      geometricMean[word] = Math.exp(sumlog/topicCount);
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