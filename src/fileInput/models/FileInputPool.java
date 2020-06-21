package fileInput.models;

import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileInputPool {
    private BlockingQueue<FileInputResult> blockingQueue;
    private int sleepTime;
    private List<FileModel> executedFiles;
    private Semaphore executedFilesSemaphore;
    private ExecutorService fileInputPool;
    private List<FileModel> files;
    private List<String> activeThreads;
    private Semaphore activeThreadsSemaphore;
    public FileInputPool(LinkedBlockingQueue<FileInputResult> queue, int sleepTime, List<String> activeThreads) {
        this.sleepTime = sleepTime;
        blockingQueue = queue;
        executedFilesSemaphore = new Semaphore(1);
        executedFiles = new ArrayList<>();
        fileInputPool = Executors.newFixedThreadPool(2);
        files = new ArrayList<>();
        this.activeThreads = activeThreads;
        activeThreadsSemaphore = new Semaphore(1);
    }

    public List<FileModel> getExecutedFiles() {
        return executedFiles;
    }

    public Semaphore getExecutedFilesSemaphore() {
        return executedFilesSemaphore;
    }

    public void executeWorker(FileInputModel model) {
        model.getFileInputWorker().setWorkers(model.getCruncherWorkers());
        //fileInputPool.execute(model.getFileInputWorker());
    }
    public void executeQueuer(FileInputQueuer queuer) {
        try {
            activeThreadsSemaphore.acquire();
            while (activeThreads.contains(queuer.getName())) {
                ;
            }
            fileInputPool.execute(queuer);
            activeThreadsSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public List<String> getActiveThreads() {
        return activeThreads;
    }

    public void addActiveThread(String thread) {
        try {
            activeThreadsSemaphore.acquire();
            activeThreads.add(thread);
            activeThreadsSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void removeActiveThread(String thread) {
        try {
            activeThreadsSemaphore.acquire();
            activeThreads.remove(thread);
            activeThreadsSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
