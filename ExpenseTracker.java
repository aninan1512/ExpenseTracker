package com.example.expensetracker;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class ExpenseTracker {

    private static final Path FILE = Paths.get("expenses.csv");
    private static final String HEADER = "Amount,Category,Description";

    public static void main(String[] args) {
        ensureFileExists();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                printMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> addExpense(scanner);
                    case "2" -> viewAllExpenses();
                    case "3" -> viewTotalSpent();
                    case "4" -> {
                        System.out.println("Goodbye 👋");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.\n");
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("""
                =========================
                💰 Expense Tracker (Java)
                1. Add Expense
                2. View All Expenses
                3. View Total Spent
                4. Exit
                =========================
                """);
        System.out.print("Choose an option: ");
    }

    private static void ensureFileExists() {
        try {
            if (Files.notExists(FILE)) {
                Files.writeString(FILE, HEADER + System.lineSeparator(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void addExpense(Scanner scanner) {
        Double amount = readDouble(scanner, "Amount: ");
        if (amount == null) return;

        System.out.print("Category: ");
        String category = scanner.nextLine().trim();
        if (category.isEmpty()) {
            System.out.println("Category cannot be empty.\n");
            return;
        }

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) {
            System.out.println("Description cannot be empty.\n");
            return;
        }

        String line = String.format(Locale.US, "%.2f,%s,%s%n",
                amount, escapeCsv(category), escapeCsv(description));

        try {
            Files.writeString(FILE, line, StandardOpenOption.APPEND);
            System.out.println("✅ Expense added successfully\n");
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage() + "\n");
        }
    }

    private static void viewAllExpenses() {
        System.out.println("\n--- All Expenses ---");
        try (Stream<String> lines = Files.lines(FILE)) {
            lines.skip(1).forEach(line -> {
                String[] parts = parseCsvLine(line);
                if (parts.length < 3) return;

                String amount = parts[0];
                String category = parts[1];
                String description = parts[2];

                System.out.printf("₹%s | %s | %s%n", amount, category, description);
            });
            System.out.println();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage() + "\n");
        }
    }

    private static void viewTotalSpent() {
        double total = 0.0;

        try (Stream<String> lines = Files.lines(FILE)) {
            total = lines.skip(1)
                    .map(ExpenseTracker::parseCsvLine)
                    .filter(parts -> parts.length >= 1)
                    .map(parts -> parts[0])
                    .mapToDouble(s -> {
                        try {
                            return Double.parseDouble(s);
                        } catch (NumberFormatException e) {
                            return 0.0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage() + "\n");
            return;
        }

        System.out.printf("%n💸 Total Spent: ₹%.2f%n%n", total);
    }

    private static Double readDouble(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        try {
            double value = Double.parseDouble(input);
            if (value <= 0) {
                System.out.println("Amount must be greater than 0.\n");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Try again.\n");
            return null;
        }
    }

    // --- Basic CSV handling (supports commas and quotes) ---

    private static String escapeCsv(String value) {
        if (value.contains("\"")) value = value.replace("\"", "\"\"");
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value + "\"";
        }
        return value;
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); // escaped quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }

        result.add(cur.toString());
        return result.toArray(new String[0]);
    }
}

