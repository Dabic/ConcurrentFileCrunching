package observers.notifications;

public abstract class Notification {
    private Object owner;
    public Notification (Object owner) {
        this.owner = owner;
    }

    public Object getOwner() {
        return owner;
    }
}
