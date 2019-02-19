package Server;

import java.io.*;
import java.net.*;
import java.util.*;

import Document.Document;
/*
 * 线程安全策略：
 * Hermes_Server 是一个服务器，其中每个用户可以访问文档进行编辑。
 * 客户端向服务器请求表单服务，服务器为客户端创建一个新的 socket 连接。
 * 并且对于每个客户端，服务器为其创建一个特定的线程。所以没有两个线程交错。
 * 此外，客户端是创建，而不是提前储存好的，所以我们不能改变或交换客户端的 socket
 *
 * 服务器上 User 计数器是同步的，不必单独增加或减少即时通信同步人数。
 *
 * 所有服务器发送给客户端的信息也都是同步的
 * 这样避免了信息交错
 *
 *  所有的 document 方法也都是同步的（即线程安全的）。
 *  通过 handleRequest 调用其中的任何一个都不会引发并发问题，
 *  因为在同一对象上同步调用两个方法是不可能的。
 *  当一个线程正在执行一个对象的 synchronized 方法时，
 *  其他所有线程对这个对象的 synchronized 方法是阻塞的，此时线程挂起，
 *  直到第一个线程结束对其的调用
 */


/**
 * RTCE的服务器类，服务器有以下任务：
 * 
 * 1. 通过以下方式激活服务器：
 * 
 *      (a) 打开一个端口。
 *      (b) 监听客户端 connection 请求。
 *      (c) 调用 Accept 接受所有客户端的请求.
 *      
 *      
 * 2. 处理来自客户的请求。这需要：
 * 
 *      (a) 解析请求，
 *          并根据我们的协议返回适当的 result 。
 *      (b) 确保请求遵循协议。
 *          如果客户端的请求不遵循协议，
 *          那么服务端就不允许客户端访问成功
 *      (c) 将请求的结果返回给服务器。
 *      (d) 如果用户发出 "edit"（'insert' 或 'delete'）请求，服务器将修改文档
 *
 *          
 * 3. 跟踪服务器端的状态：
 * 
 *      (a) 记录并跟踪已连接服务器的所有用户（或客户端）
 *      (b) 保持记录和跟踪任意时刻正在编辑的文档，服务器只能同时编辑一个文档，
 *          多个客户端允许协同即时编辑一个文本
 *      (c) 即时追踪客户端正在编辑的文档，在服务端同一时间只有一个文档可以被编辑，
 *          就是客户端正在编辑的文档
 *          
 *          
 *  4. 默认操作：
 *  
 *      (a) 向文件提供默认名称。
 *      (b) 序列化来自客户端的请求。
 *     
 */
public class Server {

    private ServerSocket serverSocket = null;
    private static int numberOfUsers = 0;
    private static int numberOfUntitledDocs = 1;
    private static Vector<Document> docs = new Vector<Document>();
    //private static Document RTCEDocument;

    private int userName = 1;

    private static Vector<User> clients = new Vector<User>();

