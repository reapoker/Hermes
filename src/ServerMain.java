import Server.ChatServer;
import Server.FileServer;
import Server.Server;

import java.io.IOException;

public class ServerMain {

    /**
     * runServer方法实际启动服务器。
     * main函数调用此命令以指定的端口启动服务器。
     *
     * @param port, 开放的端口号。
     * @throws IOException
     */
    public static void runServer(int port) throws IOException {
        Server server = new Server(port);
        ChatServer cs = new ChatServer(port + 1);
        FileServer fs = new FileServer(port + 2);
        fs.start();
        cs.start();
        server.serve();
    }

    public static void main(String args[]) {
        final int port;
        // 从Hermes.customport中获取端口，相当于一个静态变量
        String portProp = System.getProperty("Hermes.customport");
        if (portProp == null) {
            port = 8080; // 默认端口
        } else {
            port = Integer.parseInt(portProp);
        }
        try {
            runServer(port);
        } catch (IOException e) {
            throw new RuntimeException("Problem in opening server.");
        }

    }
}

