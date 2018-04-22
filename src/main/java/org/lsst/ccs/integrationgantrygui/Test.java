package org.lsst.ccs.integrationgantrygui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author tonyj
 */
public class Test {
    private static final Logger LOG = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws IOException {
        
        Path source = Paths.get("/home/tonyj/Data/arasmus/");
        Pattern pattern = Pattern.compile("BF._rng.*");    
        Path dest = Paths.get("/home/tonyj/Data/watch/");
        
        for (;;) {
            Stream<Path> find = Files.find(source,1, (Path t, BasicFileAttributes u) -> pattern.matcher(t.getFileName().toString()).matches());
            find.forEach((path) -> { 
                try {
                    Files.copy(path, dest.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING); 
                    Thread.sleep(10);
                } catch (IOException | InterruptedException ex) {
                    LOG.log(Level.SEVERE, "Error copying file", ex);
                } 
            });
        }
    }
}