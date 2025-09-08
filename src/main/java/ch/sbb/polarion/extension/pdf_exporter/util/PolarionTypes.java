package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.internal.model.TypeOpt;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.spi.CustomTypedList;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

@UtilityClass
public class PolarionTypes {

    public @NotNull String convertFieldValueToString(@Nullable Object fieldValue) {
        Locale locale = Locale.getDefault();
        TimeZone timeZone = TimeZone.getDefault();
        return convertFieldValueToString(fieldValue, locale, timeZone);
    }

    public @NotNull String convertFieldValueToString(@Nullable Object fieldValue, @NotNull Locale locale, @NotNull TimeZone timeZone) {
        if (fieldValue instanceof CustomTypedList customTypedList) {
            return convertListToString(customTypedList, locale, timeZone);
        } else {
            return convertSingleFieldValueToString(fieldValue, locale, timeZone);
        }
    }

    public @NotNull String convertSingleFieldValueToString(@Nullable Object fieldValue, @NotNull Locale locale, @NotNull TimeZone timeZone) {
        if (fieldValue == null) {
            return "";
        }

        if (fieldValue instanceof TypeOpt typeOpt) {
            return typeOpt.getName();
        } else if (fieldValue instanceof Text text) {
            return text.convertToHTML().getContent();
        } else if (fieldValue instanceof DateOnly dateOnly) {
            return DateFormat.getDateInstance(DateFormat.LONG, locale).format(dateOnly.getDate());
        } else if (fieldValue instanceof TimeOnly timeOnly) {
            return convertToTime(timeOnly.getDate(), locale, timeZone);
        } else if (fieldValue instanceof Date date) {
            return convertToDateTime(date, locale, timeZone);
        } else if (fieldValue instanceof Currency currency) {
            return currency.getValue().toString();
        } else if (fieldValue instanceof IEnumOption enumOption) {
            return enumOption.getName() != null ? enumOption.getName() : enumOption.getId();
        } else if (fieldValue instanceof IUser user) {
            return user.getLabel();
        }

        return fieldValue.toString();
    }

    @SuppressWarnings("unchecked")
    public @NotNull String convertListToString(@NotNull CustomTypedList list, @NotNull Locale locale, @NotNull TimeZone timeZone) {
        return (String) list.stream()
                .map(n -> convertFieldValueToString(n, locale, timeZone))
                .collect(Collectors.joining(", "));
    }

    public @NotNull String convertToTime(@NotNull Date fieldValue, @NotNull Locale locale, @NotNull TimeZone timeZone) {
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale);
        return formatForTimeZone(fieldValue, timeFormat, timeZone);
    }

    public @NotNull String convertToDateTime(@NotNull Date fieldValue, @NotNull Locale locale, @NotNull TimeZone timeZone) {
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
        return formatForTimeZone(fieldValue, dateTimeFormat, timeZone);
    }

    public @NotNull String formatForTimeZone(@NotNull Date fieldValue, @NotNull DateFormat format, @NotNull TimeZone timeZone) {
        format.setTimeZone(timeZone);
        return format.format(fieldValue);
    }

}
