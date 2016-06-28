import java.util.*;

class Preprocessor {
  
  private final WordFrequencyList wordfreqs = new WordFrequencyList();
  private final Set<String> toRemove = new HashSet<String>();
  private final Set<String> doNotRemove = new HashSet<String>();
  private final int minFreq = 64;
  private final int maxFreq = 1000000;
  private long tokenCount = 0;
  private long totalCount = 0;
  
  public void process(String dir) {
    wordfreqs.load(dir);
    buildRemovalList();
    System.out.println(toRemove.size() + " / " + wordfreqs.size() + " words to be removed");
    wordfreqs.removeAll(toRemove);
    wordfreqs.write(dir);
    FileLoader.processFile(dir + LDA.CLEANEDFILE, dir + LDA.PROCESSEDFILE, this::removeWords);
    System.out.println(tokenCount + " tokens kept out of " + totalCount);
  }
  
  private void buildRemovalList() {
    
    FileLoader.loadList(LDA.STOPWORDS, toRemove);
    FileLoader.loadList(LDA.SEARCHTERMS, doNotRemove);
    
    for (Map.Entry<String, Integer> e: wordfreqs.entrySet()) {
      String word = e.getKey();
      int freq = e.getValue();
      if (freq < minFreq) toRemove.add(word);
      if (freq > maxFreq) toRemove.add(word);
      if (word.length() <= 2) toRemove.add(word);
    }
    
    toRemove.removeAll(doNotRemove);
  }
  
  private String removeWords(String in){
    String[] sections = in.split("\t");
    String tags = "";
    if (sections.length != 5) {
      throw new Error("length " + sections.length + " at " + sections[0]);
    }
    String processed = processLine(sections[1]);
    return sections[0] + "\t" + processed;// + "\t" + sections[2] + "\t" + sections[3] + "\t" + sections[4];
  }
  
  private String processLine(String text) {
    String[] splitText = text.split(" ");
    List<String> out = new ArrayList<String>();
    
    for (String s: splitText) {
      if (wordfreqs.contains(s)) out.add(s);
    }
    
    tokenCount += out.size();
    totalCount += splitText.length;
    return String.join(" ", out);
  }
}