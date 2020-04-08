package com.github.neiljustice.lda;

import com.github.neiljustice.lda.topic.Topic;
import com.github.neiljustice.lda.util.MatrixUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Latent Dirichlet Allocation (LDA) is a topic model. This implementation uses a Gibbs sampler.
 */
public class LDA {

  public static final int DEFAULT_TOP_WORDS_PER_TOPIC = 10;
  public static final int DEFAULT_BURN_IN_CYCLES = 100;
  public static final int DEFAULT_SAMPLE_LAG_CYCLES = 20;
  public static final int DEFAULT_OPTIMISE_INTERVAL = 40;
  public static final int DEFAULT_PERPLEXITY_CHECK_LAG = 10;
  public static final double DEFAULT_PERPLEXITY_THRESHOLD = 0.0001d;

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
  private final GibbsSampler gibbsSampler = new GibbsSampler();
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
  /** Stop early if the change in perplexity is less than this amount. */
  private double perplexThresh;

  private LDA(Corpus corpus, int topicCount, boolean initaliseTopics, double[] initialAlpha) {
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

    beta = 0.2;
    betaSum = beta * wordCount;

    cycles = 0;
    samples = 0;

    LOGGER.info(" V : {} D : {} N : {}", wordCount, docCount, tokenCount);

    // Initialises all tokens with a randomly selected topic.
    if (initaliseTopics) {
      for (int i = 0; i < tokenCount; i++) {
        final int topic = random.nextInt(topicCount);
        corpus.setTopic(i, topic);
      }
    }

    // initialises the matrix of word occurrence count in topic,
    // and the matrix of topic occurrence count in document
    for (int i = 0; i < tokenCount; i++) {
      final int word = corpus.word(i);
      final int topic = corpus.topic(i);
      final int doc = corpus.doc(i);
      wordsInTopic[word][topic]++;
      topicsInDoc[topic][doc]++;
      tokensInTopic[topic]++;
      tokensInDoc[doc]++;
    }

    maxLength = Arrays.stream(tokensInDoc).max().orElse(-1) + 1;
    alpha = initialAlpha;
    optimiser = new AlphaOptimiser(tokensInDoc, topicCount, maxLength);
    for (int topic = 0; topic < topicCount; topic++) {
      alphaSum += alpha[topic];
    }
  }

  /**
   * Constructor to use to begin training a model on a corpus.
   *
   * @param corpus the corpus of documents to train a model on.
   * @param topicCount the number of topics the model will have.
   */
  public LDA(Corpus corpus, int topicCount) {
    this(corpus, topicCount, true, generateInitialAlpha(topicCount));
  }

  /**
   * Constructor to use to reload an already-trained or partially-trained model.
   *
   * @param model the trained or partially-trained model.
   */
  public LDA(LDAModel model) {
    this(new Corpus(model.getCorpus()), model.getTopics(), false, model.getAlpha());
    this.samples = model.getSamples();
    this.cycles = model.getCycles();
    loadParameters(model.getPhi(), model.getTheta());
  }

  private static double[] generateInitialAlpha(int topicCount) {
    final double[] a = new double[topicCount];
    Arrays.fill(a, 0.1);
    return a;
  }

  /**
   * Train a model.
   *
   * @param maxCycles the maximum number of cycles to run.
   *
   * @return the trained model.
   */
  public LDAModel train(int maxCycles) {
    return train(maxCycles, DEFAULT_BURN_IN_CYCLES, DEFAULT_SAMPLE_LAG_CYCLES,
        DEFAULT_OPTIMISE_INTERVAL, DEFAULT_PERPLEXITY_CHECK_LAG, DEFAULT_PERPLEXITY_THRESHOLD);
  }

  /**
   * Train a model.
   *
   * @param maxCycles the maximum number of cycles to run.
   * @param burnLength the number of cycles to run at the start of the process without sampling.
   *
   * @return the trained model.
   */
  public LDAModel train(int maxCycles, int burnLength) {
    return train(maxCycles, burnLength, DEFAULT_SAMPLE_LAG_CYCLES,
        DEFAULT_OPTIMISE_INTERVAL, DEFAULT_PERPLEXITY_CHECK_LAG, DEFAULT_PERPLEXITY_THRESHOLD);
  }

  /**
   * Train a model.
   *
   * @param maxCycles the maximum number of cycles to run.
   * @param burnLength the number of cycles to run at the start of the process without sampling.
   * @param sampleLag the cycles to run between each sampling.
   *
   * @return the trained model.
   */
  public LDAModel train(int maxCycles, int burnLength, int sampleLag) {
    return train(maxCycles, burnLength, sampleLag,
        DEFAULT_OPTIMISE_INTERVAL, DEFAULT_PERPLEXITY_CHECK_LAG, DEFAULT_PERPLEXITY_THRESHOLD);
  }

