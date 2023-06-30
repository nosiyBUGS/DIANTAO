package art.qiuye.diantao;

import cn.hutool.core.lang.Pair;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OcrFrame {
    private int groupID;
    private int idx;
    private String content;
    private int direction;
    private List<Pair<Integer,Integer>> pairs;
}
