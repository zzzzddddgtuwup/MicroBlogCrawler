package m.sina;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zzzzddddgtuwup on 3/22/15.
 */
public class ParseUtils {
    public static int getPageNum(String firstPageJson) throws JSONException {
        //System.out.println(firstPageJson);
        JSONArray jsonArray = new JSONArray(firstPageJson);
        JSONObject jsonObj = jsonArray.getJSONObject(1);
        if(jsonObj.has("maxPage")){
            return jsonObj.getInt("maxPage");
        }else{
            return 1;
        }
    }

    //remove all tags and whitespace
    public static String removeTag(String s) {
        return s.replaceAll("<\\/?[^>]*>", "").replaceAll(" ", "");
    }

    public static String getNumber(String s){
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(s);
        String result = m.replaceAll("").trim();
        return result;
    }
}
