package fileInput.models;

import cruncher.models.CruncherWorker;

import java.io.File;
import java.util.List;

public class FileInputResult {
    private String fileName;
    private String fileData;
    private String lastModified;
    private List<CruncherWorker> workers;
    private boolean poison;
    public FileInputResult(String fileName, String fileData, String lastModified, List<CruncherWorker> workers) {
        this.fileName = fileName;
        this.fileData = fileData;
        this.lastModified = lastModified;
        this.workers = workers;
    }
    public FileInputResult(boolean poison) {
        this.poison = poison;
    }

    public String getFileData() {
        return fileData;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "FileInputResult{" +
                "fileName='" + fileName + '\'' +
                ", lastModified='" + lastModified + '\'' +
                '}';
    }

    public List<CruncherWorker> getWorkers() {
        return workers;
    }

    public boolean isPoison() {
        return poison;
    }
}
