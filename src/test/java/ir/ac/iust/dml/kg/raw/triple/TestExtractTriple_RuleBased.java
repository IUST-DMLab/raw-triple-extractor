package ir.ac.iust.dml.kg.raw.triple;

import ir.ac.iust.dml.kg.raw.rulebased.RuleBasedTripleExtractor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@ComponentScan("ir.ac.iust.dml.kg.raw.rulebased")
public class TestExtractTriple_RuleBased {
    @Autowired
    private RuleBasedTripleExtractor extractor;

    @Test
    public void testExtractTripleRuleBased() throws IOException {

        String inputPath = "inputText.txt";
        String rulesPath = "tripleRules.txt";

        if (Files.notExists(Paths.get(inputPath)))
            Files.copy(TestExtractTriple_RuleBased.class.getResourceAsStream("/inputText.txt"), Paths.get(inputPath));
        if (Files.notExists(Paths.get(rulesPath)))
            Files.copy(TestExtractTriple_RuleBased.class.getResourceAsStream("/tripleRules.txt"), Paths.get(rulesPath));

        List<String> lines = Files.readAllLines(Paths.get(inputPath), Charset.forName("UTF-8"));
        for (String line : lines) {
            System.out.println("سلام: " + line);
        }
        List<RawTriple> tripleList = new ArrayList<RawTriple>();

        for (String line : lines) {
            tripleList.addAll(extractor.extract(null, null, line));
        }

        System.out.println(tripleList.toString());
    }
}