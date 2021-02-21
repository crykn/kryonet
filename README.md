![KryoNet](https://raw.github.com/wiki/EsotericSoftware/kryonet/images/logo.jpg)

[![Release](https://jitpack.io/v/crykn/kryonet.svg)](https://jitpack.io/#crykn/kryonet) [![Build Status](https://travis-ci.org/crykn/quakemonkey.svg?branch=master)](https://travis-ci.org/crykn/quakemonkey)

A fork of [KryoNet](https://github.com/EsotericSoftware/kryonet/), a Java library that provides a clean and simple API for efficient network communication.

This fork was specifically made for [ProjektGG](https://github.com/eskalon/ProjektGG), but also adds the most demanded features on KryoNet's issue tracker. If you have a pull request for KryoNet also consider adding it here, as KryoNet had its last release [in 2013](https://github.com/EsotericSoftware/kryonet/releases).

## Key Changes
* [Kryo 5.0.0](https://github.com/EsotericSoftware/kryo/releases/tag/kryo-parent-5.0.0) is used for the serialization (for a list of changes and new features since Kryo 3 see [here](https://groups.google.com/forum/#!msg/kryo-users/sBZ10dwrwFQ/hb6FF5ZXCQAJ); takes care of [#77](https://github.com/EsotericSoftware/kryonet/issues/77) and [#123](https://github.com/EsotericSoftware/kryonet/issues/123))
* `Listener` is now an interface ([#39](https://github.com/EsotericSoftware/kryonet/issues/39)) with empty default methods, which helps to reduce boilerplate code
* Includes a fix for the common Android 5.0 crash ([#106](https://github.com/EsotericSoftware/kryonet/issues/106), [#111](https://github.com/EsotericSoftware/kryonet/issues/111), [#120](https://github.com/EsotericSoftware/kryonet/issues/120)), toggleable by `UdpConnection#androidFixDisabled`
* The LAN Host Discovery was improved ([#133](https://github.com/EsotericSoftware/kryonet/pull/133)) and is now available to Non-Kryo-Serializations ([#127](https://github.com/EsotericSoftware/kryonet/issues/127))
* A [TypeListener](#typelisteners) was added for easier message handling (takes care of [#130](https://github.com/EsotericSoftware/kryonet/issues/130))
* Various improvements to the javadoc (especially [#35](https://github.com/EsotericSoftware/kryonet/issues/35), [#44](https://github.com/EsotericSoftware/kryonet/issues/44), [#124](https://github.com/EsotericSoftware/kryonet/issues/124), [#137](https://github.com/EsotericSoftware/kryonet/issues/137)); helps to reduce the most common developer mistakes
* KryoNet now uses a [gradle](https://gradle.org/) build setup
* Java 8+ is supported
* All the commits made on the main repo since the last release in 2013 ([#52](https://github.com/EsotericSoftware/kryonet/issues/52), [#98](https://github.com/EsotericSoftware/kryonet/issues/98), [#105](https://github.com/EsotericSoftware/kryonet/issues/105)) and a lot more minor fixes and changes (inter alia [#109](https://github.com/EsotericSoftware/kryonet/issues/109#issuecomment-643352317)); the documentation (see below) was updated as well

A more in-depth changelog is available on the [releases](https://github.com/crykn/kryonet/releases) page.

## Documentation
KryoNet is ideal for any client/server application. It is very efficient, so is especially good for games. KryoNet can also be useful for inter-process communication.

<details>
  <summary><b><i>Click to expand the whole documentation</i></b></summary>
  
- [Running a server](#running-a-server)
- [TypeListeners](#typelisteners)
- [Connecting a client](#connecting-a-client)
- [Registering classes](#registering-classes)
- [TCP and UDP](#tcp-and-udp)
- [Buffer sizes](#buffer-sizes)
- [Threading](#threading)
- [LAN server discovery](#lan-server-discovery)
- [Pluggable Serialization](#pluggable-serialization)
- [Logging](#logging)
- [Remote Method Invocation (RMI)](#remote-method-invocation)
- [KryoNet versus ?](#kryonet-versus-)

---

### Running a server

This code starts a server on TCP port 54555 and UDP port 54777:

```java
    Server server = new Server();
    server.bind(54555, 54777);
    server.start();
```

The `start()` method starts a thread to handle incoming connections, reading/writing to the socket and notifying listeners.

The following code adds a listener to handle receiving objects. Listeners should be added before binding and starting the server.

```java
    server.addListener(new Listener() {
       public void received (Connection connection, Object object) {
          if (object instanceof SomeRequest) {
             SomeRequest request = (SomeRequest)object;
             System.out.println(request.text);
    
             SomeResponse response = new SomeResponse();
             response.text = "Thanks";
             connection.sendTCP(response);
          }
       }
    });
```

<details>
  <summary><b><i>Click to expand the class definitions of 'SomeRequest' and 'SomeResponse'</i></b></summary>
  
```java
    public class SomeRequest {
       public String text;
    }
    public class SomeResponse {
       public String text;
    }
```
  
</details>

Typically a listener has a series of `instanceof` checks to decide what to do with the object received. In this example, it prints out a string and sends a response over TCP.

Note the Listener class also has `connected(Connection)` and `disconnected(Connection)` methods that can be overridden.

---

### TypeListeners

Type listeners takes care of distributing received messages to previously specified handlers. This replaces the long instanceof checks and allows for rather concise code, especially with lambdas: 

```java
TypeListener typeListener = new TypeListener();

// add a type handler for SomeRequest.class   
typeListener.addTypeHandler(SomeRequest.class,
   (con, msg) -> {
      System.out.println(msg.getSomeData());
   });
// add another one for SomeOtherRequest.class
typeListener.addTypeHandler(SomeOtherRequest.class,
   (con, msg) -> {
      con.sendTCP(new SomeResponse());
});

server.addListener(typeListener);
```

In the above example `con` is the connection to the client and `msg` is the received object - already cast to the right type.

---

### Connecting a client

This code connects to a server running on TCP port 54555 and UDP port 54777:

```java
    Client client = new Client();
    client.start();
    client.connect(5000, "192.168.0.4", 54555, 54777);
    
    SomeRequest request = new SomeRequest();
    request.text = "Here is the request";
    client.sendTCP(request);
```

The `start()` method starts a thread to handle the outgoing connection, reading/writing to the socket, and notifying listeners. Note that this method must be called before `connect(...)`, otherwise the outgoing connection will fail.

In the above example, the `connect(...)` method blocks for a maximum of 5000 milliseconds. If it times-out or the connecting otherwise fails, an exception is thrown (handling not shown in the example above). After the connection is made, the example sends a `SomeRequest` object to the server over TCP.

This code adds a listener to print out the response:

```java
    client.addListener(new Listener() {
       public void received (Connection connection, Object object) {
          if (object instanceof SomeResponse) {
             SomeResponse response = (SomeResponse)object;
             System.out.println(response.text);
          }
       }
    });
```

---

### Registering classes

For the above examples to work, the classes that are going to be sent over the network must be registered with [Kryo](https://github.com/EsotericSoftware/kryo/). Kryo takes care of serializing the objects to and from bytes.

```java
    Kryo kryo = server.getKryo();
    kryo.register(SomeRequest.class);
    kryo.register(SomeResponse.class);
    Kryo kryo = client.getKryo();
    kryo.register(SomeRequest.class);
    kryo.register(SomeResponse.class);
```

This must be done on both the client and server, before any network communication occurs. It is very important that the exact same classes are registered on both the client and server and that they are registered in the exact same order. Because of this, typically the code that registers classes is placed in a method on a class available to both the client and server.

Alternatively, Kryo can be configured to allow serialization without registering classes up front (`kryo.setRegistrationRequired(false)`). While this is useful for testing purposes, it can [pose a security risk and leads to larger packet sizes](https://github.com/EsotericSoftware/kryo#optional-registration). In addition, `kryo.setWarnUnregisteredClasses(true)` can be used to log whenever an unregistered class is serialized.

For further information on how objects are serialized for network transfer, please take a look at the [Kryo serialization library](https://github.com/EsotericSoftware/kryo). Kryo can serialize any object and supports data compression (e.g. deflate compression).

---

### TCP and UDP

KryoNet always uses a TCP port. This allows the framework to easily perform reliable communication and have a stateful connection. KryoNet can optionally use a UDP port in addition to the TCP one. While both ports can be used simultaneously, it is not recommended to send an huge amount of data on both at the same time because the two protocols can [affect each other](http://www.isoc.org/INET97/proceedings/F3/F3_1.HTM).

**TCP** is reliable, meaning objects sent are sure to arrive at their destination eventually. **UDP** is faster, but unreliable, meaning an object sent may never be delivered. Because it is faster, UDP is typically used, when many updates are being sent and it does not matter if one update is missed.

Note that KryoNet does not currently implement any extra features for UDP, such as reliability or flow control. It is left to the application to make proper use of the UDP connection. See [here](https://github.com/crykn/quakemonkey) for an example of a delta-snapshot-protocol.

---

### Buffer sizes

KryoNet uses a few buffers for serialization and deserialization that must be sized appropriately for a specific application. See the `Client` and `Server` constructors for customizing the buffer sizes. There are two types of buffers, a write buffer and an object buffer.

To receive an object graph, the bytes are stored in the object buffer until all of the bytes for the object are received, then the object is deserialized. The object buffer should be sized at least as large as the largest object that will be received.

To send an object graph, it is serialized to the write buffer where it is queued until it can be written to the network socket. Typically it is written immediately, but when sending a lot of data or when the network is slow, it may remain queued in the write buffer for a short time. The write buffer should be sized at least as large as the largest object that will be sent, plus some head room to allow for some serialized objects to be queued. The amount of head room needed is dependent upon the size of objects being sent and how often they are sent.

To avoid very large buffer sizes, object graphs can be split into smaller pieces and sent separately. Collecting the pieces and reassembling the larger object graph, or writing them to disk, etc is left to the application code. If a large number of small object graphs are queued to be written at once, it may exceed the write buffer size. `TcpIdleSender` and `InputStreamSender` can be used to queue more data only when the connection is idle. Also see the `setIdleThreshold` method on the Connection class.

---

### Threading

KryoNet imposes no restrictions on how threading is handled. The `Server` and `Client` classes have an `update()` method that accepts connections and reads or writes any pending data for the current connections. The update method should be called periodically to process network events. Both the `Client` and `Server` classes implement `Runnable` and the `run()` method continually calls update until the `stop()` method is called. 

Handing a client or server to a `java.lang.Thread` is a convenient way to have a dedicated update thread, and this is what the `start` method does. If this does not fit your needs, call `update()` manually from the thread of your choice.

Listeners are notified from the update thread, so should not block for long. To change this behavior, take a look at `ThreadedListener` and `QueuedListener`.

The update thread should never be blocked to wait for an incoming network message, as this will cause a deadlock.

---

### LAN server discovery

KryoNet can broadcast a UDP message on the LAN to discover any servers running:

```java
    InetAddress address = client.discoverHost(54777, 5000);
    System.out.println(address);
```

This will print the address of the first server found running on UDP port 54777. The call will block for up to 5000 milliseconds, waiting for a response. A more in-depth example (where additional data is sent) can be found in the `DiscoverHostTest`.

---

### Logging

KryoNet makes use of the low overhead, lightweight [MinLog logging library](https://github.com/EsotericSoftware/minlog). The logging level can be set in this way:

```java
    Log.set(LEVEL_TRACE);
```

KryoNet does minimal logging at INFO and above levels. DEBUG is good to use during development and indicates the total number of bytes for each object sent. TRACE is good to use when debugging a specific problem, but outputs too much information to leave on all the time.

MinLog supports a fixed logging level, which will remove logging statements below that level. For efficiency, KryoNet can be compiled with a fixed logging level MinLog JAR. See [here](https://github.com/EsotericSoftware/minlog#fixed-logging-levels) for more information.

---

### Pluggable Serialization

Serialization can be customized by providing a Serialization instance to the Client and Server constructors. By default KryoNet uses [Kryo](https://github.com/EsotericSoftware/kryo) (hence the name of *Kryo*Net) for serialization. Kryo uses a binary format and is [very efficient](https://github.com/EsotericSoftware/kryo#benchmarks), highly configurable and does automatic serialization for most object graphs.

Additionally, JSON serialization is provided which uses [JsonBeans](https://github.com/EsotericSoftware/jsonbeans). JSON is human readable so is convenient for use during development to monitor the data being sent and received.

---

### Remote Method Invocation

KryoNet has an easy to use mechanism for invoking methods on remote objects (RMI). This has a small amount of overhead versus explicitly sending objects. RMI can hide that methods are being marshaled and executed remotely, but in practice the code using such methods will need to be aware of the network communication to handle errors and methods that block. KryoNet's RMI is not related to the java.rmi package.

RMI is done by first calling `registerClasses`, creating an ObjectSpace and registering objects with an ID on one side of the connection:

```java
    ObjectSpace.registerClasses(endPoint.getKryo());
    ObjectSpace objectSpace = new ObjectSpace();
    objectSpace.register(42, someObject);
    // ...
    objectSpace.addConnection(connection);
```

Multiple ObjectSpaces can be created for both the client or server side. Once registered, objects can be used on the other side of the registered connections:

```java
    SomeObject someObject = ObjectSpace.getRemoteObject(connection, 42, SomeObject.class);
    SomeResult result = someObject.doSomething();
```

The `getRemoteObject(...)` method returns a proxy object that represents the specified class. When a method on the class is called, a message is sent over the connection and on the remote side the method is invoked on the registered object. The method blocks until the return value is sent back over the connection.

Exactly how the remote method invocation is performed can be customized by casting the proxy object to a RemoteObject.

```java
    SomeObject someObject = ObjectSpace.getRemoteObject(connection, 42, SomeObject.class);
    ((RemoteObject)someObject).setNonBlocking(true, true);
    someObject.doSomething();
```

Note that the SomeObject class does not need to implement RemoteObject, this is handled automatically.

The first `true` passed to `setNonBlocking` causes remote method invocations to be non-blocking. When `doSomething` is invoked, it will not block and wait for the return value. Instead the method will just return null.

The second `true` passed to `setNonBlocking` indicates that the return value of remote method invocations are to be ignored. This means the server will not waste time or bandwidth sending the result of the remote method invocation.

If the second parameter for `setNonBlocking` is false, the server will send back the remote method invocation return value. There are two ways to access a return value for a non-blocking method invocation:

```java
    RemoteObject remoteObject = (RemoteObject)someObject;
    remoteObject.setNonBlocking(true, false);
    someObject.doSomething();
    // ...
    SomeResult result = remoteObject.waitForLastResponse();

    RemoteObject remoteObject = (RemoteObject)someObject;
    remoteObject.setNonBlocking(true, false);
    someObject.doSomething();
    byte responseID = remoteObject.getLastResponseID();
    // ...
    SomeResult result = remoteObject.waitForResponse(responseID);
```

---

### KryoNet versus ?

Because KryoNet solves a specific problem (a simple networking API with a powerful serialization solution), the KryoNet API can do so very elegantly. However, KryoNet makes the assumptions that it will only be used for client/server architectures and that it will be used on both sides of the network. For you to make an informed decision, here are some common alternatives to KryoNet:

* [Netty](https://netty.io) is another popular networking framework. However, it does not offer a robust out-of-the-box serializing solution like KryoNet and seems to have a steeper learning curve. It also does not intrinsically support RMI.

* The [Apache MINA](http://mina.apache.org/) project is similar to KryoNet. MINA's API is lower level and a great deal more complicated. Even the simplest client/server will require a lot more code to be written. Furthermore, MINA is not integrated with a robust serialization framework and doesn't support RMI out-of-the-box.

* [JRakNet](https://github.com/JRakNet/JRakNet) is a java port of the C++ networking engine [RakNet](https://github.com/facebookarchive/RakNet) and based on Netty. However, it has some shortcomings featurewise (no RMI, no serialization framework) and is a lot less flexible than other networking frameworks.

* [Discontinued] The [PyroNet](https://code.google.com/p/pyronet/) project is a minimal layer over NIO. It provides TCP networking similar to KryoNet, but without the higher level features. Priobit requires all network communication to occur on a single thread.

* [Disconitnued] The [Java Game Networking](http://code.google.com/p/jgn/) project is a higher level library similar to KryoNet. JGN does not have as simple of an API.

---
  
</details>

## Download

It is recommended to use [jitpack](https://jitpack.io/#crykn/kryonet/) to import this lib. 

An example for the use with gradle (code snippets for other build tools can be found [here](https://jitpack.io/#crykn/kryonet)):

```
allprojects {
   repositories {
      // ...
      maven { url 'https://jitpack.io' }
   }
}
	
dependencies {
   compile 'com.github.crykn:kryonet:2.22.7'
}
```

## Further reading

Beyond this documentation page, you may find the following links useful:

- [Kryo](https://github.com/EsotericSoftware/kryo) (the library used for the serialization in *Kryo*Net)
- [Example code](examples/com/esotericsoftware/kryonet/examples):
   - [Simple chat server](examples/com/esotericsoftware/kryonet/examples/chat)
   - [Advanced chat server using RMI](examples/com/esotericsoftware/kryonet/examples/chatrmi) (Remote Method Invocation)
   - [A simple "game server"](examples/com/esotericsoftware/kryonet/examples/position) (proof of concept) updating the position of clients
- [Unit tests](src/test/java/com/esotericsoftware/kryonet)
