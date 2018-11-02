/*
 * Farsi Knowledge Graph Project
 *  Iran University of Science and Technology (Year 2017)
 *  Developed by Mohammad Abdous.
 */

package ir.ac.iust.dml.kg.raw.triple.extractor;

import ir.ac.iust.dml.kg.raw.DependencyParser;
import ir.ac.iust.dml.kg.raw.Normalizer;
import ir.ac.iust.dml.kg.raw.SentenceBranch;
import ir.ac.iust.dml.kg.raw.SentenceTokenizer;
import ir.ac.iust.dml.kg.raw.extractor.EnhancedEntityExtractor;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityToken;
import ir.ac.iust.dml.kg.raw.services.tree.ParsingLogic;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExtractor;
import ir.ac.iust.dml.kg.raw.utils.PathWalker;
import ir.ac.iust.dml.kg.raw.utils.URIs;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

  public void writeTriplesToFiles(String folderPath, InputType inputType, String className) throws IOException {
    if (className != null) {
      List<RawTripleExtractor> toRemove = new ArrayList<>();
      for (RawTripleExtractor extractor : extractors) {
        if (!extractor.getClass().getSimpleName().toLowerCase().equals(className.toLowerCase()))
          toRemove.add(extractor);
      }
      extractors.removeAll(toRemove);
    }

    final Path baseFolder = Paths.get(folderPath);
    final List<Path> fileList = PathWalker.INSTANCE.getPath(baseFolder,
            inputType == InputType.Raw ? null : ".*\\.json");
    final Path outputFolder = baseFolder.resolve("output");
    if (!Files.exists(outputFolder)) Files.createDirectories(outputFolder);

    int numberOfSentences = 0;
    final Map<Class, Integer> numberOfExtractedTriples = new HashMap<>();
    final Map<Class, Integer> numberOfExtractedTriplesWithAutoIri = new HashMap<>();
    final Map<Class, Integer> numberOfExtractedTriplesAbove70 = new HashMap<>();
    final Map<Class, Integer> numberOfExtractedTriplesAbove80 = new HashMap<>();
    final Map<Class, Integer> numberOfExtractedTriplesAbove90 = new HashMap<>();
    final long extractionStart = System.currentTimeMillis();
    int lastLogNumberOfSentences = 0;
    for (Path file : fileList) {
      LOGGER.info("opening file " + file.toAbsolutePath().toString());
      List<RawTriple> allFileTriples = new ArrayList<>();
      List<RawTriple> allFileTriplesAbove80 = new ArrayList<>();
      Object input;
      try {
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
      } catch (Throwable th) {
        LOGGER.error("error in file reading " + file.toAbsolutePath());
        continue;
      }

      if (numberOfSentences - lastLogNumberOfSentences > 100) {
        showStats(numberOfSentences, numberOfExtractedTriples, numberOfExtractedTriplesWithAutoIri,
                numberOfExtractedTriplesAbove90, numberOfExtractedTriplesAbove80, numberOfExtractedTriplesAbove70,
                extractionStart);
        lastLogNumberOfSentences = numberOfSentences;
      }

      for (RawTripleExtractor rawTripleExtractor : extractors) {
        try {
          final List<RawTriple> triples;
          if (input instanceof String)
            triples = rawTripleExtractor.extract(null, null,
                    SentenceBranch.summarize(Normalizer.removeBrackets((String) input)));
          else {
            //noinspection unchecked
            final List<List<ResolvedEntityToken>> sentences = (List<List<ResolvedEntityToken>>) input;
            final List<List<ResolvedEntityToken>> copy = ResolvedEntityToken.copySentences(sentences);
            if (rawTripleExtractor instanceof ParsingLogic) DependencyParser.addDependencyParseSentences(copy, false);
            triples = rawTripleExtractor.extract(null, null, copy);
          }
          if (!triples.isEmpty()) {

            for(RawTriple t : triples) {
              if(t.getSubject().contains(":")) t.setSubject(URIs.INSTANCE.prefixedToUri(t.getSubject()));
              if(t.getPredicate().contains(":")) t.setPredicate(URIs.INSTANCE.prefixedToUri(t.getPredicate()));
              if(t.getObject().contains(":")) t.setObject(URIs.INSTANCE.prefixedToUri(t.getObject()));
            }

            final Class c = rawTripleExtractor.getClass();
            increaseStats(numberOfExtractedTriples, c, triples.size());

            for (RawTriple triple : triples) {
              if (triple.getAccuracy() > 0.7) increaseStats(numberOfExtractedTriplesAbove70, c, 1);
              if (triple.getAccuracy() > 0.8) {
                increaseStats(numberOfExtractedTriplesAbove80, c, 1);
                // Filter distant supervision results < 0.9
                if (!(triple.getModule().equals("DistantSupervision") && triple.getAccuracy() < 0.9))
                  allFileTriplesAbove80.add(triple);
              }
              if (triple.getAccuracy() > 0.9) increaseStats(numberOfExtractedTriplesAbove90, c, 1);
              if (triple.getSubject().contains("/auto/") || triple.getObject().contains("/auto/"))
                increaseStats(numberOfExtractedTriplesWithAutoIri, c, 1);
            }

            allFileTriples.addAll(triples);
          }
        } catch (Exception e) {
          LOGGER.trace("error in extracting triples from " + file.toAbsolutePath(), e);
        }
      }
      Collections.sort(allFileTriples);
      Collections.reverse(allFileTriples);
      if (allFileTriples.isEmpty())
        LOGGER.info("no triples extracted from file " + file.toAbsolutePath().toString());
      else
        LOGGER.info("extracted " + allFileTriples.size() + " triples and " + allFileTriplesAbove80.size() +
                " confident triples from file " + file.toAbsolutePath().toString());
      if (!allFileTriples.isEmpty())
        ExtractorUtils.writeTriples(outputFolder.resolve(file.getFileName() + ".json"), allFileTriples);
      if (!allFileTriplesAbove80.isEmpty())
        ExtractorUtils.writeTriples(outputFolder.resolve("above_80_" + file.getFileName() + ".json"),
                allFileTriplesAbove80);
    }
    ExtractorUtils.markExtraction(outputFolder, extractionStart);
    showStats(numberOfSentences, numberOfExtractedTriples, numberOfExtractedTriplesWithAutoIri,
            numberOfExtractedTriplesAbove90, numberOfExtractedTriplesAbove80, numberOfExtractedTriplesAbove70,
            extractionStart);
    System.exit(0);
  }

  private void increaseStats(Map<Class, Integer> numberOfExtractedTriples, Class klass, int value) {
    final Integer oldValue = numberOfExtractedTriples.get(klass);
    final int newValue = (oldValue == null ? 0 : oldValue) + value;
    numberOfExtractedTriples.put(klass, newValue);
  }

  private void showStats(int numberOfSentences,
                         Map<Class, Integer> numberOfExtractedTriples,
                         Map<Class, Integer> numberOfExtractedTriplesWithAutoIri,
                         Map<Class, Integer> numberOfExtractedTriplesAbove90,
                         Map<Class, Integer> numberOfExtractedTriplesAbove80,
                         Map<Class, Integer> numberOfExtractedTriplesAbove70,
                         long startTime) {
    LOGGER.warn(String.format("%6d sentences has been processed in %d mili-seconds",
            numberOfSentences, (System.currentTimeMillis() - startTime)));
    numberOfExtractedTriples.forEach((key, value) ->
            LOGGER.warn(String.format(
                    "Number of extracted triples from %s is %d (above 90: %d, above 80: %d, above 70: %d)" +
                            " with %d auto IRIs.",
                    key.getSimpleName(), value,
                    numberOfExtractedTriplesAbove90.get(key),
                    numberOfExtractedTriplesAbove80.get(key),
                    numberOfExtractedTriplesAbove70.get(key),
                    numberOfExtractedTriplesWithAutoIri.get(key))));
  }
}