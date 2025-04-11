import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

@Entity
@Table(name = "accounts")
class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionRecord> transactions;

    public Account() {
    }

    public Account(String accountHolderName, BigDecimal balance) {
        this.accountHolderName = accountHolderName;
        this.balance = balance;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(Long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public List<TransactionRecord> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionRecord> transactions) {
        this.transactions = transactions;
    }

    @Override
    public String toString() {
        return "Account{" +
               "accountNumber=" + accountNumber +
               ", accountHolderName='" + accountHolderName + '\'' +
               ", balance=" + balance +
               '}';
    }
}

@Entity
@Table(name = "transactions")
class TransactionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "account_number", nullable = false)
    private Account account;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(nullable = false)
    private String description;

    public TransactionRecord() {
    }

    public TransactionRecord(Account account, LocalDateTime transactionDate, BigDecimal amount, TransactionType transactionType, String description) {
        this.account = account;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
               "transactionId=" + transactionId +
               ", transactionDate=" + transactionDate +
               ", amount=" + amount +
               ", transactionType=" + transactionType +
               ", description='" + description + '\'' +
               '}';
    }
}

enum TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER_SENT, TRANSFER_RECEIVED
}

@Configuration
@ComponentScan("com.example")
class AppConfig {

    @Bean
    public SessionFactory sessionFactory() {
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        configuration.addAnnotatedClass(Account.class);
        configuration.addAnnotatedClass(TransactionRecord.class);
        return configuration.buildSessionFactory();
    }
}

@Component
class BankingService {

    private final SessionFactory sessionFactory;

    public BankingService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) throws InsufficientFundsException {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Account fromAccount = session.get(Account.class, fromAccountId);
            Account toAccount = session.get(Account.class, toAccountId);

            if (fromAccount == null || toAccount == null) {
                throw new IllegalArgumentException("Invalid account numbers.");
            }

            if (fromAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds in account: " + fromAccountId);
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            session.update(fromAccount);
            session.update(toAccount);

            session.save(new TransactionRecord(fromAccount, LocalDateTime.now(), amount, TransactionType.TRANSFER_SENT, "Transfer to account " + toAccountId));
            session.save(new TransactionRecord(toAccount, LocalDateTime.now(), amount, TransactionType.TRANSFER_RECEIVED, "Transfer from account " + fromAccountId));

            tx.commit();
            System.out.println("Successfully transferred " + amount + " from account " + fromAccountId + " to " + toAccountId);

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("Transaction failed: " + e.getMessage());
            throw e; // Re-throw to be caught by the caller
        } finally {
            session.close();
        }
    }

    public void deposit(Long accountId, BigDecimal amount) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Account account = session.get(Account.class, accountId);
            if (account != null) {
                account.setBalance(account.getBalance().add(amount));
                session.update(account);
                session.save(new TransactionRecord(account, LocalDateTime.now(), amount, TransactionType.DEPOSIT, "Deposit"));
                tx.commit();
                System.out.println("Successfully deposited " + amount + " into account " + accountId);
            } else {
                System.out.println("Account not found: " + accountId);
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("Deposit failed: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public void withdraw(Long accountId, BigDecimal amount) throws InsufficientFundsException {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Account account = session.get(Account.class, accountId);
            if (account != null) {
                if (account.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientFundsException("Insufficient funds in account: " + accountId);
                }
                account.setBalance(account.getBalance().subtract(amount));
                session.update(account);
                session.save(new TransactionRecord(account, LocalDateTime.now(), amount, TransactionType.WITHDRAWAL, "Withdrawal"));
                tx.commit();
                System.out.println("Successfully withdrew " + amount + " from account " + accountId);
            } else {
                System.out.println("Account not found: " + accountId);
            }
        } catch (InsufficientFundsException e) {
            if (tx != null) tx.rollback();
            System.err.println("Withdrawal failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("Withdrawal failed: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    public Account getAccount(Long accountId) {
        Session session = sessionFactory.openSession();
        try {
            return session.get(Account.class, accountId);
        } finally {
            session.close();
        }
    }
}

class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

public class BankingApp {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        BankingService bankingService = context.getBean(BankingService.class);
        SessionFactory sessionFactory = context.getBean(SessionFactory.class);

        // Initialize some accounts
        Session initSession = sessionFactory.openSession();
        Transaction initTx = null;
        try {
            initTx = initSession.beginTransaction();
            initSession.save(new Account("Alice", new BigDecimal("1000.00")));
            initSession.save(new Account("Bob", new BigDecimal("500.00")));
            initTx.commit();
        } catch (Exception e) {
            if (initTx != null) initTx.rollback();
            e.printStackTrace();
        } finally {
            initSession.close();
        }

        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\nBanking System Menu:");
            System.out.println("1. Transfer Money");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Check Account Balance");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter sender account number: ");
                        Long fromAccount = scanner.nextLong();
                        System.out.print("Enter receiver account number: ");
                        Long toAccount = scanner.nextLong();
                        System.out.print("Enter amount to transfer: ");
                        BigDecimal transferAmount = scanner.nextBigDecimal();
                        bankingService.transferMoney(fromAccount, toAccount, transferAmount);
                        break;
                    case 2:
                        System.out.print("Enter account number to deposit into: ");
                        Long depositAccount = scanner.nextLong();
                        System.out.print("Enter amount to deposit: ");
                        BigDecimal depositAmount = scanner.nextBigDecimal();
                        bankingService.deposit(depositAccount, depositAmount);
                        break;
                    case 3:
                        System.out.print("Enter account number to withdraw from: ");
                        Long withdrawAccount = scanner.nextLong();
                        System.out.print("Enter amount to withdraw: ");
                        BigDecimal withdrawAmount = scanner.nextBigDecimal();
                        bankingService.withdraw(withdrawAccount, withdrawAmount);
                        break;
                    case 4:
                        System.out.print("Enter account number to check balance: ");
                        Long checkAccount = scanner.nextLong();
                        Account account = bankingService.getAccount(checkAccount);
                        if (account != null) {
                            System.out.println("Account " + checkAccount + " balance: " + account.getBalance());
                        } else {
                            System.out.println("Account not found.");
                        }
                        break;
                    case 0:
                        System.out.println("Exiting banking system.");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (InsufficientFundsException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("An unexpected error occurred: " + e.getMessage());
            }
        } while (choice != 0);

        if (sessionFactory != null) {
            sessionFactory.close();
        }
        context.close();
        scanner.close();
    }
}
