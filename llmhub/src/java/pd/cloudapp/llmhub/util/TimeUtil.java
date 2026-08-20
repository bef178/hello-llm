package pd.cloudapp.llmhub.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    public static String toUtcString(long epochMilli) {
        return UTC_FORMATTER.format(Instant.ofEpochMilli(epochMilli));
    }
}
