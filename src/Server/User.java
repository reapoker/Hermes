package Server;

import java.net.Socket;

import Document.Document;

/*
 * 线程安全策略
 * 用户类表示连接到服务器的每个客户端的数据。因为每个用户
 * 分配了它自己的端口和它自己的线程，我们可以通过限制来声明并发安全性。
 * 通过约束-该类中的所有方法都是在一个线程上执行的，来确定该线程定位于用户。
 * 并且不与任何其他客户端共享。此外，没有实际的竞争条件，因为每个
 * 方法负责更新不同的JComponent，因此不必担心它们的顺序。
 * 执行会干扰视图。
 */

/**
 * Hermes的用户类。有助于区别服务器上的User
 * Hermes每个User有4个属性：
 *              1)  所在线程。
 *              2)  用户名（序列号）。
 *              3)  所有的socket.
 * 		        4)  当前正在处理的文档。
 */
public class User {
    Thread thread;
    Document doc;
    String name;
    Socket socket;

    /**
     * User的构造函数。
     * 以三个属性作为参数。
     * 获取线程、它的名称和socket。
     * @param t, 客户端所在线程。
     * @param name, 客户端名字。
     * @param socket, 客户端正在使用的socket。
     */
    public User(Thread t, String name, Socket socket) {
        this.thread = t;
        this.name = name;
        this.socket = socket;
    }


    /**
     * 设置客户端线程的方法。
     * @param t, 给客户端的线程。
     */
    public void setThread(Thread t) {
        this.thread = t;
    }


    /**
     * 来设置客户端文档的方法。
     * @param doc, 你想给它的文档。
     */
    public void setDoc(Document doc) {
        this.doc = doc;
    }

    /**
     * 设置客户端名称的方法。
     * @param name, 给文档的名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 设置客户端套接字的方法。
     * @param socket, 要分配给用户的套接字。
     * 通常这应该是不改的。
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * 获取客户端正在运行的线程的方法。
     * @return Thread. 客户端正在运行的线程。
     */
    public Thread getThread() {
        return this.thread;
    }

    /**
     * 获取客户端当前正在关注的文档对象的方法。
     * @return Document. 客户端当前正在运行的文档对象。
     */
    public Document getDoc() {
        return this.doc;
    }

    /**
     * 获取客户端名称的方法。
     * @return String, 客户端的名称。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取客户端使用连接到服务器的套接字的方法。
     * @return Socket, 客户端正在使用连接到服务器的套接字。
     */
    public Socket getSocket() {
        return this.socket;
    }
}