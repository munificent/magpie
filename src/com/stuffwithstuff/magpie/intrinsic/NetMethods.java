package com.stuffwithstuff.magpie.intrinsic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

//TODO(bob): This is all very rough and hacked together.
public class NetMethods {
  // TODO(bob): Hackish.
  @Def("_setClasses(== ServerSocket, == Socket)")
  public static class SetClasses implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      sServerSocketClass = right.getField(0).asClass();
      sSocketClass = right.getField(1).asClass();
      
      return context.nothing();
    }
  }

  @Def("(== ServerSocket) new(port is Int)")
  @Doc("Creates a new ServerSocket listening on the given port.")
  public static class ServerSocket_New implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      try {
        ServerSocket socket = new ServerSocket(right.asInt());
        return context.instantiate(sServerSocketClass, socket);
      } catch (IOException e) {
        throw context.error(Name.IO_ERROR, e.getMessage());
      }
    }
  }

  @Def("(is ServerSocket) accept()")
  @Doc("Waits until a connection is made to the ServerSocket and then\n" +
       "returns a Socket to communicate.")
  public static class Accept implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      ServerSocket serverSocket = (ServerSocket) left.getValue();
      
      try {
        SocketWrapper socket = new SocketWrapper(serverSocket.accept());
        return context.instantiate(sSocketClass, socket);
      } catch (IOException e) {
        throw context.error(Name.IO_ERROR, e.getMessage());
      }
    }
  }

  @Def("(is Socket) readLine()")
  @Doc("Reads a line of text from the Socket. Returns nothing if the\n" +
       "there is no more to read.")
  public static class ReadLine implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      SocketWrapper socket = (SocketWrapper) left.getValue();
      
      String line;
      try {
        line = socket.readLine();
        if (line == null) return context.nothing();
        return context.toObj(line);
      } catch (IOException e) {
        throw context.error(Name.IO_ERROR, e.getMessage());
      }
    }
  }

  @Def("(is Socket) write(text as String)")
  @Doc("Writes the given string to the Socket.")
  public static class Write implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      SocketWrapper socket = (SocketWrapper) left.getValue();
      socket.write(right.asString());
      return context.nothing();
    }
  }

  @Def("(is Socket) close()")
  @Doc("Closes the Socket.")
  public static class Close implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      SocketWrapper socket = (SocketWrapper) left.getValue();
      try {
        socket.close();
        return context.nothing();
      } catch (IOException e) {
        throw context.error(Name.IO_ERROR, e.getMessage());
      }
    }
  }
  
  private static class SocketWrapper {
    public SocketWrapper(Socket socket) throws IOException {
      mSocket = socket;
      mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      mOut = new PrintStream(mSocket.getOutputStream());
    }
    
    public void close() throws IOException {
      mSocket.close();
    }
    
    public String readLine() throws IOException {
      return mIn.readLine();
    }
    
    public void write(String text) {
      mOut.print(text);
    }
    
    private final Socket mSocket;
    private final BufferedReader mIn;
    private final PrintStream mOut;
  }
  
  private static ClassObj sServerSocketClass;
  private static ClassObj sSocketClass;
}
