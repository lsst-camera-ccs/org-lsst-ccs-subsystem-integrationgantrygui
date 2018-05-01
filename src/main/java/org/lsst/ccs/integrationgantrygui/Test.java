package org.lsst.ccs.integrationgantrygui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private static final DateFormat df = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss");

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) throws IOException {
        
        Path source = Paths.get(System.getProperty("watchDir","/home/tonyj/Data/arasmus/"));
        Pattern watchPattern = Pattern.compile(System.getProperty("watchPattern",".+(\\d)_rng\\d+.*"));    
        Path dest = Paths.get(System.getProperty("watchDir","/mnt/ramdisk"));
        
        for (;;) {
            Stream<Path> find = Files.find(source,1, (Path t, BasicFileAttributes u) -> watchPattern.matcher(t.getFileName().toString()).matches());
            find.forEach((path) -> { 
                try {
                    Files.copy(path, dest.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING); 
                    Thread.sleep(10);
                } catch (IOException | InterruptedException ex) {
                    LOG.log(Level.SEVERE, "Error copying file", ex);
                } 
            });
            // Write the .txt files to simulate Seth's data analysis
            String date = df.format(new Date());
            List<String> lines = Collections.singletonList(date);
            for (int i=0; i<4; i++) {
                Path path = dest.resolve(Paths.get(String.format("xxx%d_rng0000.txt",i)));
                Files.deleteIfExists(path);
                Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
    }
}