package art.qiuye.diantao;


import java.util.Map;
import java.util.Vector;

/**
 * @author hy
 * @create 2023/4/24 0024
 */

public class Config {
    public String adb_path;
    public Vector<String> devices;
    public Vector<Integer> items_time;
    public Vector<Integer> items_interval;
    public Vector<Integer> items_count;
    public Integer walk_base;
    public Integer walk_step;
    public Integer work_base;
    public Integer work_step;
    public Vector<String> return_work;
    public Vector<String> return_wave;
    public Vector<String> return_walk;

    public Map<String,String> quicks;

    public Integer walk_lines;
    public Integer work_lines;

    public Boolean random_point;

    public String config_name;
    public String quicks_path;

    public String from;

    public String to;
}
