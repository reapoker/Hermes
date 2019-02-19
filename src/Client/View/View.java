package Client.View;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import Client.Controller.Controller;
import Client.Controller.RoomController;
import Client.Model.Model;
import Document.FileReaders;
import Document.FileWriters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
/*
 * 线程安全性参数
 *
 * 方法setViewText是线程安全的，因为它使用的是JTextArea内置方法中的setText方法
 * 此方法是线程安全的。
 *
 * 即使swing组件不是线程安全的，每个客户机都有自己的GUI，它是专用于此特定客户端的单独线程。
 * 因为所有的JComponent都是在用户唯一的swing事件分派线程上创建的。
 * 我们可以要求限制线程安全。这意味着所有组件都是不可访问的。
 * 其他用户，只有本地用户可以访问它们。
 * 此外，竞争机制由控制器及其工作人员和服务器决定，用来防止不合逻辑的操作顺序。
 * 因此，即使竞争条件会发生，控制器和服务器将解决它。
 */
/**
 * View类创建客户端的图形用户界面。
 * 该类通过以下方式创建GUI：
 *      (a) 添加所有必要的组件
 *      (b) 建立GUI的起始状态。
 *      (c) 注册动作侦听器并通知控制器事件发生。
 * @author reapoker
 */
@SuppressWarnings("serial")
public class View extends JFrame implements ActionListener {

    /*
     * JComponents类必要变量
     */
    private RSyntaxTextArea textPane;
    private JTree documentTree;
    private JTextField wordToReplaceField, wordToReplaceWithField;
    private JComboBox<Integer> fontSize;
    private JComboBox<String> fontName;
    private JComboBox<String> colorOptions;
    private JButton boldButton, italicButton, replaceButton,newDocButton,switchButton, plainButton,
    saveFileButton, openFileButton, copyButton, cutButton, pasteButton, replaceOneButton, replaceAllButton;
    boolean setText = false;
    private  DefaultMutableTreeNode top = new DefaultMutableTreeNode("Active Documents");
    private String switchTo = "";


    /*
     * 为JCompontents变量赋值
     */

    //初始化welcome信息
    private final String initialHello = "欢迎使用 Hermes ，这是一款即时协同编辑软件" + String.format("%n")+ String.format("%n")
            + "版本：v 0.1.1" + String.format("%n")
            + "作者：reapoker" + String.format("%n")
            + String.format("%n") + "请通过点击 new 按钮，" +
            " 或选中右侧可编辑文件点击 switch 按钮来释放文本编辑域！" + String.format("%n")+
            String.format("%n") + "欢迎使用Hermes的以下功能：" + String.format("%n")+
            String.format("%n") + "Styling 风格选项：" + String.format("%n") +
            "       1) Plain - 使用默认风格。" +String.format("%n") +
            "       2) Bold - 使用粗体文本。" + String.format("%n") +
            "       3) Italic - 使用斜体文本。" + String.format("%n") +
            "       4) Size - 改变字体大小。" + String.format("%n") +
            "       5) Font - 改变字体风格。" + String.format("%n") +
            "       6) Color - 改变字体颜色。"  + String.format("%n") +
            String.format("%n") + "Editing 编辑选项：" + String.format("%n") +
            "       1) Copy - 复制一段文本。" + String.format("%n") +
            "       2) Cut - 剪切一段文本。" + String.format("%n") +
            "       3) Paste - 黏贴一段文本。" + String.format("%n") +
            "       4) Replace - 替换首次出现的一个替换选项" +
            " ('replace') 或者所有的替换选项 ('replace all')。" +String.format("%n") +
            String.format("%n") + "Files 文件选项：" + String.format("%n") +
            "       1) New - 创建一个新文件，目前支持txt和java格式。" + String.format("%n") +
            "       2) Switch - 选择另一个文件打开。" + String.format("%n") +
            "       3) Save - 将当前文档保存到本地。" + String.format("%n") +
            "       4) Open - 从本地导入一个已有的文件。"  + String.format("%n") +
            String.format("%n")+"联系我们："+
            String.format("%n")+"Email reapoker@outlook.com"+
            String.format("%n")+"QQ 936771816"+String.format("%n")+
            String.format("%n")+"Have a good time!";

    /*
     * Room必要的组件变量
     */

    private JTextArea textArea;//聊天消息记录
    private JTextArea txt_msg;//发送消息的文本区
    private JTextField txt_port;//端口
    private JTextField txt_hostIP;//ip地址
    private JTextField txt_name;//用户名
    private JButton btn_start;//连接按钮
    private JButton btn_stop;//断开按钮
    private JButton btn_send;//发送消息的按钮
    private JButton btn_sendFile;//发送消息的按钮

