package plastephi.messaging;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static plastephi.messaging.helper.JsonHelper.getSortedFirstElement;

public class JSONParserTest {

    private static final ZoneId currZone = ZoneId.of("UTC");
    private static final ZonedDateTime zdt = ZonedDateTime.now(currZone);
    private static final LocalDate ldt = LocalDate.now(currZone);
    private final static String test04 = "{ \"uid\": 33, \"zdt\": \"" + zdt + "\", \"ldt\": \"" + ldt + "\"}";
    private final static String test01 = """
            { "uid": 1220, "name": "video 01", "bool": true, "bool2": "ja" }
            """;
    private final static String test02 = """
            { "multiply":
                {
                "side1": { "uid": 31, "name": "video first", "bool": "false"},
                "side2": { "uid": 21, "name": "video second", "bool": "true"}
              }
            }
            """;
    private final static String test03 = """
            { "a": { "b": { "c": { "d": { "e":
                {
                 "side1": { "uid": 55 },
                 "side2": { "uid": 56 }
                }
            } } } } }
            """;
    private final static String test05 = """
            { "b1": true, "b2": "TrUE", "b3": "Si", "b4": "JA", "b5": "1", "b6": "Yes",
              "b7": "error", "b8": false, "b9" : "FALSE", "b10": "no", "b11": "0", "b12": 0}
            """;
    @Test
    public void testEmpty() {
        assertEquals(Optional.empty(), getSortedFirstElement(new JSONObject("{}"), "any", String.class));
        assertEquals(Optional.empty(), getSortedFirstElement(new JSONObject("{\"ldt\":\"" + ldt +"\"}"), "xxx", String.class));
    }

    @Test
    public void testRecursiveJsonFinding() {
        JSONObject jsonObject = new JSONObject(test01);

        assertEquals(1220L, getSortedFirstElement(jsonObject, "uid", Long.class).get());
        assertEquals("video 01", getSortedFirstElement(jsonObject, "name", String.class).get());
        assertEquals(Boolean.TRUE, getSortedFirstElement(jsonObject, "bool", Boolean.class).get());
        assertEquals(Boolean.TRUE, getSortedFirstElement(jsonObject, "bool2", Boolean.class).get());
    }

    @Test
    public void testSorting() {
        JSONObject jsonObject = new JSONObject(test02);
        assertEquals(21, getSortedFirstElement(jsonObject, "uid", Integer.class).get());
        assertEquals("video first", getSortedFirstElement(jsonObject, "name", String.class).get());
        assertEquals(Boolean.FALSE, getSortedFirstElement(jsonObject, "bool", Boolean.class).get());
    }

    @Test
    public void testDeepDiving() {
        JSONObject jsonObject = new JSONObject(test03);
        assertEquals(55, getSortedFirstElement(jsonObject, "uid", Integer.class).get());
    }

    @Test
    public void testSomeTransformer() {
        JSONObject jsonObject = new JSONObject(test04);
        assertEquals(zdt, getSortedFirstElement(jsonObject, "zdt", ZonedDateTime.class).get());
        assertEquals(ldt, getSortedFirstElement(jsonObject, "zdt", LocalDate.class).get());
    }

    @Test
    public void testBoolTransformer() {
        JSONObject jsonObject = new JSONObject(test05);
        Stream.of("b1", "b2", "b3", "b4", "b5", "b6").forEach(key -> assertEquals(Boolean.TRUE, getSortedFirstElement(jsonObject, key, Boolean.class).get()));
        Stream.of("b7", "b8", "b9", "b10", "b11", "b12").forEach(key -> assertNotEquals(Boolean.TRUE, getSortedFirstElement(jsonObject, key, Boolean.class).get()));

    }

    @Test
    public void testFailure() {
        assertEquals(Optional.empty() ,getSortedFirstElement(new JSONObject("{\"x\":\"x\"}"), "x", Long.class));
    }
}

