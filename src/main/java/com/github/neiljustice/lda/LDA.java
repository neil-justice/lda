package com.github.neiljustice.lda;

import com.github.neiljustice.lda.topic.Topic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Latent Dirichlet Allocation (LDA) is a topic model.
 *
 * TODO it would be good to be able to load a test dataset and evaluate its probability according to the trained model
 * TODO some sort of test for convergence?
 * TODO re-implement serialisation/pausing/restarting
 * TODO allow extracting topic distribution of each document
 */
public class LDA {

  private static final Logger LOGGER = LogManager.getLogger(LDA.class);
  public static final int DEFAULT_TOP_WORDS_PER_TOPIC = 10;
  public static final int DEFAULT_BURN_IN_CYCLES = 100;
  public static final int DEFAULT_SAMPLE_LAG_CYCLES = 20;
  public static final int DEFAULT_OPTIMISE_INTERVAL = 40;
  public static final int DEFAULT_PERPLEXITY_CHECK_LAG = 10;

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
  private GibbsSampler gibbsSampler = new GibbsSampler();
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

    LOGGER.info(" V: {} D: {} N: {}", wordCount, docCount, tokenCount);
    LOGGER.info("{} run so far, with {} samples taken.", cycles, samples);

    randomiseTopics();
    initialiseMatrices();
    initHyper();
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

  public void train(int maxCycles) {
    train(maxCycles, DEFAULT_BURN_IN_CYCLES, DEFAULT_SAMPLE_LAG_CYCLES, DEFAULT_OPTIMISE_INTERVAL, DEFAULT_PERPLEXITY_CHECK_LAG);
  }

  public void train(int maxCycles, int burnLength) {
    train(maxCycles, burnLength, DEFAULT_SAMPLE_LAG_CYCLES, DEFAULT_OPTIMISE_INTERVAL, DEFAULT_PERPLEXITY_CHECK_LAG);
  }

  public void train(int maxCycles, int burnLength, int sampleLag) {
    train(maxCycles, burnLength, sampleLag, DEFAULT_OPTIMISE_INTERVAL, DEFAULT_PERPLEXITY_CHECK_LAG);
  }

  public void train(int maxCycles, int burnLength, int sampleLag, int optimiseInterval) {
    train(maxCycles, burnLength, sampleLag, optimiseInterval, DEFAULT_PERPLEXITY_CHECK_LAG);
  }

  public void train(int maxCycles, int burnLength, int sampleLag, int optimiseInterval, int perplexLag) {
    this.maxCycles = maxCycles + cycles;
    this.burnLength = burnLength;
    this.sampleLag = sampleLag;
    this.optimiseInterval = optimiseInterval;
    this.perplexLag = perplexLag;
    cycles();
  }

  public void print() {
    print(DEFAULT_TOP_WORDS_PER_TOPIC);
  }

  public void print(int topN) {
    final List<Topic> topics = LDAUtils.termScore(phi(), corpus.dictionary(), topN);
    for (Topic topic : topics) {
      System.out.println(topic);
    }
  }

  /**
   * Get the topics, and the top 10 terms for each topic.
   */
  public List<Topic> getTopics() {
    return getTopics(DEFAULT_TOP_WORDS_PER_TOPIC);
  }

  /**
   * Get the topics, and the top N terms for each topic.
   *
   * @param topN terms per topic.
   */
  public List<Topic> getTopics(int topN) {
    return LDAUtils.termScore(phi(), corpus.dictionary(), topN);
  }

  private void cycles() {

    double avg = 0;
    for (; cycles < maxCycles; cycles++) {
      final long s = System.nanoTime();

      gibbsSampler.cycle();
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

  private class GibbsSampler {

    public void cycle() {
      for (int i = 0; i < tokenCount; i++) {
        final int word = corpus.word(i);
        final int oldTopic = corpus.topic(i);
        final int doc = corpus.doc(i);
        tokensInTopic[oldTopic]--;
        wordsInTopic[word][oldTopic]--;
        topicsInDoc[oldTopic][doc]--;

        final int newTopic = sample(word, doc);
        if (newTopic != oldTopic) {
          moves++;
        }

        tokensInTopic[newTopic]++;
        wordsInTopic[word][newTopic]++;
        topicsInDoc[newTopic][doc]++;
        corpus.setTopic(i, newTopic);
      }
    }

    private int sample(int word, int doc) {
      final double[] probabilities = new double[topicCount];
      double sum = 0;

      for (int topic = 0; topic < topicCount; topic++) {
        probabilities[topic] = (topicsInDoc[topic][doc] + alpha[topic])
            * ((wordsInTopic[word][topic] + beta)
            / (tokensInTopic[topic] + betaSum));
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
  }
}