    private JPanel topPanel;//顶部编辑选项面板
    private JPanel downPanel;//下部文本域的面板

    private JPanel southPanel;//发送面板
    private JPanel panel;//两个发送按钮的面板
    private JScrollPane rightScroll;//右侧聊天消息滚动版面
    private JScrollPane leftScroll;//左侧用户列表滚动面板
    private JScrollPane msgScroll;//发送消息滚动面板
    private JSplitPane centerSplit;//中间编辑框的分割面板
    private JSplitPane rightSplit;//右侧聊天室的分割面板
    private JSplitPane leftSplit;//左侧用户列表和文档树的分割面板

    private DefaultListModel<String> listModel = new DefaultListModel<String>();//用户列表模型
    private JList<String> userList = new JList<String>(listModel);//用户列表

    private String currentUser;

    private String ServerIP, ServerPORT;

    private String chatUser;

    private boolean isGroup;

    // private List<User> onlineUsers;

    private static String[] DEFAULT_FONT  = new String[] {
            "Table.font"
            , "TableHeader.font"
            , "CheckBox.font"
            , "Tree.font"
            , "Viewport.font"
            , "ProgressBar.font"
            , "RadioButtonMenuItem.font"
            , "ToolBar.font"
            , "ColorChooser.font"
            , "ToggleButton.font"
            , "Panel.font"
            , "TextArea.font"
            , "Menu.font"
            , "TableHeader.font"
            , "OptionPane.font"
            , "MenuBar.font"
            , "Button.font"
            , "Label.font"
            , "PasswordField.font"
            , "ScrollPane.font"
            , "MenuItem.font"
            , "ToolTip.font"
            , "List.font"
            , "EditorPane.font"
            , "Table.font"
            , "TabbedPane.font"
            , "RadioButton.font"
            , "CheckBoxMenuItem.font"
            , "TextPane.font"
            , "PopupMenu.font"
            , "TitledBorder.font"
            , "ComboBox.font"
    };

    //颜色菜单的颜色名称。
    private final String[] colorsName = {"<html> <font color=#000000>black</font>",
            "<html><font color=#0000FF>blue</font>","<html> <font color=#808080>gray</font>",
            "<html><font color=#008000>green</font>","<html><font color=#FFA500>orange</font>",
            "<html> <font color=#FFC0CB>pink</font>", "<html> <font color=#FF0000>red</font>",
            "<html> <font color=#FFD700>yellow</font>"
    };

    //大小菜单的大小
    private static final Integer[] sizes = {
        8, 9, 10, 12, 14, 16, 18, 20, 24, 26, 28, 36, 48, 72
    };
    //字体菜单的字体
    private static final String[] fonts = {
        "Arial","Courier New","Georgia","Times New Roman","Verdana"
    };

    /*
     *  MVC类变量
     */
    Model model = new Model(this);
    RoomController Client = new RoomController(this);
    Controller controller = new Controller(this, model, Client);

    /*
     * 文件导入导出的变量
     */
    FileReaders fr = new FileReaders();
    FileWriters fw = new FileWriters();
    static private String fileRead = null;
    static private String fileName = null;
    private JFileChooser fileChooser = new JFileChooser();

    /*
     * 正则匹配 ip 和 端口 值
     */
    private static String ipAddress = null;
    private final String ValidIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
    private final String ValidHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

    /**
     * GUI客户端的构造函数。执行三项主要任务：
     *      1) 通过将JComponents添加到面板来构建GUI。
     *      2) 为最终用户的操作添加动作监听器。
     *      3) 设置GUI的初始状态。
     */
    public View() {
        super("Hermes"); // 为GUI设置初始标题

        initGUI();
        AddListeners();
    }

