package com.github.neiljustice.lda.preprocess;

import com.github.neiljustice.lda.Corpus;
import com.github.neiljustice.lda.util.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * Customisable wrapper around the classes used to preprocess text.
 * Use the static defaultPipeline method to create one with the default settings
 * for the component classes.
 */
public class PreprocessingPipeline {
  private TextCleaner textCleaner;
  private Tokeniser tokeniser;
  private StopwordsRemover stopwordsRemover;
  private Preprocessor preprocessor;

  public PreprocessingPipeline(TextCleaner textCleaner,
                               Tokeniser tokeniser,
                               StopwordsRemover stopwordsRemover,
                               Preprocessor preprocessor) {
    this.textCleaner = textCleaner;
    this.tokeniser = tokeniser;
    this.stopwordsRemover = stopwordsRemover;
    this.preprocessor = preprocessor;
  }

  public static PreprocessingPipeline defaultPipeline() {
    final TextCleaner textcleaner = new TextCleaner();
    final Tokeniser tokeniser = new Tokeniser();
    final StopwordsRemover stopwordsRemover = new StopwordsRemover();
    final Preprocessor preprocessor = new Preprocessor();

    return new PreprocessingPipeline(textcleaner, tokeniser, stopwordsRemover, preprocessor);
  }

  public static PreprocessingPipeline noOpPipeline() {
    final TextCleaner textcleaner = new TextCleaner(Collections.emptyList());
    final Tokeniser tokeniser = new Tokeniser();
    final StopwordsRemover stopwordsRemover = new StopwordsRemover(Collections.emptySet());
    final Preprocessor preprocessor = new Preprocessor()
        .useMaxPerc(false)
        .useMinPerc(false)
        .useMaxDocs(false)
        .useMinDocs(false)
        .useMaxFreq(false)
        .useMinFreq(false)
        .useMaxLength(false)
        .useMinLength(false);

    return new PreprocessingPipeline(textcleaner, tokeniser, stopwordsRemover, preprocessor);
  }


  /**
   * Load and pre-process a file.  Assumes that each line in the file represents a document.
   *
   * Assumes the file is UTF-8 encoded.
   *
   * @param filename the path to the file to process.
   */
  public Corpus preprocess(String filename) {
    return preprocess(FileUtils.readFile(filename));
  }

  /**
   * Load and pre-process a file.  Assumes that each line in the file represents a document.
   *
   * @param filename the path to the file to process.
   * @param charset the file encoding.
   */
  public Corpus preprocess(String filename, Charset charset) {
    return preprocess(FileUtils.readFile(filename, charset));
  }

  /**
   * Load and pre-process a file.  Assumes that each line in the file represents a document.
   *
   * Assumes the file is UTF-8 encoded.
   *
   * @param file the file to process.
   */
  public Corpus preprocess(File file) {
    return preprocess(FileUtils.readFile(file));
  }
  /**
   * Load and pre-process a file.  Assumes that each line in the file represents a document.
   *
   * @param file the file to process.
   * @param charset the file encoding.
   */
  public Corpus preprocess(File file, Charset charset) {
    return preprocess(FileUtils.readFile(file, charset));
  }

  /**
   * Pre-process a list of strings, each of which is treated as a document.
   */
  public Corpus preprocess(List<String> documents) {

    final List<String> cleaned = textCleaner.clean(documents);
    final List<List<String>> tokenised = tokeniser.tokenise(cleaned);
    stopwordsRemover.removeFrom(tokenised);
    preprocessor.process(tokenised);

    return new Corpus(tokenised);
  }

  public TextCleaner getTextCleaner() {
    return textCleaner;
  }

  public void setTextCleaner(TextCleaner textCleaner) {
    this.textCleaner = textCleaner;
  }

  public Tokeniser getTokeniser() {
    return tokeniser;
  }

  public void setTokeniser(Tokeniser tokeniser) {
    this.tokeniser = tokeniser;
  }

  public StopwordsRemover getStopwordsRemover() {
    return stopwordsRemover;
  }

  public void setStopwordsRemover(StopwordsRemover stopwordsRemover) {
    this.stopwordsRemover = stopwordsRemover;
  }

  public Preprocessor getPreprocessor() {
    return preprocessor;
  }

  public void setPreprocessor(Preprocessor preprocessor) {
    this.preprocessor = preprocessor;
  }
}