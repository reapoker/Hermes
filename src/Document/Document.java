package Document;

import java.util.ArrayList;

import Server.Queue;
import Server.User;
/*
 * 线程安全策略
 * 文档类保存用户正在编辑的实际文档。
 * 构造函数不需要同步，因为它生成一个正在受限的新对象。
 * 因此，无需同步它。
 *
 * 所有可能使数据突变并且是class条件对象的方法都被
 * 同步，以保证它们是线程安全的。
 */
/**
 * Hermes 的文档类
 * 有两个基本属性：  表示文档名的字符串 docName
 *                  表示文档文本的StringBuffer data 。
 * @author reapoker
 *
 */
public class Document {

    private String docName;
    private String style="1 0 12 0";
    private StringBuffer data;
    private Queue requestQueue;
    private ArrayList<User> listOfClients = new ArrayList<User>();
    /**
     * Document类的构造函数。 所需的唯一参数是文档作为名称的字符串。
     * 数据被实例化为空 StringBuffer（长度16）。
     * @param docName, 文档名
     */
    public Document(String docName) {
        this.docName = docName;
        this.data = new StringBuffer();
        requestQueue = new Queue(this);
    }

    /**
     * 在文档中的给定位置插入文本。
     * 这是 Hermes 支持的两个“edit”中的第一个。
     * 插入文本
     * @param pos, 请求插入位置。
     * @param text, 要插入的文本。
     */
    public synchronized void insert(int pos, String text) {

        if (pos >= 0 && pos <= this.data.length()) this.data.insert(pos, text);
        else if (pos >= 0) {
            int len = this.data.length();
            for (int i = 0; i <= pos - len - 1; i++) {
                this.data.append(" ");
            }
            this.data.append(text);
        }
    }

    /**
     * 一种获取处理文档的所有用户的列表的方法，以便服务器能够在给定时间知道哪些用户正在处理给定文档。
     * @return ArrayList<User>,  Users 用户的列表。
     */
    public  ArrayList<User> getList(){
        return listOfClients;
    }

    /**
     * 一种向ArrayList添加用户的方法，用于存储给定时间处理文档的所有用户。
     * @param u, 要添加的用户。
     */
    public void setUser(User u){
        listOfClients.add(u);
    }

    /**
     * 删除文档中给定位置的文本。
     * 这是 Hermes 支持的两个“edit”中的第二个。
     * @param beginPos, 从该位置开始删除。
     * @param endPos, 到该位置结束。
     */
    public synchronized void delete(int beginPos, int endPos) {
        try {
            this.data.delete(beginPos, endPos);
        } catch (StringIndexOutOfBoundsException e) {
            throw new RuntimeException("Index bounds error in delete of Document.");
        }
    }

    /**
     * 将给定字符串“replaceFrom”的所有实例替换为另一个字符串“replaceTo”的方法。
     * @param replaceFrom, 要替换的字符串。
     * @param replaceTo, 要替换的字符串。
     */
    public synchronized void replaceAll(String replaceFrom, String replaceTo) {
        String dataTemp = this.data.toString();
        String replacedDataTemp = dataTemp.replaceAll(replaceFrom, replaceTo);
        this.data = new StringBuffer(replacedDataTemp);
    }

    /**
     * 将给定字符串“replaceFrom”的第一个实例替换为另一个字符串“replaceTo”的方法。
     * @param replaceFrom, 要替换的字符串。
     * @param replaceTo, 要替换的字符串。
     */
    public synchronized void replaceOne(String replaceFrom, String replaceTo) {
        String dataTemp = this.data.toString();
        String replacedDataTemp = dataTemp.replaceFirst(replaceFrom, replaceTo);
        this.data = new StringBuffer(replacedDataTemp);
    }


    /**
     * 将文档名称设置为给定字符串的方法。
     * @param docName
     */
    public void setName(String docName) {
        this.docName = docName;
    }

    /**
     * 直接设置文档的数据的方法。但必须给它一个StringBuffer
     * @param data, 给文档的数据。
     */
    public synchronized void setDate(StringBuffer data) {
        this.data = data;
    }

