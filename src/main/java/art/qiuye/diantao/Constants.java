package art.qiuye.diantao;

import cn.hutool.core.io.file.LineSeparator;

import java.util.regex.Pattern;

/**
 * @author hy
 * @create 2023/4/23 0023
 */
public class Constants {

    //type
    public static final String T_WALK = "walk";
    public static final String T_WORK = "work";
    public static final String T_CMD = "command";

    public static final String T_OTHER = "other";


    public static final String OVERFLOW = "overflow";

    public static final Integer common_x = 950;

    public static final String BSY="$$$";
    public static final String CMT="###";



    public static final String IDT_ACTIVITY = "[act]";

    public static final String C_SLEEP = "c_sleep";

    //指令
    public static final String INS_SW="sw";
    public static final String INS_SW_FROM="swf";
    public static final String INS_SW_REVERSE="swr";
    public static final String INS_SW_TO="swt";

    public static final String INS_NO_MODIFY="nom";
    public static final String INS_JJ ="jj";
    public static final String INS_PREB ="preb";

    public static final String TB_CONFIG="dian_config";

    public static final String FD_VENDOR = "vendor";

    public static final String IDT_CMD = "[cmd]";
    public static final String IDT_REF = "[ref]";
    public static final String IDT_REF_COMMON = "[refc]";
    public static final Pattern PATTERN_JJ = Pattern.compile("jj\\d+");
    public static final Pattern PATTERN_VARIABLES = Pattern.compile("@[\\u4E00-\\u9FA5a-z\\d\\(\\),]+");
    public static final Pattern PATTERN_VARIABLES_SAVE = Pattern.compile("(?<=variables\\\":\\s)\\{.+?\\}",Pattern.DOTALL);
    public static final Pattern PATTERN_OP_CODE = Pattern.compile("^[a-z]+");
    public static final String PATTERN_VERSION_S = ("(?<=\"version\":)\\d+");
    public static final String PATTENR_ACTIVITY_STR = "(?<=\\n\\[act\\];)[\\w\\d,;_\\.]+";

    public static final Pattern PATTERN_BRACKET = Pattern.compile("(?<=\\()([\\d,]+?)(?=\\))");



    public static final String LF = LineSeparator.LINUX.getValue();
    public static final String CR_LF = LineSeparator.LINUX.getValue();
}
