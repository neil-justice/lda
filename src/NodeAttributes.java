/* loads files containing node attribute info such as follower count. */
import java.io.*;
import java.util.*;

public class NodeAttributes {
  
  private final Translator translator;
  private final String dir;
  private final String delimiter = ",";
  private final int order;
  private final int[] followers;
  private final int[] friends;
  private final int[] wordCount;
  private int[] attribute; // used to read in files
  
  public NodeAttributes(SQLConnector c, String dir, int order) {
    translator = new Translator(c);
    this.dir = dir;
    this.order = order;
    
    friends   = read(dir + CTUT.ATTR_FRIENDS);
    followers = read(dir + CTUT.ATTR_FOLLOWERS);
    wordCount = read(dir + CTUT.ATTR_WORDCOUNT);
  }
  
  private int[] read(String filename) {
    attribute = new int[order];
    List<String> list = FileLoader.readFile(filename, this::lineReader);
    return attribute;
  }
  
  private void lineReader(String in) {
    String[] split = in.split(delimiter);
    long raw = Long.parseLong(split[0]);
    int attr = Integer.parseInt(split[1]);
    int node = translator.getDocIndex(raw);
    attribute[node] = attr;    
  }
  
  public int[] friends() { return friends; }
  public int friends(int node) { return friends[node]; }
  
  public int[] followers() { return followers; }
  public int followers(int node) { return followers[node]; }
  
  public int[] wordCount() { return wordCount; }
  public int wordCount(int node) { return wordCount[node]; }
}