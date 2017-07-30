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
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mohammad on 7/25/2017.
 */
@Service
public class TripleExtractor {

    private final Log logger = LogFactory.getLog(getClass());
    @Autowired
    private RuleBasedTripleExtractor extractor;

    public void writeTriplesToFiles(String folderPath) throws IOException {
        File folder = new File(folderPath);
        List<File> fileList = Arrays.asList(folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt"); // or something else
            }
        }));
        List<RawTripleExtractor> rawTripleExtractors = new ArrayList<RawTripleExtractor>();
        rawTripleExtractors.add(extractor);

        int index = 0;
        for (File file : fileList) {
            index++;
            List<RawTriple> allFileTriples = new ArrayList<RawTriple>();
            for (RawTripleExtractor rawTripleExtractor : rawTripleExtractors) {
                if (file.isFile()) {
                    List<String> lines = FileUtils.readLines(file, "UTF-8");

                    for (String line : lines) {
                        List<String> sentences = SentenceTokenizer.SentenceSplitterRaw(line);
                        for (String sentence : sentences) {

                            try {
                                List<RawTriple> triples = rawTripleExtractor.extract(null, null, sentence);
                                allFileTriples.addAll(triples);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }


                }

            }
            String outPutFolderPath = folderPath + "\\output\\";
            File outputFolder = new File(outPutFolderPath);
            Path filePath = Paths.get(outPutFolderPath, index + ".json");
            if (!outputFolder.exists()) {
                outputFolder.mkdir();
            }
            RawTripleExporter rawTripleExporter = new RawTripleExporter(filePath);
            if (allFileTriples.size() > 0)
                rawTripleExporter.writeTripleList(allFileTriples);
        }


    }
}
