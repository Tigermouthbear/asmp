package dev.tigr.asmp.callback;

/**
 * @author Tigermouthbear 2/7/21
 * @param <T> type of returned value
 * same as callback info but has value returned
 */
public class CallbackInfoReturnable<T> {
    private T value = null;
    private boolean cancelled = false;

    public CallbackInfoReturnable() {
    }

    public CallbackInfoReturnable(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }
}
