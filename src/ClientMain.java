import Client.View.ClientView;
import Client.Controller.RoomController;
import Client.Controller.Controller;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import javax.swing.*;

public class ClientMain {
    public static void main(String args[]) {
        try {
            System.setProperty("sun.java2d.noddraw","true");
            BeautyEyeLNFHelper.frameBorderStyle = BeautyEyeLNFHelper.FrameBorderStyle.translucencySmallShadow;
            UIManager.put("RootPane.setupButtonVisible",false);
            BeautyEyeLNFHelper.debug = true;
            BeautyEyeLNFHelper.translucencyAtFrameInactive = false;
            BeautyEyeLNFHelper.launchBeautyEyeLNF();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Controller c = new Controller();
    }
}
