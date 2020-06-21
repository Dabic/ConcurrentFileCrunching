package fileInput.models;

public class FileModel {
    private String filePath;
    private long lastModified;

    public FileModel(String filePath, long lastModified) {
        this.filePath = filePath;
        this.lastModified = lastModified;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
