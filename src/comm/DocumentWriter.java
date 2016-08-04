import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.IOException;
import java.util.*;
import java.time.Instant;

public class DocumentWriter {
  private final CommunityStructure structure;
  private final NodeAttributes attributes;
  private final String dir;
  private final Graph g;
  private final int topicCount;
  private final int docCount;
  private final int layers;
  private final long ID;
  private final String del = ",";
  private final String ext = ".csv";
  
  public DocumentWriter(CommunityStructure structure, String dir, Graph g) {
    this.g = g;
    this.structure = structure;
    this.dir = dir;
    topicCount = structure.topicCount();
    docCount = structure.docCount();
    layers = structure.layers();
    ID = Instant.now().getEpochSecond();
    attributes = structure.nodeAttributes();
  }
  
  public void write() {
    for (int layer = 0; layer < layers; layer++) {
      write(layer);
    }
  }
  
  public void write(int layer) {
    if (layer >= layers) throw new Error("layer doesn't exist");
    
    Path filepath = Paths.get(dir + "L" + layer + "-dinfo-" + ID + ext);
    List<String> data = prepareData(layer);
    
    try {
      Files.write(filepath, data, Charset.forName("UTF-8"));
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  private List<String> prepareData(int layer) {
    List<String> data = new ArrayList<String>();
    data.add(prepareHeader());
    
    for (int doc = 0; doc < docCount; doc++) {
      data.add(prepareLine(layer, doc));
    }
    
    return data;
  }
  
  private String prepareHeader() {
    StringBuilder builder = new StringBuilder();
    builder.append("ID");
    builder.append(del);
    builder.append("degree");
    builder.append(del);
    builder.append("comm");
    builder.append(del);
    builder.append("JSD");
    builder.append(del);
    builder.append("E");    
    builder.append(del);
    builder.append("followers");    
    builder.append(del);
    builder.append("friends");
    builder.append(del);
    builder.append("words");
    builder.append(del);
    builder.append("best");
    for (int topic = 0; topic < topicCount; topic++) {
      builder.append(del);
      builder.append("theta (" + topic + " doc)");
    }
    return builder.toString();
  }
  
  private String prepareLine(int layer, int doc) {
    StringBuilder builder = new StringBuilder();
    int comm = structure.community(layer, doc);
    
    builder.append(doc);
    builder.append(del);
    builder.append(g.degree(doc));
    builder.append(del);
    builder.append(comm);
    builder.append(del);
    builder.append(structure.JSDiv(layer, doc, comm));
    builder.append(del);
    builder.append(structure.docEntropy(doc));
    builder.append(del);
    builder.append(attributes.followers(doc));
    builder.append(del);
    builder.append(attributes.friends(doc));
    builder.append(del);
    builder.append(attributes.wordCount(doc));
    builder.append(del);
    builder.append(structure.bestTopicInDoc(doc));
    
    for (int topic = 0; topic < topicCount; topic++) {
      builder.append(del);
      builder.append(structure.theta(topic, doc));
    }
    
    return builder.toString();
  }
  
}