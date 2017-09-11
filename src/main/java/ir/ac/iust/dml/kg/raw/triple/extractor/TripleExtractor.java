package ir.ac.iust.dml.kg.raw.triple.extractor;

import ir.ac.iust.dml.kg.raw.SentenceTokenizer;
import ir.ac.iust.dml.kg.raw.extractor.EnhancedEntityExtractor;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityToken;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExtractor;
import ir.ac.iust.dml.kg.raw.utils.PathWalker;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mohammad on 7/25/2017.
 */
@Service
public class TripleExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentenceTokenizer.class);
  private final List<RawTripleExtractor> extractors;

  @Autowired
  public TripleExtractor(List<RawTripleExtractor> extractors) {
    this.extractors = extractors;
  }

  @SuppressWarnings("unused")
  public enum InputType {
    Raw, Repository
  }

  public void writeTriplesToFiles(String folderPath, InputType inputType) throws IOException {
//    extractors.remove(1);
    final Path baseFolder = Paths.get(folderPath);
    final List<Path> fileList = PathWalker.INSTANCE.getPath(baseFolder,
        inputType == InputType.Raw ? null : ".*\\.json");
    final Path outputFolder = baseFolder.resolve("output");
    if (!Files.exists(outputFolder)) Files.createDirectories(outputFolder);

    int numberOfSentences = 0;
    final Map<Class, Integer> numberOfExtractedTriples = new HashMap<>();
    final Long extractionStart = System.currentTimeMillis();
    for (Path file : fileList) {
      List<RawTriple> allFileTriples = new ArrayList<>();
      Object input;
      if (inputType == InputType.Raw) {
        final String texts = FileUtils.readFileToString(file.toFile(), "UTF-8");
        //String outputText = rfinder.getAnnotationTextAfterCoref(fileRawText);
        final List<String> sentences = SentenceTokenizer.SentenceSplitterRaw(texts);
        numberOfSentences += sentences.size();
        input = texts;
      } else {
        final List<List<ResolvedEntityToken>> tokens = EnhancedEntityExtractor.importFromFile(file);
        assert tokens != null;
        numberOfSentences += tokens.size();
        input = tokens;
      }

      if (numberOfSentences % 100 == 0)
        LOGGER.warn(String.format("%6d sentences has been processed.", numberOfSentences));

      for (RawTripleExtractor rawTripleExtractor : extractors) {
        try {
          final List<RawTriple> triples;
          if (input instanceof String)
            triples = rawTripleExtractor.extract(null, null, (String) input);
          else
            //noinspection unchecked
            triples = rawTripleExtractor.extract(null, null, (List<List<ResolvedEntityToken>>) input);
          if (!triples.isEmpty()) {
            final Integer oldValue = numberOfExtractedTriples.get(rawTripleExtractor.getClass());
            final int newValue = (oldValue == null ? 0 : oldValue) + triples.size();
            numberOfExtractedTriples.put(rawTripleExtractor.getClass(), newValue);
          }
          allFileTriples.addAll(triples);
        } catch (Exception e) {
//          LOGGER.error("error in extracting triples from " + file.toAbsolutePath(), e);
        }
      }
      if (!allFileTriples.isEmpty())
        ExtractorUtils.writeTriples(outputFolder.resolve(file.getFileName() + ".json"), allFileTriples);
    }
    ExtractorUtils.markExtraction(outputFolder, extractionStart);
  }
}
