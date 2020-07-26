package zone.nora.simplestats.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class QuestData {
    @SerializedName("completions")
    public JsonArray completions = new JsonArray();

    public static int getData(JsonObject quests) {
        if (quests == null) return 0;
        Map<String, QuestData> map = new Gson().fromJson(quests, new TypeToken<Map<String, QuestData>>(){}.getType());
        return map.values().stream().mapToInt(it -> it.completions.size()).sum();
    }
}
