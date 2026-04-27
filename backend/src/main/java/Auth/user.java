package Auth;

public class user {
    public enum Role{
        USER,
        ADMIN
    }
    
    private String username;
    private String password;
    private Role role;
    private boolean canTransfer;

    public user(String username, String password, Role role){
        this.username=username;
        this.password=password;
        this.role=role;

        if(role == Role.ADMIN){
            this.canTransfer=false;//Admin doesn't have access to transfer
        }
        else{ 
            this.canTransfer=true;//only user has that control
        }
    }

    public String getUsername(){
        return username;
    }
    public String getPassword(){
        return password;
    }
    public Role getRole(){
        return role;
    }
    public boolean canTransfer(){
        return canTransfer;
    }
    public void setTransferPermission(boolean value){
        this.canTransfer=value;
    }
    public boolean isAdmin(){
        return (role == Role.ADMIN);
    }
    public boolean isTransferPermission() {
        return canTransfer;
    }
}
