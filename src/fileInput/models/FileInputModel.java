package fileInput.models;

import cruncher.models.CruncherWorker;

import java.util.ArrayList;
import java.util.List;

public class FileInputModel {
    private String id;
    private FileInputWorker fileInputWorker;
    private List<String> directories;
    private List<CruncherWorker> cruncherWorkers;
    private boolean executed = false;
    public FileInputModel(String id, FileInputWorker fileInputWorker) {
        this.id = id;
        this.fileInputWorker = fileInputWorker;
        cruncherWorkers = new ArrayList<>();
        directories = new ArrayList<>();
    }

    public void addDirectory(String directory) {
        directories.add(directory);
    }

    public void removeDirectory(String directory) {
        directories.remove(directory);
    }

    public String getId() {
        return id;
    }

    public FileInputWorker getFileInputWorker() {
        return fileInputWorker;
    }

    public List<String> getDirectories() {
        return directories;
    }

    public List<CruncherWorker> getCruncherWorkers() {
        return cruncherWorkers;
    }

    @Override
    public String toString() {
        return "FileInputModel{" +
                "id='" + id + '\'' +
                '}';
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isExecuted() {
        return executed;
    }
}
