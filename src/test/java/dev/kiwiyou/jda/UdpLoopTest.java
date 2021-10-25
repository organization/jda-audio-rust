package dev.kiwiyou.jda;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;

public class UdpLoopTest {
    private UdpLoop loop;
    private UdpSender sender;

    @Test
    @DisplayName("UdpLoop send fails when full")
    void testCapacityFull() {
        this.loop = new UdpLoop(2);
        this.sender = loop.newSender();
        var address = new InetSocketAddress("127.0.0.1", 8080);
        Assertions.assertAll(
                () -> Assertions.assertTrue(sender.queuePacket(ByteBuffer.allocate(0), address)),
                () -> Assertions.assertTrue(sender.queuePacket(ByteBuffer.allocate(0), address)),
                () -> Assertions.assertFalse(sender.queuePacket(ByteBuffer.allocate(0), address))
        );
    }

    @Test
    @DisplayName("UdpLoop isEmpty works")
    void testEmpty() {
        this.loop = new UdpLoop(1);
        this.sender = loop.newSender();
        var address = new InetSocketAddress("127.0.0.1", 8080);
        Assertions.assertTrue(this.sender.isEmpty());
        this.sender.queuePacket(ByteBuffer.allocate(0), address);
        Assertions.assertFalse(this.sender.isEmpty());
    }

    @Test
    @DisplayName("UdpLoop stops")
    void testStop() {
        this.loop = new UdpLoop(1);
        this.loop.stop();
        Assertions.assertTimeout(Duration.ZERO, this.loop::run);
    }

    @AfterEach
    void disposeResources() {
        if (this.loop != null) {
            this.loop.dispose();
        }
        if (this.sender != null) {
            this.sender.dispose();
        }
    }
}