package app.chat.test;

public class Main {

    public static void main(String[] args) {
        System.out.println("Сработал main");
        try {
            Class.forName("MyClass");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
