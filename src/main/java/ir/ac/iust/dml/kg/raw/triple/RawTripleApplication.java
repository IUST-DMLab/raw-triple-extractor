package ir.ac.iust.dml.kg.raw.triple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ir.ac.iust.dml.kg.raw")
public class RawTripleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RawTripleApplication.class, args);
    }
}
