package ir.ac.iust.dml.kg.raw.triple;

import ir.ac.iust.dml.kg.raw.triple.extractor.TripleExtractor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RawTripleExtractorRunner implements CommandLineRunner {

    private final Log logger = LogFactory.getLog(getClass());
    @Autowired
    private TripleExtractor tripleExtractor;

    @Override
    public void run(String... args) throws Exception {
        logger.info("ApplicationStartupRunner run method Started !!");
        String folderPath = args[0];
        tripleExtractor.writeTriplesToFiles(folderPath);
        //2 dependecny extrat triple and parse tree extractor and next add third deendecy
    }
}
