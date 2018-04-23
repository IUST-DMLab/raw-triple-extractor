/*
 * Farsi Knowledge Graph Project
 *  Iran University of Science and Technology (Year 2018)
 *  Developed by Majid Asgari.
 */

package ir.ac.iust.dml.kg.raw.triple;

import ir.ac.iust.dml.kg.raw.services.unsupervised.UnsupervisedTripleExtractor;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TestExtractTriple_Unsupervised {
  private UnsupervisedTripleExtractor extractor = new UnsupervisedTripleExtractor();

  @Test
  public void testExtractTripleRuleBased() throws IOException {

    String inputPath = "inputText.txt";

    if (Files.notExists(Paths.get(inputPath)))
      Files.copy(TestExtractTriple_RuleBased.class.getResourceAsStream("/inputText.txt"), Paths.get(inputPath));

    List<String> lines = Files.readAllLines(Paths.get(inputPath), Charset.forName("UTF-8"));
    String text = lines.stream().map(Object::toString).collect(Collectors.joining("\n"));

    List<RawTriple> tripleList = extractor.extract(null, null, text);

    for (RawTriple triple : tripleList) System.out.println(triple.toString());
  }
}
