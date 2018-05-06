package org.lsst.ccs.integrationgantrygui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @author tonyj
 */
public class JSONParser {

    private final ScriptEngine engine;
    JSONParser () {
        ScriptEngineManager sem = new ScriptEngineManager();
        this.engine = sem.getEngineByName("javascript");
    }

    public Object parse(List<String> json) throws ScriptException {
        String script = "Java.asJSONCompatible(" + String.join("\n",json) + ")";
        return engine.eval(script);
    }
    
    public Map<String, List<Integer>> parseROI(List<String> json) throws ScriptException {
        return (Map<String, List<Integer>>) parse(json);
    }
    
    public static void main(String[] args) throws ScriptException {
        JSONParser parser = new JSONParser();
        Map<String, List<Integer>> roi = parser.parseROI(Collections.singletonList("{\"1\": [240, 339, 1120, 1520], \"0\": [400, 499, 1120, 1520], \"3\": [240, 339, 1120, 1520], \"2\": [400, 499, 1120, 1520]}"));
        System.out.println(roi);
    }
}