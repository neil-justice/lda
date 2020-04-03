package com.github.neiljustice.lda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Latent Dirichlet Allocation (LDA) is a topic model.
 */
public class LDA {

  private static final Logger LOGGER = LogManager.getLogger(LDA.class);

  private final Corpus corpus;
  private final Random random = new Random();
  /** number of unique words */
  private final int wordCount;
  /** Number of docs */
  private final int docCount;
  /** Total number of tokens */
  private final int tokenCount;
  private final int topicCount;
  private final int[] tokensInTopic;
  private final int[] tokensInDoc;
  private final int[][] wordsInTopic;
  private final int[][] topicsInDoc;
  /** Multinomial dist. of words in topics */
  private final double[][] phiSum;
  /** Multinomial dist. of topics in docs. */
  private final double[][] thetaSum;
  /** Hyperparameter */
  private final double[] alpha;
  /** Hyperparameter */
  private final double beta;
  private final double betaSum;
  private AlphaOptimiser optimiser;
  private double alphaSum;
  /** Length of longest doc */
  private int maxLength;
  private int optimiseInterval;
  private int cycles;
  /** Cycles to run */
  private int maxCycles;
  /** No. of times the phi and theta sums have been added to */
  private int samples;
  /** Length of burn-in phase to allow markov chain to converge. */
  private int burnLength;
  /** Cycles to skip samples from between samples, giving us decorrelated states of the markov chain. */
  private int sampleLag;
  /** How often to measure perplexity. */
  private int perplexLag;
  /** No. of tokens who have changed topic this cycle. */
  private int moves = 0;
  
  // multithreading stuff:
  private final int P = 3; // no. of processors.
  private final ExecutorService exec = Executors.newFixedThreadPool(P);
  private final List<GibbsSampler> gibbsSamplers = new ArrayList<>(P);
  private final CyclicBarrier barrier = new CyclicBarrier(P);
  private final int[] docPartStart  = new int[P + 1];
  private final int[] wordPartStart = new int[P + 1];
  private final int[] tokenPartStart= new int[P + 1];
                                      
  public LDA(Corpus corpus, int topicCount) {
    this.topicCount = topicCount;

    this.corpus = corpus;
    wordCount = corpus.wordCount();
    docCount = corpus.docCount();
    tokenCount = corpus.size();

    tokensInTopic = new int[topicCount];
    tokensInDoc = new int[docCount];
    wordsInTopic = new int[wordCount][topicCount];
    topicsInDoc = new int[topicCount][docCount];

    phiSum = new double[wordCount][topicCount];
    thetaSum = new double[topicCount][docCount];

    alpha = new double[topicCount];
    beta = 0.2;
    betaSum = beta * wordCount;

    cycles = 0;
    samples = 0;

    System.out.println(" V : " + wordCount +
        " D : " + docCount +
        " N : " + tokenCount + " P : " + P);

    System.out.println("" + cycles + " run so far, with " + samples + " samples taken.");

    randomiseTopics();
    initialiseMatrices();
    initMulti();
    initHyper();
  }

  private void initMulti() {
    int docPartSize = docCount / P;
    int wordPartSize = wordCount / P;

    for (int i = 0; i < P; i++) {
      docPartStart[i] = i * docPartSize;
      wordPartStart[i] = i * wordPartSize;
      tokenPartStart[i] = corpus.getDocStartPoint(docPartStart[i]);
    }
    docPartStart[P] = docCount;
    wordPartStart[P] = wordCount;
    tokenPartStart[P] = tokenCount;

    for (int proc = 0; proc < P; proc++) {
      gibbsSamplers.add(new GibbsSampler(proc));
    }
  }

  // Initialises all tokens with a randomly selected topic.
  private void randomiseTopics() {
    for (int i = 0; i < tokenCount; i++) {
      final int topic = random.nextInt(topicCount);
      corpus.setTopic(i, topic);
    }
  }

  // initialises the matrix of word occurrence count in topic,
  // and the matrix of topic occurrence count in document
  private void initialiseMatrices() {
    for (int i = 0; i < tokenCount; i++) {
      final int word = corpus.word(i);
      final int topic = corpus.topic(i);
      final int doc = corpus.doc(i);
      wordsInTopic[word][topic]++;
      topicsInDoc[topic][doc]++;
      tokensInTopic[topic]++;
      tokensInDoc[doc]++;
    }
  }

  private void initHyper() {
    int max = 0;
    for (int doc = 0; doc < docCount; doc++) {
      final int length = tokensInDoc[doc];
      if (length > max) {
        max = length;
      }
    }
    maxLength = max + 1;
    System.out.println(max);

    optimiser = new AlphaOptimiser(tokensInDoc, topicCount, maxLength);
    for (int topic = 0; topic < topicCount; topic++) {
      alpha[topic] = 0.1;
      alphaSum += alpha[topic];
    }
  }

  public void run(int maxCycles) {
    this.maxCycles = maxCycles + cycles;
    perplexLag = 10;
    burnLength = 100;
    sampleLag = 20;
    optimiseInterval = 40;
    cycles();
  }

  //close DB connection and shutdown thread pool
  public void quit() {
    exec.shutdown();
  }

  public void print() {
    // printWords();
    // printDocs();
    // LDAUtils.mostCommon(phi(), translator);

    LDAUtils.termScore(phi(), corpus.dictionary());
  }