    /**
     * 初始化GUI界面
     */
    private void initGUI(){
        Toolkit toolkit = getToolkit();
        setIconImage(toolkit.getImage("icons/Hermes3.jpg"));

        //关闭窗口在不活动时的半透明效果，设置此开关量为false 即表示关闭之
        UIManager.put("RootPane.setupButtonVisible", false);
//        UIManager.setLookAndFeel();

        int screenWidth = toolkit.getScreenSize().width;
        int screenHeight = toolkit.getScreenSize().height;
        setLocation((screenWidth - getWidth()) / 50,
                (screenHeight - getHeight()) / 80);//在屏幕中设置显示的位置

        // 调整默认字体
        for (int i = 0; i < DEFAULT_FONT.length; i++)
            UIManager.put(DEFAULT_FONT[i], new Font("Microsoft YaHei UI", Font.PLAIN, 15));


        /*
         * 请求一个有效的IP地址，如果IP无效或连接失败，将提示用户输入另一个IP地址。
         */

        btn_start = new JButton("连接");
        btn_stop = new JButton("退出");

        //jbutton事件绑定必须在JOptionPane.showOptionDialog()之前，不然这个对话框会阻塞，方法不会立即执行
        btn_start.addActionListener(this);
        btn_stop.addActionListener(this);
        JButton[] btn = {btn_start, btn_stop};

//        JPanel btn = new JPanel();
//        btn.setLayout(new BoxLayout(btn, BoxLayout.X_AXIS));
        txt_hostIP = new JTextField("127.0.0.1");
        txt_name = new JTextField();
        txt_hostIP.setPreferredSize(new Dimension (150, 1));
        txt_name.setPreferredSize(new Dimension (150, 1));

        JPanel myPanel = new JPanel();
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
        myPanel.add(new JLabel("服务器IP:"), "North");
        myPanel.add(txt_hostIP);
        myPanel.add(new JLabel("用户名:"), "South");
        myPanel.add(txt_name);
//        myPanel.add(btn);
        while ((ipAddress == null)||(currentUser ==null)) {
            int res = JOptionPane.showOptionDialog(null, myPanel,
                    "Insert IP address and hostname", 0,
                    JOptionPane.INFORMATION_MESSAGE, new ImageIcon("icons/Hermes.jpg"), btn, btn[0]);
            if (res == JOptionPane.OK_OPTION) {
                while ((!ipAddress.matches(ValidIpAddressRegex))
                        && (!ipAddress.matches(ValidHostnameRegex))
                        && res == JOptionPane.OK_OPTION
                ) {
                    res = JOptionPane.showConfirmDialog(null, myPanel,
                            "Invalid ip address or hostname,please again.",JOptionPane.OK_CANCEL_OPTION);
                }
            }

        }
        controller.setIPAddress(ipAddress); // 设置Hermes服务器的IP地址或服务器名

        /*
         * 初始化 JTextArea 和 JTree
         */
        textPane = new RSyntaxTextArea();//JTextArea()用来编辑多行的文本
        textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textPane.setCodeFoldingEnabled(true);
        textPane.setLineWrap(true);//设置行过长的时自动换行。
        textPane.setEditable(false);//设置不可编辑
        textPane.setWrapStyleWord(true);//设置单词过长的时把长单词移到下一行。
        textPane.setText(initialHello);
        documentTree=new JTree();//创建文本树


        /*
         * 初始化 Room 中的配置
         */
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setForeground(Color.gray);

        txt_msg = new JTextArea();
//        txt_port = new JTextField("8080");
//        txt_hostIP = new JTextField();
//        txt_name = new JTextField("");
//        btn_start = new JButton("连接");
//        btn_stop = new JButton("退出");
        btn_send = new JButton("发送");
        btn_sendFile = new JButton("发送文件");

//        listModel = new DefaultListModel<String>();
//        userList = new JList<String>(listModel);


        topPanel = new JPanel();
        downPanel = new JPanel();
        southPanel = new JPanel();
        panel = new JPanel();

        /*
         * Replace screen
         */
        wordToReplaceField = new JTextField(20);
        wordToReplaceWithField = new JTextField(20);
        replaceOneButton = new JButton("Replace");
        replaceAllButton = new JButton("Replace All");

        /*
         * 初始化 JButtons
         */
        //加粗
        ImageIcon boldPic = new ImageIcon("icons/bold.png");
        boldButton = new JButton(boldPic);
        boldButton.setMaximumSize(boldButton.getPreferredSize());
        //斜体
        ImageIcon italicPic = new ImageIcon("icons/italic.png");
        italicButton = new JButton(italicPic);
        italicButton.setMaximumSize(italicButton.getPreferredSize());
        //朴素风格
        ImageIcon plainPic = new ImageIcon("icons/plain.png");
        plainButton = new JButton(plainPic);
        plainButton.setMaximumSize(plainButton.getPreferredSize());
        //替换
        ImageIcon replace = new ImageIcon("icons/replace.png");
        replaceButton = new JButton(replace);
        replaceButton.setMaximumSize(replaceButton.getPreferredSize());
        //保存文件
        ImageIcon savePic = new ImageIcon("icons/save.png");
        saveFileButton = new JButton(savePic);
        saveFileButton.setMaximumSize(saveFileButton.getPreferredSize());
        //打开文件
        ImageIcon openPic = new ImageIcon("icons/open.png");
        openFileButton = new JButton(openPic);
        openFileButton.setMaximumSize(openFileButton.getPreferredSize());
        //复制
        ImageIcon copyPic = new ImageIcon("icons/copy.png");
        copyButton = new JButton(copyPic);
        copyButton.setMaximumSize(copyButton.getPreferredSize());
        //剪切
        ImageIcon cutPic = new ImageIcon("icons/cut.png");
        cutButton = new JButton(cutPic);
        cutButton.setMaximumSize(cutButton.getPreferredSize());
        //粘贴
        ImageIcon pastePic = new ImageIcon("icons/paste.png");
        pasteButton = new JButton(pastePic);
        pasteButton.setMaximumSize(pasteButton.getPreferredSize());
        //新建
        ImageIcon newFilePic = new ImageIcon("icons/new.png");
        newDocButton = new JButton(newFilePic);
        //选择
        ImageIcon switchPic = new ImageIcon("icons/switch.png");
        switchButton = new JButton(switchPic);

        /*
         * 初始化 JComboBox 显示编辑选项
         */
        fontSize = new JComboBox<>(sizes);
        fontSize.setSelectedIndex(3);//选择索引3处的项。
        fontSize.setMaximumSize(fontSize.getPreferredSize());
        fontSize.setPreferredSize(new Dimension(80,7));
        fontName = new JComboBox<>(fonts);
        fontName.setSelectedIndex(3);
        fontName.setMaximumSize(fontName.getPreferredSize());
        fontName.setPreferredSize(new Dimension(80,7));
        colorOptions = new JComboBox<>(colorsName);
        colorOptions.setMaximumSize(colorOptions.getPreferredSize());
        colorOptions.setPreferredSize(new Dimension(80,7));

        /*
            创建JTree的根并向其追加文档。
            创建一个允许一次选择的树。
         */
        documentTree = new JTree(top);
        documentTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        /*
         * 禁用所有JTextArea，JButtons和JComboBoxes：
         * 这将防止用户不在实际文档上尝试编辑。
         * 解锁new按钮和switc按钮，单击它们将释放所有组件。
         */
        plainButton.setEnabled(false);
        boldButton.setEnabled(false);
        italicButton.setEnabled(false);
        copyButton.setEnabled(false);
        cutButton.setEnabled(false);
        pasteButton.setEnabled(false);
        replaceButton.setEnabled(false);
        saveFileButton.setEnabled(false);
        openFileButton.setEnabled(false);
        fontSize.setEnabled(false);
        fontName.setEnabled(false);
        colorOptions.setEnabled(false);

        /*
         * 创建图形用户界面的布局
         */

        //顶部编辑选项
        topPanel.setBorder(new TitledBorder("编辑选项"));

        GridBagLayout gridBagLayout = new GridBagLayout();//网格组布局管理器
        GridBagConstraints constraints = new GridBagConstraints();

        topPanel.setLayout(gridBagLayout);//采用网格布局


        constraints.fill = GridBagConstraints.BOTH;//当组件不能填满其格时，通过 insets来指定四周（即上下左右）所留空隙

        //编辑选项的按钮
        constraints.weightx = 3.0;// 当窗口放大时，长度随之变化
        constraints.insets = new Insets(0, 6, 0, 6);
        gridBagLayout.setConstraints(newDocButton, constraints);
        topPanel.add(newDocButton);
        gridBagLayout.setConstraints(switchButton, constraints);
        topPanel.add(switchButton);
        gridBagLayout.setConstraints(plainButton, constraints);
        topPanel.add(plainButton);
        gridBagLayout.setConstraints(boldButton, constraints);
        topPanel.add(boldButton);
        gridBagLayout.setConstraints(italicButton, constraints);
        topPanel.add(italicButton);
        constraints.insets = new Insets(15, 6, 15, 6);
        gridBagLayout.setConstraints(fontSize, constraints);
        topPanel.add(fontSize);
        gridBagLayout.setConstraints(fontName, constraints);
        topPanel.add(fontName);
        gridBagLayout.setConstraints(colorOptions, constraints);
        topPanel.add(colorOptions);
        constraints.insets = new Insets(0, 6, 0, 6);
        gridBagLayout.setConstraints(copyButton, constraints);
        topPanel.add(copyButton);
        gridBagLayout.setConstraints(cutButton, constraints);
        topPanel.add(cutButton);
        gridBagLayout.setConstraints(pasteButton, constraints);
        topPanel.add(pasteButton);
        gridBagLayout.setConstraints(replaceButton, constraints);
        topPanel.add(replaceButton);
        gridBagLayout.setConstraints(saveFileButton, constraints);
        topPanel.add(saveFileButton);
        gridBagLayout.setConstraints(openFileButton, constraints);
        topPanel.add(openFileButton);



        //下部文本域
        downPanel.setLayout(new  BorderLayout());//边界布局管理器

        //右侧聊天室
        rightScroll = new JScrollPane(textArea);//聊天消息记录的滚动文本框
        rightScroll.setBorder(new TitledBorder("聊天消息"));//设置边框

        southPanel = new JPanel(new BorderLayout());//消息发送区
        southPanel.setBorder(new TitledBorder("发送"));//边框设置
        msgScroll = new JScrollPane(txt_msg);//发送消息的滚动文本框
        southPanel.add(msgScroll, "Center");//位置设置
        panel = new JPanel();//发送和发送文件按钮
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        btn_send.setMargin(new Insets(5, 20, 5, 20));
        btn_sendFile.setMargin(new Insets(5, 20, 5, 20));
        panel.add(btn_sendFile);
        panel.add(btn_send);
        southPanel.add(panel, "South");

        rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightScroll, southPanel);
        rightSplit.setDividerLocation(350);//水平放置

