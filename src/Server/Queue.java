package Server;

import java.util.ArrayList;

import Document.Document;
/*
 * 线程安全策略：初始化后，多线程操作方法都包含在同步块中。
 * requestQueue获取这些方法任一个期间的锁，即一次只能有一个用户修改队列。
 * 这确保了没有竞争条件，并且队列是完全线程安全的。
 */
/**
 * 表示请求对象的队列。具有两个方法：
 * addRequest（向队列添加新请求）和resolveRequest（在队列前端解析请求）对队列中的其他请求应用任何必要的操作转换。
 * 常量是与这个请求队列关联的文档。
 * 与文档存在一对一的对应关系，并且这个分配在任何时候都不会改变。
 */
public class Queue {
    public final Document doc;
    private ArrayList<Request> requestQueue = new ArrayList<Request>();
    // 使用ArrayList而不是List，因为我们需要能够获取元素的索引并在某个索引处插入
    
    /**
     * Queue的构造函数方法，它跟踪针对特定文档的编辑请求，并将它们应用于文档的服务器副本（真值）。
     * 还负责告诉Request对象何时应用操作转换。
     * 
     * @param doc 与此队列关联的文档。
     */
    public Queue(Document doc) {
        this.doc = doc;
    }
    
    /**
     * 将请求添加到队列末尾的方法。
     * @param request 添加的请求。
     */
    public void addRequest(Request request) {
        synchronized(requestQueue) {
            requestQueue.add(request);
        }
    }
    
    /**
     * 将请求添加到队列指定索引中的方法。
     * @param request 添加的请求。
     */
    public void addRequestAtIndex(int index, Request request) {
        synchronized(requestQueue) {
            requestQueue.add(index, request);
        }
    }
    
    /**
     * 从队列中移除第一个请求实例的方法。
     * @param request 移除的请求。
     */
    public void removeRequest(Request request) {
        synchronized(requestQueue) {
            requestQueue.remove(request);
        }
    }
    
    /**
     * 查找队列中第一个请求实例索引的方法。
     * @param request 查询的请求。
     * @return 要查询请求的索引。
     */
    public int findRequest(Request request) {
        synchronized(requestQueue) {
            return requestQueue.indexOf(request);
        }
    }
    
    /**
     * 解析队列前面的请求。这包括调用它的applyEdit()方法，迭代队列的其余部分以应用任何必要的操作转换。
     */
    public void resolveRequest() {
        synchronized(requestQueue) {
            Request request = requestQueue.get(0);
            requestQueue.remove(0);
            request.applyEdit(); // 对文档进行更改
            if (!requestQueue.isEmpty()) {
                for (Request req : requestQueue) {
                    req.applyTransform(request); // 转换基于此队列的所有连续编辑请求
                }
            }
        }
    }
}