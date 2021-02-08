package dev.tigr.asmp.callback;

/**
 * @author Tigermouthbear 2/7/21
 * provides a way of telling modificate where to inject return
 */
public class CallbackInfo {
    private boolean cancelled = false;

    public void cancel() {
        cancelled = true;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
