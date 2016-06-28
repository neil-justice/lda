import java.util.*;

class LDA {
  
  public static final String FREQFILE = "wordfreqs.txt";
  public static final String CLEANEDFILE = "clean.txt";
  public static final String PROCESSEDFILE = "processed.txt";
  public static final String DATABASE = "data.db";
  public static final String STOPWORDS = "lists/stopwords.txt";
  public static final String SEARCHTERMS = "lists/searchterms.txt";
  
  public static void main(String[] args) {
    LDA lda = new LDA();
    lda.run(args);
  }
    
  private void run(String[] args) {
    if (args.length < 1) throw new Error("Arguments required");
    if ("-t".equals(args[0])) test();
    FileManager fm = new FileManager(args);

    switch(args[0]) {
      case "-p": //  args[1] should be a text file
        fm.createOutputDirectory();
        clean(fm.filepath(), fm.dir());
        process(fm.dir());
        break;
      case "-l": // args[1] should be either PROCESSEDFILE or DATABASE
        fm.loadOutputDirectory();
        run(fm.dir(), fm.filenameWithExt());
        break;
      default:
    }
  }
  
  private void run(String dir, String filename) {
    if (DATABASE.equals(filename)) {
      System.out.println("Database found at " + dir + ".  Loading...");
      Corpus c = new CorpusBuilder().fromDatabase(filename).build();
      c.run();
    }
    else if (PROCESSEDFILE.equals(filename)) {
      System.out.println("processed file found.  Loading...");
      Corpus c = new CorpusBuilder().fromFile(dir).build();      
      c.run();
    }
    else {
      throw new Error("File must be " + DATABASE + " or " + PROCESSEDFILE);
    }
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
    System.exit(1);
  }
}