package kernel;

public class ModeBit {
    public enum Mode{
        USER_MODE,
        KERNEL_MODE
    }

    private Mode currentMode;

    public ModeBit(){
        currentMode=Mode.USER_MODE;
    }

    public synchronized void enterKernelMode(){
        currentMode=Mode.KERNEL_MODE;
    }

    public synchronized void enterUserMode(){
        currentMode=Mode.USER_MODE;
    }

    public synchronized boolean isKernelMode(){
        return currentMode==Mode.KERNEL_MODE;
    }
}
