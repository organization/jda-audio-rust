package dev.kiwiyou.jda;

public class UdpStopper {
    private final long handle;
    private boolean disposed = false;

    public UdpStopper(long handle) {
        this.handle = handle;
    }

    public void stop() {
        if (!disposed) {
            UdpLoopLibrary.signalStop(this.handle);
            this.disposed = true;
        }
    }

    public void dispose() {
        if (!disposed) {
            UdpLoopLibrary.disposeSender(this.handle);
            this.disposed = true;
        }
    }
}
