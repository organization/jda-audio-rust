package dev.kiwiyou.jda;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class UdpSender {
    private final long handle;
    private final ByteBuffer packetBuffer;
    private boolean disposed = false;
    private final int bufferCapacity;

    public UdpSender(int bufferCapacity, long handle) {
        this.handle = handle;
        this.packetBuffer = ByteBuffer.allocateDirect(bufferCapacity);
        this.bufferCapacity = bufferCapacity;
    }

    public boolean queuePacket(ByteBuffer packet, InetSocketAddress address) {
        if (disposed) {
           return false;
        }
        this.packetBuffer.clear();
        this.packetBuffer.put(packet);
        String host = address.getAddress().getHostAddress();
        return UdpLoopLibrary.queuePacket(this.handle, this.packetBuffer, host, address.getPort());
    }

    public boolean isEmpty() {
        if (disposed) {
            return false;
        }
        return UdpLoopLibrary.isEmpty(this.handle);
    }

    public void dispose() {
        if (!disposed) {
            UdpLoopLibrary.disposeSender(this.handle);
            disposed = true;
        }
    }

    public UdpSender derive() {
        long newHandle = UdpLoopLibrary.cloneSender(this.handle);
        return new UdpSender(this.bufferCapacity, newHandle);
    }
}
