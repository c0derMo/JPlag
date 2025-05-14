import java.util.Arrays;

public class IfElse2 {
    // Some Line comments
    public static void main(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException();
        } else {
            System.out.println(Arrays.toString(args));
        }
    }
}
