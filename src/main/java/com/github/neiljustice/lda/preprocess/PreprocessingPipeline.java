package com.github.neiljustice.lda.preprocess;

import java.util.*;
import com.github.neiljustice.lda.util.*;

/**
 * Customisable wrapper around the classes used to preprocess text.
 * Use the static defaultPipeline method to create one with the default settings
 * for the component classes.
 */
public class PreprocessingPipeline {
  private final TextCleaner textcleaner;
  private final Tokeniser tokeniser;
  private final StopwordsRemover stopwordsRemover;
  private final Preprocessor preprocessor;
  
  public PreprocessingPipeline(TextCleaner textcleaner,
                               Tokeniser tokeniser,
                               StopwordsRemover stopwordsRemover,
                               Preprocessor preprocessor) {
    this.textcleaner = textcleaner;
    this.tokeniser = tokeniser;
    this.stopwordsRemover = stopwordsRemover;
    this.preprocessor = preprocessor;
  }
  
  public Corpus run(String filename) {
    return run(FileUtils.readFile(filename));
  }
  
  public Corpus run(File file) {
    return run(FileUtils.readFile(file));
  }

  public Corpus run(List<String> documents) {

    List<String> cleaned = textcleaner.clean(documents);
    List<List<String>> tokenised = tokeniser.tokenise(clean);
    stopwordsRemover.removeFrom(tokenised);
    preprocessor.process(tokenised);
    
    return new Corpus(tokenised);
  }
  
  public static PreprocessingPipeline defaultPipeline() {
    TextCleaner textcleaner = new TextCleaner();
    Tokeniser tokeniser = new Tokeniser();
    StopwordsRemover stopwordsRemover = new stopwordsRemover();
    Preprocessor preprocessor = new Preprocessor();
    return new ProcessingPipeline(textcleaner, 
                                  tokeniser, 
                                  stopwordsRemover, 
                                  preprocessor);
  }  
}