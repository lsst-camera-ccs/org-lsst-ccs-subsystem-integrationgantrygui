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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import nom.tam.fits.TruncatedFileException;

/**
 * Main class for the integration gantry gui
 *
 * @author tonyj
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private final Path watchDir = Paths.get(System.getProperty("watchDir","/mnt/ramdisk"));
    private final Pattern watchPattern = Pattern.compile(System.getProperty("watchPattern",".+(\\d)_rng\\d+.*"));
    private final Map<Integer, Integer> cameraMap = new HashMap<>();
    private int i = 0;
    private int j = 0;
    private final AtomicInteger count = new AtomicInteger();

    Main() {
        cameraMap.put(2, 0);
        cameraMap.put(3, 1);
        cameraMap.put(0, 2);
        cameraMap.put(1, 3);
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
                    LOG.log(Level.INFO, "Frame rate {0} fps", count.getAndSet(0));
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
                    Matcher matcher = watchPattern.matcher(fileName);
                    if (matcher.matches()) {
                        int index = Integer.parseInt(matcher.group(1));
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
                                    LOG.log(Level.FINE,"{0}% of files were processed\n", j);
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
