package system;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CsvStorage {
    public static List<String[]> readRows(File file) throws IOException{
        List<String[]> rows = new ArrayList<>();
        if(!file.exists()){
            return rows;
        }

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        for(int i = 1; i < lines.size(); i++){
            String line = lines.get(i).trim();
            if(line.isEmpty()){
                continue;
            }
            rows.add(parseLine(line));
        }
        return rows;
    }

    public static void writeRows(File file, String header, List<String[]> rows) throws IOException{
        ensureParent(file);
        try(PrintWriter writer = new PrintWriter(new FileWriter(file, false))){
            writer.println(header);
            for(String[] row : rows){
                writer.println(toLine(row));
            }
        }
    }

    public static void appendRow(File file, String header, String[] row) throws IOException{
        ensureParent(file);
        boolean needsHeader = !file.exists() || file.length() == 0;
        try(PrintWriter writer = new PrintWriter(new FileWriter(file, true))){
            if(needsHeader){
                writer.println(header);
            }
            writer.println(toLine(row));
        }
    }

    public static String toLine(String[] row){
        List<String> escaped = new ArrayList<>();
        for(String value : row){
            escaped.add(escape(value));
        }
        return String.join(",", escaped);
    }

    private static String escape(String value){
        if(value == null){
            return "";
        }
        boolean quote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        if(quote){
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static String[] parseLine(String line){
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for(int i = 0; i < line.length(); i++){
            char c = line.charAt(i);
            if(c == '"'){
                if(inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"'){
                    current.append('"');
                    i++;
                }
                else{
                    inQuotes = !inQuotes;
                }
            }
            else if(c == ',' && !inQuotes){
                values.add(current.toString());
                current.setLength(0);
            }
            else{
                current.append(c);
            }
        }

        values.add(current.toString());
        return values.toArray(new String[0]);
    }

    private static void ensureParent(File file){
        File parent = file.getParentFile();
        if(parent != null && !parent.exists()){
            parent.mkdirs();
        }
    }
}
