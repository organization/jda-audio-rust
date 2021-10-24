use std::net::{SocketAddr, ToSocketAddrs, UdpSocket};

use crossbeam_channel::{bounded, select, Receiver, Sender};
use jni::objects::{JByteBuffer, JClass, JString};
use jni::sys::{jboolean, jint, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use log::{debug, error};

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    queuePacket
 * Signature: (JLjava/nio/ByteBuffer;Ljava/lang/String;I)Z
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_queuePacket(
    env: JNIEnv,
    _: JClass,
    sender: jlong,
    packet: JByteBuffer,
    host: JString,
    port: jint,
) -> jboolean {
    debug!("queuePacket call");
    let sender = unsafe { Box::from_raw(sender as *mut Sender<(Vec<u8>, SocketAddr)>) };
    let addr = (env.get_string(host).unwrap().to_str().unwrap(), port as u16)
        .to_socket_addrs()
        .unwrap()
        .next()
        .unwrap();
    let is_success = Box::leak(sender)
        .try_send((
            env.get_direct_buffer_address(packet).unwrap().to_vec(),
            addr,
        ))
        .is_ok();
    if is_success {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    isEmpty
 * Signature: (J)Z
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_isEmpty(
    _: JNIEnv,
    _: JClass,
    sender: jlong,
) -> jboolean {
    debug!("isEmpty call");
    let sender = unsafe { Box::from_raw(sender as *mut Sender<(Vec<u8>, SocketAddr)>) };
    if Box::leak(sender).is_empty() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    disposeSender
 * Signature: (J)V
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_disposeSender(
    _: JNIEnv,
    _: JClass,
    sender: jlong,
) {
    debug!("disposeSender call");
    unsafe { Box::from_raw(sender as *mut Sender<(Vec<u8>, SocketAddr)>) };
}

struct UdpLoop {
    rx: Receiver<(Vec<u8>, SocketAddr)>,
    tx: Sender<(Vec<u8>, SocketAddr)>,
    stop_rx: Receiver<()>,
    stop_tx: Sender<()>,
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    createLoop
 * Signature: ()J
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_createLoop(
    _: JNIEnv,
    _: JClass,
    capacity: jint,
) -> jlong {
    debug!("createLoop call");
    let (tx, rx) = bounded(capacity as usize);
    let (stop_tx, stop_rx) = bounded(1);
    let udp_loop = UdpLoop {
        rx,
        tx,
        stop_rx,
        stop_tx,
    };
    let handle = Box::leak(Box::new(udp_loop)) as *mut _;
    handle as jlong
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    runLoop
 * Signature: (J)V
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_runLoop(
    _: JNIEnv,
    _: JClass,
    handle: jlong,
) {
    debug!("runLoop call");
    let UdpLoop { rx, stop_rx, .. } = *unsafe { Box::from_raw(handle as *mut UdpLoop) };
    let stream = UdpSocket::bind("0.0.0.0:0").expect("cannot bind udpsocket");
    loop {
        select! {
            recv(stop_rx) -> _ => break,
            recv(rx) -> result => match result {
                Ok((packet, addr)) => {
                    stream.send_to(&packet, addr).unwrap();
                },
                Err(e) => {
                    error!("error sending packet: {}", e);
                }
            },
        }
    }
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    createStopper
 * Signature: (J)J
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_createStopper(
    _: JNIEnv,
    _: JClass,
    handle: jlong,
) -> jlong {
    debug!("createStopper call");
    let udp_loop = Box::leak(unsafe { Box::from_raw(handle as *mut UdpLoop) });
    Box::leak(Box::new(udp_loop.stop_tx.clone())) as *mut _ as jlong
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    signalStop
 * Signature: (J)V
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_signalStop(
    _: JNIEnv,
    _: JClass,
    handle: jlong,
) {
    debug!("signalStop call");
    let stopper = unsafe { Box::from_raw(handle as *mut Sender<()>) };
    stopper.send(()).unwrap();
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    getSender
 * Signature: (J)J
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_getSender(
    _: JNIEnv,
    _: JClass,
    handle: jlong,
) -> jlong {
    debug!("getSender call");
    let udp_loop = Box::leak(unsafe { Box::from_raw(handle as *mut UdpLoop) });
    Box::leak(Box::new(udp_loop.tx.clone())) as *mut _ as jlong
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    cloneSender
 * Signature: (J)J
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_cloneSender(
    _: JNIEnv,
    _: JClass,
    handle: jlong,
) -> jlong {
    debug!("cloneSender call");
    let sender = Box::leak(unsafe { Box::from_raw(handle as *mut Sender<(Vec<u8>, SocketAddr)>) });
    Box::leak(Box::new(sender.clone())) as *mut _ as jlong
}

/*
 * Class:     dev_kiwiyou_jda_UdpLoopLibrary
 * Method:    initDebugLogger
 * Signature: ()V
 */
#[no_mangle]
pub extern "system" fn Java_dev_kiwiyou_jda_UdpLoopLibrary_initDebugLogger(_: JNIEnv, _: JClass) {
    pretty_env_logger::init();
}
