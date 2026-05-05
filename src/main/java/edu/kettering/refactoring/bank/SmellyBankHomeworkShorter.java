package edu.kettering.refactoring.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Refactoring Homework: Bank Accounts (Checking/Savings)
 *
 *
 * Smells included:
 *  - mysterious names, WITH comments: z, t, q
 *  - Duplicated code (reordered / slightly different)
 *  - Long function + long parameter list
 *  - Loop(s) that can be map/filter transforms
 *  - Data updates mixed with other commands 
 */
public class SmellyBankHomeworkShorter {

    // --- Domain (mutable on purpose) ---
    static abstract class BankAccount {
        private final String id;
        private final String owner;
        protected double bal;
        private boolean flagged;

        protected BankAccount(String id, String owner, double bal) {
            this.id = id; this.owner = owner; this.bal = bal;
        }

        public String getId() { return id; }
        public String getOwner() { return owner; }
        public double getBalance() { return bal; }
        public boolean getFlagged() { return flagged; }
        public void setFlagged(boolean v) { flagged = v; }
        public abstract String getType();
    }

    static class CheckingAccount extends BankAccount {
        private final double overdraft;

        public CheckingAccount(String id, String owner, double bal, double overdraft) {
            super(id, owner, bal); 
            this.overdraft = overdraft;
        }

        public double overdraft() { return overdraft; }
        public String getType() { return "CHECKING"; }
    }

    static class SavingsAccount extends BankAccount {

        public SavingsAccount(String id, String owner, double bal) {
            super(id, owner, bal); 
        }

        public String getType() { return "SAVINGS"; }
    }

    static class Transaction {
        final String acctId, kind, memo; // kind: DEPOSIT/WITHDRAW ONLY
        final double amt;

        Transaction(String acctId, String kind, double amt, String memo) {
            this.acctId = acctId; 
            this.kind = kind; 
            this.amt = amt; 
            this.memo = memo;
        }
    }

    static class BatchProcessingOptions {
        private final boolean includeZeroAmountTransactions;
        private final double flagLargeTransactionThreshold;
        private final double vipBalanceThreshold;

        BatchProcessingOptions(
                boolean includeZeroAmountTransactions,
                double flagLargeTransactionThreshold,
                double vipBalanceThreshold
        ) {
            this.includeZeroAmountTransactions = includeZeroAmountTransactions;
            this.flagLargeTransactionThreshold = flagLargeTransactionThreshold;
            this.vipBalanceThreshold = vipBalanceThreshold;
        }

        public boolean includeZeroAmountTransactions() { return includeZeroAmountTransactions; }
        public double flagLargeTransactionThreshold() { return flagLargeTransactionThreshold; }
        public double vipBalanceThreshold() { return vipBalanceThreshold; }
    }

    static class ReportOptions {
        private final boolean debug;
        private final String currency;
        private final int digits;
        private final boolean rounding;

        ReportOptions(boolean debug, String currency, int digits, boolean rounding) {
            this.debug = debug;
            this.currency = currency;
            this.digits = digits;
            this.rounding = rounding;
        }

        public boolean debug() { return debug; }
        public String currency() { return currency; }
        public int digits() { return digits; }
        public boolean rounding() { return rounding; }
    }

    static class TransactionApplicationResult {
        private final int appliedTransactionCount;
        private final int skippedTransactionCount;
        private final double absoluteAppliedAmountTotal;

        TransactionApplicationResult(
                int appliedTransactionCount,
                int skippedTransactionCount,
                double absoluteAppliedAmountTotal
        ) {
            this.appliedTransactionCount = appliedTransactionCount;
            this.skippedTransactionCount = skippedTransactionCount;
            this.absoluteAppliedAmountTotal = absoluteAppliedAmountTotal;
        }

        public int appliedTransactionCount() { return appliedTransactionCount; }
        public int skippedTransactionCount() { return skippedTransactionCount; }
        public double absoluteAppliedAmountTotal() { return absoluteAppliedAmountTotal; }
    }