  /**
   * Train a model.
   *
   * @param maxCycles the maximum number of cycles to run.
   * @param burnLength the number of cycles to run at the start of the process without sampling.
   * @param sampleLag the cycles to run between each sampling.
   * @param optimiseInterval the cycles to run between each hyperparameter optimisation pass.
   *
   * @return the trained model.
   */
  public LDAModel train(int maxCycles, int burnLength, int sampleLag, int optimiseInterval) {
    return train(maxCycles, burnLength, sampleLag,
        optimiseInterval, DEFAULT_PERPLEXITY_CHECK_LAG, DEFAULT_PERPLEXITY_THRESHOLD);
  }

  /**
   * Train a model.
   *
   * @param maxCycles the maximum number of cycles to run.
   * @param burnLength the number of cycles to run at the start of the process without sampling.
   * @param sampleLag the cycles to run between each sampling.
   * @param optimiseInterval the cycles to run between each hyperparameter optimisation pass.
   * @param perplexLag the cycles to run between each perplexity check.
   *
   * @return the trained model.
   */
  public LDAModel train(int maxCycles, int burnLength, int sampleLag, int optimiseInterval, int perplexLag) {
    return train(maxCycles, burnLength, sampleLag,
        optimiseInterval, perplexLag, DEFAULT_PERPLEXITY_THRESHOLD);
  }

  /**
   * Train a model.
   *
   * @param maxCycles the maximum number of cycles to run.
   * @param burnLength the number of cycles to run at the start of the process without sampling.
   * @param sampleLag the cycles to run between each sampling.
   * @param optimiseInterval the cycles to run between each hyperparameter optimisation pass.
   * @param perplexLag the cycles to run between each perplexity check.
   * @param perplexThresh if the difference between the previous perplexity and the current perplexity is smaller
   *                      than this value, finish the training.
   *
   * @return the trained model.
   */
  public LDAModel train(int maxCycles, int burnLength, int sampleLag, int optimiseInterval, int perplexLag, double perplexThresh) {
    this.maxCycles = maxCycles + cycles;
    this.burnLength = burnLength;
    this.sampleLag = sampleLag;
    this.optimiseInterval = optimiseInterval;
    this.perplexLag = perplexLag;
    this.perplexThresh = perplexThresh;
    cycles();
    return getModel();
  }

  /**
   * Log the topics, and the top 10 terms for each topic, at INFO level.
   */
  public void logTopics() {
    logTopics(DEFAULT_TOP_WORDS_PER_TOPIC);
  }

  /**
   * Log the topics, and the top N terms for each topic, at INFO level.
   *
   * @param topN number of terms to log per topic.
   */
  public void logTopics(int topN) {
    final List<Topic> topics = LDAUtils.termScore(phi(), corpus.dictionary(), topN);
    for (Topic topic : topics) {
      LOGGER.info(topic);
    }
  }

  /**
   * Get the topics, and the top 10 terms for each topic.
   * <p>
   * Use phi() to get the raw distibutions indexed by token index rather than string.
   */
  public List<Topic> getTopics() {
    return getTopics(DEFAULT_TOP_WORDS_PER_TOPIC);
  }

  /**
   * Get the topics, and the top N terms for each topic.
   * <p>
   * Use phi() to get the raw distibutions indexed by token index rather than string.
   *
   * @param topN terms per topic.
   */
  public List<Topic> getTopics(int topN) {
    return LDAUtils.termScore(phi(), corpus.dictionary(), topN);
  }

  public LDAModel getModel() {
    return new LDAModel(corpus, phi(), theta(), alpha, topicCount, samples, cycles);
  }

  private void cycles() {
    long avg = 0;
    double prevPerplexity = 0d;
    for (; cycles < maxCycles; cycles++) {
      final long s = System.nanoTime();

      gibbsSampler.cycle();
      if (cycles >= burnLength && cycles % optimiseInterval == 0) {
        optimiseAlpha();
      }
      if (cycles >= burnLength && cycles % sampleLag == 0) {
        updateParameters();
        logTopics();
      }
      if (cycles % perplexLag == 0) {
        final double perplexity = perplexity();
        LOGGER.info("perplexity: {}", perplexity);
        if (Math.abs(prevPerplexity - perplexity) < perplexThresh) {
          LOGGER.info("Perplexity change since last check below threshold: prev: {}, curr: {}", prevPerplexity, perplexity);
          break;
        }
        prevPerplexity = perplexity();
      }
      final long e = System.nanoTime();
      final long time = TimeUnit.NANOSECONDS.toSeconds(e - s);
      avg += time;
      logCycle(time);
    }
    avg /= maxCycles;
    LOGGER.info("Avg. seconds taken: {}", avg);
  }

  private void logCycle(long time) {
    if (!LOGGER.isInfoEnabled()) {
      return;
    }

    final String cycleType;
    if (cycles < burnLength) {
      cycleType = "burn-in";
    } else if (cycles % sampleLag == 0) {
      cycleType = "sampling";
    } else if (cycles % optimiseInterval == 0) {
      cycleType = "optimising";
    } else {
      cycleType = "sample-lagging";
    }

    LOGGER.info("Cycle: {}, seconds taken {} ({})", cycles, time, cycleType);
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
    if (LOGGER.isInfoEnabled()) {
      for (int topic = 0; topic < topicCount; topic++) {
        LOGGER.info("t {} alpha {}", +topic, alpha[topic]);
      }
    }

  }

