package com.ninjaflip.androidrevenge.core.apktool.filecomputing;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Solitario on 23/05/2017.
 * <p>
 * This class contains the result of text search per file
 */
public class TextSearchResultPerFile {
    private String filePath; // path of current file
    private List<Integer> lineNumbers = new ArrayList<Integer>(); // list of line number containing the search pattern
    private int nbOccurrences = 0; // nb occurrences of search pattern in current file
    private JSONArray arrayResults = new JSONArray();

    public TextSearchResultPerFile(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }

    public int getNbOccurrences() {
        return nbOccurrences;
    }

    public JSONArray getArrayResults() {
        return arrayResults;
    }

    public void addNewFound(int lineNumber, int startIndex, int endIndex) {
        lineNumbers.add(lineNumber);
        JSONObject obj = new JSONObject();
        obj.put("line", lineNumber);
        obj.put("start", startIndex);
        obj.put("end", endIndex);
        arrayResults.add(obj);
        nbOccurrences++;
    }


    public void sortLineNumber(){
        Collections.sort(lineNumbers, new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return (i1 < i2 ? -1 : (i1.equals(i2) ? 0 : 1));
            }
        });
    }
}

