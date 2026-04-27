package Auth;

public class Session {
    // Static memory: keeps the user consistently logged in across all REST API requests
    private static user currentUser = null;

    public static void login(user User){
        currentUser = User;
    }

    public static void logout(){
        currentUser = null;
    }

    public static user getCurrentUser(){
        return currentUser;
    }

    public static boolean isLoggedIn(){
        return currentUser != null;
    }
}
