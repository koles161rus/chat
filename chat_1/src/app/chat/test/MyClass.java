package app.chat.test;

public class MyClass {

    static {
        System.out.println("Класс MyClass загрузился в память");
    }

    MyClass(){
        System.out.println("Сработал конструктор");
    }
}
