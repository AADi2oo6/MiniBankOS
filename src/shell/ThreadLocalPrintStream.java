package shell;

import java.io.*;

/**
 * A PrintStream that writes to:
 *   1. The original System.out (server console — always)
 *   2. The current thread's registered client stream (if set)
 *
 * This lets each client thread see its own output without modifying any existing code.
 */
public class ThreadLocalPrintStream extends PrintStream {

    private final PrintStream serverConsole;
    private static final ThreadLocal<PrintStream> clientStream = new ThreadLocal<>();

    public ThreadLocalPrintStream(PrintStream original) {
        super(original, true);
        this.serverConsole = original;
    }

    /** Call this at the start of a client handler thread */
    public static void setClientStream(PrintStream ps) {
        clientStream.set(ps);
    }

    /** Call this when a client disconnects */
    public static void clearClientStream() {
        clientStream.remove();
    }

    private void writeToClient(String msg) {
        PrintStream cs = clientStream.get();
        if (cs != null) {
            cs.println(msg);
            cs.flush();
        }
    }

    @Override
    public void println(String x) {
        serverConsole.println(x);
        writeToClient(x);
    }

    @Override
    public void println(Object x) {
        String s = String.valueOf(x);
        serverConsole.println(s);
        writeToClient(s);
    }

    @Override
    public void println() {
        serverConsole.println();
        PrintStream cs = clientStream.get();
        if (cs != null) cs.println();
    }

    @Override
    public void print(String x) {
        serverConsole.print(x);
        PrintStream cs = clientStream.get();
        if (cs != null) cs.print(x);
    }

    @Override
    public void print(Object x) {
        serverConsole.print(x);
        PrintStream cs = clientStream.get();
        if (cs != null) cs.print(x);
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        String s = String.format(format, args);
        serverConsole.print(s);
        PrintStream cs = clientStream.get();
        if (cs != null) cs.print(s);
        return this;
    }
}
