package Document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
/*
 * 线程安全性策略
 * 通过限制-所有读取都在一个线程上执行，该线程只与
 * 当前用户，正在其系统上调用文件读取器的本地实例。
 * 因此没有竞争条件或并发问题。
 * 最后，所有允许用户在RTCE上创建文件的方法都不属于这个类。
 * 已经是线程安全的。
 */
/**
 * 文件读取器类将允许用户从本地系统导入 Hermes 文件（file.txt）。
 * 进入 Hermes 编辑器。该类使用文件读取器从系统导入文件并进行转换
 * 进入一个字符串。
 *
 * 如果用户试图导入不支持的文件，则不被允许。
 * @author reapoker
 *
 */
public class FileReaders {
    private static BufferedReader b;
    /**
     * FielToString方法将有效的Hermes文件转换为字符串。
     * @param fileName - 要从系统导入的文件的有效文件路径。
     * @return  文档的文件的字符串表示形式。
     */
    public String FileToString(String fileName) {
        String outValue = "";
        try {
            String eol = System.getProperty( "line.separator" );
            FileReader fin = new FileReader(fileName);
            b = new BufferedReader(fin);

            String currentLine; 
            while ((currentLine = b.readLine()) != null) {
                outValue= outValue + currentLine + eol;
            }
        }
        catch (FileNotFoundException ef) {
            throw new RuntimeException("File not found error in FileReaders.");
        }
        catch (IOException ei) {
            throw new RuntimeException("IO Exception: could not perform " +
            		"input/output correctly in FileReaders");
        }
        return outValue;
    }  
}