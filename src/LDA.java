import java.util.*;

class LDA {
  
  public static final String freqFile = "wordfreqs.txt";
  public static final String cleanedFile = "clean.txt";
  public static final String processedFile = "processed.txt";
  public static final String stopwords = "data/stopwords.txt";
  public static final String searchterms = "data/searchterms.txt";
  
  public static void main(String[] args) {
    LDA lda = new LDA();
    lda.run(args);
  }
    
  private void run(String[] args) {
    if (args.length < 1) throw new Error("Arguments required");
    if ("-t".equals(args[0])) test();

    switch(args[0]) {
      case "-p":
        String dir = FileManager.prepareDirectory(args);
        clean(args[1], dir);
        process(dir);
        break;
      case "-l":
        run(args[1]);
        break;
      default:
    }
  }
  
  private void run(String dir) {
    Corpus c = new CorpusBuilder().fromFile(dir).build();
    c.run();
  }
  
  private void clean(String in, String dir) {
    TextCleaner tc = new TextCleaner();
    tc.clean(in, dir);
  }
  
  private void process(String dir) {
    Preprocessor p = new Preprocessor();
    p.process(dir);
  }
  
  private void test() {
    TextCleaner.main(null);
  }
}