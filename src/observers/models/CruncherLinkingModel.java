package observers.models;

public class CruncherLinkingModel {
    private String fileInputModelName;
    private String cruncherName;

    public CruncherLinkingModel(String fileInputModelName, String cruncherName) {
        this.fileInputModelName = fileInputModelName;
        this.cruncherName = cruncherName;
    }

    public String getCruncherName() {
        return cruncherName;
    }

    public String getFileInputModelName() {
        return fileInputModelName;
    }

    @Override
    public String toString() {
        return "CruncherLinkingModel{" +
                "fileInputModelName='" + fileInputModelName + '\'' +
                ", cruncherName='" + cruncherName + '\'' +
                '}';
    }
}
