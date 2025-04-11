import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 1. Define a Course class
class Course {
    private String courseName;
    private String duration;

    public Course(String courseName, String duration) {
        this.courseName = courseName;
        this.duration = duration;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Course{" +
               "courseName='" + courseName + '\'' +
               ", duration='" + duration + '\'' +
               '}';
    }
}

// 2. Define a Student class
class Student {
    private String name;
    private Course course;

    public Student(String name, Course course) {
        this.name = name;
        this.course = course;
    }

    public String getName() {
        return name;
    }

    public Course getCourse() {
        return course;
    }

    public void displayDetails() {
        System.out.println("Student Name: " + name);
        System.out.println("Enrolled in: " + course);
    }
}

// 3. Use Java-based configuration (@Configuration and @Bean)
@Configuration
class AppConfig {

    @Bean
    public Course mathCourse() {
        return new Course("Mathematics", "3 months");
    }

    @Bean
    public Course scienceCourse() {
        return new Course("Science", "4 months");
    }

    @Bean
    public Student student1() {
        return new Student("Alice", mathCourse()); // Injecting mathCourse bean
    }

    @Bean
    public Student student2() {
        return new Student("Bob", scienceCourse()); // Injecting scienceCourse bean
    }
}

public class SpringDiExample {
    public static void main(String[] args) {
        // 4. Load the Spring context in the main method
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Get the student beans from the context
        Student student1 = context.getBean("student1", Student.class);
        Student student2 = context.getBean("student2", Student.class);

        // Print student details
        System.out.println("--- Student 1 Details ---");
        student1.displayDetails();

        System.out.println("\n--- Student 2 Details ---");
        student2.displayDetails();

        // Close the Spring context
        context.close();
    }
}
