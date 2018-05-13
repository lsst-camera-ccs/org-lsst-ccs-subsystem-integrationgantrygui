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
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import nom.tam.fits.TruncatedFileException;
import javax.script.ScriptException;

/**
 * Main class for the integration gantry gui
 *
 * @author tonyj
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private final Path watchDir = Paths.get(System.getProperty("watchDir", "/mnt/ramdisk"));
    private final Pattern watchPattern = Pattern.compile(System.getProperty("watchPattern", ".+(\\d)_rng\\d+.*"));
    private final Pattern textPattern = Pattern.compile("((horiz)?\\s*((\\d|\\.)*)?\\s*((\\d|\\.)*)?.*)\\s+\\|\\s+((vert)?\\s*((\\d|\\.)*)?\\s*((\\d|\\.)*)?.*)");

    private final Map<Integer, Integer> cameraMap = new HashMap<>();
    private int totalFiles = 0;
    private int processFiles = 0;
    private final AtomicInteger count = new AtomicInteger();
    private JSONParser parser;
    private IntegrationGantryFrame frame;
    private LinkedBlockingQueue[] queues;

    Main() {
        cameraMap.put(0, 2);
        cameraMap.put(1, 3);
        cameraMap.put(2, 0);
        cameraMap.put(3, 1);
    }

    private void start() throws IOException, InterruptedException {
        ExecutorService workQueue = Executors.newCachedThreadPool();
        frame = new IntegrationGantryFrame();

        java.awt.EventQueue.invokeLater(() -> {
            frame.setVisible(true);
        });

        queues = new LinkedBlockingQueue[4];
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
                    frame.setFPS(count.getAndSet(0));
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, "Exception in timer thread", ex);
                }
            }
        };
        workQueue.execute(runnable);
        parser = new JSONParser();

        Stream<Path> pathList = Files.list(watchDir);
        pathList.forEach((path) -> {
            Path fullPath = watchDir.resolve(path);
            String fileName = fullPath.getFileName().toString();
            Matcher matcher = watchPattern.matcher(fileName);
            if (matcher.matches()) {
                // TODO: Only handle most recent file for each index
                int index = Integer.parseInt(matcher.group(1));
                int mappedIndex = cameraMap.get(index);
                if (fileName.endsWith(".fits")) {
                    handleFitsFile(fullPath, mappedIndex, path);
                } else if (fileName.endsWith(".txt")) {
                    handleTextFile(fullPath, mappedIndex);
                }
            } else if (fileName.endsWith(".json")) {
                handleJSONFile(fileName, fullPath);
            }
        });

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
                        int mappedIndex = cameraMap.get(index);
                        if (fileName.endsWith(".fits")) {
                            handleFitsFile(fullPath, mappedIndex, path);
                        } else if (fileName.endsWith(".txt")) {
                            handleTextFile(fullPath, mappedIndex);
                        }
                    } else if (fileName.endsWith(".json")) {
                        handleJSONFile(fileName, fullPath);
                    }
                });
                take.reset();
            }
        }
    }

    private void handleFitsFile(Path fullPath, int index, Path path) {
        if (fullPath.toFile().length() == 5_071_680 || fullPath.toFile().length() == 10_137_600) {

            LinkedBlockingQueue<Path> queue = queues[index];
            Path poll = queue.poll(); // Discard any files not yet processed
            if (poll == null) {
                processFiles++;
            }
            queue.add(watchDir.resolve(path));
            totalFiles++;
            if (totalFiles % 100 == 0) {
                LOG.log(Level.FINE, "{0}% of files were processed\n", processFiles);
                processFiles = 0;
            }
        }
    }

    private void handleTextFile(Path fullPath, int index) {
        try {
            List<String> text = Files.readAllLines(fullPath);
            Matcher matcher = textPattern.matcher(text.get(0));
            if (matcher.matches()) {
                int findex = index;
                double h1 = parseDouble(matcher.group(3));
                double h2 = parseDouble(matcher.group(5));
                double v1 = parseDouble(matcher.group(9));
                double v2 = parseDouble(matcher.group(11));
                // Parse the string...
                SwingUtilities.invokeLater(() -> {
                    frame.setLabel(findex, h1, h2, v1, v2);
                });
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error reading text file", ex);
        }
    }

    private void handleJSONFile(String fileName, Path fullPath) {
        boolean isHorizontal = fileName.contains("horiz");
        try {
            Map<String, List<Integer>> rois = parser.parseROI(Files.readAllLines(fullPath));
            rois.forEach((t, u) -> {
                int index = Integer.parseInt(t);
                int mappedIndex = cameraMap.get(index);
                SwingUtilities.invokeLater(() -> {
                    frame.setROI(isHorizontal, mappedIndex, u);
                });
            });
        } catch (IOException | ScriptException ex) {
            LOG.log(Level.SEVERE, "Error reading json file", ex);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Main main = new Main();
        main.start();
    }

    private double parseDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException x) {
            return Double.NaN;
        }
    }
}
