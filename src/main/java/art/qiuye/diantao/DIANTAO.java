package art.qiuye.diantao;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.sql.SQLException;
import java.util.Arrays;

public class DIANTAO {
    public static final Log log= LogFactory.get();
    private double[] DIAMOND=new double[]{1,1,1,2,5,20,20,60};
    private double[] ENERGY=new double[]{0.5,1,1.5,3,5,10,30,60,180};
    private double[] WALK=new double[]{0.5,1,2,3,4,5,10,15,20,30,60,120,240};//m
    private double[] EXPRESS=new double[]{0.75,1,3,5,20,20,20,60};//m
    public static void main(String[] args) throws SQLException {
        /*JFrame frame = new JFrame("title");
        frame.setSize(400,600);
        JDesktopPane desktopPane=new JDesktopPane();
        JInternalFrame internalFrame = new JInternalFrame("internal",true,true);
        desktopPane.add(internalFrame);
        frame.setContentPane(desktopPane);
//        frame.add(internalFrame);
        internalFrame.setSize(100,100);
        frame.setVisible(true);
        internalFrame.setVisible(true);
        internalFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);*/

//        Entity config = Db.use().get(new Entity("config"));
//        StaticLog.info("##config {} ", config);
        String[] ss={"a","b","c"};
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i];
            if(s.equals("c")) ss[i] = "1";
        }
        System.out.println(Arrays.toString(ss));

    }



}

