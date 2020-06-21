package cruncher.models;

import fileInput.models.FileInputResult;
import javafx.concurrent.Task;

import java.util.*;
import java.util.concurrent.*;

public class CruncherPool {

    private LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue;
    private LinkedBlockingQueue<FileInputResult> manipulatorQueue;
    private List<CruncherWorker> cruncherWorkerList;
    private int cruncherLimit;
    private ExecutorService cruncherPool;
    public CruncherPool(LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue, int cruncherLimit) {
        this.cruncherWorkerList = new ArrayList<>();
        cruncherPool = Executors.newFixedThreadPool(10);
        this.outputQueue = outputQueue;
        this.cruncherLimit = cruncherLimit;
    }

    public void executeCruncher(CruncherWorker worker) {
        cruncherPool.execute(worker);
    }
    public void addCruncher(CruncherWorker worker) {
        cruncherWorkerList.add(worker);
    }

    public void removeCruncher(CruncherWorker worker) {
        cruncherWorkerList.remove(worker);
    }

    public List<CruncherWorker> getCruncherWorkerList() {
        return cruncherWorkerList;
    }

    public ExecutorService getCruncherPool() {
        return cruncherPool;
    }
}