        //左侧用户列表和文档树
        leftScroll = new JScrollPane(userList);//用户列表
        leftScroll.setBorder(new TitledBorder("在线用户"));
        JScrollPane qPane = new JScrollPane(documentTree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);//文档树
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, qPane, leftScroll);

        //中间文档编辑区
        JScrollPane scrollBarText = new JScrollPane(textPane);


        //downPanel.add(rightSplit, "East");
        //downPanel.add(scrollBarText, "Center");


        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, scrollBarText, rightSplit);
        center.setDividerLocation(900);

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left,
                center);
        centerSplit.setDividerLocation(250);

        setLayout(new BorderLayout());
        add(topPanel, "North");
        add(centerSplit, "Center");
    }

    /**
     * 添加监听事件
     */
    private void AddListeners(){
        /*
         * 为各个组件附加监听事件。
         */
        newDocButton.addActionListener(this);
        switchButton.addActionListener(this);
        plainButton.addActionListener(this);
        boldButton.addActionListener(this);
        italicButton.addActionListener(this);
        fontSize.addActionListener(this);
        fontName.addActionListener(this);
        colorOptions.addActionListener(this);
        replaceButton.addActionListener(this);
        saveFileButton.addActionListener(this);
        fileChooser.addActionListener(this);
        openFileButton.addActionListener(this);
        copyButton.addActionListener(this);
        cutButton.addActionListener(this);
        pasteButton.addActionListener(this);
        replaceAllButton.addActionListener(this);
        replaceOneButton.addActionListener(this);
        btn_send.addActionListener(this);
        btn_sendFile.addActionListener(this);

        /*
         * 创建一个实现了insertUpdate()和removeUpdate()的文档监听器。
         */
        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent changeEvent) {
            }
            public void insertUpdate(DocumentEvent insertEvent) {
                if(!setText) {
                    int pos = insertEvent.getOffset();
                    String text;
                    try {
                        text = textPane.getText(pos,1);

                        controller.handleInsertUpdate(pos, text);
                    } catch (BadLocationException e) {
                        throw new RuntimeException("Bad location in insertUpdate.");
                    }
                }
            }

            /*
             * RemoveUpdate 从文档中检测删除事件。
             * 它通过删除用户要删除的字符串来更新文档的文本。
             */
            public void removeUpdate(DocumentEvent removeEvent) {
                if(!setText) {
                    int endLocation = removeEvent.getOffset() + removeEvent.getLength();
                    int startLocation = removeEvent.getOffset();
                    if(startLocation == endLocation) {
                        startLocation--;
                    }
                    if(startLocation > endLocation) {
                        int switchInteger = startLocation;
                        startLocation = endLocation;
                        endLocation = switchInteger;
                    }
                    controller.handleDeleteUpdate(startLocation,endLocation);}
            }
        };
        textPane.getDocument().addDocumentListener(documentListener);
        /*
         * 将侦听器附加到列表，这样我们就能用新的文档更新它
         */
        documentTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent eventForTree) {
                if(eventForTree.getPath().getLastPathComponent().toString().replace(String.format("%n"), "").contains(".txt")
                        ||eventForTree.getPath().getLastPathComponent().toString().replace(String.format("%n"), "").contains(".java")){
                    switchTo = eventForTree.getPath().getLastPathComponent().toString().replace(System.getProperty("line.separator"), "");
                    controller.setSwitchTo(switchTo);
                }
            }
        });
        // 切换窗口
        userList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                String content = (String) userList.getSelectedValue();
                int i = userList.getSelectedIndex();
                if (content != null && content.contains("(New Message)")) {
                    chatUser = content.substring(0, content.indexOf('('));
                    listModel.add(i, chatUser);
                    listModel.remove(i + 1);
                } else {
                    chatUser = content;
                }
                if (chatUser.contains("GroupChat")) {
                    isGroup =true;
                }
                else {
                    isGroup = false;
                }
                textArea.setText("");
                List<String> chatRecords = Client.getChatRecords(chatUser);
                for (int j = 0; j < chatRecords.size(); j++) {
                    StringTokenizer stringTokenizer = new StringTokenizer(chatRecords.get(j), "[#]");
                    receiveMessage(stringTokenizer.nextToken(), stringTokenizer.nextToken());
                }
            }
        });

        //关闭窗口时自动断开连接
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                txt_name.grabFocus();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                Client.sendMessage("[OFFLINE]");
                Client.disconnect();
                View.super.dispose();
                System.exit(0);
            }
        });
    }

    /**
     * 处理GUI中 JComponents 生成的 ActionEvent 事件
     * @param e
     */
    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        controller.handleAction(e);
        /*
         * 单击“replace”按钮打开“replace”窗口
         */
        if(e.getSource() == replaceButton) {
            JPanel myPanel = new JPanel();
            myPanel.add(new JLabel("Find word:"));
            myPanel.add(wordToReplaceField);
            myPanel.add(new JLabel("Replace with:"));
            myPanel.add(wordToReplaceWithField);
            myPanel.add(replaceOneButton);
            myPanel.add(replaceAllButton);
            JDialog dialog = new JDialog();
            dialog.add(myPanel);
            dialog.setSize(260, 170);
            dialog.setLocation(550, 280);
            dialog.setVisible(true);
        }

        /*
         * 打开一个保存文件窗口，这将允许用户在他的系统上保存文件。
         */
        if(e.getSource() == saveFileButton) {

            if (fileChooser.showSaveDialog(View.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                FileWriter f;
                try {
                    f = new FileWriter(file.getPath());
                    BufferedWriter b = new BufferedWriter(f);
                    PrintWriter out = new PrintWriter(b);
                    fw.writeData(textPane.getText(), out);
                    out.close();
                } catch (IOException fileWriterException) {
                    throw new RuntimeException("Problem saving the file.");
                }
            }
        }

        /*
         * 打开一个已打开的文件的窗口，这将允许用户在其系统中找到文件并上传它。
         */
        if(e.getSource() == openFileButton) {
            if (fileChooser.showOpenDialog(View.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                fileName = (file.getName());
                String fileToRead =file.toString();
                fileRead = fr.FileToString(fileToRead);
                if(fileRead != null && fileName != null){
                    controller.setOpenFile(fileName, fileRead);
                }
            }
        }

        /*
         * 连接服务器
         */
        if(e.getSource() == btn_start){
            ipAddress = txt_hostIP.getText();
            currentUser = txt_name.getText();

            String port = "8081";
            String ip = ipAddress;
            String user = currentUser;

            if (ip.isEmpty() || port.isEmpty()) {
                JOptionPane.showMessageDialog(this, "IP地址和端口不能为空",
                        "", JOptionPane.WARNING_MESSAGE);
            } else if (user.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名不能为空",
                        "", JOptionPane.WARNING_MESSAGE);
            } else {
                ServerIP = ip;
                System.out.println(ServerIP+" "+user);
                ServerPORT = port;
                listModel.addElement("GroupChat");
                Client.connect(ServerIP, Integer.parseInt(port), user);//设置聊天室的IP地址和用户名
                isGroup = true;
                chatUser = "GroupChat";
                userList.setSelectedIndex(0);
                Window win = SwingUtilities.getWindowAncestor(btn_start);  //找到该组件所在窗口
                win.dispose();  //关闭
            }
        }

        // 断开按钮点击
        if(e.getSource() == btn_stop){
//            Client.sendMessage("[OFFLINE]");
//            Client.disconnect();
            this.dispose();
            System.exit(0);
        }

        // 发送消息
        if(e.getSource() == btn_send){
            String message = txt_msg.getText();
            if (!message.isEmpty() && !isGroup) {
                Client.sendMessage("P2P[#]" + message + "[#]" + chatUser);
                receiveMessage(currentUser, message);
                txt_msg.setText("");
            } else if (!message.isEmpty() && isGroup) {
                Client.sendMessage("GROUP[#]" + message);
                txt_msg.setText("");
                receiveMessage(currentUser, message);
            } else {
                JOptionPane.showMessageDialog(this, "消息不能为空",
                        "", JOptionPane.WARNING_MESSAGE);
            }
        }

        // 发送文件
        if(e.getSource() == btn_sendFile){
            JFileChooser fd = new JFileChooser();
            fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fd.showOpenDialog(null);
            File f = fd.getSelectedFile();
            if (f != null) {
                try {
                    String filename = f.getAbsolutePath();
                    if (isGroup) {
                        Client.sendFile("GROUP[#]" + filename, filename);
                    } else {
                        Client.sendFile("P2P[#]" + filename + "[#]" + chatUser, filename);
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                    // TODO: handle exception
                }
            }
        }

    }

    /**
     * 获取 new 文档的 JButton 的方法。
     * @return a newDocButton JButton.
     */
    public JButton getNewDocButton() {
        return newDocButton;
    }

    /**
     * 获取 switch 文档的 JButton 的方法。
     * @return a switchButton JButton.
     */
    public JButton getSwitchButton() {
        return switchButton;
    }


    /**
     * 获取 switch 文档 JButton 的方法。
     * @return a replace JButton
     */
    public JButton getReplaceButton() {
        return replaceButton;
    }
    /**
     * 获取 ReplaceAll 文档的 JButton 的方法。
     * @return a replaceAllButton JButton.
     */
    public JButton getReplaceAllButton() {
        return replaceAllButton;
    }

    /**
     * 获取 ReplaceOne 文档的 JButton 的方法。
     * @return a replaceOneButton JButton.
     */
    public JButton getReplaceOneButton() {
        return replaceOneButton;
    }

    /**
     * 该方法用于 replace()方法替换字符串。
     * 这将是我们想要替换的词。
     * @return a String of the word that we want to replace.
     */
    public String getWordToReplaceText() {
        return wordToReplaceField.getText();
    }

    /**
     * 该方法用于 replace()方法替换字符串。
     * 这将是我们想要替换的词。
     * @return a String of the word that we would like to replace with.
     */
    public String getWordToReplaceWithText() {
        return wordToReplaceWithField.getText();
    }

    /**
     * 获取拷贝JButt的方法。
     * @return the copyButton JButton.
     */
    public JButton getCopyButton() {
        return copyButton;
    }

    /**
     * 获取剪切JButt的方法。
     * @return the cutButton JButton.
     */
    public JButton getCutButton() {
        return cutButton;
    }

    /**
     * 获取paste JButton的方法。
     * @return the pasteButton JButton.
     */
    public JButton getPasteButton() {
        return pasteButton;
    }
    /**
     * 获取textPane的方法。
     * @return the textPane JTextArea.
     */
    public JTextArea getTextPane() {
        return textPane;
    }

    /**
     * 获取普通样式JButton的方法。
     * @return the plainButton JButton.
     */
    public JButton getplainButton() {
        return plainButton;
    }

    /**
     * 获取粗体JButt的方法。
     * @return the boldButton JButton.
     */
    public JButton getBoldButton() {
        return boldButton;
    }

    /**
     * 获取斜体JButt的方法。
     * @return the italicButton JButton.
     */
    public JButton getItalicButton() {
        return italicButton;
    }


    /**
     * 获取保存文件JButton的方法
     * @return the save file JButton
     */
    public JButton getSaveFileButton() {
        return saveFileButton;
    }

    /**
     * 获取打开文件JButton的方法
     * @return the open file JButton
     */
    public JButton getOpenFileButton() {
        return openFileButton;
    }



    /**
     * 获取字体大小JComboBox的方法。
     * @return the fontSize JComboBox.
     */
    public JComboBox<Integer> getFontSize() {
        return fontSize;
    }

    /**
     * 获取字体名称JComboBox的方法。
     * @return the fontName JComboBox.
     */
    public JComboBox<String> getFontName() {
        return fontName;
    }

    /**
     * 获取颜色JComboBox的方法。
     * @return the colorOptions JComboBox.
     */
    public JComboBox<String> getColorOptions() {
        return colorOptions;
    }

    /**
     * 获取打开文件JFileChooser的方法。
     * @return the fileChooser JFileChooser.
     */
    public JFileChooser getOpenFileChooser() {
        return fileChooser;
    }



    /**
     * 获取文件的文件名从我们的本地系统中打开的方法。
     * @return a String of the file name.
     */
    public String getOpenFileName() {
        return fileName;
    }

    /**
     * 从我们想要从系统打开的文件中获取文件文本的方法。
     * @return a String of the file content.
     */
    public String getOpenFileRead() {
        return fileRead;
    }

    /**
     * 在视图更新之后，锁定documentListner以检测文件中的新文档集的方法。
     *
     * //SetText 是一种线程安全的方法，它将防止一致性问题。
     * SetText 在 TextArea 中是线程安全的，但对于 RSyntaxTextArea 不是，所以后来加上了synchronized
     */
    public synchronized void setViewText(String text) {
        setText = true;
        textPane.setText(text);
        setText = false;
    }

    /**
     * 插入后将插入符号位置设置为原始位置的方法。
     * @param position - 我们希望设置的插入符号的原位置。
     */
    public void setLocationAfterInsert(int position) {
        textPane.setCaretPosition(position);
    }

    /**
     * 将JFrame的标题设置为当前活动文档名称的方法。这将向用户指示正在处理什么文件。
     * @param fileName - 用户正在工作的当前活动文档的名称。
     */
    public void setViewTitle(String fileName) {
        setTitle(fileName);
    }

    /**
     * 在首次创建新文档或切换到文档树中的活动文档之后，释放屏幕JComponents的方法。
     * @param bool - 针对Jcomponents锁的布尔值（true or false）。
     */
    public void releaseScreen(boolean bool) {
        textPane.setEditable(bool);
        plainButton.setEnabled(bool);
        boldButton.setEnabled(bool);
        italicButton.setEnabled(bool);
        copyButton.setEnabled(bool);
        cutButton.setEnabled(bool);
        pasteButton.setEnabled(bool);
        replaceButton.setEnabled(bool);
        saveFileButton.setEnabled(bool);
        openFileButton.setEnabled(bool);
        fontSize.setEnabled(bool);
        fontName.setEnabled(bool);
        colorOptions.setEnabled(bool);
    }

    /**
     * 基于文档名称更新文档树的方法。
     * @param docName - 将在文档树中更新的文档名称。
     */
    public void updateDocTree(String docName) {
        buildTheDocumentTree(docName, top);
        ((DefaultTreeModel)documentTree.getModel()). //沿用上级model
        nodeStructureChanged((DefaultMutableTreeNode)top);
    }

    /**
     * 该方法获取文档名称的列表，并将所有这些文档添加到树的根。
     * 活动文档列表来自 Hermes 服务器
     * @param docName - 服务器上所有活动文档的列表。
     * @param treeRoot - 树的根
     */
    private void buildTheDocumentTree(String docName, DefaultMutableTreeNode treeRoot ){
        DefaultMutableTreeNode category = null;
        category = new DefaultMutableTreeNode(docName);
        treeRoot.add(category);
    }

    /**
     * 试图连接到有效IP地址或有效主机名的方法。如果连接失败，该方法将尝试连接和提示用户输入另一个IP地址。
     * @param chosenIp - 要连接到的IP地址或有效主机名。
     */
    public void setIpAddressAgain(String chosenIp) {
        ipAddress = chosenIp ;
        while ((!ipAddress.matches(ValidIpAddressRegex))
                && (!ipAddress.matches(ValidHostnameRegex))) {
            ipAddress = JOptionPane.showInputDialog("您选择的IP地址或主机名不是有效的IP地址或主机名。请插入有效的IP地址或主机名：",
                    "127.0.0.1");
        }
    }

    /**
     * 聊天室接受消息的方法
     * @param user
     * @param message
     */
    public void receiveMessage(String user, String message) {
        textArea.append(user + " :\r\n");
        textArea.append("        ");
        textArea.append(message);
        textArea.append("\r\n\r\n");
    }

    /**
     * 更新聊天室界面
     * @param command
     * @param message
     * @param sender
     */
    public void updateGUI(String command, String message, String sender) {
        if (command.equals("GROUP")) {
            if (chatUser.equals("GroupChat")) {
                receiveMessage(sender, message);
            } else {
                String name = listModel.elementAt(0);
                listModel.remove(0);
                listModel.add(0, name + "(New Message)");
            }
            return;
        }

        if (command.equals("P2P")) {
            if (chatUser.equals(sender)) {
                receiveMessage(sender, message);
            } else {
                for (int i = 0; i < listModel.size(); i++) {
                    String name = listModel.elementAt(i);
                    if (name.contains(sender)) {
                        listModel.remove(i);
                        listModel.add(i, name + "(New Message)");
                        return;
                    }
                }
            }
            return;
        }

        if (command.equals("ONLINE")) {
            listModel.addElement(message);
        }

        if (command.equals("OFFLINE")) {
            for (int i = 0; i < listModel.size(); i++) {
                String name = listModel.elementAt(i);
                if (name.contains(message)) {
                    listModel.remove(i);
                    return;
                }
            }
        }

        if (command.equals("FILE")) {
            JOptionPane.showMessageDialog(this, message, "系统消息", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 获取窗格上文本的方法。主要用于测试。
     * @return String the text that the JTextPane contains.
     */
    public String  getText() {
        return textPane.getText();
    }
}