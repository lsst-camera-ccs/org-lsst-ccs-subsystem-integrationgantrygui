package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class for the integration gantry gui
 *
 * @author tonyj
 */
public class Main {

    private final Path watchDir = Paths.get("/home/tonyj/Data/watch");
    private final Map<String, Integer> cameraMap = new HashMap<>();

    Main() {
        cameraMap.put("BF2", 0);
        cameraMap.put("BF3" ,1);
        cameraMap.put("BF0" ,2);
        cameraMap.put("BF1" ,3);  
    }

    private void start() throws IOException, InterruptedException {
        ExecutorService workQueue = Executors.newFixedThreadPool(4);
        IntegrationGantryFrame frame = new IntegrationGantryFrame();

        java.awt.EventQueue.invokeLater(() -> {
            frame.setVisible(true);
        });
        
        try (WatchService watchService = watchDir.getFileSystem().newWatchService()) {
            watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            for (;;) {
                WatchKey take = watchService.take();
                take.pollEvents().stream().map((event) -> (Path) event.context()).forEach((path) -> {
                    System.out.println(path);
                    workQueue.execute(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                Integer index = cameraMap.get(path.getFileName().toString().substring(0,3));
                                if (index != null) {
                                    long start = System.currentTimeMillis();
                                    BufferedImage image = FitsFast.readFits(watchDir.resolve(path).toFile());
                                    long stop = System.currentTimeMillis();
                                    System.out.printf("Reading %s took %dms\n", path, stop - start);
                                    frame.setImage(index, image);
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                    });

                });
                take.reset();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Main main = new Main();
        main.start();
    }

}
