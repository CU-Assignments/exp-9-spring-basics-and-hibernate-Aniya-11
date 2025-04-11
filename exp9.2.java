import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import javax.persistence.*;
import java.util.List;
import java.util.Scanner;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private int age;

    public Student() {
    }

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Student{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", age=" + age +
               '}';
    }
}

public class HibernateCRUDApp {

    private static SessionFactory sessionFactory;

    static {
        try {
            // 1. Configure Hibernate using hibernate.cfg.xml (place this file in the src/main/resources folder)
            Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            configuration.addAnnotatedClass(Student.class); // Register the Student entity
            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    // 3. Implement Hibernate SessionFactory to perform CRUD operations
    public static void createStudent(Student student) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(student);
            tx.commit();
            System.out.println("Student created: " + student);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public static Student readStudent(int id) {
        Session session = sessionFactory.openSession();
        try {
            Student student = session.get(Student.class, id);
            if (student != null) {
                System.out.println("Student found: " + student);
            } else {
                System.out.println("Student with ID " + id + " not found.");
            }
            return student;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    public static void updateStudent(int id, String newName, int newAge) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Student student = session.get(Student.class, id);
            if (student != null) {
                student.setName(newName);
                student.setAge(newAge);
                session.update(student);
                tx.commit();
                System.out.println("Student updated: " + student);
            } else {
                System.out.println("Student with ID " + id + " not found, cannot update.");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public static void deleteStudent(int id) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Student student = session.get(Student.class, id);
            if (student != null) {
                session.delete(student);
                tx.commit();
                System.out.println("Student deleted with ID: " + id);
            } else {
                System.out.println("Student with ID " + id + " not found, cannot delete.");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public static List<Student> getAllStudents() {
        Session session = sessionFactory.openSession();
        try {
            return session.createQuery("from Student", Student.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\nHibernate CRUD Operations:");
            System.out.println("1. Create Student");
            System.out.println("2. Read Student");
            System.out.println("3. Update Student");
            System.out.println("4. Delete Student");
            System.out.println("5. List All Students");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter student name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter student age: ");
                    int age = scanner.nextInt();
                    createStudent(new Student(name, age));
                    break;
                case 2:
                    System.out.print("Enter student ID to read: ");
                    int readId = scanner.nextInt();
                    readStudent(readId);
                    break;
                case 3:
                    System.out.print("Enter student ID to update: ");
                    int updateId = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
                    System.out.print("Enter new student name: ");
                    String newName = scanner.nextLine();
                    System.out.print("Enter new student age: ");
                    int newAge = scanner.nextInt();
                    updateStudent(updateId, newName, newAge);
                    break;
                case 4:
                    System.out.print("Enter student ID to delete: ");
                    int deleteId = scanner.nextInt();
                    deleteStudent(deleteId);
                    break;
                case 5:
                    List<Student> allStudents = getAllStudents();
                    if (allStudents != null && !allStudents.isEmpty()) {
                        System.out.println("\nAll Students:");
                        for (Student s : allStudents) {
                            System.out.println(s);
                        }
                    } else {
                        System.out.println("No students found.");
                    }
                    break;
                case 0:
                    System.out.println("Exiting application.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 0);

        // Close the SessionFactory when the application exits
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        scanner.close();
    }
}
