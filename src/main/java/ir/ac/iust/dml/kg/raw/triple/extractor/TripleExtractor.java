package ir.ac.iust.dml.kg.raw.triple.extractor;

import ir.ac.iust.dml.kg.raw.SentenceTokenizer;
import ir.ac.iust.dml.kg.raw.rulebased.RuleBasedTripleExtractor;
import ir.ac.iust.dml.kg.raw.triple.RawTriple;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExporter;
import ir.ac.iust.dml.kg.raw.triple.RawTripleExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mohammad on 7/25/2017.
 */
public class TripleExtractor {

    @Autowired
    private final Log logger = LogFactory.getLog(getClass());

    public void writeTriplesToFiles() throws IOException {
        File folder = new File("D:\\files");
        List<File> fileList = Arrays.asList(folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt"); // or something else
            }
        }));
        List<RawTripleExtractor> rawTripleExtractors = new ArrayList<RawTripleExtractor>();
        rawTripleExtractors.add(new RuleBasedTripleExtractor());

        for (RawTripleExtractor rawTripleExtractor : rawTripleExtractors) {
            for (File file : fileList) {
                if (file.isFile()) {
                    List<String> lines = FileUtils.readLines(file, "UTF-8");
                    List<RawTriple> allFileTriples = new ArrayList<RawTriple>();
                    for (String line : lines) {
                        List<String> sentences = SentenceTokenizer.SentenceSplitterRaw(line);
                        for (String sentence : sentences) {
                            if (sentence.length() > 20 && sentence.length() < 200) {
                                try {
                                    List<RawTriple> triples = rawTripleExtractor.extract(null, null, sentence);
                                    allFileTriples.addAll(triples);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    Path filePath = Paths.get(System.getProperty("user.home"), "test.json");
                    RawTripleExporter rawTripleExporter = new RawTripleExporter(filePath);
                    rawTripleExporter.writeTripleList(allFileTriples);


                }

            }
        }


    }
}
