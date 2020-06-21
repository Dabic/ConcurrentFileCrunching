package fileInput.models;

import cruncher.models.CruncherWorker;
import javafx.concurrent.Task;

import java.util.concurrent.LinkedBlockingQueue;

public class FileInputQueuer extends Task<String> {
    private FileInputResult fileInputResult;
    private LinkedBlockingQueue<FileInputResult> outputQueue;
    private String name;
    private FileInputPool fileInputPool;
    public FileInputQueuer(FileInputPool fileInputPool, String name, FileInputResult fileInputResult) {
        this.fileInputResult = fileInputResult;
        this.outputQueue = outputQueue;
        this.name = name;
        this.fileInputPool = fileInputPool;
    }

    @Override
    protected String call() throws Exception {
        fileInputPool.addActiveThread(name);
        for (CruncherWorker worker : fileInputResult.getWorkers()) {
            worker.getInputQueue().add(fileInputResult);
        }
        fileInputPool.removeActiveThread(name);
        return null;
    }

    public String getName() {
        return name;
    }
}
