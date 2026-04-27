package Auth;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import system.CsvStorage;
import system.PageTable;

public class AuthManager {
    private Map<String, user> users;
    private PageTable pageTable;
    private static final String AUTH_HEADER="username,password,role,canTransfer,state";
    
    public AuthManager(){
        users=new HashMap<>();
        pageTable=new PageTable();
        loadUsers();

        if(!users.containsKey("root")){
            //default admin(root user)
            user admin=new user("root", "root123", user.Role.ADMIN);
            users.put("root",admin);
            persistUsers();
        }
    }


    //register new user
    public boolean register(String username, String password){
        if(users.containsKey(username)){
            return false;
        }
        user newuser=new user(username, password, user.Role.USER);
        users.put(username, newuser);
        if(!persistUsers()){
            users.remove(username);
            return false;
        }
        return true;
    }


    //login validation
    public user login(String username, String password){
        if(!users.containsKey(username)){
            return null;
        }
        user user=users.get(username);
        if(user.getPassword().equals(password)){
            return user;
        }
        return null;
    }

    //get user (admin ops of seeing the username of an account)
    public user getuser(String username){
        return (users.containsKey(username))?users.get(username):null;
    }

    //get all users (admin ops of seeing roster and permissions)
    public List<user> getAllUsers(){
        return new ArrayList<>(users.values());
    }

    public boolean setTransferPermission(String username, boolean canTransfer){
        user target=users.get(username);
        if(target==null){
            return false;
        }
        boolean oldValue=target.canTransfer();
        target.setTransferPermission(canTransfer);
        if(!persistUsers()){
            target.setTransferPermission(oldValue);
            return false;
        }
        return true;
    }

    public boolean deeteuser(String username){
        if(!users.containsKey(username)){
            return false;
        }

        //don't delete root
        if(username.equals("root")) return false;

        users.remove(username);
        persistUsers();
        return true;
    }
    public boolean deleteUser(String username){
        user removed=users.remove(username);
        if(removed==null){
            return false;
        }
        if(!persistUsers()){
            users.put(username, removed);
            return false;
        }
        return true;
    }

    private void loadUsers(){
        try{
            pageTable.ensureFile(PageTable.AUTH_FILE, AUTH_HEADER);
            List<String[]> rows=CsvStorage.readRows(pageTable.getFile("login"));
            for(String[] row:rows){
                if(row.length<5 || !row[4].equalsIgnoreCase("COMMIT")){
                    continue;
                }
                user.Role role=user.Role.valueOf(row[2]);
                user loadedUser=new user(row[0], row[1], role);
                loadedUser.setTransferPermission(Boolean.parseBoolean(row[3]));
                users.put(row[0], loadedUser);
            }
        }
        catch(IOException | IllegalArgumentException e){
            System.out.println("User storage load failed.");
        }
    }

    private boolean persistUsers(){
        List<String[]> rows=new ArrayList<>();
        user rootUser=users.get("root");
        if(rootUser!=null){
            rows.add(toCsvRow(rootUser));
        }
        for(user u:users.values()){
            if(u.getUsername().equals("root")){
                continue;
            }
            rows.add(toCsvRow(u));
        }

        try{
            CsvStorage.writeRows(pageTable.getFile("login"), AUTH_HEADER, rows);
            return true;
        }
        catch(IOException e){
            System.out.println("User storage update failed.");
            return false;
        }
    }

    private String[] toCsvRow(user u){
        return new String[]{
            u.getUsername(),
            u.getPassword(),
            u.getRole().name(),
            Boolean.toString(u.canTransfer()),
            "COMMIT"
        };
    }
}
