package art.qiuye.diantao.action;

import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static art.qiuye.diantao.Constants.IDT_CMD;
import static art.qiuye.diantao.DIANTAO_UI.getTextArea;

/**
 * @author hy
 * @create 2023/11/1 0001
 */
public class CtrlUpDownAction extends AbstractAction{
    private int ad;

    public CtrlUpDownAction(int ad) {
        this.ad = ad;
    }

    @SneakyThrows
    @Override
    public void actionPerformed(ActionEvent e) {
        JTextArea area=getTextArea(e);
        int linenumber = area.getLineOfOffset(area.getCaretPosition());
        int start = area.getLineStartOffset(linenumber);
        int end = area.getLineEndOffset(linenumber);
        String line = area.getText(start, end - start);
        if(StrUtil.isBlank(line))return;
        int count;
        if(line.startsWith(IDT_CMD)){
            String s = line.split(";")[1];
            count = Integer.parseInt(s);
            area.replaceRange(String.valueOf(count-1),start+IDT_CMD.length()+1,start+IDT_CMD.length()+1+s.length());
        }else {
            String[] split = line.split(";");
            String s = null;
            String command = e.getActionCommand();
            if(command==null) command = "1";
            switch (command){
                case "1":s=split[0];break;//count
                case "2":s=split[1];start+=split[0].length()+1;break;//total time
                case "4":
                case "5":
                    s=split[2];start+=split[0].length()+1+split[1].length()+1;break;//interval
                case "3":
                    s=split[3];
                    if(s.startsWith("$") || s.startsWith("#")){
                        start+=split[0].length()+1+split[1].length()+1+split[2].length()+1+1;
                        s = s.substring(1);
                    }else return;
                    break;//$1
            }
            count = Integer.parseInt(s);
            area.replaceRange(String.valueOf(count+ad),start,start+s.length());
        }

    }
}
