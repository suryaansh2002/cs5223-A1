public class Logger {

    public static void info(String message) {
        System.out.println("Info- " + message);
    }

    public static void error(String message) {
        System.out.println("Error- " + message + " !!");
    }

    public static void exception (Exception exception) {
        System.out.println("Exception- " + exception + " !!");
    }
}
