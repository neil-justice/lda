import java.util.*;
import java.io.*;
import java.nio.file.*;

class LDA {
  
  public static final String freqFile = "wordfreqs.txt";
  public static final String cleanedFile = "clean.txt";
  public static final String processedFile = "processed.txt";
  public static final String stopwords = "data/stopwords.txt";
  public static final String searchterms = "data/searchterms.txt";
  
  public static void main(String[] args) {
    /* 
     * Steps:
     * - find number of documents, tokens, unique words
     * - give each word an id (map)
     * - for each doc, map word id ocurrences to count
     */
    LDA lda = new LDA();
    lda.run(args);
  }
    
  private void run(String[] args) {
    if (args.length < 1) throw new Error("Arguments required");
    if ("-t".equals(args[0])) test();

    switch(args[0]) {
      case "-c":
      String dir = prepareDirectory(args);
        clean(args[1], dir);
        process(dir);
        break;
      default:
    }
  }
  
  private void test() {
    TextCleaner.main(null);
  }
  
  private void clean(String in, String dir) {
    TextCleaner tc = new TextCleaner();
    try {
      tc.clean(in, dir);
    }
    catch(Exception e) {
      throw new Error("input file not found");
    }
  }
  
  private void process(String dir) {
    Preprocessor p = new Preprocessor();
    p.process(dir);
  }
  
  private String prepareDirectory(String[] args) {
    Path p = checkFilename(args);

    String filename = p.getFileName().toString();
    if (filename.indexOf(".") > 0) {
      filename = filename.substring(0, filename.lastIndexOf("."));
    }
    
    String dir = "out/" + filename + "/";
    File file = new File(dir);

    // if (file.exists()) throw new Error("text is already clean");
    file.mkdir();
    if (!file.isDirectory()) throw new Error("Directory creation failed");
    
    return dir;
  }
  
  private Path checkFilename(String[] args) {
    if (args.length < 2) throw new Error("Please provide a file name");
    Path p = Paths.get(args[1]);
    if (!p.toFile().exists()) throw new Error("Missing input file");
    
    return p;
  }  
}