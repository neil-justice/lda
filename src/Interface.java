import java.util.*;

class Interface {
  private final String dir;
  private final Scanner input = new Scanner(System.in);
  private final FileTracker ft;
  private final SQLConnector c;
  private final PartitionWriter writer;
  private final Map<String, Runnable> commands = new HashMap<>();
  private final Map<String, Structurable> clusterers = new HashMap<>();
  private CommunityStructure structure;
  private Corpus corpus;
  private Graph g;
  private String[] cmd;
  private boolean quit = false;
  private final String usageErrorMsg = "Invalid usage.  Type 'help' for usage info.";
  
  public Interface(String dir) {
    this.dir = dir;
    ft = new FileTracker(dir);
    c = new SQLConnector(dir);
    writer = new PartitionWriter(dir);
    init();
  }
  
  public Interface() {
    dir = chooseDir();
    ft = new FileTracker(dir);
    c = new SQLConnector(dir);
    writer = new PartitionWriter(dir);
    init();    
  }
  
  private void init() {
    c.open();
    showInfo();
    
    commands.put("p", this::process);
    commands.put("process", this::process);
    commands.put("reload", this::reload);
    commands.put("write-db", this::reload);
    commands.put("load", this::load);
    commands.put("r", this::run);
    commands.put("run", this::run);
    commands.put("print", this::print);
    commands.put("help", this::help);
    commands.put("q", this::quit);
    commands.put("quit", this::quit);
    commands.put("chart", this::viewCharts);
    commands.put("louvain-seed", this::genLouvainSeed);
    commands.put("write", this::write);
    commands.put("compare", this::compare);
    commands.put("compare-js", this::batchCompare);
    commands.put("modularity", this::modularity);
    commands.put("mod", this::modularity);
    commands.put("coocurrence", this::topicCoocurrence);
    
    clusterers.put("louvain", this::louvain);
    clusterers.put("infomap", this::infomapResults);
    clusterers.put("js", this::clusterUsingJSDivergence);
    clusterers.put("trivial", this::minimiseTrivially);
    clusterers.put("random", this::compareToRandom);
    clusterers.put("import", this::importClustering);
    clusterers.put("temper", this::temper);
    clusterers.put("hybrid", this::hybrid);
    clusterers.put("purity", this::purity);
  }
  
  private String chooseDir() {
    System.out.println("Open which directory?");
    DirectoryLoader dl = new DirectoryLoader();
    String[] dirs = dl.directories();
    boolean chosen = false;
    int choice = -1;
    
    dl.printDirectories();
    
    while(!chosen) {
      try {
        System.out.println("Enter a number: ");
        String line = input.nextLine();
        choice = Integer.parseInt(line);
        if (choice >= dirs.length) throw new Exception();
        else chosen = true;
      } catch (Exception e) {
        chosen = false;
      }
    }
    dl.setDirectory(choice);
    
    return dl.dir();
  }

  public void loop() {
    while(!quit) {
      System.out.println("Enter a command: ");
      String line = input.nextLine();
      cmd = line.split(" ");
      if (cmd.length > 0) {
        if (clusterers.containsKey(cmd[0])) structure = clusterers.get(cmd[0]).run();
        else if (commands.containsKey(cmd[0])) commands.get(cmd[0]).run();
        else System.out.println("Command not recognised.  Try 'help'.");
      } 
    }
  }
  
  private void compare() {
    g = getGraph();
    MutualInformation NMI = new MutualInformation(g.order());
    if (cmd.length == 5) {
      CommunityStructure s1 = clusterers.get(cmd[1]).run();
      int layer1 = parse(cmd[2], "Layer must be a non-negative number.");
      CommunityStructure s2 = clusterers.get(cmd[3]).run();
      int layer2 = parse(cmd[4], "Layer must be a non-negative number.");
      
      if (s1 == null || s2 == null) System.out.println("No such clusterer.");
      else if (layer1 >= s1.layers()) System.out.println("No such layer.");
      else if (layer2 >= s2.layers()) System.out.println("No such layer.");
      else NMI.compare(s1, layer1, s2, layer2);
    }
    else System.out.println(usageErrorMsg);
  }
  
  private void batchCompare() {
    g = getGraph();
    MutualInformation NMI = new MutualInformation(g.order());
    if (structure == null) structure = louvain();
    CommunityStructure js = clusterUsingJSDivergence();
    
    for (int layer = 0; layer < structure.layers(); layer++) {
      NMI.compare(structure, layer, js, 0);
    }
  }
  
  private CommunityStructure purity() {
    g = reloadGraph();
    PurityClusterer clusterer = new PurityClusterer(g, c.getTheta());
    return getStructure(clusterer);
  }
  
  private CommunityStructure hybrid() {
    g = reloadGraph();
    HybridClusterer clusterer = new HybridClusterer(g, MatrixTransposer.transpose(c.getTheta()));
    return getStructure(clusterer);
  }
  
  private CommunityStructure minimiseTrivially() {
    g = getGraph();
    TrivialEntropyMinimiser clusterer = new TrivialEntropyMinimiser(g.order(), c.getTheta());
    return getStructure(clusterer);
  }
  
