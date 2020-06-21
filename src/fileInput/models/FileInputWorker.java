package fileInput.models;

import cruncher.models.CruncherWorker;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class FileInputWorker extends Task<String> {

    private List<File> files;
    private LinkedBlockingQueue<FileInputResult> queue;
    private List<FileModel> executedFiles;
    private Semaphore executedFilesSemaphore;
    private int sleepTime;
    private List<CruncherWorker> workers;
    private boolean poisoned = false;
    private Semaphore filesSemaphore;
    private LinkedBlockingQueue<File> inputQueue;
    private String name;
    private Semaphore sleepSemaphore;
    private FileInputPool pool;
    public FileInputWorker(FileInputPool pool, String name, LinkedBlockingQueue<File> inputQueue, List<File> files, LinkedBlockingQueue<FileInputResult> queue, List<FileModel> executedFiles, Semaphore executedFilesSemaphore, int sleepTime) {
        this.files = files;
        this.executedFiles = executedFiles;
        this.queue = queue;
        this.sleepTime = sleepTime;
        this.executedFilesSemaphore = executedFilesSemaphore;
        this.workers = new ArrayList<>();
        this.inputQueue = inputQueue;
        filesSemaphore = new Semaphore(1);
        this.name = name;
        sleepSemaphore = new Semaphore(1);
        this.pool = pool;
    }

    public void fetchFiles(File root) {
        File[] fileList = root.listFiles();
        if (fileList == null)
            return;
        else {
            for (File file : fileList) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".txt")) {
                        if (isFileInExecutedFiles(file.getPath())) {
                            if (isFileModified(file.getPath(), file.lastModified())) {
                                addToExecutedFiles(file, "oldFile");
                                updateMessage(file.getName());
                                queueFile(file);
                            }
                        } else {
                            addToExecutedFiles(file, "newFile");
                            updateMessage(file.getName());
                            queueFile(file);
                        }
                    }
                } else if (file.isDirectory()) {
                    fetchFiles(file);
                }
            }
        }
    }

    public void addToExecutedFiles(File file, String type) {
        FileModel fileModel = new FileModel(file.getAbsolutePath(), file.lastModified());
        if (type.equals("newFile")) {
            try {
                executedFilesSemaphore.acquire();
                executedFiles.add(fileModel);
                executedFilesSemaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (type.equals("oldFile")) {
            try {
                executedFilesSemaphore.acquire();
                for (FileModel model : executedFiles) {
                    if (model.getFilePath().equals(fileModel.getFilePath())) {
                        model.setLastModified(fileModel.getLastModified());
                    }
                }
                executedFilesSemaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void queueFile(File file) {
        BufferedReader br = null;
        StringBuilder data = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                data.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (CruncherWorker worker : workers) {
            updateTitle(name + "," + "started," + file.getName() + "," + worker.getCruncherName());
        }
        FileInputQueuer queuerTask = new FileInputQueuer(pool, name, new FileInputResult(file.getName(), data.toString(), String.valueOf(file.lastModified()), workers));
        pool.executeQueuer(queuerTask);
    }

    public boolean isFileInExecutedFiles(String filePath) {
        for (FileModel file : executedFiles) {
            if (file.getFilePath().equals(filePath)) {
               return true;
            }
        }
        return false;
    }

    public boolean isFileModified(String filePath, long lastModified) {
        for (FileModel file : executedFiles) {
            if (file.getFilePath().equals(filePath)) {
                if (file.getLastModified() != lastModified)
                    return true;
            }
        }
        return false;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    @Override
    protected String call() throws Exception {
        try {
            while (true) {
                if (!poisoned) {
                    File check = inputQueue.poll();
                    if (check != null && check.getName().equals("poison_file.txt")) {
                        poisoned = true;
                        System.out.println(name + " died");
                        succeeded();
                        break;
                    }
                    sleepSemaphore.acquire();
                    filesSemaphore.acquire();
                    sleepSemaphore.release();
                    for (File file : files)
                        fetchFiles(file);
                    updateMessage("Idle");
                    filesSemaphore.release();
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {

                    }
                }
            }
        } catch (OutOfMemoryError error) {
            files = null;
            System.gc();
            cancelled();
        }
        return null;
    }

    @Override
    protected void cancelled() {
        updateMessage("outOfMemory");
    }

    public void setWorkers(List<CruncherWorker> workers) {
        this.workers = workers;
    }

    public void paused() {
        try {
            sleepSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void started() {
        sleepSemaphore.release();
    }
    public void addFile(File file) {
        this.files.add(file);
    }
    public List<File> getFiles() {
        return files;
    }

    public LinkedBlockingQueue<File> getInputQueue() {
        return inputQueue;
    }
}
