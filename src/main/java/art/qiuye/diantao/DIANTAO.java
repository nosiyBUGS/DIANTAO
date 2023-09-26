package art.qiuye.diantao;

import cn.hutool.core.util.ReUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.StaticLog;

import java.sql.SQLException;
import java.util.List;

import static art.qiuye.diantao.Constants.PATTERN_VAR;

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
        String c = "$$$点淘-凌晨\n" +
                "###[act];TaoLiveHomeActivity;c_sleep\n" +
                "[cmd];1;atc 2:02;t@app;s8\n" +
                "[cmd];1;c3,t@首页直播,s3;t@元宝中心;s3;b;t元宝中心;s3;\n" +
                "\n";
        List<String> group0 = ReUtil.findAllGroup0(PATTERN_VAR, c);
        StaticLog.info("##group0 {} ", group0);
    }



}

