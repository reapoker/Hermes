package Client.Controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;

import Client.Model.Model;
import Client.View.ClientView;
import Client.View.View;
/*
 * 线程安全策略
 *      控制器与服务器通信并从服务器请求实时更新，
 *      并实时更新服务器上的文档。
 *      每个客户端都有自己的控制器，它位于一个单独的线程swing事件调度线程中。
 *      此外，每个与服务器的连接都是在特定的套接字和线程上执行的
 *      是客户独有的。因此，客户端与其他客户端隔离
 *
 *      与客户端服务器通信相关的所有操作都在内部执行
 *      一个swing工作线程，可以防止并发问题。而且make请求方法
 *      与来自特定客户端的请求不会相互交错的当前信号同步
 *
 *      构造函数不需要同步，因为它正在创建一个受限制的新对象。
 *      因此，Java不会让你同步它。
 *
 *      最后，即使服务器延迟和请求排队，我们也可以保证完整的客户端功能
 *      感谢我们的swing中的方法在后台线程中执行所有操作并进行更新
 */

/**
 * 我们MVC设计模式实现的Controller部分。
 * 它负责：
 * <p>
 * （1）运行客户端主方法。
 * <p>
 * （2）连接到服务器。
 * <p>
 * （3）处理视图生成的所有事件
 * 由客户触发。
 * <p>
 * （4）向服务器发出适当的请求。
 * 在需要时响应用户在视图上触发的操作。
 * <p>
 * （5）处理来自服务器的响应。
 * <p>
 * （4）将该信息传递给模型
 * 如有必要，它可以更新视图。
 *
 * @author reapoker
 */

public class Controller implements ActionListener {
    /*
     * MVC的类变量
     */
    private View view;
    private Model model;
    RoomController rc;

    private static String ipAddress = null;
    private String switchTo = "";

    static PrintWriter outStream;
    static BufferedReader inStream;

    /*
     * Timer定时器监听更新
     */
    private Timer listTimer = new Timer(5000, this);
    private Timer viewTimer = new Timer(2000, this);
    private Timer styleTimer = new Timer(2000, this);

    /*
     * 要更新的初始样式值并发送到模型以更新view
     */
    private static int fontName = 3;
    private static int fontStyle = 0;
    private static int fontSize = 12;
    private static int color = 0;

    private static boolean madeConnection = false;
    /*
     * 用于复制，剪切和粘贴的粘贴板
     */
    private static String clipBoard = null;

