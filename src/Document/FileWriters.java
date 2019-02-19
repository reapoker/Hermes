package Document;

import java.io.IOException;
import java.io.PrintWriter;
/*
 * 线程安全策略
 * 通过限制-所有写操作都在一个线程上执行，该线程只与当前用户有关，
 * 当前用户正在他或她的系统上调用文件编写器的本地实例，
 * 因此不存在竞争条件或并发问题。
 */
/**
 * 文件编写器类将使用户能够将 Hermes 文件（file.txt）从编辑器导出（保存）到本地系统。
 * 该类使用文件编写器将文件从文本编辑器导出到用户的。
 * @author reapoker
 *
 */
public class FileWriters {
    /**
     * writeData方法将允许用户将文件从RTCE导出到其系统中的file.txt。
     * @param fileTowrite - 要在用户系统上创建的(/path/file.txt)文件的有效文件和路径名。
     * @param out - printWriter，用于将文档中的文本插入用户系统上创建的文件
     */
    public void writeData(String fileTowrite, PrintWriter out) throws IOException { 
        out.println(fileTowrite);  // 将文档中的文本插入到用户系统上的文件.txt上
    } 
}