    public static void main(String[] args) {
        List<BankAccount> accounts = new ArrayList<>();
        accounts.add(new CheckingAccount("C-100", "A. Chen", 250, 100));
        accounts.add(new SavingsAccount("S-200", "B. Patel", 1200));
        accounts.add(new CheckingAccount("C-300", "C. Rivera", 40, 50));
        accounts.add(new SavingsAccount("S-400", "D. Smith", 9000));

           // Five transactions chosen to exercise all behavior paths
        List<Transaction> transactions = List.of(
        // 1) Normal withdrawal from checking (allowed)
        new Transaction("C-100", "WITHDRAW", 75, "ATM withdrawal"),

        // 2) Withdrawal from checking that exceeds overdraft (DECLINED)
        new Transaction("C-300", "WITHDRAW", 120, "Billpay overdraft test"),

        // 3) Withdrawal from savings that would go negative (DECLINED)
        new Transaction("S-200", "WITHDRAW", 1300, "Savings overdraft test"),

        // 4) Large deposit that triggers FLAG + VIP NOTE
        new Transaction("S-400", "DEPOSIT", 1500, "Bonus deposit"),

        // 5) Small deposit to verify normal deposit path
        new Transaction("C-100", "DEPOSIT", 25, "Cash deposit")
);
        BatchProcessingOptions batchProcessingOptions = new BatchProcessingOptions(
                false,
                1000.0,
                5000.0
        );
        ReportOptions reportOptions = new ReportOptions(
                true,
                "USD",
                2,
                true
        );

        System.out.println(processDailyBatch(
                accounts, transactions,
                batchProcessingOptions,
                reportOptions
        ));
    }

    // Long function + long-ish parameter list + mixed responsibilities (intentionally)
    public static String processDailyBatch(
            List<BankAccount> inputAccounts,
            List<Transaction> inputTransactions,
            BatchProcessingOptions processingOptions,
            ReportOptions reportOptions
    ) {
        StringBuilder report = new StringBuilder();
        report.append("=== BANK BATCH REPORT ===\n");

        Map<String, BankAccount> accountsById = new HashMap<>();
        for (BankAccount account : inputAccounts) accountsById.put(account.getId(), account);

        List<Transaction> transactions = filterTransactions(
                inputTransactions,
                processingOptions,
                reportOptions,
                report
        );

        int appliedTransactionCount = 0;
        int skippedTransactionCount = 0;
        double absoluteAppliedAmountTotal = 0.0;

        report.append("\n-- APPLY --\n");
        for (Transaction transaction : transactions) {
            TransactionApplicationResult transactionApplicationResult = applyTransaction(
                    transaction,
                    accountsById,
                    processingOptions,
                    reportOptions,
                    report
            );
            appliedTransactionCount += transactionApplicationResult.appliedTransactionCount();
            skippedTransactionCount += transactionApplicationResult.skippedTransactionCount();
            absoluteAppliedAmountTotal += transactionApplicationResult.absoluteAppliedAmountTotal();
        }

        report.append("-- POST-CHECKS --\n");
        for (BankAccount account : inputAccounts) {
            if (account instanceof CheckingAccount c) {
                if (account.getBalance() < -c.overdraft()) { account.setFlagged(true); report.append("Flag ").append(account.getId()).append(" beyond overdraft\n"); }
            } else {
                if (account.getBalance() < 0) { account.setFlagged(true); report.append("Flag ").append(account.getId()).append(" negative savings\n"); }
            }
        }


        report.append("\n-- SUMMARY A --\n");
        for (BankAccount account : inputAccounts)
            report.append(account.getId()).append(" ").append(account.getType()).append(" ").append(account.getOwner())
                    .append(" bal=").append(fmt(account.getBalance(), reportOptions.digits(), reportOptions.rounding()))
                    .append(account.getFlagged() ? " [FLAG]" : "").append("\n");

        report.append("\n-- TOTALS --\n");
        report.append("applied=").append(appliedTransactionCount).append(" skipped=").append(skippedTransactionCount)
                .append(" absTotal=").append(fmt(absoluteAppliedAmountTotal, reportOptions.digits(), reportOptions.rounding())).append(" ").append(reportOptions.currency()).append("\n");

        report.append("\n-- SUMMARY B --\n");
        for (int i = inputAccounts.size() - 1; i >= 0; i--) {
            BankAccount account = inputAccounts.get(i);
            report.append("[").append(account.getType()).append("] ").append(account.getOwner())
                    .append(" id=").append(account.getId())
                    .append(" bal=").append(fmt(account.getBalance(), reportOptions.digits(), reportOptions.rounding()))
                    .append(account.getFlagged() ? " *" : "").append("\n");
        }

        return report.toString();
    }

    static List<Transaction> filterTransactions(
            List<Transaction> inputTransactions,
            BatchProcessingOptions processingOptions,
            ReportOptions reportOptions,
            StringBuilder report
    ) {
        List<Transaction> transactions = new ArrayList<>();
        for (Transaction transaction : inputTransactions) {
            if (processingOptions.includeZeroAmountTransactions() || transaction.amt != 0.0) {
                transactions.add(transaction);
            } else if (reportOptions.debug()) {
                report.append("[dbg] filtered zero transaction for ").append(transaction.acctId).append("\n");
            }
        }
        return transactions;
    }

