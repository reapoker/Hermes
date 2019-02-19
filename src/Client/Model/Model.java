package Client.Model;

import Client.View.View;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Vector;
import java.awt.Color;
import java.awt.Font;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/*
 * 线程安全性策略
 * Model类根据来自控制器的输入更新用户的视图
 * 
 * 构造函数不需要同步，因为它正在生成受限的新对象。
 * 因此，Java不会让你同步它。
 *
 * 所有数据类型都是线程安全的，因为它们要么是线程安全的数据类型（vectors），要么是不可变的（unmodifiableMap）
 * 
 * updateText方法是线程安全的，因为它在视图中调用线程安全的方法。
 * 
 * 通过限制-这个类中的所有方法都在一个线程上执行，该线程本地化为用户并且不与其他任何客户端共享。
 * 此外，没有实际的竞争条件，因为每个方法负责更新不同的JComponent，所以不必担心它们的执行顺序会干扰视图。
 */

 
/**
 * MVC设计模Model部分的实现。这一部分将：
 * 
 *      (1) 存储视图的信息。也存储样式信息。
 *      (2) 在GUI上进行以下更新：
 *          (a) 更新文本
 *          (b) 更新样式
 *          (c) 更新文档树
 *          (d) 对按钮的加锁和解锁
 *      
 */
public class Model {
    
    private Vector<String> inTree = new Vector<String>(); // 存储当前在树中的所有文档。
    private View view;
    private int fontSize;
    private int styleType;
    private int colorType;
    int fontName;
    
    // 用于将整数映射到它们所表示的字体样式的映射。
    @SuppressWarnings("serial")
    private  Map<Integer, Integer> styleMap = 
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                        {
                            put(0, Font.PLAIN);
                            put(1, Font.BOLD);
                            put(2, Font.ITALIC);
                        }
                    });
    
    //用于将整数映射到它们所代表的颜色的样式。
    @SuppressWarnings("serial")
    private Map<Integer, Color> colorMap = 
            Collections.unmodifiableMap(new HashMap<Integer, Color>() {
        {
            put(0, Color.BLACK);
            put(1, Color.BLUE);
            put(2, Color.GRAY);
            put(3, Color.GREEN);
            put(4, Color.ORANGE);
            put(5, Color.PINK);
            put(6, Color.RED);
            put(7, Color.YELLOW);
        }
    });
    
    private String[] fonts = {"Arial","Courier New","Georgia","Times New Roman","Verdana"};

    /**
     * 绑定视图，设置初始样式
     *
     * @param v 与该 Model 关联的视图
     */
    public Model(View v) {
        view = v;
        fontSize = 12;
        styleType = Font.PLAIN;
        colorType = 0;
        fontName= 3;   
    }

    @SuppressWarnings("unchecked")
    /**
     * 获取文档树中所有文件的名称 vector 的方法。
     * @return
     */
    public Vector<String> getInTree() {
        return (Vector<String>) inTree.clone();
    }
    
    /**
     * 设置GUI文本的方法。
     * @param text, 要在GUI上显示的文本。
     * @throws UnsupportedEncodingException
     */
    // SetViewText 是线程安全的！
    public void updateText(String text) throws UnsupportedEncodingException{
        String newText = URLDecoder.decode(text,"UTF-8");
        view.setViewText(newText);
    }
    
    /**
     * 更新GUI标题的方法。
     * @param title, 给GUI的标题。
     */
    public void updateViewTitle(String title) {
        view.setViewTitle(title);   
    }
    
    /**
     * 更新文档树的方法。同时更新 View 和 Model 中储存的文档树。
     * @param response, 文档树上的文件名
     */
    public void updateDocTree(String response) {
        view.updateDocTree(response);
        inTree.add(response);
    }
    
    /**
     * 设置视图存储字体大小的方法。
     * @param size, the font size
     */
    public void setFontSize(int size ) {
        fontSize = size;  
    }
    
    /**
     * 设置用于视图的样式的方法。
     * @param style, 为视图存储的样式的整数映射。
     */
    public void setStyle(int style) {
        styleType = style;
    }
    
    /**
     * 设置视图所用颜色的方法。
     * @param color, 映射到视图中存储的颜色的整数映射。
     */
    public void setColor(int color) {
        colorType = color;
    }
    
    /**
     * 设置文档存储的字体类型的方法。
     * @param name, 在视图中使用的字体名称的整数映射。
     */
    public void setFontName(int name) {
        fontName = name;
    }
    
    /**
     * 获取视图中使用的样式的方法。
     * @return int, 映射到视图中使用的样式的整数映射。
     */
    public int getStyle() {
        return this.styleType;
    }
    
    /**
     * 获取视图中使用的颜色的方法。
     * @return int, 映射到视图中使用的颜色的整数映射。
     */
    public int getColor() {
        return this.colorType;
    }

    /**
     * 获取视图中使用的字体名称的方法。
     * @return int, 映射到视图中使用的字体名称的整数映射。
     */
    public int getFontName() {
        return this.fontName;
    }
    
    /**
     * 获取视图中使用的字体大小的方法。
     * @return int, 映射到视图中使用的字体大小的整数映射。
     */
    public int getFontSize() {
        return this.fontSize;     
    }
    
    /**
     * 设置GUI样式的方法。将所有整数映射为各种样式作为参数。
     * @param fontName, 要给GUI的字体名称的整数映射。
     * @param fontStyle, 到要给GUI的字体样式的整数映射。
     * @param fontSize, 要给GUI的字体大小的整数映射。
     * @param color, 要设置GUI字体的颜色的整数映射。
     */
    public void setViewStyle(int fontName, int fontStyle, int fontSize, int color) {
        view.getTextPane().setFont(new Font(fonts[fontName], styleMap.get(fontStyle),fontSize));
        view.getTextPane().setForeground(colorMap.get(color));          
    }
    
    /**
     * 用于更新视图并释放所有按钮和组合框的方法，以便在用户单击“new”或“switch”之后可以使用所有选项。
     * @param bool
     */
    public void releaseScreen(boolean bool) {
        view.releaseScreen(bool);
    }
}