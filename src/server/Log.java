package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//日志储存
public class Log {
    private static final List<String> log=new ArrayList<>();
    private static int count;
    public Log(){}

    public static void add(String msg) 
    {
        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.add(msg + "---" + currentTimestamp);
        count++;
    }

    public static int getLogCount()
    {
        return count;
    }

    public static void print()
    {
        for (String entry : log) 
        {  
            System.out.println(entry);
        }
    }

    public static void initLog()
    {
        log.clear();
        count=0;
    }
}
