package org.lsst.ccs.integrationgantrygui;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import javax.script.ScriptException;
import org.lsst.ccs.bus.data.KeyValueData;
import org.lsst.ccs.bus.data.KeyValueDataList;
import org.lsst.ccs.utilities.taitime.CCSTimeStamp;

/**
 * Main class for the integration gantry gui
 *
 * @author tonyj
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    static final int NCAMERAS = 4;
    private final Path watchDir = Paths.get(System.getProperty("watchDir", "/mnt/ramdisk"));
    private final Pattern watchPattern = Pattern.compile(System.getProperty("watchPattern", "[a-z|A-Z|_]+(\\d)_rng\\d+.*"));
    private final Pattern textPattern = Pattern.compile("((horiz)?\\s*((\\d|\\.)*)?\\s*((\\d|\\.)*)?.*)\\s+\\|\\s+((vert)?\\s*((\\d|\\.)*)?\\s*((\\d|\\.)*)?.*)");
    private final Map<Integer, Integer> cameraMap = new HashMap<>();
    private int totalFiles = 0;
    private int processFiles = 0;
    private final AtomicInteger count = new AtomicInteger();
    private JSONParser parser;
    private IntegrationGantryFrame frame;
    private LinkedBlockingQueue[] queues;
    private final Map<Integer, KeyValueDataList> trendingMap = new ConcurrentHashMap<>();
    private List<Number>[][] rois = new List[2][NCAMERAS];

    Main() {
        cameraMap.put(0, 2);
        cameraMap.put(1, 3);
        cameraMap.put(2, 0);
        cameraMap.put(3, 1);
    }

    /**
     *
     * @param startGUI If <code>false</code>does not start the GUI (used when we
     * just want to run as a headless subsystem).
     * @throws IOException
     */
    void start(boolean startGUI) throws IOException {
        ExecutorService workQueue = Executors.newCachedThreadPool();

        if (startGUI) {
            frame = new IntegrationGantryFrame();

            java.awt.EventQueue.invokeLater(() -> {
                frame.setVisible(true);
            });

            queues = new LinkedBlockingQueue[NCAMERAS];
            for (int i = 0; i < queues.length; i++) {
                LinkedBlockingQueue<Path> queue = new LinkedBlockingQueue<>(1);
                int index = i;
                queues[i] = queue;
                Runnable runnable = () -> {
                    for (;;) {
                        try {
                            Path path = queue.take();
                            Timed.execute(() -> {
                                ScalableImageProvider image = FitsFast.readFits(path.toFile());
                                frame.setImage(index, image);
                                count.getAndIncrement();
                                return null;
                            }, "Processing image took %dms");

                        } catch (InterruptedException | BufferUnderflowException ex) {
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
        }
        parser = new JSONParser();

        Stream<Path> pathList = Files.list(watchDir);
        Path[] fitsFiles = new Path[NCAMERAS];
        Path[] textFiles = new Path[NCAMERAS];
        Path[] jsonFiles = new Path[2];
        pathList.forEach((path) -> {
            Path fullPath = watchDir.resolve(path);
            String fileName = fullPath.getFileName().toString();
            Matcher matcher = watchPattern.matcher(fileName);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                int mappedIndex = cameraMap.get(index);
                if (fileName.endsWith(".fits")) {
                    if (fitsFiles[mappedIndex] == null || fullPath.toFile().lastModified() > fitsFiles[mappedIndex].toFile().lastModified()) {
                        fitsFiles[mappedIndex] = fullPath;
                    }
                } else if (fileName.endsWith(".txt")) {
                    if (textFiles[mappedIndex] == null || fullPath.toFile().lastModified() > textFiles[mappedIndex].toFile().lastModified()) {
                        textFiles[mappedIndex] = fullPath;
                    }
                }
            } else if (fileName.endsWith(".json")) {
                boolean isHorizontal = fileName.contains("horiz");
                int mappedIndex = isHorizontal ? 0 : 1;
                if (jsonFiles[mappedIndex] == null || fullPath.toFile().lastModified() > jsonFiles[mappedIndex].toFile().lastModified()) {
                    jsonFiles[mappedIndex] = fullPath;
                }
            }
        });
        if (startGUI) {
            for (int i = 0; i < NCAMERAS; i++) {
                Path fullPath = fitsFiles[i];
                if (fullPath != null) {
                    handleFitsFile(fullPath, i);
                }
            }
        }

        for (int i = 0; i < 2; i++) {
            Path fullPath = jsonFiles[i];
            if (fullPath != null) {
                handleJSONFile(fullPath.getFileName().toString(), fullPath);
            }
        }

        for (int i = 0; i < NCAMERAS; i++) {
            Path fullPath = textFiles[i];
            if (fullPath != null) {
                handleTextFile(fullPath, i);
            }
        }

        Runnable fileWatcher = () -> {
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
                            if (fileName.endsWith(".fits") && startGUI) {
                                handleFitsFile(fullPath, mappedIndex);
                            } else if (fileName.endsWith(".txt")) {
                                handleTextFile(fullPath, mappedIndex);
                            }
                        } else if (fileName.endsWith(".json")) {
                            handleJSONFile(fileName, fullPath);
                        }
                    });
                    take.reset();
                }
            } catch (IOException | InterruptedException x) {
                throw new RuntimeException("Error in watch loop", x);
            }
        };
        workQueue.submit(fileWatcher);
    }

    private void handleFitsFile(Path fullPath, int index) {
        if (fullPath.toFile().length() == 5_071_680 || fullPath.toFile().length() == 10_137_600) {

            LinkedBlockingQueue<Path> queue = queues[index];
            Path poll = queue.poll(); // Discard any files not yet processed
            if (poll == null) {
                processFiles++;
            }
            queue.add(fullPath);
            totalFiles++;
            if (totalFiles % 100 == 0) {
                LOG.log(Level.FINE, "{0}% of files were processed\n", processFiles);
                processFiles = 0;
            }
        }
    }

    private void handleTextFile(Path fullPath, int index) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(fullPath);
            List<String> text = Files.readAllLines(fullPath);
            if (!text.isEmpty()) {
                Matcher matcher = textPattern.matcher(text.get(0));
                if (matcher.matches()) {
                    int findex = index;
                    double h1 = parseDouble(matcher.group(3));
                    double h2 = parseDouble(matcher.group(5));
                    double v1 = parseDouble(matcher.group(9));
                    double v2 = parseDouble(matcher.group(11));
                    // store the data for trending
                    storeTrendingData(index, lastModifiedTime, h1, h2, v1, v2);
                    // Uodate gui
                    if (frame != null) {
                        SwingUtilities.invokeLater(() -> {
                            frame.setLabel(findex, h1, h2, v1, v2);
                        });
                    }
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error reading text file", ex);
        }
    }

    private void handleJSONFile(String fileName, Path fullPath) {
        boolean isHorizontal = fileName.contains("horiz");
        try {
            Map<String, List<Number>> rois = parser.parseROI(Files.readAllLines(fullPath));
            rois.forEach((t, u) -> {
                int index = Integer.parseInt(t);
                int mappedIndex = cameraMap.get(index);
                storeROI(isHorizontal, index, u);
                if (frame != null) {
                    SwingUtilities.invokeLater(() -> {
                        frame.setROI(isHorizontal, mappedIndex, u);
                    });
                }
            });
        } catch (IOException | ScriptException ex) {
            LOG.log(Level.SEVERE, "Error reading json file", ex);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Main main = new Main();
        main.start(true);
    }

    private double parseDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException x) {
            return Double.NaN;
        }
    }

    private void storeROI(boolean isHorizontal, int index, List<Number> roi) {
        LOG.log(Level.FINE, "Setting {0} roi index {1} to {2}", new Object[]{isHorizontal, index, roi});
        rois[isHorizontal ? 0 : 1][index] = roi;
    }

    private void storeTrendingData(int index, FileTime lastModifiedTime, double h1, double h2, double v1, double v2) {
        LOG.log(Level.FINE, "storeTrendingData index {0} date {1} to {2},{3},{4},{5}", new Object[]{index, lastModifiedTime, h1, h2, v1, v2});
        KeyValueDataList dl = new KeyValueDataList(CCSTimeStamp.currentTimeFromMillis(lastModifiedTime.toMillis()));
        List<Number> horizontalROI = rois[0][index];
        List<Number> verticalROI = rois[1][index];
        if (horizontalROI != null && verticalROI != null) {
            String prefix = "Camera" + (index+1) + "/";
            dl.addData(prefix + "h1", h1 + horizontalROI.get(1).doubleValue());
            dl.addData(prefix + "h2", h2 + horizontalROI.get(1).doubleValue());
            dl.addData(prefix + "hGap", 25 * (h2 - h1));
            dl.addData(prefix + "v1", v1 + verticalROI.get(0).doubleValue());
            dl.addData(prefix + "v2", v2 + verticalROI.get(0).doubleValue());
            dl.addData(prefix + "vGap", 25 * (v2 - v1));
            trendingMap.put(index, dl);
        }
    }

    KeyValueData getTrendingForCamera(int index) {
        return trendingMap.get(index);
    }
}
