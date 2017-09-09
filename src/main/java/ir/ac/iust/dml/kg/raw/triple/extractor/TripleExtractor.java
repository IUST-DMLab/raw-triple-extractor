package ir.ac.iust.dml.kg.raw.triple.extractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.ac.iust.dml.kg.raw.SentenceTokenizer;
import ir.ac.iust.dml.kg.raw.coreference.ReferenceFinder;
import ir.ac.iust.dml.kg.raw.extractor.EnhancedEntityExtractor;
import ir.ac.iust.dml.kg.raw.extractor.ResolvedEntityToken;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExporter;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by mohammad on 7/25/2017.
 */
@Service
public class TripleExtractor {

    private final Log logger = LogFactory.getLog(getClass());
    @Autowired
    private List<RawTripleExtractor> extractors;
    ReferenceFinder rfinder = new ReferenceFinder();

    public void writeTriplesToFiles(String folderPath) throws IOException {
        extractors.remove(1);
        File folder = new File(folderPath);
        List<File> fileList = Arrays.asList(folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv"); // or something else
            }
        }));
        String outPutFolderPath = folderPath + "\\output\\";
        File outputFolder = new File(outPutFolderPath);

        if (outputFolder.exists()) {
            FileUtils.cleanDirectory(outputFolder);
            FileUtils.forceDelete(outputFolder);
        }
        FileUtils.forceMkdir(outputFolder);

        int numberOfSentences = 0;
        final Map<Class, Integer> numberOfExtractedTriples = new HashMap<>();
        Long extractionStart = System.currentTimeMillis();
        for (File file : fileList) {
            List<RawTriple> allFileTriples = new ArrayList<RawTriple>();

            if (file.isFile()) {
                // List<String> lines = FileUtils.readLines(file, "UTF-8");
                String fileRawText = FileUtils.readFileToString(file, "UTF-8");
                //String outputText = rfinder.getAnnotationTextAfterCoref(fileRawText);

                // for (String line : lines) {
                List<String> sentences = SentenceTokenizer.SentenceSplitterRaw(fileRawText);
                //  for (String sentence : sentences) {
                numberOfSentences++;
                if (numberOfSentences % 100 == 0)
                    logger.warn(String.format("%6d sentences has been processed.", numberOfSentences));

                for (RawTripleExtractor rawTripleExtractor : extractors) {
                    try {
                        //   List<RawTriple> triples = rawTripleExtractor.extract(null, null, sentence);
                        List<List<ResolvedEntityToken>> tokens = EnhancedEntityExtractor.importFromFile(file.toPath());
                        List<RawTriple> triples = rawTripleExtractor.extract(null, null, tokens);
                        if (!triples.isEmpty()) {
                            final Integer oldValue = numberOfExtractedTriples.get(rawTripleExtractor.getClass());
                            final int newValue = (oldValue == null ? 0 : oldValue) + triples.size();
                            numberOfExtractedTriples.put(rawTripleExtractor.getClass(), newValue);
                /*logger.warn(String.format("%28s has extracted %4d (total %4d) triples from %s",
                        rawTripleExtractor.getClass().getSimpleName(), triples.size(), newValue, sentence));*/
                        }
                        allFileTriples.addAll(triples);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
                // }
                //}
                Path filePath = Paths.get(outPutFolderPath, FilenameUtils.getBaseName(file.getName()) + ".json");


                if (allFileTriples.size() > 0) {
                    RawTripleExporter rawTripleExporter = new RawTripleExporter(filePath);
                    rawTripleExporter.writeTripleList(allFileTriples);
                }

            }


        }
        Long extractionEnd = System.currentTimeMillis();
        Path filePath = Paths.get(outPutFolderPath, "epoch.json");
        Info info = new Info();
        info.setExtractionStart(extractionStart.toString());
        info.setExtractionEnd(extractionEnd.toString());
        info.setModule("raw_dependency_pattern");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileUtils.write(filePath.toFile(), gson.toJson(info), "UTF-8", false);

    }
}
