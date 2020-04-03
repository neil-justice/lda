package com.github.neiljustice.lda.preprocess;

import com.github.neiljustice.lda.Corpus;
import com.github.neiljustice.lda.util.FileUtils;

import java.io.File;
import java.util.List;

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

  public static PreprocessingPipeline defaultPipeline() {
    final TextCleaner textcleaner = new TextCleaner();
    final Tokeniser tokeniser = new Tokeniser();
    final StopwordsRemover stopwordsRemover = new StopwordsRemover();
    final Preprocessor preprocessor = new Preprocessor();
    return new PreprocessingPipeline(textcleaner,
        tokeniser,
        stopwordsRemover,
        preprocessor);
  }

  public Corpus run(String filename) {
    return run(FileUtils.readFile(filename));
  }

  public Corpus run(File file) {
    return run(FileUtils.readFile(file));
  }

  public Corpus run(List<String> documents) {

    final List<String> cleaned = textcleaner.clean(documents);
    final List<List<String>> tokenised = tokeniser.tokenise(cleaned);
    stopwordsRemover.removeFrom(tokenised);
    preprocessor.process(tokenised);

    return new Corpus(tokenised);
  }
}