    /**
     * 创建服务器端的socket，监听指定端口上的connection连接。
     * @param port 端口号, 0 <= port <= 65535.
     */
    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Hermes start at 127.0.0.1:" + port);
        } catch (IOException e) {
            throw new RuntimeException("Problem creating port.");
        }
    }

    /**
     * runServer方法实际启动服务器。
     * main函数调用此命令以指定的端口启动服务器。
     * @param  port, 开放的端口号。
     * @throws IOException
     */
    public static void runServer(int port) throws IOException {
        Server server = new Server(port);
        server.serve();
    }

    /**
     * 运行服务器，监听客户端连接并处理它们。
     * 除非出现异常，否则永远不会return。
     * @throws IOException 如果主服务器socket中断
     * (个别客户端的IOExceptions不必中断服务器serve()).
     */
    public void serve() throws IOException {
        while (true) {
            // 此处阻塞，直到有客户端接入
            final Socket socket = serverSocket.accept();         
            updateUsers(1);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    Thread.yield();  // 线程让步，使当前线程从运行状态变为就绪状态，给其他线程一个开始的机会，以便公平竞争。
                    try {
                        handleConnection(socket);
                    } catch (IOException e) {
                        e.printStackTrace(); // 但不结束serve()方法
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }            
                }
            });
            clients.add(new User(t, String.valueOf(userName), socket));
            userName++;
            t.start();// 开启线程
        }
    }

    /**
     * 用于Hermes服务器处理单用户connection。
     * 接受来自客户端的请求，并依据请求内容向各个客户端发送信息，实时更新。
     * 客户端断开连接时return。
     * @param socket 客户端套接字
     * @throws IOException 如果连接有错误或意外终止
     */
    @SuppressWarnings("unchecked")
    private void handleConnection(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        try {
            User curClient = null;
            for (User c : clients) {
                if (c.getSocket().equals(socket)) curClient = c;
            }
            for (String line = in.readLine(); line != null; line = in.readLine()) {

                String output = handleRequest(line, curClient);
                if(output.equals("exit" + String.format("%n") + "EOF")) {
                    break;
                }
                if(output != null) {
                    out.println(output);
                }
                out.flush();
            }
        } catch (SocketException se) { 
            socket.close();
        }
        finally {   
            Vector<User> cloned = new Vector<User>();
            cloned = (Vector<User>) clients.clone();
            for (User c : cloned) {
                if (c.getSocket().equals(socket)) {
                    c.getSocket().close();
                    clients.removeElement(c);
                }
            }
            updateUsers(-1);
            out.close();
            in.close();
        }
    }

    /**
     * 处理客户端请求的方法。
     * 通过正则匹配解析来自客户端的请求，执行适当的操作来更改服务器状态。
     * 并根据我们的协议返回每个请求给客户端的结果。
     * 
     * @param userInput, 表示来自客户端的请求的字符串。
     * @param curClient, 表示当前的客户端。
     * @return String, 对给定请求的响应。
     * @throws UnsupportedEncodingException 如果客户端使用错误的编码通过连接发送请求。
     */
    public static String handleRequest(String userInput, User curClient) throws UnsupportedEncodingException {
        // \d匹配数字，但需要转义\\，
        // \p{ASCII}表示ASCII形式的字符串，
        // \w 在正则表达式中表示一个“字”（数字，字符，下划线）
        // \s匹配空格
        String regex = "(view)|(insert \\d+ \\p{ASCII}*)|(delete \\d+ \\d+)|(help)|(list)|" +
                "(switch \\w+(\\.txt|\\.java))|(new \\w+(\\.txt|\\.java))|(new)|(exit)|(replaceAll \\p{ASCII}+\\s\\p{ASCII}+$)|" +
                "(replaceOne \\p{ASCII}+\\s\\p{ASCII}+$)|(style \\d+ \\d+ \\d+ \\d+)|(giveStyle)|(hello)";
        String input = URLDecoder.decode(userInput, "UTF-8");
        if(!input.matches(regex)) {
            //无效输入
            return "Invalid Request."
            + String.format("%n") + "EOF";
        }
        String[] tokens = input.split(" ");//view  help  list  givStyle  hello
        String[] editTokens= input.split(" ",3);//insert  delete
        String[] replaceTokens = userInput.split(" ");//replaceOne  replaceAll

//        System.out.println(curClient+":"+input);

        if (tokens[0].equals("view")) {
            // 'view' request
            return viewRequest(curClient);

        } else if (tokens[0].equals("hello")) {
            // 'hello' request
            return helloRequest();

        } else if (tokens[0].equals("replaceAll")) {
            //replaceAll request
            return replaceAllRequest(replaceTokens, curClient);

        } else if (tokens[0].equals("replaceOne")) {
            //replaceOne request
            return replaceOneRequest(replaceTokens, curClient);

        } else if (tokens[0].equals("insert")) {
            // 'insert' request
            return insertRequest(editTokens, curClient);

        } else if (tokens[0].equals("delete")) {
            // 'delete' request
            return deleteRequest(editTokens, curClient);

        } else if (tokens[0].equals("help")) {
            // 'help' request
            return helpRequest();

        } else if (tokens[0].equals("list")) {
            // 'list' request
            return listRequest();

        } else if (tokens[0].equals("switch")) {
            // 'switch' request
            return switchRequest(tokens, curClient);

        } else if (tokens[0].equals("new")) {
            // 'new' request
            return newRequest(tokens, curClient);

        } else if (tokens[0].equals("style")) {
            // 'style' request
            return styleUpdateRequest(tokens,curClient);

        } else if (tokens[0].equals("giveStyle") ) {
            // 'giveStyle' request
            return giveStyleRequest(curClient);

        } else if (tokens[0].equals("exit")) {
            // 'exit' request
            return exitRequest();

        } else {
            throw new RuntimeException("End of handleRequest. Should no have gotten here");
        }
    }


    ///////////////////以下是具体的请求处理方法////////////////////


    /**
     * 视图请求的辅助方法。给用户查看文档的当前状态。
     * @param curClient, 发出请求的用户。
     * @return String. 在“view”请求时文档看起来是什么样子。
     * @throws UnsupportedEncodingException 
     */
    public static String viewRequest(User curClient) throws UnsupportedEncodingException {
        return URLEncoder.encode(curClient.getDoc().getAllText(),"UTF-8") 
                +String.format("%n") 
                + "EOF";
    }

    /**
     * Hello请求的辅助方法。这主要是为了终端使用 Hermes，这样可以看到连接了多少用户。
     * @return String, the hello message.
     */
    private synchronized static String helloRequest() {
        String helloMessage = "Welcome to Hermes " + Integer.toString(getNumberOfUsers()) +
                " people are currently connected. Type help for help."
                + String.format("%n") + "EOF";
        return helloMessage;
    }

    /**
     * replaceAll请求的帮助方法。表示获取来自用户的请求的token，并调用必要的方法来将单词的所有出现替换为另一个。
     * @param tokens, 根据用户请求建立的token。具有调用replaceAll方法所需的所有信息。
     * @param curClient, 正在更改的文档的客户端。
     * @return String, 替换文字之后，新的文档的字符串。
     * @throws UnsupportedEncodingException
     */
    public static String replaceAllRequest(String[] tokens, User curClient) throws UnsupportedEncodingException {
        String replaceFromInitial = tokens[1];
        String replaceToInitial = tokens[2];
        String replaceFrom = URLDecoder.decode(replaceFromInitial, "UTF-8");
        String replaceTo = URLDecoder.decode(replaceToInitial, "UTF-8");

        curClient.getDoc().replaceAll(replaceFrom, replaceTo);
        String result = URLEncoder.encode(curClient.getDoc().getAllText(),"UTF-8");
        return result  +String.format("%n")
                + "EOF";
    }

    /**
     * replaceOne请求的帮助方法。获取来自用户的请求的token，并调用必要的方法来将单词出现的第一个替换为另一个。
     * @param tokens, 根据用户请求建立的token。
     * 具有调用replaceOne方法所需的所有信息。
     * @param curClient, 正在更改的文档的客户端。
     * @return String, 替换文字之后，新的文档的字符串。
     * @throws UnsupportedEncodingException
     */
    public static String replaceOneRequest(String[] tokens, User curClient) throws UnsupportedEncodingException {
        String replaceFromInitial = tokens[1];
        String replaceToInitial = tokens[2];
        String replaceFrom = URLDecoder.decode(replaceFromInitial, "UTF-8");
        String replaceTo = URLDecoder.decode(replaceToInitial, "UTF-8");

        curClient.getDoc().replaceOne(replaceFrom, replaceTo);
        String result = URLEncoder.encode(curClient.getDoc().getAllText(), "UTF-8");
        return result + String.format("%n")
                + "EOF";
    }

    /**
     * 用于insert请求的帮助方法。我们协议中的两个“编辑”中的第一个。此方法对文件进行实际编辑。
     * @param tokens 根据用户请求建立的token。
     * 具有调用insert方法所需的所有信息。
     * @param curClient, 做出请求的当前用户
     * @return String, 修改后文档的字符串形式
     * @throws UnsupportedEncodingException
     */
    public static String insertRequest(String[] tokens, User curClient) throws UnsupportedEncodingException {
        int pos = Integer.valueOf(tokens[1]);
        String text = tokens[2];
        curClient.getDoc().getQueue().addRequest(new Request("insert", pos, text, curClient.getDoc()));
        curClient.getDoc().getQueue().resolveRequest();
        return URLEncoder.encode(curClient.getDoc().getAllText(), "UTF-8"
        )+String.format("%n")
                + "EOF";
    }

    /**
     * 删除请求的辅助方法。第二个“编辑”由我们的议定书支持。此方法对文件进行实际编辑。
     * @param tokens 根据用户请求建立的token。
     * 具有调用delete方法所需的所有信息。
     * @param curClient, 做出请求的当前用户
     * @return String, 修改后文档的字符串形式
     * @throws UnsupportedEncodingException
     */
    public static String deleteRequest(String[] tokens, User curClient) throws UnsupportedEncodingException {

        int beginPos=Integer.valueOf(tokens[1]);
        int endPos=Integer.valueOf(tokens[2]);
        curClient.getDoc().getQueue().addRequest(new Request("delete", beginPos, endPos, curClient.getDoc()));
        curClient.getDoc().getQueue().resolveRequest();
        return URLEncoder.encode(curClient.getDoc().getAllText(),"UTF-8")
                +String.format("%n")
                + "EOF";
    }

    /**
     * 该方法在用户发出帮助请求时向用户构建帮助消息。
     * @return String help massage.
     */
    private synchronized static String helpRequest() {
        final String helpMessage = "Welcome to Hermes. Please use one of the follwoing commands:"
                + String.format("%n") +
                "view- shows the user the current document" +
                String.format("%n") +
                "insert x text- insert text to a numeric postion"
                + String.format("%n") +
                "delete x text- delete text from a numeric postion"
                + String.format("%n") +
                "help- shows the user the help menu"
                + String.format("%n") +
                "list- shows the user a list of current open documents"
                + String.format("%n") +
                "switch file - allows the user to switch from one open document to another"
                + String.format("%n") +
                "new file - creates a new file, if file is not specified it will be named by the server"
                + String.format("%n") + "exit- close the connection to the server"
                + String.format("%n") + "hello- return an hello message"
                + String.format("%n")
                + "EOF";
        return helpMessage;
    }

    /**
     * 用于获取当前服务器上的所有文件的列表请求的Help方法。
     * @return String, 当前存储在服务器上的所有文件的名称。
     */
    public static String listRequest() {
        String listOfDocs = "";
        for(int i = 0; i<docs.size();i++) {
            listOfDocs += docs.get(i).getName()+" "+ String.format("%n");
        }
        if (listOfDocs.equals("")) {
            return "There are no existing files on the server."
                    +String.format("%n")
                    + "EOF";
        }
        return listOfDocs
                + String.format("%n")
                + "EOF";
    }

    /**
     * 切换请求的辅助方法。允许用户将焦点切换到另一个文档。
     * 从现在起，所有编辑和样式请求都将被发送到交换式文档，并且任何视图都将表示这个交换式文档中的数据。
     * @param tokens, 根据用户请求建立的token。
     * 具有调用switch方法所需的所有信息。
     * @param curClient, 正在更改的文档的客户端。
     * @return String, 最终所选的文档的视图
     * @throws UnsupportedEncodingException
     */
    public static String switchRequest(String[] tokens, User curClient) throws UnsupportedEncodingException {
        String fileName = tokens[1];
        for (Document doc: docs){
            if(doc.getName().equals(fileName)) {
                //将当前客户端添加到文档的用户列表
                doc.setUser(curClient);

                //如果当前客户端已有一个文档，那就从该文档的用户列表中移除该用户
                if(curClient.getDoc() != null) {
                    curClient.getDoc().getList().remove(curClient);
                }
                //将当前客户端所属文档设置为选择的文档
                curClient.setDoc(doc);
                return URLEncoder.encode(curClient.getDoc().getAllText(),"UTF-8")
                        + String.format("%n")
                        + "EOF";
            }
        }
        return "File does not exist, can't swtich"
                + String.format("%n")
                + "EOF";
    }

    /**
     * new请求的帮助方法。当客户端打开一个未创建的新文档时，必须先创建新文档。
     * 对于可以创建的新文档的数量没有理论限制。用户可以在创建新文件时提供任何名称。
     * 自动添加TXT后缀。如果用户没有指定名称，则将由服务器指派一个通用名称（"untitled[file number].txt". ）。
     * 将新文档添加到服务器所保存的文档列表中。
     * @param tokens
     * @param curClient
     * @return String. 新文档的视图。也可能是空文档。
     */
    public static String newRequest(String[] tokens, User curClient) {

        if (tokens.length == 1) {
            String name = "untitled" + String.valueOf(numberOfUntitledDocs) + ".txt";
            Document doc = new Document(name);

            //将当前客户端添加到文档的用户列表
            doc.setUser(curClient);
            //如果当前客户端已有一个文档，那就从该文档的用户列表中移除该用户
            if(curClient.getDoc() != null) {
                curClient.getDoc().getList().remove(curClient);
            }
            //将当前客户端所属文档设置为创建新的文档
            curClient.setDoc(doc);
            updateDocumentCounter(1);

        }
        else {

            String name = tokens[1];
            // 以后可能会考虑在文件末尾检查.txt。已完成
            Document documentName =new Document(name);
            // 将当前客户端添加到文档的用户列表
            documentName.setUser(curClient);
            // 如果当前客户端已有一个文档，那就从该文档的用户列表中移除该用户
            if(curClient.getDoc() != null){
                curClient.getDoc().getList().remove(curClient);
            }

            //将当前客户端所属文档设置为创建新的文档
            curClient.setDoc(documentName);
        }
        docs.add(curClient.getDoc());
        return curClient.getDoc().getName()
                + String.format("%n")
                + "EOF";
    }

    /**
     * style请求的帮助方法。更改客户端正在处理的当前文档的样式。
     * @param tokens, 表示样式设置的数字的String数组。
     * @param curClient, 发出请求的客户端。
     * @return 给定文档的新样式设置。
     * @throws UnsupportedEncodingException
     */
    public static String styleUpdateRequest(String[] tokens, User curClient) throws UnsupportedEncodingException {
        curClient.getDoc().setStyle(tokens[1] + " " + tokens[2] + " " + tokens[3] + " " +tokens[4]);
        return URLEncoder.encode(curClient.getDoc().getStyle(),"UTF-8")
                +String.format("%n")
                + "EOF";
    }

    /**
     * giveStyle请求的辅助方法。给用户当前正在处理的文档的当前样式。
     * @param curClient, 做出请求的客户端
     * @return String. 给定当前文档的当前样式设置。
     * @throws UnsupportedEncodingException
     */
    public static String giveStyleRequest(User curClient) throws UnsupportedEncodingException {
        return URLEncoder.encode(curClient.getDoc().getStyle(),"UTF-8") 
                +String.format("%n") 
                + "EOF";
    }

    /**
     * Helper方法处理exit请求。
     * @return 字符串exit，告诉客户端断开连接。
     */
    public synchronized static String exitRequest() {
        return "exit" + String.format("%n") + "EOF";
    }


    ///////////////请求方法结束///////////////



    /**
     * 用来增加或减少连接到Hermes的User数量的辅助方法，以便我们能够跟踪他们，并提供有用的用户反馈。
     * @param updateNum, 一个正数或负数.
     */
    public synchronized static void updateUsers(int updateNum) {
        numberOfUsers += updateNum;
    }

    /**
     * 获取连接到服务器的用户数的方法。
     * @return int, 表示连接到服务器的用户数
     */
    public synchronized static int getNumberOfUsers() {
        return numberOfUsers;
    }
    
    /**
     * 辅助方法，用于将服务器上的文档数量增加num。
     * 主要用于给文档指定不同的默认名称。
     * @param num, 更新numberOfUntitledDocs的数值。
     *
     */
    public synchronized static void updateDocumentCounter(int num) {
        numberOfUntitledDocs += num;
    }
    
    
    /**
     * 获取文档计数器当前状态的方法
     * @return int 表示当前服务器上文档数量的整数。
     */
    public synchronized static int getDocumentCounter() {
       return numberOfUntitledDocs;
    }

    /**
     * 启动在默认端口（8080）上运行的Hermes服务器的main函数。
     */
//    public static void main(String[] args) {
//        final int port;
//        // 从Hermes.customport中获取端口，相当于一个静态变量
//        String portProp = System.getProperty("Hermes.customport");
//        if (portProp == null) {
//            port = 8080; // 默认端口
//        } else {
//            port = Integer.parseInt(portProp);
//        }
//        try {
//            runServer(port);
//        } catch (IOException e) {
//            throw new RuntimeException("Problem in opening server.");
//        }
//    }
}