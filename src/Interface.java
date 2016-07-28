import java.util.*;

class Interface {
  private final String dir;
  private final Scanner input = new Scanner(System.in);
  private final FileTracker ft;
  private final SQLConnector c;
  private CommunityStructure structure;
  private Corpus corpus;
  private boolean quit = false;
  
  public Interface(String dir) {
    this.dir = dir;
    ft = new FileTracker(dir);
    c = new SQLConnector(dir);
    showInfo();
  }
  
  public Interface() {
    System.out.println("Open which directory?");
    DirectoryLoader dl = new DirectoryLoader();
    String[] dirs = dl.directories();
    boolean chosen = false;
    int choice = -1;
    
    for (int i = 0; i < dirs.length; i++) {
      System.out.println(i + " : " + dirs[i]);
    }
    
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
    
    dir = dl.dir();
    ft = new FileTracker(dir);
    c = new SQLConnector(dir);
    c.open();
    showInfo();
  }

  public void loop() {
    while(!quit) {
      System.out.println("Enter a command: ");
      String line = input.nextLine();
      String[] cmd = line.split(" ");
      if (cmd.length > 0) {
        switch(cmd[0]) {
          case "p":
          case "process":
            process();
            break;
          case "reload":
            reload(cmd);
            break;
          case "load":
            load(cmd);
            break;
          case "r":
          case "run":
            run(cmd);
            break;
          case "print":
            print();
            break;
          case "help":
            help();
            break;
          case "q":
          case "quit":
            quit = true;
            if (corpus != null) corpus.quit();
            break;
          case "chart":
            viewCharts();
            break;
          case "louvain":
            runLouvainDetector();
            break;
          case "infomap":
            loadInfomapResults();
            break;
          case "random":
            compareToRandom();
            break;
          case "js":
            clusterUsingJSDivergence();
            break;
          default:
            System.out.println("Command not recognised.");
        }
      } 
    }
  }
  
  private void clusterUsingJSDivergence() {
    Graph g = getGraph();
    JSClusterer clusterer = new JSClusterer(g, c.getTheta());
    structure = new CommunityStructure(clusterer.run(), c.getTheta());
  }
  
  private void compareToRandom() {
    if (structure == null) runLouvainDetector();
    RandomCommunityAssigner assigner = new RandomCommunityAssigner(structure.communityLayers());
    
    CommunityStructure randomStructure = new CommunityStructure(assigner.run(), c.getTheta());
  }
  
  private void runLouvainDetector() {
    Graph g = getGraph();
    LouvainDetector ld = new LouvainDetector(g);
    structure = new CommunityStructure(ld.run(), c.getTheta());
  }
  
  private void loadInfomapResults() {
    if (!ft.hasInfomap()) throw new Error("No infomap data at " + CTUT.INFOMAP);
    
    InfomapResultsReader irr = new InfomapResultsReader(dir + CTUT.INFOMAP);
    structure = new CommunityStructure(irr.run(), c.getTheta());
  }
  
  private void viewCharts() {
    if (structure == null) runLouvainDetector();
    GUI gui = new GUI(structure);
    // DocumentSimilaritySpace simSpace = new DocumentSimilaritySpace(structure);
    // simSpace.run();
  }
  
  private void process() {
    Preprocessor p = new Preprocessor();
    p.process(dir);
    System.out.println("Text processed.");
  }
  
  private void reload(String[] cmd) {
    if (corpus != null) corpus.quit();
    
    if (cmd.length == 2) {
      int topics = parse(cmd[1]);   
      if (topics > 0) {
        corpus = new CorpusBuilder(topics, c).fromFile(dir).build();
        System.out.println("Corpus reloaded.");
      }
    }
    else System.out.println("Choose a topic count.");
  }
  
  private void load(String[] cmd) {
    if (!ft.isInDB()) {
      System.out.println("load to DB first.");
      return;
    }
    
    if (cmd.length == 2) {
      int topics = parse(cmd[1]);   
      if (topics > 0) {
        corpus = new CorpusBuilder(topics, c).fromDatabase(dir).build();
        System.out.println("Corpus loaded.");
      }
    }
    else System.out.println("Choose a topic count.");
  }
  
  private void run(String[] cmd) {
    if (corpus == null) {
      System.out.println("Load a corpus first.");
      return;
    }
    
    if (cmd.length == 2) {
      int cycles = parse(cmd[1]);
      if (cycles > 0) corpus.run(cycles);
    }
  }
  
  private void print() {
    if (corpus == null) System.out.println("Load a corpus first.");
    else corpus.print();
  }
  
  private void showInfo() {
    System.out.println("In directory : " + dir);
    System.out.println("Cleaned:      " + ft.isClean());
    System.out.println("Processed:    " + ft.isProcessed());
    System.out.println("In DB:        " + ft.isInDB());
    System.out.println("Graph data:   " + ft.hasGraph());
    System.out.println("Infomap data: " + ft.hasInfomap());
  }
  
  private void help() {
    showInfo();
    System.out.println("Commands:");
    System.out.println("  process         -- processes the clean file, removing stop words and so on");
    System.out.println("  reload [topics] -- (re)-initialises db from processed file.");
    System.out.println("  load [topics]   -- loads corpus from db.");
    System.out.println("  run [cycles]    -- runs the specified no. of cycles.");
    System.out.println("  print           -- prints [topic][word matrix] and termscore");
    System.out.println("  help            -- shows this list");
    System.out.println("  quit            -- exits the program");
  }
  
  private int parse(String text) {
    int val;
    try {
      val = Integer.parseInt(text);
      if (val < 0) throw new Exception();
      return val;
    } catch (Exception e) {
      System.out.println("Invalid input.  This command needs a +ve number");
      return -1;
    }
  }
  
  private Graph getGraph() {
    if (!ft.hasGraph()) throw new Error("No graph data at" + dir + CTUT.GRAPH);
    return new GraphBuilder().fromFileAndDB(dir + CTUT.GRAPH, c).build();
  }
}