  private void cycles() {

    double avg = 0;
    for (; cycles < maxCycles; cycles++) {
      final long s = System.nanoTime();
      cycle();
      if (cycles >= burnLength && cycles % optimiseInterval == 0) {
        optimiseAlpha();
      }
      if (cycles >= burnLength && cycles % sampleLag == 0) {
        updateParameters();
        print();
      }
      final long e = System.nanoTime();
      final double time = (e - s) / 1000000000d;
      avg += time;
      System.out.print("Cycle " + cycles);
      System.out.printf(", seconds taken: %.03f", time);
      if (cycles < burnLength) {
        System.out.print(" (burn-in)");
      } else if (cycles % sampleLag != 0) {
        System.out.print(" (sample-lagging)");
      } else if (cycles % optimiseInterval == 0) {
        System.out.print(" (optimising)");
      }
      System.out.println();
      if (cycles % perplexLag == 0) {
        System.out.println("perplexity: " + perplexity());
      }
      moves = 0;
    }
    avg /= maxCycles;
    System.out.printf("Avg. seconds taken: %.03f%n", avg);
  }

  private void cycle() {
    try {
      exec.invokeAll(gibbsSamplers);
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }
    // check if all tokens have been through the gibbs sampler
    // int ch = corpus.check();
    // if (ch != 0) System.out.println("" + ch + "/" + tokenCount + " unchecked!");
  }

  private void optimiseAlpha() {
    final int[][] docTopicCountHist = new int[topicCount][maxLength];
    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        final int count = topicsInDoc[topic][doc];
        docTopicCountHist[topic][count]++;
      }
    }
    alphaSum = optimiser.optimiseAlpha(alpha, docTopicCountHist);
    for (int topic = 0; topic < topicCount; topic++) {
      System.out.println("t " + topic + " alpha " + alpha[topic]);
    }

  }

  // if the markov chain is out of burn-in phase, and the thinning interval
  // has passed (the thinning interval allows us to obtain decorrelated states
  // of the markov chain), then this round of sampling is added to the -sums.
  private void updateParameters() {
    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        thetaSum[topic][doc] += (topicsInDoc[topic][doc] + alpha[topic])
            / (tokensInDoc[doc] + alphaSum);
      }
    }
    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phiSum[word][topic] += (wordsInTopic[word][topic] + beta)
            / (tokensInTopic[topic] + wordCount * beta);
      }
    }
    samples++;
  }

  private double[][] phi() {
    final double[][] phi = new double[wordCount][topicCount];

    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phi[word][topic] = phiSum[word][topic] / samples;
      }
    }

    return phi;
  }

  // private void loadParameters() {
  //   double[][] phi = c.getPhi();
  //   double[][] theta = c.getTheta();
  //
  //   for (int topic = 0; topic < topicCount; topic++) {
  //     for (int doc = 0; doc < docCount; doc++) {
  //       thetaSum[topic][doc] += theta[topic][doc] * samples;
  //     }
  //   }
  //   for (int word = 0; word < wordCount; word++) {
  //     for (int topic = 0; topic < topicCount; topic++) {
  //       phiSum[word][topic] += phi[word][topic] * samples;
  //     }
  //   }
  // }

  private double[][] theta() {
    final double[][] theta = new double[topicCount][docCount];

    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        theta[topic][doc] = thetaSum[topic][doc] / samples;
      }
    }

    return theta;
  }

  public double perplexity() {
    double sum = 0d;
    for (int token = 0; token < tokenCount; token++) {
      final int doc = corpus.doc(token);
      final int topic = corpus.topic(token);
      final int word = corpus.word(token);
      final double theta = (topicsInDoc[topic][doc] + alpha[topic]) / (tokensInDoc[doc] + alphaSum);
      final double phi = (wordsInTopic[word][topic] + beta) / (tokensInTopic[topic] + wordCount * beta);
      sum += Math.log(phi * theta);
    }
    sum = 0 - (sum / (double) tokenCount);
    return Math.exp(sum);
  }

  // implements the multithreading collapsed gibbs sampling algorithm put
  // forward by Yan et. al. (2009)
  class GibbsSampler implements Callable<Object> {
    private final int proc;           // thread id
    private int localMoves = 0;
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
          final int word = corpus.word(i);
          final int wps = (epoch + proc) % P;
          // checks if the word is in the word partition
          if (word >= wordPartStart[wps] && word < (wordPartStart[wps + 1])) {
            final int oldTopic = corpus.topic(i);
            final int doc = corpus.doc(i);
            localTokensInTopic[oldTopic]--;
            wordsInTopic[word][oldTopic]--;
            topicsInDoc[oldTopic][doc]--;

            final int newTopic = sample(word, oldTopic, doc);
            if (newTopic != oldTopic) {
              localMoves++;
            }

            localTokensInTopic[newTopic]++;
            wordsInTopic[word][newTopic]++;
            topicsInDoc[newTopic][doc]++;
            corpus.setTopic(i, newTopic);
            // count++;
          }
        }
        synchronise();
      }
      // System.out.println("proc " + proc + " count: " + count + "/" + tokenCount);
      return null;
    }

    private int sample(int word, int oldTopic, int doc) {
      final double[] probabilities = new double[topicCount];
      double sum = 0;

      for (int topic = 0; topic < topicCount; topic++) {
        probabilities[topic] = (topicsInDoc[topic][doc] + alpha[topic])
            * ((wordsInTopic[word][topic] + beta)
            / (localTokensInTopic[topic] + betaSum));
        sum += probabilities[topic];
      }

      int newTopic = -1;
      double sample = random.nextDouble() * sum; // between 0 and sum of all probs

      while (sample > 0.0) {
        newTopic++;
        sample -= probabilities[newTopic];
      }

      if (newTopic == -1) {
        throw new IllegalStateException("Sampling failure. Sample: " + sample + " sum: " + sum);
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
      } catch (InterruptedException | BrokenBarrierException e) {
        System.out.println(e.getMessage());
        System.exit(1);
      }
    }
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
}