import java.util.*;
import java.io.*;
import gnu.trove.map.hash.*;
import gnu.trove.list.array.TLongArrayList;

public class CorpusBuilder {
  
  private final TLongArrayList documents = new TLongArrayList();
  private final TObjectIntHashMap<String> words = new TObjectIntHashMap<>();
  private Tokens tokens;
  private int tokenCount = 0;
  private int wordCount;
  private int docCount;
  private int topicCount;
  private String dir;
  
  public CorpusBuilder(int topicCount) {
    this.topicCount = topicCount;
  }
  
  public CorpusBuilder fromFile(String dir) {
    this.dir = dir;
    tokens = new Tokens();
    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(dir + LDA.PROCESSEDFILE)));
      String line;
      int i = 0;
      
      while ((line = reader.readLine()) != null) {
        String[] splitLine = line.split("\t");
        if (splitLine.length != 2) {
          throw new Error("length " + splitLine.length + " at " + splitLine[0]);
        }
        processLine(Long.parseLong(splitLine[0]), splitLine[1].split(" "));
        i++;
        if (i % 1000 == 0) System.out.println(i + " documents loaded");
      }
    } catch (NumberFormatException e) {
      throw new Error("invalid doc ID");
    } catch (FileNotFoundException e) {
      throw new Error("file " + LDA.PROCESSEDFILE + " not found at " + dir);
    } catch (IOException e) {
      throw new Error("IO error");
    }
    
    wordCount = words.size();
    docCount = documents.size();
    tokens.shuffle();
    writeDB();
    return this;
  }
  
  public CorpusBuilder fromDatabase(String dir) {
    this.dir = dir;
    SQLConnector c = new SQLConnector(dir);
    
    c.open();
    System.out.println("Database found at " + dir + LDA.DATABASE + "  Loading...");
    c.showPragmas();
    tokens     = c.getTokens();
    tokenCount = tokens.size();
    wordCount  = c.getCount("Word");
    docCount   = c.getCount("Doc");
    c.close();
    
    return this;
  }
  
  private void writeDB() {
    SQLConnector c = new SQLConnector(dir);
    c.open();
    
    try {
      System.out.println("Creating database...");
      c.createDrop();
      c.showPragmas();
      System.out.println("Database created.  Beginning write.");
      c.buildDocumentDictionary(documents);
      c.buildWordDictionary(words);
      c.buildTokenList(tokens);
      System.out.println("Database written.");
      } finally {
      c.close();
    }
  }
  
  private void processLine(long id, String[] tokens) {
    documents.add(id);
    int doc = documents.size() - 1;
    
    for (String word: tokens) {
      processWord(word, doc);
    }
  }
  
  private void processWord(String word, int doc) {
    int in = 0;
    
    if (!words.containsKey(word)) {
      in = words.size();
      words.put(word, in);
    }
    else in = words.get(word);
    tokens.add(in, doc);
    tokenCount++;
  }
  
  public Tokens tokens() { return tokens; }
  public int wordCount() { return wordCount; }
  public int docCount() { return docCount; }
  public int tokenCount() { return tokenCount; }
  public int topicCount() { return topicCount; }
  public String dir() { return dir; }
  public Corpus build() { return new Corpus(this); }
}