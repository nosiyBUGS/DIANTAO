package art.qiuye.diantao;

import java.util.List;

/**
 * @author hy
 * @create 2023/7/13 0013
 */
public class SlotGroup {
    public List<Slot> slots;
    public String[] activityNames;

    public String[] exceptionCmds;

    public boolean c_sleep;//direct whther interrupt sleep,otherwise call suspend

    public double totalTime;
}
