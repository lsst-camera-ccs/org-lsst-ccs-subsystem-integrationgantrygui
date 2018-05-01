package org.lsst.ccs.integrationgantrygui;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import nom.tam.fits.TruncatedFileException;

/**
 * Main class for the integration gantry gui
 *
 * @author tonyj
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private final Path watchDir = Paths.get("/home/ccs/arasmus/mvIMPACT_acquire");
    private final Map<String, Integer> cameraMap = new HashMap<>();
    private final Map<String, Integer> cameraMap2 = new HashMap<>();
    private int i = 0;
    private int j = 0;
    private final AtomicInteger count = new AtomicInteger();

    Main() {
        cameraMap.put("BF2", 0);
        cameraMap.put("BF3", 1);
        cameraMap.put("BF0", 2);
        cameraMap.put("BF1", 3);

        cameraMap2.put("gaps_2", 0);
        cameraMap2.put("gaps_3", 1);
        cameraMap2.put("gaps_0", 2);
        cameraMap2.put("gaps_1", 3);
    }

    private void start() throws IOException, InterruptedException {
        ExecutorService workQueue = Executors.newCachedThreadPool();
        IntegrationGantryFrame frame = new IntegrationGantryFrame();

        java.awt.EventQueue.invokeLater(() -> {
            frame.setVisible(true);
        });

        LinkedBlockingQueue[] queues = new LinkedBlockingQueue[4];
        for (int i = 0; i < queues.length; i++) {
            LinkedBlockingQueue<Path> queue = new LinkedBlockingQueue<>(1);
            int index = i;
            queues[i] = queue;
            Runnable runnable = () -> {
                for (;;) {
                    try {
                        Path path = queue.take();
                        ScalableImageProvider image = FitsFast.readFits(path.toFile());
                        frame.setImage(index, image);
                        count.getAndIncrement();
                    } catch (InterruptedException | IOException | TruncatedFileException | BufferUnderflowException ex) {
                        LOG.log(Level.SEVERE, "Exception in animation thread", ex);
                    }
                }
            };
            workQueue.execute(runnable);
        }

        @SuppressWarnings("SleepWhileInLoop")
        Runnable runnable = () -> {
            for (;;) {
                try {
                    System.out.printf("Frame rate %dfps\n", count.getAndSet(0));
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, "Exception in timer thread", ex);
                }
            }
        };
        workQueue.execute(runnable);

        try (WatchService watchService = watchDir.getFileSystem().newWatchService()) {
            watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            for (;;) {
                WatchKey take = watchService.take();
                take.pollEvents().stream().map((event) -> (Path) event.context()).forEach((path) -> {
                    Path fullPath = watchDir.resolve(path);
                    String fileName = fullPath.getFileName().toString();
                    Integer index = cameraMap.get(fullPath.getFileName().toString().substring(0, 3));
                    if (index == null) {
                        index = cameraMap2.get(fullPath.getFileName().toString().substring(0, 6));
                    }
                    if (index != null) {
                        if (fileName.endsWith(".fits")) {
                            if (fullPath.toFile().length() == 5_071_680 || fullPath.toFile().length() == 10_137_600) {

                                LinkedBlockingQueue<Path> queue = queues[index];
                                Path poll = queue.poll(); // Discard any files not yet processed
                                if (poll == null) {
                                    j++;
                                }
                                queue.add(watchDir.resolve(path));
                                i++;
                                if (i % 100 == 0) {
                                    System.out.printf("%d%% of files were processed\n", j);
                                    j = 0;
                                }
                            }
                        } else if (fileName.endsWith(".txt")) {
                            try {
                                List<String> text = Files.readAllLines(fullPath);
                                if (!text.isEmpty()) {
                                    int findex = index;
                                    SwingUtilities.invokeLater(() -> {
                                        frame.setLabel(findex, text.get(0));
                                    });
                                }
                            } catch (IOException ex) {
                                LOG.log(Level.SEVERE, "Error reading text file", ex);
                            }
                        }
                    }
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
