package Server;

import Document.Document;
/*
 * 线程安全策略：由于这个类的主要多线程操作方法applyTransform专门从队列中的resolveRequest方法中调用，
 * 所以队列必须作为先决条件锁定，因此在请求解析期间不能由其他线程进行更改。
 * 我们还在applyEdit方法中同步文档，以避免出现竞争条件的可能性。
 */
/**
 * 为指定文档所做的编辑请求。
 * 具有五个字段：requestType（插入或删除）、parentDoc（发出请求的文档）、startPos（编辑的开始位置）、
 *              endPos（编辑的结束位置）、insertText（只用于插入要添加的文本）。
 * 常量是parentDoc，即应用编辑的文档，它是不可变的。
 */
public class Request {
    public final Document parentDoc;

    private String requestType;
    private int startPos;
    private int endPos;
    private String insertText;

    /**
     * 请求的构造方法，根据从客户端发送到服务器的插入和删除消息来分派。
     * 请求对象的参数由消息给出。
     */

    /**
     * 插入请求的构造方法：
     * 
     * @param type, 表示请求类型的字符串（插入或删除）。
     * @param start, 表示请求的起始索引的整数。
     * //@param int end, 表示请求结束索引的整数。
     * @param text, 要插入的文本字符串（为删除请求留空）。
     * @param doc, 要编辑的文档。
     */
    public Request(String type, int start, String text, Document doc) {
        requestType = type;
        startPos = start;
        endPos = start + text.length();
        insertText = text;
        parentDoc = doc;
    }

    /**
     * 删除请求的构造函数。这需要一个起始位置和一个字符串的结束位置。
     *
     * @param type, 所需的请求类型。在这种情况下是“delete”。
     * @param start, 表示请求的开始索引的整数。
     * @param end, 表示请求结束索引的整数。
     * @param doc, 要编辑的文档。
     */
    public Request(String type, int start, int end, Document doc) {
        requestType = type;
        startPos = start;
        endPos = end;
        insertText = "";
        parentDoc = doc;
    }

    /**
     * 对请求应用操作转换，基于进行编辑时产生的位移来更改startPos和endPos字段。
     * 解决文本编辑冲突的问题。
     * 有关每个可能的配置所需的操作转换的完整描述，请参阅RequestTest.java。
     * 
     * @param other 正在由服务器解析的请求；即在队列中此请求之前的请求。
     */
    public void applyTransform(Request other) {
        if (other.getStartPos() >= endPos) // 若上一次请求的起始位置大于该次请求的结束位置，就没有什么影响
            return;
        else if (other.getEndPos() <= startPos) { // 上一次请求的起始位置小于该次请求的结束位置，同样没有文本的编辑重叠
            if (other.requestType == "insert") {
                startPos += (other.getEndPos() - other.getStartPos());
                endPos += (other.getEndPos() - other.getStartPos());
            } else { // other.requestType == "delete"
                startPos -= (other.getEndPos() - other.getStartPos());
                endPos -= (other.getEndPos() - other.getStartPos());
            }
        } else { // 编辑的文本出现重叠
            if (other.requestType == "delete" && requestType == "delete") { // delete-delete
                if (other.getStartPos() >= startPos && other.getEndPos() >= endPos) // 上一次 starts 和 ends 都在 这次 之后
                    endPos = other.getStartPos();
                else if (other.getStartPos() <= startPos && other.getEndPos() <= endPos) { // 上一次 starts和ends 都在 B 之前
                    startPos = other.getEndPos();
                    startPos -= other.getEndPos() - other.getStartPos();
                    endPos -= other.getEndPos() - other.getStartPos();
                } else if (other.getStartPos() <= startPos && other.getEndPos() >= endPos) { // 这一次包含于上一次之中
                    parentDoc.getQueue().removeRequest(this); // 无额外的删除
                } else if (other.getStartPos() >= startPos && other.getEndPos() <= endPos) { // 上一次包含于这次一次之中
                    endPos = other.getStartPos() + endPos - other.getEndPos();
                }
            } else if (other.requestType == "insert" && requestType == "insert") { // insert-insert
                startPos = other.getStartPos(); // 将插入位置移动到插入部分的开始处
                endPos = startPos + insertText.length();
            } else if (other.requestType == "delete" && requestType == "insert") { // delete-insert
                startPos = other.getStartPos(); // 将插入位置移动到删除部分的开始位置
                endPos = startPos + insertText.length();
            } else { // insert-delete
                int shift = other.getInsertText().length();
                Queue queue = parentDoc.getQueue();
                endPos = other.getStartPos(); // startPos, other.getStartPos()
                Request sub = new Request("delete", other.getStartPos() + shift,
                                                    other.getEndPos() + shift, parentDoc);
                queue.addRequestAtIndex(queue.findRequest(this) + 1, sub);
                // other.getEndPos() + shift, endPos + shift
            }
        }
    }

    /**
     * 解析请求，编辑当前序列。
     */
    public void applyEdit() {
        if (requestType == "insert") {
            synchronized(parentDoc) {
                parentDoc.insert(startPos, insertText);
            }
        } else { // requestType == "delete"
            synchronized(parentDoc) {
                parentDoc.delete(startPos, endPos);
            }
        }
    }

    /**
     * 解析请求，将编辑应用到当前序列。
     * 专门用于JUnit测试的方法。
     * @param currText 当前文本序列的字符串。
     * @return String 表示已编辑后的序列。
     */
    public String applyEditTesting(String currText) {
        StringBuilder sb = new StringBuilder(currText);
        if (requestType == "insert") {
            sb.insert(startPos, insertText);
            return sb.toString();
        } else { // requestType == "delete"
            sb.delete(startPos, endPos);
            return sb.toString();
        }
    }

    /**
     * 获取请求的编辑类型的方法。
     * @return String 表示编辑类型的请求类型字符串。
     */
    public String getRequestType() {
        return requestType;
    }

    /**
     * 获取请求编辑的起始位置的方法。
     * @return int startPos 这个编辑开始的索引。
     */
    public int getStartPos() {
        return startPos;
    }

    /**
     * 获取请求编辑的结束位置的方法。
     * @return int endPos 这个编辑结束的索引。
     */
    public int getEndPos() {
        return endPos;
    }

    /**
     * 获取该请求插入的文本的方法。
     * @return String insertText 要插入的文本。
     */
    public String getInsertText() {
        return insertText;
    }
}