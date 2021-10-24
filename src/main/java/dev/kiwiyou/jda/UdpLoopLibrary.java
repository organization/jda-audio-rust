package dev.kiwiyou.jda;

import com.sedmelluq.lava.common.natives.NativeLibraryLoader;

import java.nio.ByteBuffer;

public class UdpLoopLibrary {
    private static final NativeLibraryLoader nativeLoader =
            NativeLibraryLoader.create(UdpLoopLibrary.class, "udp_queue");

    static {
        nativeLoader.load();
    }

    public static native boolean queuePacket(long senderHandle, ByteBuffer packet, String host, int port);
    public static native boolean isEmpty(long senderHandle);
    public static native void disposeSender(long senderHandle);
    public static native long createLoop(int capacity);
    public static native void runLoop(long loopHandle);
    public static native long createStopper(long loopHandle);
    public static native void signalStop(long stopperHandle);
    public static native long getSender(long loopHandle);
    public static native long cloneSender(long loopHandle);

    public static native void initDebugLogger();
}