    public Controller() {
        /**
         * SwingUtilities.invokeLater()方法使事件派发线程上的可运行对象排队。
         * 当可运行对象排在事件派发队列的队首时，就调用其run方法。
         * 其效果是允许事件派发线程调用另一个线程中的任意一个代码块。
         */
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("static-access")

            public void run() {
                View main = new View();

                main.setDefaultCloseOperation(main.EXIT_ON_CLOSE);
                // 点击X按钮关闭
                main.pack();//调整此窗口的大小，以适合其子组件的首选大小和布局。
                main.setSize(1400, 800);
                main.setVisible(true);
                while (!madeConnection) {
                    try {
                        openConnection();
                    } catch (UnknownHostException e1) {
                        ipAddress = null;
                        while (ipAddress == null) {
                            ipAddress = JOptionPane.showInputDialog("The connection could not be " +
                                    "established. Please make sure Server is running.", "127.0.0.1");
                        }
                    } catch (IOException e1) {
                        ipAddress = null;
                        while (ipAddress == null) {
                            ipAddress = JOptionPane.showInputDialog("The connection could not be " +
                                    "established. Please try again:", "127.0.0.1");
                        }
                    }
                }
            }
        });
    }

    /**
     * 控制器的构造函数。获取与MVC模式相关联的视图和模型。
     *
     * @param v, the view of the MVC.
     * @param m, the model of the MVC.
     */
    public Controller(View v, Model m,RoomController r) {
        view = v;
        model = m;
        rc = r;
        listTimer.setInitialDelay(500);
        listTimer.start();
        viewTimer.setInitialDelay(100);
        styleTimer.setInitialDelay(0);
        rc.start();
    }

    /**
     * 打开与服务器的连接的方法。
     * 打开数据流，以便可以对服务器进行请求，并从服务器读取请求。
     * 读取第一条socket连接，然后保持连接打开。
     *
     * @throws UnknownHostException
     * @throws IOException
     */
    public static void openConnection() throws UnknownHostException, IOException {
        Socket socket = new Socket(ipAddress, 8080);
        outStream = new PrintWriter(socket.getOutputStream(), true);
        inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        madeConnection = true;
        JOptionPane.showMessageDialog(null, "The connection was established succesfully!");
    }

    /**
     * 监听Controller中计时器的 ActionListner。
     * 当计时器启动时，控制器将通知模型更新视图。
     * listTimer - 一个更新服务器上活动文档列表的计时器。
     * viewTimer - 一个用于最新文档更新视图的计时器。
     * styleTimer - 一个定时器，用于更新每个文档最新样式的视图。
     * 在SwingWorker内部执行的，以确保并发性，并防止GUI在延迟时冻结
     */
    public void actionPerformed(final ActionEvent controllerEvent) {
        SwingWorker<?, ?> worker = new SwingWorker<String, Void>() {
            @Override
            public String doInBackground() throws IOException {

                if (controllerEvent.getSource() == listTimer) {
                    String request = makeRequest("list");
                    return request;
                }
                if (controllerEvent.getSource() == viewTimer) {
                    String requestView = makeRequest("view");
                    return requestView;
                }
                if (controllerEvent.getSource() == styleTimer) {
                    String requestStyle = makeRequest("giveStyle");
                    return requestStyle;
                }
                throw new RuntimeException("Problem in timer actionevent.");
            }

            @Override
            public void done() {
                try {
                    /*
                     * 获取服务器上的活动文档的当前列表，并更新用户可以切换到的活动文档树。
                     */
                    if (controllerEvent.getSource() == listTimer) {
                        String listResponse = get();
                        if (!listResponse.contains("There are no existing files on the server.")) {
                            String[] allDocsSplit = listResponse.split(" ");
                            for (String name : allDocsSplit) {
                                if (!model.getInTree().contains(name)) {
                                    model.updateDocTree(name);
                                }
                            }
                        }
                    }
                    /*
                     * 获取当前更新的文档，并向用户呈现其他用户对同一文档所做的更改。
                     */
                    if (controllerEvent.getSource() == viewTimer) {
                        String viewResponse = get();
                        int beforeSet = view.getTextPane().getCaretPosition();
                        model.updateText(viewResponse);
                        int afterSet = view.getTextPane().getCaretPosition();
                        if (beforeSet <= afterSet) {
                            view.getTextPane().setCaretPosition(beforeSet);
                        }
                    }
                    /*
                     * 从服务器获取当前样式，并用用户对文档设置的最新样式更新用户的活动文档。
                     */
                    if (controllerEvent.getSource() == styleTimer) {
                        String styleResponse = get();
                        String[] style = splitStyle(styleResponse);
                        setStylingForFile(style);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }
                } catch (Exception e) {
                }
                ;
            }
        };
        worker.execute();
    }

    /**
     * 处理 GUI 的 Swing工作者线程
     * The worker 将根据用户在GUI上的操作向服务器发出请求。
     * 例如，单击新按钮将生成一个make new文件请求，该请求将在服务器上创建新文档。
     *
     * @param e 由GUI中的一个 JComponents 生成的 ActionEvent 事件。
     */
    public void handleAction(final ActionEvent e) {

        SwingWorker<?, ?> worker = new SwingWorker<String, Void>() {

            @Override
            public String doInBackground() throws IOException {
                /*
                 * 一个新的文档按钮被按下，将生成一个新的文档。
                 * 如果用户没有指定一个新的文件，文件将被命名为 untitled#.txt.
                 * 如果用户确实指定了一个新的文件名，这个名字将被使用。
                 * 如果用户忘记添加.txt后缀，我们将为用户添加后缀。
                 */
                if (e.getSource() == view.getNewDocButton()) {
                    String answer;
                    String newFileRequest = "";
                    answer = JOptionPane.showInputDialog("Insert a file name", "");
                    if (answer != null) {
                        if (answer.equals("")) {
                            newFileRequest = "new";
                        } else {
                            answer = answer.replace(" ", "");
                            if (!answer.endsWith(".txt")&&!answer.endsWith(".java")) {
                                answer += ".txt";
                            }
                            newFileRequest = "new " + answer;
//                            System.out.println(newFileRequest);
                        }
                        if (answer.equals("") || (answer.endsWith(".txt")||answer.endsWith(".java"))
                                && !model.getInTree().contains(answer)) {
                            String documentName = makeRequest(newFileRequest);
                            return documentName;
                        }
                    }
                }

                /*
                 * 按下“switch”按钮。将生成对服务器的文件请求的切换
                 * 如果用户没有指定一个文件进行切换，则什么都不会发生。
                 */
                if (e.getSource() == view.getSwitchButton()) {
                    if (!switchTo.equals("")) {
                        switchTo = switchTo.replace(String.format("%n"), "");
//                        System.out.println("switchTo:"+switchTo);
                        String switchReq = makeRequest("switch " + switchTo);
                        return switchReq;
                    }
                }

                /*
                 * 按下了 replaceOne 按钮，将向服务器发送替换文档中单词的首次出现的请求。
                 */
                if (e.getSource() == view.getReplaceOneButton()) {
                    if (!view.getWordToReplaceText().equals("")
                            && !view.getWordToReplaceText().equals(" ")
                            && !view.getWordToReplaceWithText().equals("")
                            && !view.getWordToReplaceWithText().equals(" ")) {

                        String replaceFrom = view.getWordToReplaceText();
                        String replaceTo = view.getWordToReplaceWithText();
                        String replaceOneReq = makeRequest("replaceOne " + URLEncoder.encode(replaceFrom, "UTF-8")
                                + " " + URLEncoder.encode(replaceTo, "UTF-8"));
                        return replaceOneReq;
                    }
                }

                /*
                 * 按下 replaceAll 按钮，将向服务器发送替换文档中单词的所有出现的请求。
                 */
                if (e.getSource() == view.getReplaceAllButton()) {

                    if (!view.getWordToReplaceText().equals("")
                            && !view.getWordToReplaceText().equals(" ")
                            && !view.getWordToReplaceWithText().equals("")
                            && !view.getWordToReplaceWithText().equals(" ")) {

                        String replaceFrom = view.getWordToReplaceText();
                        String replaceTo = view.getWordToReplaceWithText();
                        String replaceOneReq = makeRequest("replaceAll " + URLEncoder.encode(replaceFrom, "UTF-8")
                                + " " + URLEncoder.encode(replaceTo, "UTF-8"));
                        return replaceOneReq;
                    }
                }

                /*
                 * 按下 plain 按钮，将向服务器发送返回原始样式的请求。
                 */
                if (e.getSource() == view.getplainButton()) {
                    fontStyle = 0;
                    fontName = 1;
                    fontSize = 12;
                    color = 0;
                    String stylePlainReequest = makeRequest("style " + fontName + " " + fontStyle
                            + " " + fontSize + " " + color);
                    return stylePlainReequest;
                }

                /*
                 *  bold 按钮被按下，粗体请求将被发送到服务器。
                 */
                if (e.getSource() == view.getBoldButton()) {
                    if (fontStyle != 1) {
                        fontStyle = 1;
                    } else {
                        fontStyle = 0;
                    }
                    String styleBoldRequest = makeRequest("style " + fontName + " " + fontStyle
                            + " " + fontSize + " " + color);
                    return styleBoldRequest;
                }
                /*
                 * 按下 italic 按钮，将向服务器发送对文本进行斜体显示的请求
                 */
                if (e.getSource() == view.getItalicButton()) {
                    if (fontStyle != 2) {
                        fontStyle = 2;
                    } else {
                        fontStyle = 0;
                    }
                    String styleItalicRequest = makeRequest("style " + fontName + " " + fontStyle
                            + " " + fontSize + " " + color);
                    return styleItalicRequest;
                }
                /*
                 * 改变字体大小，从列表中选择了一个新的字体大小。
                 * 将更新字体大小的请求发送到服务器。
                 */
                if (e.getSource() == view.getFontSize()) {
                    fontSize = (Integer) view.getFontSize().getSelectedItem();
                    String styleSizeRequest = makeRequest("style " + fontName + " " + fontStyle
                            + " " + fontSize + " " + color);
                    return styleSizeRequest;
                }
                /*
                 * 更改字体类型，从列表中选择新字体。
                 * 将更新字体类型的请求发送到服务器。
                 */
                if (e.getSource() == view.getFontName()) {
                    fontName = view.getFontName().getSelectedIndex();
                    String styleNameRequest = makeRequest("style " + fontName + " " + fontStyle
                            + " " + fontSize + " " + color);
                    return styleNameRequest;
                }
                /*
                 *  更改颜色，从列表中挑选出一种新的颜色。
                 *  将更新颜色的请求发送到服务器。
                 */
                if (e.getSource() == view.getColorOptions()) {
                    color = view.getColorOptions().getSelectedIndex();
                    String styleColorRequest = makeRequest("style " + fontName + " " + fontStyle
                            + " " + fontSize + " " + color);
                    return styleColorRequest;
                }
                /*
                 * 按下 copy 按钮，用户选择的文本将被保存到剪贴板。
                 */
                if (e.getSource() == view.getCopyButton()) {
                    if (clipBoard != null) {
                        clipBoard = "";
                    }
                    int endLocation = view.getTextPane().getSelectionEnd();
                    int startLocation = view.getTextPane().getSelectionStart();
                    try {
                        String textToCopy = view.getTextPane().getText(startLocation, endLocation - startLocation);
                        return textToCopy;
                    } catch (BadLocationException e1) {
                        throw new RuntimeException("BadLocationException in the copyButton doInBackGround.");
                    }
                }
                /*
                 * 按下 cut 按钮，用户选择的文本将被保存到剪贴板，并将此文本的删除请求发送到服务器。
                 */
                if (e.getSource() == view.getCutButton()) {
                    if (clipBoard != null) {
                        clipBoard = "";
                    }
                    int endLocation = view.getTextPane().getSelectionEnd();
                    int startLocation = view.getTextPane().getSelectionStart();
                    try {
                        clipBoard = view.getTextPane().getText(startLocation, endLocation - startLocation);
                        return makeRequest("delete " + startLocation + " " + endLocation);
                    } catch (BadLocationException e1) {
                        throw new RuntimeException("BadLocationException in the cutButton doInBackGround.");
                    }
                }
                /*
                 * 按下 paste 按钮，存储在剪贴板中的文本将被插入到当前插入符号位置。
                 * 插入请求将被发送到服务器。如果没有文本被保存到剪贴板，粘贴不会做任何事情。
                 */
                if (e.getSource() == view.getPasteButton()) {
                    if (clipBoard != null) {
                        try {
                            int pos = view.getTextPane().getCaretPosition();
                            String pasteRequest = makeRequest("insert " + pos + " " + clipBoard);
                            return pasteRequest;
                        } catch (IOException e) {
                            throw new RuntimeException("IOException in the pasteButton doInBackground.");
                        }

                    }
                }
//                System.out.println("did not do anything");
                return "did not do anything";
            }

            @Override
            /*
             * done()获取在后台执行的操作的值，并使控制器能够更新model，从而相应地更新view。（基于MVC模型）。
             */
            public void done() {
                try {
                    /*
                     * 服务器创建了一个新文件之后。控制器将用新文件更新模型。
                     * 该文档将被添加到文档列表树中，将给用户一个空白文档，并将从服务器监听样式和内容的更改。
                     */
                    if (e.getSource() == view.getNewDocButton()) {
                        String response = get().trim();
                        if (!response.contains("did not do anything") && !response.equals("Invalid Request.")) {
                            model.updateText("");
                            model.updateViewTitle(response);
                            model.updateDocTree(response);
                            viewTimer.start();
                            styleTimer.restart();
                            model.releaseScreen(true);
                        }
                    }

                    /*
                     * 服务器切换用户文档后。 controller将更新关于交换文档的model。
                     * model将更新view，并将向用户呈现切换文档的内容。
                     * controller将侦听来自服务器的关于此文档的内容或样式更改的更新。
                     */
                    if (e.getSource() == view.getSwitchButton()) {
                        String switchResponse = get();
//                        System.out.println("Response:"+switchResponse);
                        if (model.getInTree().size() != 0) {
                            viewTimer.start();
                            styleTimer.restart();
                        }
                        if (!switchResponse.contains("did not do anything") && !switchResponse.equals("Invalid Request.")) {
                            model.updateViewTitle(switchTo);
                            model.updateText(switchResponse);
                            model.releaseScreen(true);
                        }
                    }

                    /*
                     * 发生替换一次之后，服务器向controller返回一个带有替换单词的新文档。
                     * controller将通知model更新view。
                     */
                    if (e.getSource() == view.getReplaceOneButton()) {
                        String replaceOneResponse = get();
                        if (!replaceOneResponse.contains("did not do anything")) {
                            model.updateText(replaceOneResponse);
                        }
                    }

                    /*
                     * 发生替换所有被替代词之后，服务器向controller返回一个带有替换单词的新文档。
                     * controller将通知model更新view。
                     */
                    if (e.getSource() == view.getReplaceAllButton()) {
                        String replaceAllResponse = get();
                        if (!replaceAllResponse.contains("did not do anything")) {
                            model.updateText(replaceAllResponse);
                        }
                    }

                    /*
                     * 按下 plain 按钮后，服务器向控制器返回当前活动文档样式的更新。
                     * 控制器将通知模型使用原始的平面样式更新视图。
                     */
                    if (e.getSource() == view.getplainButton()) {
                        String plainResponse = get();
                        String[] plainStyle = splitStyle(plainResponse);
                        setStylingForFile(plainStyle);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }

                    /*
                     * 按下 bold 按钮后，服务器向控制器返回关于新样式的更新。
                     * 控制器将通知模型以用粗体文本更新视图。
                     */
                    if (e.getSource() == view.getBoldButton()) {
                        String boldResponse = get();
                        String[] boldStyle = splitStyle(boldResponse);
                        setStylingForFile(boldStyle);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }

                    /*
                     * 按下 italic 按钮后，服务器向控制器返回关于新样式的更新。
                     * 控制器将通知模型以用斜体文本更新视图。
                     */
                    if (e.getSource() == view.getItalicButton()) {
                        String italicResponse = get();
                        String[] italicStyle = italicResponse.split(" ");
                        setStylingForFile(italicStyle);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }
                    /*
                     * 在选择字体大小之后，服务器向控制器返回关于新样式的更新。
                     * 控制器将更新模型以更新具有新字体大小的视图。
                     */
                    if (e.getSource() == view.getFontSize()) {
                        String sizeResponse = get();
                        String[] sizeStyle = splitStyle(sizeResponse);
                        setStylingForFile(sizeStyle);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }
                    /*
                     * 在选择字体名称之后，服务器向控制器返回关于新样式的更新。
                     * 控制器将通知模型以用新字体更新视图。
                     */
                    if (e.getSource() == view.getFontName()) {
                        String nameResponse = get();
                        String[] nameStyle = splitStyle(nameResponse);
                        setStylingForFile(nameStyle);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }
                    /*
                     * 在选择颜色之后，服务器向控制器返回关于新样式的更新。
                     * 控制器将通知模型以更新具有新颜色的视图。
                     */
                    if (e.getSource() == view.getColorOptions()) {
                        String colorResponse = get();
                        String[] colorStyle = splitStyle(colorResponse);
                        setStylingForFile(colorStyle);
                        model.setViewStyle(fontName, fontStyle, fontSize, color);
                    }
                    /*
                     * 单击 copy 后，视图中的文本将保存到剪贴板。
                     */
                    if (e.getSource() == view.getCopyButton()) {
                        clipBoard = get();
                    }
                    /*
                     * 在执行 paste 服务器之后，向控制器返回文档的新更新，控制器将更新模型，该模型将更新视图以呈现新修改的文档。
                     */
                    if (e.getSource() == view.getPasteButton()) {
                        String pasteReq;
                        try {
                            pasteReq = get();
                            int beforeSet = view.getTextPane().getCaretPosition();
                            try {
                                if (!pasteReq.contains("did not do anything") && !pasteReq.equals("Invalid Request.")) {
                                    model.updateText(pasteReq);
                                }
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException("Bad encoding in paste.");
                            }
                            int afterSet = view.getTextPane().getCaretPosition();
                            if (beforeSet <= afterSet) {
                                view.getTextPane().setCaretPosition(beforeSet);
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException("InterruptedException in pasteButton done().");
                        } catch (ExecutionException e) {
                            throw new RuntimeException("ExecutionException in the pasteButton done().");
                        }
                    }
                    /*
                     * 在服务器执行 cut 之后，向控制器返回文档的新更新，控制器将更新模型，
                     * 该模型将更新视图以呈现新的修改文档。
                     */
                    if (e.getSource() == view.getCutButton()) {
                        String cutReq;

                        try {
                            cutReq = get();
                            int beforeSet = view.getTextPane().getCaretPosition();
                            try {
                                model.updateText(cutReq);
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException("Bad encoding in cut.");
                            }
                            int afterSet = view.getTextPane().getCaretPosition();
                            if (beforeSet <= afterSet) {

                                view.getTextPane().setCaretPosition(beforeSet);
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException("InterruptedException in the cutButton done().");
                        } catch (ExecutionException e) {
                            throw new RuntimeException("ExecutionException in the cutButton done().");
                        }
                    }

                } catch (Exception exc) {
                    throw new RuntimeException("Problem in the cutButton done() method.");
                }
            }
        };
        worker.execute();
    }

    /**
     * 处理插入更新的方法。在文档中更新插入。
     * 控制器将通知服务器关于文档的更新。
     * 服务器将在插入之后向客户端返回关于新文档的更新。
     * 控制器将更新将拥有该视图的模型。
     *
     * @param pos   - 指示文本插入的位置。
     * @param text- 在给定位置插入的内容
     */
    public void handleInsertUpdate(final int pos, final String text) {
        SwingWorker<?, ?> worker = new SwingWorker<String, Void>() {
            /*
             * insert方法是在swing worker的用户事件调度线程EDT线程中执行的，以确保并发性，并防止GUI由于延迟而冻结。
             */

            //处理一个swingworker实例的最后计算结果，当这个函数被执行完后，
            // 说明这个线程的所有顺序执行的程序已经执行完了。
            @Override
            public String doInBackground() throws IOException {
                try {
                    String getFromServer = makeRequest("insert " + pos + " " + text);
                    return getFromServer;
                } catch (IOException e) {
                    throw new RuntimeException("Problem in handleInserUpdate swingworker" +
                            "in controller class.");
                }
            }

            //doInBackground方法完成后，在EDT中被执行。
            @Override
            public void done() {
                String insertReq;

                try {
                    insertReq = get();//等待doInBackground​计算完成，返回doInBackground​的返回值。
                    int beforeSet = view.getTextPane().getCaretPosition();
                    try {
                        model.updateText(insertReq);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Bad encoding in insertRequest in controller.");
                    }
                    int afterSet = view.getTextPane().getCaretPosition();

                    if (beforeSet <= afterSet) {
                        //设置光标位置
                        view.getTextPane().setCaretPosition(beforeSet);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("InterruptedException in the insert request done() method.");
                } catch (ExecutionException e) {
                    throw new RuntimeException("ExecutionException in the insert request done() method.");
                }
            }
        };
        worker.execute();
    }

    /**
     * 处理删除更新的方法。文档中的删除编辑。
     * 控制器将通知服务器关于文档的更新。
     * 服务器将在插入之后向客户端返回关于新文档的更新。
     * 控制器将更新将更新视图的模型。
     *
     * @param startLocation  - 开始删除的位置
     * @param startLocation- 结束删除的位置
     */
    public void handleDeleteUpdate(final int startLocation, final int endLocation) {
        SwingWorker<?, ?> worker = new SwingWorker<String, Void>() {
            @Override
            /*
             * 删除方法是在swing worker的用户事件调度线程EDT线程中执行的，以确保并发性，并防止GUI由于延迟而冻结。
             */
            public String doInBackground() throws IOException {
                try {
                    String deleteRequest = makeRequest("delete " + startLocation + " " + endLocation);
                    return deleteRequest;
                } catch (IOException e) {
                    throw new RuntimeException("Problem in the handle delete update doInBackGround().");
                }
            }

            @Override
            public void done() {
                String deleteRequest;

                try {
                    deleteRequest = get();
                    int beforeSet = view.getTextPane().getCaretPosition();
                    try {
                        model.updateText(deleteRequest);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Bad encoding in the delete request in the controller.");
                    }
                    int afterSet = view.getTextPane().getCaretPosition();
                    if (beforeSet <= afterSet) {

                        view.getTextPane().setCaretPosition(beforeSet);
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException("InterruptedException in the delete request done() method.");
                } catch (ExecutionException e) {
                    throw new RuntimeException("ExecutionException in the insert request done() method.");
                }
            }
        };
        worker.execute();

    }

    /**
     * 获取用户通过 open 按钮打开的文件的方法。
     * 该方法将通知服务器为该文件创建新文档。
     * 将 fileReadrs 读取的文本插入到新文档中。
     * 将用户切换到新文档。
     * 如果文件已经在列表中，则控制器将不创建该文件。
     *
     * @param fileName - 用户打开的文件的名称。
     * @param fileRead - 用户打开的文件的内容
     */
    public void setOpenFile(final String fileName, final String fileRead) {
        SwingWorker<?, ?> worker = new SwingWorker<String, Void>() {
            @Override
            public String doInBackground() throws IOException {
                /*
                 *  Open 是在swing worker线程中执行的，以确保并发性，并防止GUI由于延迟而冻结。
                 */
                makeRequest("new " + fileName); // 创建新文件
                String openResponse = makeRequest("switch " + fileName); // 切换到新文件
                makeRequest("insert " + "0 " + fileRead); // 在打开的文件中插入文本
                return openResponse;
            }

            @Override
            public void done() {
                String openFileResponse = null;
                try {
                    openFileResponse = get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("InterruptedException in open file done() " +
                            "method. Problem opening file.");
                } catch (ExecutionException e) {
                    throw new RuntimeException("ExecutionException in the open file " +
                            "done() method. Problem opening file.");
                }
                if (model.getInTree().size() != 0) {
                    viewTimer.start();
                    styleTimer.restart();

                }
                if (!openFileResponse.contains("did not do anything") && !openFileResponse.equals("Invalid Request.")) {
                    model.updateViewTitle(switchTo);
                    try {
                        model.updateText(openFileResponse);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Bad encoding in the open file in the controller class.");
                    }
                    model.releaseScreen(true);
                }
            }
        };
        worker.execute();
    }

    /**
     * 向服务器发出请求的方法。当用户触发需要更新服务器的操作时使用。
     * 参数是一个String类型，它应该是从用户到服务器的请求类型之一，应该遵循正确的协议和语法。
     * <p>
     * 然而，如果请求没有遵循协议，服务器将相应地作出响应。
     * <p>
     * 返回来自服务器的响应。
     *
     * @param userInput, 请求
     * @return String, 服务器的响应
     * @throws IOException
     */
    public synchronized static String makeRequest(String userInput) throws IOException {
        String fromUser = URLEncoder.encode(userInput, "UTF-8");
        String fromUserReplace = userInput;

        String returnString = "";
        String fromServer = null;

        if (userInput.startsWith("replace")) {
            try {
                outStream.println(fromUserReplace);
                while (!(fromServer = inStream.readLine()).equals("EOF")) {

                    if (!fromServer.equals("EOF")) {
                        returnString += fromServer;
                    }
                    if (fromServer.equals("exit")) {
                        break;
                    }
                }
            } catch (Exception er) {
                throw new RuntimeException("Problem in makeRequest method.");
            }
            fromUser = null;

        } else {
            try {
                outStream.println(fromUser);
                while (!(fromServer = inStream.readLine()).equals("EOF")) {

                    if (!fromServer.equals("EOF")) {
                        returnString += fromServer;
                    }
                    if (fromServer.equals("exit")) {
                        break;
                    }
                }
            } catch (Exception er) {
                throw new RuntimeException("Problem in makeRequest method.");
            }
            fromUser = null;
        }
        return URLDecoder.decode(returnString, "UTF-8");
    }

    /**
     * 一种将样式字符串从服务器分割为存储在服务器上的样式化映射的4个关键组件的方法。
     *
     * @param styleFromServer - 表示当前文档样式的字符串。
     * @return 由4个键组成的String数组，该数组允许模型使用新的样式更新视图。
     */
    private String[] splitStyle(String styleFromServer) {
        String[] style = styleFromServer.split(" ");
        return style;

    }

    /**
     * 给定一组样式为字符串的键，将这些键转换成整数。
     * 该模型将使用整数来检测其 model 中的正确样式。
     * 该方法是必需的，因为流和编码/解码需要一个字符串。
     *
     * @param stylingToSet - a String array of styling keys
     */
    private void setStylingForFile(String[] stylingToSet) {
        fontName = Integer.parseInt(stylingToSet[0]);
        fontStyle = Integer.parseInt(stylingToSet[1]);
        fontSize = Integer.parseInt(stylingToSet[2]);
        color = Integer.parseInt(stylingToSet[3]);
    }

    /**
     * 设置用户希望切换到的文档名称
     *
     * @param switchString - 用户要求切换到的文档名。
     */
    public void setSwitchTo(String switchString) {
        switchTo = switchString;
    }

    /**
     * 一种设置服务器连接的IP地址的方法。允许控制器向指定服务器打开套接字
     *
     * @param ip - 将要使用服务器socket的IP地址打开到
     */
    public void setIPAddress(String ip) {
        ipAddress = ip;
    }

    /**
     * 获取服务器连接所设置的IP地址的方法。
     * 主要用于测试
     */
    public String getIPAddress() {
        return ipAddress;
    }


    /**
     * 指示到服务器的连接是否成功
     *
     * @return true- 如果建立到服务器的连接为true，否则为false。
     */
    public boolean getConnectionMade() {
        return madeConnection;
    }


}