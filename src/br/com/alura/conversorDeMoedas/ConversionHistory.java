package br.com.alura.conversorDeMoedas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe para armazenar e exibir o histórico de conversões de moeda.
 */
public class ConversionHistory {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final List<ConversionRecord> history;

    /**
     * Construtor para inicializar o histórico de conversões.
     */
    public ConversionHistory() {
        this.history = new ArrayList<>();
    }

    /**
     * Adiciona uma nova conversão ao histórico.
     *
     * @param fromCurrency Moeda de origem
     * @param toCurrency Moeda de destino
     * @param amount Valor original
     * @param convertedAmount Valor convertido
     */
    public void addConversion(String fromCurrency, String toCurrency, double amount, double convertedAmount) {
        ConversionRecord record = new ConversionRecord(
                LocalDateTime.now(),
                fromCurrency,
                toCurrency,
                amount,
                convertedAmount
        );
        history.add(record);
    }

    /**
     * Exibe o histórico de conversões.
     */
    public void displayHistory() {
        if (history.isEmpty()) {
            System.out.println("Nenhuma conversão realizada ainda.");
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            ConversionRecord record = history.get(i);

            System.out.printf("%d. [%s] %.2f %s → %.2f %s%n",
                    i + 1,
                    record.timestamp.format(DATE_FORMATTER),
                    record.originalAmount,
                    record.fromCurrency,
                    record.convertedAmount,
                    record.toCurrency
            );
        }
    }

    /**
     * Classe interna para representar um registro de conversão.
     */
    private static class ConversionRecord {
        private final LocalDateTime timestamp;
        private final String fromCurrency;
        private final String toCurrency;
        private final double originalAmount;
        private final double convertedAmount;

        /**
         * Construtor para criar um novo registro de conversão.
         *
         * @param timestamp Data e hora da conversão
         * @param fromCurrency Moeda de origem
         * @param toCurrency Moeda de destino
         * @param originalAmount Valor original
         * @param convertedAmount Valor convertido
         */
        public ConversionRecord(LocalDateTime timestamp, String fromCurrency, String toCurrency,
                                double originalAmount, double convertedAmount) {
            this.timestamp = timestamp;
            this.fromCurrency = fromCurrency;
            this.toCurrency = toCurrency;
            this.originalAmount = originalAmount;
            this.convertedAmount = convertedAmount;
        }
    }
}