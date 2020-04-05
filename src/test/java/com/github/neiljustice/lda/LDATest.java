package com.github.neiljustice.lda;

import com.github.neiljustice.lda.preprocess.PreprocessingPipeline;
import com.github.neiljustice.lda.topic.Topic;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertTrue;

public class LDATest {

  LDA lda;

  Corpus corpus;

  @Test
  public void shouldDetectTopics() {
    final PreprocessingPipeline pipeline = PreprocessingPipeline.noOpPipeline();

    corpus = pipeline.preprocess(generateDocs(100, 100));

    lda = new LDA(corpus, 2);
    lda.train(100, 10, 2, 20, 2);
    final List<Topic> topics = lda.getTopics(4);
    for (Topic topic : topics) {
      if (topic.getWords().contains("a")) {
        assertTrue(topic.getWords().containsAll(Arrays.asList("a", "b", "c", "d")));
      }
      if (topic.getWords().contains("1")) {
        assertTrue(topic.getWords().containsAll(Arrays.asList("1", "2", "3", "4")));
      }
    }
  }

  private List<String> generateDocs(int count, int maxWords) {
    final List<String> topicA = Arrays.asList(
        "a", "b", "c", "d", "filler"
    );
    final List<String> topicB = Arrays.asList(
        "1", "2", "3", "4", "filler"
    );
    final List<String> docs = new ArrayList<>();
    for (int doc = 0; doc < count; doc++) {
      final int max = ThreadLocalRandom.current().nextInt(maxWords);
      final boolean topic = ThreadLocalRandom.current().nextBoolean();
      final StringBuilder builder = new StringBuilder();
      for (int word = 0; word < max; word++) {
        builder.append(" ").append((topic ? topicA : topicB).get(ThreadLocalRandom.current().nextInt(topicA.size() - 1)));
      }
      docs.add(builder.toString());
    }
    return docs;
  }
}