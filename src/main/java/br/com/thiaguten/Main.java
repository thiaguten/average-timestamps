package br.com.thiaguten;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToLongFunction;
import org.bson.Document;

public class Main {

  public static final String TM_FORMAT = "%02dd %02dh:%02dm:%02ds";

  public static void main(String[] args) {

    Document chat1 = new Document()
        .append("requestedAt", Instant.now().toEpochMilli())
        .append("acceptedAt", Instant.now().plusSeconds(3).toEpochMilli())
        .append("closedAt", Instant.now().plusSeconds(86400).toEpochMilli());

    Document chat2 = new Document()
        .append("requestedAt", Instant.now().plusSeconds(4).toEpochMilli())
        .append("acceptedAt", Instant.now().plusSeconds(8).toEpochMilli())
        .append("closedAt", Instant.now().plusSeconds(1800).toEpochMilli());

    Document chat3 = new Document()
        .append("requestedAt", Instant.now().plusSeconds(10).toEpochMilli())
        .append("acceptedAt", Instant.now().plusSeconds(16).toEpochMilli())
        .append("closedAt", Instant.now().plusSeconds(1800 * 2).toEpochMilli());

    Document chat4 = new Document()
        .append("requestedAt", Instant.now().plusSeconds(16).toEpochMilli())
        .append("acceptedAt", Instant.now().plusSeconds(20).toEpochMilli())
        .append("closedAt", Instant.now().plusSeconds(86400).toEpochMilli());

    final List<Document> chats = Arrays.asList(chat1, chat2, chat3, chat4);

    final double tma = calculateTMA(chats);
    final double tme = calculateTME(chats);
    // usar esse metodo somente se o ID do operador for informado na requisição.
//    double operatorTMA = calculateOperatorTMA(chats);
    final Document chatsHourMetrics = new Document(
        "chatsHourMetrics", Arrays.asList(
        new Document("tma", formatTM(tma)), new Document("tme", formatTM(tme))));

    System.out.printf("TMA: %.2f%n", tma);
    System.out.printf("TME: %.2f%n", tme);
    System.out.println(chatsHourMetrics.toJson());
  }

  public static double calculateTMA(final List<Document> chats) {
    // TMA = Tempo médio de atendimento do chat.
    return calculateTM(chats, chat -> {
      long acceptedAt = chat.getLong("acceptedAt");
      // Caso o chat não possua a propriedade closedAt é porque o mesmo ainda está em andamento.
      // Assumir então o timestamp atual como valor do closedAt para um cálculo em tempo real do
      // tempo de atendimento até o momento.
      long closedAt = (Long) chat.getOrDefault("closedAt", Instant.now().toEpochMilli());
      // TA = tempo de atendimento (elapsed time).
      return closedAt - acceptedAt;
    });
  }

  public static double calculateTME(final List<Document> chats) {
    // TME = Tempo médio de espera do chat.
    return calculateTM(chats, chat -> {
      long requestedAt = chat.getLong("requestedAt");
      long acceptedAt = chat.getLong("acceptedAt");
      // TE = tempo de espera (elapsed time).
      return acceptedAt - requestedAt;
    });
  }

  // TMA = Tempo médio de atendimento do operador.
  public static double calculateOperatorTMA(final List<Document> chats) {
    // TODO verificar o formato do documento para pegar os valores abaixo.
    final ToLongFunction<Document> operatorTAMapper = chat -> {
      long startedAt = chat.getLong("startedAt");
      long endedAt = chat.getLong("endedAt");
      // TA = tempo de atendimento (elapsed time).
      return endedAt - startedAt;
    };
    return calculateTM(chats, operatorTAMapper);
  }

  // Calcula o Tempo Médio - TMA ou TME, depende da função passada como parâmetro.
  public static <T> double calculateTM(final List<T> chats, final ToLongFunction<T> mapper) {
    return chats.stream().mapToLong(mapper).average().orElse(0.0);
  }

  // Formata o Tempo Médio.
  public static String formatTM(final double tm) {
    if (tm > 0.0) {
      long tmInMillis = Math.round(tm);
      // https://www.mkyong.com/java/how-to-calculate-date-time-difference-in-java/
      long tmInSeconds = tmInMillis / 1000 % 60;
      long tmInMinutes = tmInMillis / (60 * 1000) % 60;
      long tmInHours = tmInMillis / (60 * 60 * 1000) % 24;
      long tmInDays = tmInMillis / (24 * 60 * 60 * 1000);
      return String.format(TM_FORMAT, tmInDays, tmInHours, tmInMinutes, tmInSeconds);
    } else {
      return String.format(TM_FORMAT, 0, 0, 0, 0);
    }
  }
}
