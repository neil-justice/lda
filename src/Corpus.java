import java.util.*;

public class Corpus {
  
  private final List<Token> tokens;
  private final Translator translator; // used to translate from ID to word/doc
  private final int wordCount;         // no. of unique words
  private final int docCount;          // no. of docs
  private final int tokenCount;        // total no. of tokens
  private final int topics = 100;      // no. of topics
  
  public Corpus(CorpusBuilder builder) {
    tokens = builder.tokens();
    wordCount = builder.wordCount();
    docCount = builder.docCount();
    tokenCount = builder.tokenCount();
    translator = new Translator(builder.words(), builder.documents());
  }
  
  private void initialise() {
    
  }
  
  // Initialises all tokens with a randomly selected topic.
  private void assignInitialTopics() {
    Random rand = new Random();
    for (Token t: tokens) {
      t.topic(rand.nextInt(topics));
    }
  }
  
  private void initialiseMatrices() {
    
  }
}