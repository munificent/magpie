package com.stuffwithstuff.magpie.intrinsic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.IO;

public class ProcessMethods {
  @Def("execute(command is String)")
  @Doc("Spawns a new process, executes the given command in it, and waits " +
       "it to end. Returns the process's output and exit code.")
  public static class Execute implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String command = right.asString();

      Runtime runtime = Runtime.getRuntime();
      try {
        Process process = runtime.exec(command);
        int exit = process.waitFor();
        String output = IO.readAll(new BufferedReader(
            new InputStreamReader(process.getInputStream())));
        
        List<String> keys = new ArrayList<String>();
        keys.add("out");
        keys.add("exit");

        Map<String, Obj> record = new HashMap<String, Obj>();
        record.put("out", context.toObj(output));
        record.put("exit", context.toObj(exit));
        
        return context.toObj(keys, record);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      
      return context.nothing();
    }
  }
}
