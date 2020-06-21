package observers.models;

public class DirectoryNotificationModel {
    private String fileInputName;
    private String directoryPath;

    public DirectoryNotificationModel(String fileInputName, String directoryPath) {
        this.fileInputName = fileInputName;
        this.directoryPath = directoryPath;
    }

    public String getFileInputName() {
        return fileInputName;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }
}
