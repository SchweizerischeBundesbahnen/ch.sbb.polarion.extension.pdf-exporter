package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.internal.model.TypeOpt;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.internal.PrimitiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolarionTypesTest {

    private Locale defaultLocale;
    private TimeZone defaultTz;

    @BeforeEach
    void saveDefaults() {
        defaultLocale = Locale.getDefault();
        defaultTz = TimeZone.getDefault();
    }

    @AfterEach
    void restoreDefaults() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTz);
    }

    @Test
    void convertSingleFieldValueToString_handlesNull() {
        String result = PolarionTypes.convertSingleFieldValueToString(null, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(result).isEmpty();
    }

    @Test
    void convertsTypeOptByName() {
        TypeOpt typeOpt = mock(TypeOpt.class);
        when(typeOpt.getName()).thenReturn("MyType");
        String result = PolarionTypes.convertSingleFieldValueToString(typeOpt, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(result).isEqualTo("MyType");
    }

    @Test
    void convertsTextToHtmlContent() {
        Text text = Text.html("Hello <b>World</b>");
        String result = PolarionTypes.convertSingleFieldValueToString(text, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(result).isEqualTo("Hello <b>World</b>");
    }

    @Test
    void convertsDateOnlyWithLocale() throws Exception {
        String result = PolarionTypes.convertSingleFieldValueToString(DateOnly.parse("2023-10-20"), Locale.US, TimeZone.getTimeZone("UTC"));
        assertThat(result).isEqualTo("October 20, 2023");
    }

    @Test
    void convertsTimeOnlyWithTimezone() throws Exception {
        String result = PolarionTypes.convertSingleFieldValueToString(TimeOnly.parse("12:01:02.000 +0000"), Locale.US, TimeZone.getTimeZone("CET"));
        // Locale.US long time style should produce 'PM' marker
        assertThat(result).contains("PM");
        assertThat(result).contains(":01:02");
    }

    @Test
    void convertsDateWithTimezone() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS Z").parse("2018-09-09 12:13:14.000 +0000");
        String result = PolarionTypes.convertSingleFieldValueToString(date, Locale.US, TimeZone.getTimeZone("Europe/Zurich"));
        assertThat(result).contains("September 9, 2018");
        assertThat(result).contains("2:13:14 AM"); // CET/CEST offset vs UTC
    }

    @Test
    void convertsCurrencyValueToString() {
        Currency currency = new Currency(BigDecimal.TEN);
        String result = PolarionTypes.convertSingleFieldValueToString(currency, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(result).isEqualTo("10.00");
    }

    @Test
    void convertsEnumOptionPrefersNameThenId() {
        IEnumOption withName = new EnumOption("status", "open", "Open", 1, false);
        String r1 = PolarionTypes.convertSingleFieldValueToString(withName, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(r1).isEqualTo("Open");

        IEnumOption withoutName = new EnumOption("status", "open", null, 1, false);
        String r2 = PolarionTypes.convertSingleFieldValueToString(withoutName, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(r2).isEqualTo("open");
    }

    @Test
    void convertsUserByLabel() {
        IUser user = mock(IUser.class);
        when(user.getLabel()).thenReturn("System Administrator");
        String result = PolarionTypes.convertSingleFieldValueToString(user, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(result).isEqualTo("System Administrator");
    }

    @Test
    void fallsBackToToString() {
        Object obj = new Object() {
            @Override
            public String toString() {
                return "X";
            }
        };
        String result = PolarionTypes.convertSingleFieldValueToString(obj, Locale.US, TimeZone.getTimeZone("CET"));
        assertThat(result).isEqualTo("X");
    }

    @Test
    void convertsCustomTypedListUsingJoinAndInnerConversion() {
        IPObject iPObject = mock(IPObject.class);
        IListType listType = mock(IListType.class);
        when(listType.getItemType()).thenReturn(new PrimitiveType("string"));
        CustomTypedList list = new CustomTypedList(iPObject, listType, false, List.of("A", "B"));
        String result = PolarionTypes.convertListToString(list, Locale.US, TimeZone.getTimeZone("UTC"));
        assertThat(result).isEqualTo("A, B");
    }

    @Test
    void topLevelConvertDispatchesListVsSingle() {
        // list path
        IPObject iPObject = mock(IPObject.class);
        IListType listType = mock(IListType.class);
        when(listType.getItemType()).thenReturn(new PrimitiveType("string"));
        CustomTypedList list = new CustomTypedList(iPObject, listType, false, List.of("A", "B"));
        String r1 = PolarionTypes.convertFieldValueToString(list, Locale.US, TimeZone.getTimeZone("UTC"));
        assertThat(r1).isEqualTo("A, B");

        // single path
        String r2 = PolarionTypes.convertFieldValueToString("C", Locale.US, TimeZone.getTimeZone("UTC"));
        assertThat(r2).isEqualTo("C");
    }

    @Test
    void overloadWithoutLocaleUsesDefaults() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Date date = new GregorianCalendar(2020, Calendar.JANUARY, 15, 10, 5, 0).getTime();
        String result = PolarionTypes.convertFieldValueToString(date);
        assertThat(result).contains("January 15, 2020");
    }
}
