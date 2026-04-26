package Auth;

public class Session {
    // ThreadLocal: each thread (each client connection) has its OWN session
    private static final ThreadLocal<user> currentUser = new ThreadLocal<>();

    public static void login(user User){
        currentUser.set(User);
    }

    public static void logout(){
        currentUser.remove();
    }

    public static user getCurrentUser(){
        return currentUser.get();
    }

    public static boolean isLoggedIn(){
        return currentUser.get() != null;
    }
}