    static TransactionApplicationResult applyTransaction(
            Transaction transaction,
            Map<String, BankAccount> accountsById,
            BatchProcessingOptions processingOptions,
            ReportOptions reportOptions,
            StringBuilder report
    ) {
        int appliedTransactionCount = 0;
        int skippedTransactionCount = 0;
        double absoluteAppliedAmountTotal = 0.0;

        BankAccount account = accountsById.get(transaction.acctId);

        if (account == null) {
            skippedTransactionCount++;
            if (reportOptions.debug()) report.append("[dbg] unknown ").append(transaction.acctId).append("\n");
            return new TransactionApplicationResult(
                    appliedTransactionCount,
                    skippedTransactionCount,
                    absoluteAppliedAmountTotal
            );
        }

        report.append(transaction.kind).append(" acct=").append(account.getId())
                .append(" owner=").append(account.getOwner())
                .append(" amt=").append(fmt(transaction.amt, reportOptions.digits(), reportOptions.rounding())).append(" ").append(reportOptions.currency())
                .append(" memo=").append(transaction.memo).append("\n");

        TransactionApplicationResult transactionApplicationResult;
        if (transaction.kind.equals("DEPOSIT")) {
            transactionApplicationResult = handleDeposit(transaction, account, reportOptions, report);
        } else if (transaction.kind.equals("WITHDRAW")) {
            transactionApplicationResult = handleWithdrawal(transaction, account, reportOptions, report);
        } else {
            skippedTransactionCount++;
            report.append("  SKIP unknown kind\n");
            transactionApplicationResult = new TransactionApplicationResult(
                    appliedTransactionCount,
                    skippedTransactionCount,
                    absoluteAppliedAmountTotal
            );
        }

        appliedTransactionCount += transactionApplicationResult.appliedTransactionCount();
        skippedTransactionCount += transactionApplicationResult.skippedTransactionCount();
        absoluteAppliedAmountTotal += transactionApplicationResult.absoluteAppliedAmountTotal();

        if (Math.abs(transaction.amt) >= processingOptions.flagLargeTransactionThreshold()) {
            account.setFlagged(true);
            report.append("  ** FLAG large transaction **\n");
        }
        if (account.getBalance() >= processingOptions.vipBalanceThreshold()) report.append("  VIP NOTE\n");
        report.append("\n");

        return new TransactionApplicationResult(
                appliedTransactionCount,
                skippedTransactionCount,
                absoluteAppliedAmountTotal
        );
    }

    static TransactionApplicationResult handleDeposit(
            Transaction transaction,
            BankAccount account,
            ReportOptions reportOptions,
            StringBuilder report
    ) {
        int appliedTransactionCount = 0;
        int skippedTransactionCount = 0;
        double absoluteAppliedAmountTotal = 0.0;

        account.bal += transaction.amt;
        appliedTransactionCount++;
        absoluteAppliedAmountTotal += Math.abs(transaction.amt);
        report.append("  newBal=").append(fmt(account.bal, reportOptions.digits(), reportOptions.rounding())).append("\n");

        return new TransactionApplicationResult(
                appliedTransactionCount,
                skippedTransactionCount,
                absoluteAppliedAmountTotal
        );
    }

    static TransactionApplicationResult handleWithdrawal(
            Transaction transaction,
            BankAccount account,
            ReportOptions reportOptions,
            StringBuilder report
    ) {
        int appliedTransactionCount = 0;
        int skippedTransactionCount = 0;
        double absoluteAppliedAmountTotal = 0.0;
        boolean ok;

        if (account instanceof CheckingAccount c) ok = (account.bal - transaction.amt) >= -c.overdraft();
        else ok = (account.bal - transaction.amt) >= 0;

        if (!ok) {
            skippedTransactionCount++;
            report.append("  DECLINED\n");
        } else {
            account.bal -= transaction.amt;
            appliedTransactionCount++;
            absoluteAppliedAmountTotal += Math.abs(transaction.amt);
            report.append("  newBal=").append(fmt(account.bal, reportOptions.digits(), reportOptions.rounding())).append("\n");
        }

        return new TransactionApplicationResult(
                appliedTransactionCount,
                skippedTransactionCount,
                absoluteAppliedAmountTotal
        );
    }

    static String fmt(double v, int digits, boolean rounding) {
        if (!rounding) return Double.toString(v);
        double f = Math.pow(10, digits);
        double r = Math.round(v * f) / f;
        return String.format(Locale.US, "%." + digits + "f", r);
    }
}