  /**
   * If the markov chain is out of burn-in phase, and the thinning interval
   * has passed (the thinning interval allows us to obtain decorrelated states
   * of the markov chain), then this round of sampling is added to the phi and
   * theta sums.
   */
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

  /**
   * Calculate the probability distribution for each token appearing in each topic.
   * <p>
   * Use getTopics() for the string forms of the tokens.
   *
   * @return a 2D matrix of size word count * topic count
   */
  public double[][] phi() {
    final double[][] phi = new double[wordCount][topicCount];

    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phi[word][topic] = phiSum[word][topic] / samples;
      }
    }

    return phi;
  }

  /**
   * Recreate phiSum and thetaSum from a given phi and theta.
   */
  private void loadParameters(double[][] phi, double[][] theta) {
    for (int topic = 0; topic < topicCount; topic++) {
      for (int doc = 0; doc < docCount; doc++) {
        thetaSum[topic][doc] += theta[topic][doc] * samples;
      }
    }
    for (int word = 0; word < wordCount; word++) {
      for (int topic = 0; topic < topicCount; topic++) {
        phiSum[word][topic] += phi[word][topic] * samples;
      }
    }
  }

  /**
   * Calculate the probability distribution for each topic appearing in each document.
   *
   * @return a 2D matrix of size topic count * document count
   */
  public double[][] theta() {
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

    return Math.exp(0 - (sum / (double) tokenCount));
  }

  /**
   * Generate documents according to the model's probability distribution of word occurrence.
   * Uses the model's prior knowledge of topic probability distribution (the alpha
   * hyperparameter) to decide on what distribution of topics to draw from.
   *
   * @param numDocs the number of documents to generate.
   * @return the documents, with each document being an array of token indexes.
   */
  public List<int[]> generateDocs(int numDocs) {
    return generateDocs(numDocs, alpha);
  }

  /**
   * Generate documents according to the model's probability distribution of word occurrence.
   *
   * @param numDocs the number of documents to generate.
   * @param topicProbs the probability of each topic to occur.
   * @return the documents, with each document being an array of token indexes.
   */
  public List<int[]> generateDocs(int numDocs, double[] topicProbs) {
    if (topicProbs.length != topicCount) {
      throw new IllegalArgumentException("topic probability distribution length must be " + topicCount);
    }

    final double[][] phi = phi();
    final List<int[]> docs = new ArrayList<>(numDocs);

    for (int i = 0; i < numDocs; i++) {
      docs.add(generateDoc(phi, topicProbs, Arrays.stream(topicProbs).sum()));
    }

    return docs;
  }

  /**
   * Generate documents according to the model's probability distribution of word occurrence.
   * Uses the model's prior knowledge of topic probability distribution (the alpha
   * hyperparameter) to decide on what distribution of topics to draw from.
   *
   * @param numDocs the number of documents to generate.
   * @return the documents, with each document being a string.
   */
  public List<String> generateTextDocs(int numDocs) {
    return generateTextDocs(numDocs, alpha);
  }

  /**
   * Generate documents according to the model's probability distribution of word occurrence.
   *
   * @param numDocs the number of documents to generate.
   * @param topicProbs the probability of each topic to occur.
   * @return the documents, with each document being a string.
   */
  public List<String> generateTextDocs(int numDocs, double[] topicProbs) {
    if (topicProbs.length != topicCount) {
      throw new IllegalArgumentException("topic probability distribution length must be " + topicCount);
    }

    final double[][] phi = phi();
    final List<String> docs = new ArrayList<>(numDocs);

    for (int doc = 0; doc < numDocs; doc++) {
      final StringBuilder builder = new StringBuilder();
      final int[] raw = generateDoc(phi, topicProbs, Arrays.stream(topicProbs).sum());
      for (int word : raw) {
        builder.append(corpus.dictionary().getToken(word));
        builder.append(" ");
      }
      builder.deleteCharAt(builder.length() - 1);
      docs.add(builder.toString());
    }

    return docs;
  }

  private int[] generateDoc(double[][] phi, double[] topicProbs, double topicProbsSum) {
    final int docLength = Probability.sampleFromPoissonDist(tokensInDoc);
    final int[] doc = new int[docLength];
    for (int i = 0; i < docLength; i++) {
      final int topic = Probability.sampleFromMultinomialDist(topicProbs, topicProbsSum);
      final double sum = MatrixUtils.sumColumn(phi, topic);
      final int word = Probability.sampleFromMultinomialDist(phi, topic, sum);
      doc[i] = word;
    }
    return doc;
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

      return Probability.sampleFromMultinomialDist(probabilities, sum);
    }
  }
}