    /**
     * 获取文档名称的方法
     * @return String, 文档名。
     */
    public String getName() {
        return this.docName;
    }

    /**
     * 获取文档中的所有数据的方法。
     * 返回数据的副本，因此要求 StringBuffer 不可变。
     * @return StringBuffer, 表示文档中数据的StringBuffer。
     */
    public synchronized StringBuffer getData()  {
        String dataString = this.data.toString();
        StringBuffer dataCopy = new StringBuffer(dataString);
        return dataCopy;
    }

    /**
     * 将文档中的所有文本作为字符串获取的方法。
     * @return String， 文档的文本实例。
     */
    public synchronized String getAllText() {
        return this.data.toString();
    }

    /**
     * 设置文档样式的方法。以空格分隔的4个数字串的输入。
     * eg: "1 1 12 1"
     * Format: <br>
     * "x_0, x_1, x_2, x_3": <br>
     * x_0: Font Name.<br>
     * x_1: Style (0 = plain, 1 = bold, 2 = italic)<br>
     * x_2: Font Size.<br>
     * x_3: Colour.<br>
     * @param styleToSet, 包括4个数字。
     */
    public synchronized void setStyle(String styleToSet) {
        this.style = styleToSet;
    }

    /**
     * 获取文档样式的方法。返回值是一个由空格分隔的4个数字的字符串返回。
     * eg: "1 1 12 1"
     * Format: <br>
     * "x_0, x_1, x_2, x_3": <br>
     * x_0: Font Name.<br>
     * x_1: Style (0 = plain, 1 = bold, 2 = italic)<br>
     * x_2: Font Size.<br>
     * x_3: Color.<br>
     * @return String, 文档的样式为4个数字的字符串。
     */
    public synchronized String getStyle() {
        return this.style;
    }
    /**
     * 在给定范围内获取文档内文本的方法。
     * @param beginPos, 从该处开始截取。
     * @param endPos, 到该处结束。
     * @return String, 在 beginPos 和 endPos 位置之间的文本。
     */
    public synchronized String getTextAtPos(int beginPos, int endPos) {
        return this.data.subSequence(beginPos, endPos).toString();
    }

    /**
     * 得到一个给定位置的字符的方法。
     * @param pos, 你想要得到的字符的位置。
     * @return char, 在该位置的字符。
     */
    public synchronized char getCharAt(int pos) {
        try {
            return this.data.charAt(pos);
        } catch(IndexOutOfBoundsException e) {
            throw new RuntimeException("No char at this point.");        
        }
    }

    /**
     * 获取文档中文本的长度。
     * @return int, 文档的长度。
     */
    public int getLength() {
        return this.data.length();
    }

    /**
     * 用于给定文档的编辑请求的队列 Public getter 。
     * @return Queue, 该文档的 Queue 。
     */
    public Queue getQueue() {
        return requestQueue;
    }

    /**
     * 制作 Document 的浅拷贝的方法。
     * @return Document, 文档的副本。
     */
    public Document copy() {
        Document doc = new Document(this.getName());
        doc.setDate(this.getData());
        return doc;
    }

    /**
     *  Document 文件类型的哈希函数。
     *  用于比较同一文档的多个版本并管理操作转换的方法。
     *  使用getAllText()代替直接访问数据字段，因为StringBuffer不能被哈希。
     * 
     * @return Integer 表示文档的哈希值。
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.getAllText() == null) ? 0 : this.getAllText().hashCode());
        result = prime * result + ((docName == null) ? 0 : docName.hashCode());
        return result;
    }

    /**
     * 测试两个 Documents 文档的相等性。
     * 用于比较同一文档的多个版本并管理操作转换的方法。
     * 
     * @return True 如果两个文件相等，否则为 False 。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Document other = (Document) obj;
        if (this.getAllText() == null) {
            if (other.getAllText() != null)
                return false;
        } else if (!this.getAllText().equals(other.getAllText()))
            return false;
        if (docName == null) {
            if (other.docName != null)
                return false;
        } else if (!docName.equals(other.docName))
            return false;
        return true;
    }
}