  private CommunityStructure clusterUsingJSDivergence() {
    g = getGraph();
    Clusterer clusterer;
    
    if (!ft.hasJSPartInfo()) {
      clusterer = new JSClusterer(g, MatrixTransposer.transpose(c.getTheta()));
      return getStructure(clusterer, CTUT.JS_PARTITION_SET);
    }
    else {
      clusterer = new PartitionReader(dir + CTUT.JS_PARTITION_SET);
      return getStructure(clusterer);
    }
  }
  
  private CommunityStructure louvain() {
    g = reloadGraph();
    Clusterer clusterer;
    
    if (!ft.hasLouvainPartInfo()) {
      clusterer = new LouvainDetector(g);
      return getStructure(clusterer);
    }
    else {
      clusterer = new PartitionReader(dir + CTUT.LOUVAIN_PARTITION_SET);
      return getStructure(clusterer);
    }    
  }
  
  private CommunityStructure infomapResults() {
    if (!ft.hasInfomap()) throw new Error("No infomap data at " + CTUT.INFOMAP);
    
    InfomapResultsReader irr = new InfomapResultsReader(dir + CTUT.INFOMAP);
    return getStructure(irr);
  }  
  
  private CommunityStructure compareToRandom() {
    if (structure == null) structure = louvain();
    RandomCommunityAssigner assigner = new RandomCommunityAssigner(structure.communityLayers());
    return getStructure(assigner);
  }
  
  private CommunityStructure importClustering() {
    Clusterer clusterer;
    if (cmd.length == 2) {
      clusterer = new PartitionReader(cmd[1]);
      return getStructure(clusterer);
    }
    else System.out.println(usageErrorMsg);
    return null;
  }
  
  private CommunityStructure temper() {
    int layer;
    if (cmd.length == 2) {
      layer = parse(cmd[1], "Layer must be a non-negative number.");
    }
    else layer = 0;
    
    if (structure == null) structure = louvain();
    g = GraphUtils.loadPartitionSet(new GraphBuilder()
                                    .fromFileAndDB(dir + CTUT.GRAPH, c)
                                    .build(),
                                    structure.communities(layer));
    Temperer temperer = new Temperer(g, MatrixTransposer.transpose(c.getTheta()));
    return getStructure(temperer);      
  }
  
  private void genLouvainSeed() {
    g = reloadGraph();
    int it = 10;
    if (cmd.length == 2) {
      it = parse(cmd[1], "num. iterations must be a non-negative number.");      
    }
    LouvainSelector selector = new LouvainSelector(dir, c);
    selector.run(it);
  }
  
  private void write() {
    if (structure == null) structure = louvain();
    g = getGraph();
    CommunityWriter cWriter = new CommunityWriter(structure, dir);
    DocumentWriter dWriter = new DocumentWriter(structure, dir, g);
    PhiWriter pWriter = new PhiWriter(c.getPhi(), new Translator(c), dir);
    cWriter.write();
    dWriter.write();
    pWriter.write();
  }  
  
  private void viewCharts() {
    if (cmd.length == 2) {
      GUI gui;
      if (structure == null) structure = louvain();
      int layer = parse(cmd[1], "Layer must be a non-negative number.");   
      if (layer >= 0 && layer < structure.layers()) gui = new GUI(structure, layer, c.getPhi());
      else System.out.println("Layer does not exist.");
    }
    else System.out.println(usageErrorMsg);    
    // DocumentSimilaritySpace simSpace = new DocumentSimilaritySpace(structure);
    // simSpace.run();
  }
  
  private void topicCoocurrence() {
    if (cmd.length == 2) {
      if (structure == null) structure = louvain();
      TopicCoocurrenceMonitor tcm = new TopicCoocurrenceMonitor(structure);
      int layer = parse(cmd[1], "Layer must be a non-negative number.");   
      if (layer > 0 && layer < structure.layers()) tcm.run(layer);
      else System.out.println("Layer does not exist.");
    }
    else System.out.println(usageErrorMsg);
  }
  
  private void modularity() {
    if (structure == null) structure = louvain();
    System.out.println("L Modularity");
    for (int layer = 0; layer < structure.layers(); layer++) {
      g = GraphUtils.loadPartitionSet(getGraph(), structure.communities(layer));
      System.out.println(layer + " " + g.modularity());
    }
    g = null; // otherwise something seems to get messed up 
  }  
  
  private void process() {
    Preprocessor p;
    if (cmd.length == 1) p = new Preprocessor();
    else if (cmd.length == 3) {
      int min = parse(cmd[1], "Min word freq. must be a non-negative number.");
      int max = parse(cmd[2], "Max word freq. must be a non-negative number.");
      p = new Preprocessor(min, max);
    }
    else {
      System.out.println(usageErrorMsg);
      return;
    }
    p.process(dir);
    System.out.println("Text processed.");
  }
  
