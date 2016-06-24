import java.util.*;

public class Corpus {
  
  private final Tokens tokens;
  private final Translator translator; // used to translate from ID to word/doc
  private final Random rand = new Random();
  private final int wordCount;              // no. of unique words
  private final int docCount;               // no. of docs
  private final int tokenCount;             // total no. of tokens
  private final int topicCount = 30;
  private final int cycles = 100;
  private final int[] tokensInTopic = new int[topicCount];
  private final int[][] wordsInTopic;
  private final int[][] topicsInDoc;
  private final double alpha; // hyperparameters
  private final double beta;  // hyperparameters
  
  public Corpus(CorpusBuilder builder) {
    tokens = builder.tokens();
    wordCount = builder.wordCount();
    docCount = builder.docCount();
    tokenCount = builder.tokenCount();
    translator = new Translator();
    
    wordsInTopic = new int[wordCount][topicCount];
    topicsInDoc = new int[topicCount][docCount];
    alpha = 50 / (double) topicCount;
    beta = 200 / (double) wordCount;
  }
  
  public void run() {
    System.out.println(" V : " + wordCount + 
                       " D : " + docCount + 
                       " N : " + tokenCount);
    assignInitialTopics();
    initialiseMatrices();
    for (int i = 0; i < cycles; i++) {
      cycle();
      System.out.println("cycle " + i);
    }
    print();
    termScore();
  }
  
  private void cycle() {
    for (int i = 0; i < tokenCount; i++) {
      int word = tokens.word(i);
      int oldTopic = tokens.topic(i);
      int doc = tokens.doc(i);
      
      tokensInTopic[oldTopic]--;      
      wordsInTopic[word][oldTopic]--;
      topicsInDoc[oldTopic][doc]--;
      tokens.setTopic(i, -1);
      
      double[] probabilities = new double[topicCount];
      double sum = 0;
      for (int topic = 0; topic < topicCount; topic++) {
        probabilities[topic] = (wordsInTopic[word][topic] + beta)
                             * (topicsInDoc[topic][doc] + alpha)
                             / (tokensInTopic[topic] + docCount);
        sum += probabilities[topic];
      }
      int newTopic = -1;
      double sample = rand.nextDouble() * sum;
			while (sample > 0.0) {
				newTopic++;
				sample -= probabilities[newTopic];
			}
			if (newTopic == -1) {
				throw new IllegalStateException ("New topic not sampled.  sample: " + sample + " sum: " + sum);
			}
      
      tokensInTopic[newTopic]++;      
      wordsInTopic[word][newTopic]++;
      topicsInDoc[newTopic][doc]++;
      tokens.setTopic(i, newTopic);
    }
  }
  
  // Initialises all tokens with a randomly selected topic.
  private void assignInitialTopics() {
    Random rand = new Random();
    for (int i = 0; i < tokenCount; i++) {
      int topic = rand.nextInt(topicCount);
      tokens.setTopic(i, topic);
      tokensInTopic[topic]++;
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
  
  private void print() {
    System.out.println("");
    for (int word = 0; word < wordCount; word++) {
      System.out.printf("%15s", translator.getWord(word));
      for (int topic = 0; topic < topicCount; topic++) {
        System.out.printf("%4d", wordsInTopic[word][topic]);
      }
      System.out.println("");
    }
  }
}