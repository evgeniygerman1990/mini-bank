package german.dev;

import german.dev.config.HibernateUtil;
import german.dev.config.JdbcConnectionFactory;
import german.dev.demo.HibernateDemo;
import org.hibernate.SessionFactory;
import german.dev.dao.hibernate.HibernateAccountRepository;
import german.dev.dao.hibernate.HibernateTransferRepository;
import german.dev.dao.hibernate.HibernateTransactionManager;
import german.dev.dao.jdbc.JdbcAccountRepository;
import german.dev.dao.jdbc.JdbcTransactionManager;
import german.dev.dao.jdbc.JdbcTransferRepository;
import german.dev.service.DefaultTransferService;
import german.dev.service.ReportService;
import german.dev.service.TransferService;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "hibernate";
        if (!"jdbc".equals(mode) && !"hibernate".equals(mode)) {
            throw new IllegalArgumentException("Неизвестный режим: " + mode);
        }
        if ("jdbc".equals(mode)) {
            JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory();
            JdbcAccountRepository jdbcAccountRepository =
                    new JdbcAccountRepository(jdbcConnectionFactory);
            JdbcTransferRepository jdbcTransferRepository =
                    new JdbcTransferRepository(jdbcConnectionFactory);
            JdbcTransactionManager jdbcTransactionManager =
                    new JdbcTransactionManager(jdbcConnectionFactory);


            TransferService jdbcTransferService = new DefaultTransferService(
                    jdbcAccountRepository,
                    jdbcTransferRepository,
                    jdbcTransactionManager);

            ReportService jdbcReportService = new ReportService(
                    jdbcAccountRepository,
                    jdbcTransferRepository);
            runDemo(jdbcTransferService, jdbcReportService);
        } else {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            try {
                if (args.length > 1 && "--orm-demo".equals(args[1])) {
                    new HibernateDemo(sessionFactory).run();
                    return;
                }

                HibernateAccountRepository hibernateAccountRepository =
                        new HibernateAccountRepository(sessionFactory);

                HibernateTransferRepository hibernateTransferRepository =
                        new HibernateTransferRepository(sessionFactory);

                HibernateTransactionManager hibernateTransactionManager =
                        new HibernateTransactionManager(sessionFactory);


                TransferService hibernateTransferService = new DefaultTransferService(
                        hibernateAccountRepository,
                        hibernateTransferRepository,
                        hibernateTransactionManager);


                ReportService hibernateReportService = new ReportService(
                        hibernateAccountRepository,
                        hibernateTransferRepository);
                runDemo(hibernateTransferService, hibernateReportService);
            } finally {
                HibernateUtil.closeSessionFactory();
            }
        }
    }

    private static void runDemo(TransferService transferService, ReportService reportService) {
        boolean executeTransfer = false;
        System.out.println("Запуск демонстрации Mini Bank");
        reportService.printClientBalanceSummary();
        reportService.printAccountsWithTransfers();
        reportService.printClientAccountsReport(1L);
        reportService.printAccountTransferHistory("002");
        reportService.printRichClients(new BigDecimal("100000.00"));
        if (executeTransfer) {
            transferService.transfer("002", "004",
                    new BigDecimal("100.00"), "Демонстрационный перевод");
        } else {
            System.out.println("Демонстрационный перевод отключён");
        }
    }
}
