package art.qiuye.diantao.hire;

import art.qiuye.diantao.Constants;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static art.qiuye.diantao.Constants.*;

public class Hire {
    public String id;
    public String status;//被雇佣中 | 可挖走
    public boolean digable;
    public String digPoint;

    public boolean end;

    public int price=0;

    public static List<Hire> generateList(String ocrText){
        JSONObject json = JSON.parseObject(ocrText);
        if(json.getIntValue("code")!=0)return Collections.emptyList();
        JSONArray results = json.getJSONArray("result");
        List<Hire> hires = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            Hire h = new Hire();
            JSONObject r = results.getJSONObject(i);
            if(r.getString("content").equals(HIRE_DIGABLE)) {
                if(i>0)
                    h.id = results.getJSONObject(i-1).getString("content");
                h.status = r.getString("content");
                if(i+1==results.size())continue;
                JSONObject dig = results.getJSONObject(i + 1);
                h.digable = true;
                JSONArray fr = dig.getJSONArray("frame");
                int _x1 = Integer.parseInt(fr.get(0).toString().split(",")[0]);
                int _x2 = Integer.parseInt(fr.get(1).toString().split(",")[0]);
                int _y1 = Integer.parseInt(fr.get(0).toString().split(",")[1]);
                int _y2 = Integer.parseInt(fr.get(2).toString().split(",")[1]);
                String dig_point = StrUtil.format("{} {}", (_x1 + _x2) / 2, (_y1 + _y2) / 2);
                h.digPoint = dig_point;
                hires.add(h);
            }
        }
        if(!hires.isEmpty()) hires.get(hires.size() - 1).end = ocrText.contains(HIRE_END);
        else {
            Hire hire=new Hire();
            hire.end = ocrText.contains(HIRE_END);
            hires.add(hire);
        }
        return hires;
    }

    @Override
    public String toString() {
        return "Hire{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", digPoint='" + digPoint + '\'' +
                ", end=" + end +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Hire)
            return ((Hire) obj).id.equals(this.id);
        return super.equals(obj);
    }
}
