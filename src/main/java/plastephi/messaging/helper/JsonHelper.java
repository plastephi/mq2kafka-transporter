package plastephi.messaging.helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.standard.TemporalAccessorParser;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public final class JsonHelper {
    private static final Logger _logger = LoggerFactory.getLogger(JsonHelper.class);

    private static final List<String> BOOL_VALUES =  Arrays.asList( "true", "ja", "yes", "si", "1");
    private static final DateTimeFormatter DTFmt = DateTimeFormatter.ISO_DATE_TIME;
    public static final Object NOT_AVAILABLE = null;


    public static boolean isValidJson(String text){
        try {
             new JSONObject(text);
             _logger.info("text is valid json");
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getSortedFirstElement(JSONObject jsonObject, String key, Class<T> clazz) {
        List<String> input = loopThroughJson(jsonObject, key);
        if (input.isEmpty()) return Optional.empty();
        if (input.size() > 1) {
            try {
                input.sort(Comparator.naturalOrder());
            } catch (Exception sortException) {
                _logger.warn("sorting failed; use unsorted list", sortException);
            }
        }

        final var elem = input.get(0).trim();
        try {
            if (Temporal.class.isAssignableFrom(clazz)) {
                TemporalAccessorParser accessorParser = new TemporalAccessorParser((Class<? extends TemporalAccessor>) clazz, DTFmt);
                return (Optional<T>) Optional.ofNullable(accessorParser.parse(elem, Locale.getDefault()));
            } else if (Boolean.class.isAssignableFrom(clazz)) {
                return (Optional<T>) Optional.ofNullable(Boolean.valueOf(BOOL_VALUES.stream().anyMatch(elem::equalsIgnoreCase)));
            } else {
                return Optional.ofNullable(clazz.getDeclaredConstructor(String.class).newInstance(elem));
            }
        } catch (ParseException | InvocationTargetException | IllegalAccessException
                 | NoSuchMethodException | InstantiationException e) {
            _logger.warn("cannot parse the element '{}' to '{}'; {}", elem, clazz.getName(), e.getMessage() );
            return  Optional.empty();
        }
    }



    public static List<String> loopThroughJson(JSONObject jsonObject, String key) {
        List<String> accumulatedValues = new ArrayList<>();
        for (String currentKey : jsonObject.keySet()) {
            Object value = jsonObject.get(currentKey);
            if (currentKey.equals(key)) {
                accumulatedValues.add(value.toString());
            }

            if (value instanceof JSONObject) {
                accumulatedValues.addAll(loopThroughJson((JSONObject) value, key));
            } else if (value instanceof JSONArray) {
                accumulatedValues.addAll(getValuesInArray((JSONArray) value, key));
            }
        }
        return accumulatedValues;
    }

    private static List<String> getValuesInArray(JSONArray jsonArray, String key) {
        List<String> accumulatedValues = new ArrayList<>();
        for (Object obj : jsonArray) {
            if (obj instanceof JSONArray) {
                accumulatedValues.addAll(getValuesInArray((JSONArray) obj, key));
            } else if (obj instanceof JSONObject) {
                accumulatedValues.addAll(loopThroughJson((JSONObject) obj, key));
            }
        }
        return accumulatedValues;
    }
}