  private void reload() {
    if (corpus != null) corpus.quit();
    
    if (cmd.length == 2) {
      int topics = parse(cmd[1], "Topic count must be a non-negative number.");   
      if (topics > 0) {
        corpus = new CorpusBuilder(topics, c).fromFile(dir).build();
        System.out.println("Corpus reloaded.");
      }
    }
  }
  
  private void load() {
    if (!ft.isInDB()) {
      System.out.println("load to DB first.");
      return;
    }
    
    if (cmd.length == 2) {
      int topics = parse(cmd[1], "Topic count must be a non-negative number.");   
      if (topics > 0) {
        corpus = new CorpusBuilder(topics, c).fromDatabase(dir).build();
        System.out.println("Corpus loaded.");
      }
    }
  }
  
  private void run() {
    if (corpus == null) {
      System.out.println("Load a corpus first.");
      return;
    }
    
    if (cmd.length == 2) {
      int cycles = parse(cmd[1], "Number of cycles must be a positive number.");
      if (cycles > 0) corpus.run(cycles);
    }
  }
  
  private void print() {
    if (corpus == null) LDAUtils.termScore(c.getPhi(), new Translator(c));
    else corpus.print();
  }
  
  private void quit() {
    quit = true;
    if (corpus != null) corpus.quit();
  }
  
  private void help() {
    showInfo();
    System.out.println("LDA commands:");
    System.out.println("  process<[min][max]> -- processes the clean file, removing stop words");
    System.out.println("                         and so on. Default min and max freqs. are 10, 1000000");
    System.out.println("  write-db [topics] -- initialise db from processed file.");
    System.out.println("  reload [topics]   -- (re)-initialises db from processed file.");
    System.out.println("  load [topics]     -- loads corpus from db.");
    System.out.println("  run [cycles]      -- runs the specified no. of cycles.");
    System.out.println("  print             -- prints topic termscores.");
    System.out.println("");
    System.out.println("Clustering commands:");
    System.out.println("  louvain           -- clusters graph using the Louvain method.");
    System.out.println("  louvain-seed <[itr]> -- generate and store the best random seed out of [itr] for");
    System.out.println("                       the Louvain method.  Default is 10.");
    System.out.println("  infomap           -- reads in an infomap .tree file.");
    System.out.println("  js                -- clusters using Jensen-Shannon Divergence.");
    System.out.println("  random            -- compare the current partition set to a random one");
    System.out.println("                       where the size of each partition is the same.");
    System.out.println("  trivial           -- partition set which trivially minimises average entropy.");
    System.out.println("  import [file]     -- import partition set from file");
    System.out.println("  chart [layer]     -- display charts for current partition set.");
    System.out.println("  write             -- write out community and document info.");
    System.out.println("  compare [cls][layer][cls2][layer2] -- calculates the NMI of two partitonn sets.");
    System.out.println("  compare-js        -- calculates the NMI between each layer of the current partition set");
    System.out.println("                       and the JSD-based clusterer");
    System.out.println("  modularity        -- show the modularity of the current partition set");
    System.out.println("  coocurrence [layer] -- show which topics co-ocurr regularly");
    System.out.println("");
    System.out.println("Meta-commands:");
    System.out.println("  help              -- shows this list.");
    System.out.println("  quit              -- exits the program.");
  }
  
  private void showInfo() {
    System.out.println("In directory: " + dir);
    System.out.println("Cleaned:      " + ft.isClean());
    System.out.println("Processed:    " + ft.isProcessed());
    System.out.println("In DB:        " + ft.isInDB());
    System.out.println("Graph data:   " + ft.hasGraph());
    System.out.println("Infomap data: " + ft.hasInfomap());
    System.out.println("Louvain data: " + ft.hasLouvainPartInfo());
    System.out.println("JSDclus data: " + ft.hasJSPartInfo());
  } 
          
  private int parse(String text, String err) {
    int val;
    try {
      val = Integer.parseInt(text);
      if (val < 0) throw new Exception();
      return val;
    } catch (Exception e) {
      System.out.println("Invalid input. " + err);
      return -1;
    }
  }
  
  private Graph getGraph() {
    if (g != null) return g;
    else return reloadGraph();
  }
  
  private Graph reloadGraph() {
    if (!ft.hasGraph()) throw new Error("No graph data at" + dir + CTUT.GRAPH);
    return new GraphBuilder().fromFileAndDB(dir + CTUT.GRAPH, c).build();
  }
  
  private CommunityStructure getStructure(Clusterer clusterer, String filename) {
    List<int[]> communities = clusterer.run();
    writer.write(communities, filename);
    return getStructure(communities);
  }
  
  private CommunityStructure getStructure(Clusterer clusterer) {
    return getStructure(clusterer.run());
  }
  
  private CommunityStructure getStructure(List<int[]> communities) {
    if (g == null) g = getGraph();
    NodeAttributes attributes = new NodeAttributes(c, dir, g.order());
    CommunityStructure s = new CommunityStructure(communities, c.getTheta(),
                                                  attributes);
    return s;        
  }
  
  public interface Structurable {
    public CommunityStructure run();
  }
}