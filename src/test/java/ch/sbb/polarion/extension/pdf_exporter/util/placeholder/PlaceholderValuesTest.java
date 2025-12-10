package ch.sbb.polarion.extension.pdf_exporter.util.placeholder;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.internal.model.StatusOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.platform.security.ILoginPolicy;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.internal.PrimitiveType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceholderValuesTest {

    @Mock
    private IModule module;

    private static final IUser user = mockUser();

    @ParameterizedTest
    @MethodSource("provideFields")
    void shouldConvertStandardTypes(String fieldName, Object fieldValue, String expectedString) {
        if (fieldName.startsWith("nonCustomField")) {
            lenient().when(module.getCustomField(fieldName)).thenThrow(IllegalArgumentException.class);
            lenient().when(module.getValue(fieldName)).thenReturn(fieldValue);
        } else {
            when(module.getCustomField(fieldName)).thenReturn(fieldValue);
        }
        lenient().when(module.getCustomField(PlaceholderValues.DOC_LANGUAGE_FIELD)).thenReturn(new EnumOption("Locale", "en", "English", 1, false));
        lenient().when(module.getCustomField(PlaceholderValues.DOC_TIME_ZONE_FIELD)).thenReturn("CET");
        PlaceholderValues placeholder = PlaceholderValues.builder().build();
        placeholder.addCustomVariables(module, Set.of(fieldName));
        Map<String, String> customVariables = placeholder.getAllVariables();
        assertThat(customVariables).containsEntry(fieldName, expectedString);
    }

    private static Stream<Arguments> provideFields() throws ParseException {
        IPObject iPObject = mock(IPObject.class);
        IListType stringListPrototype = mock(IListType.class);
        when(stringListPrototype.getItemType()).thenReturn(new PrimitiveType("string"));
        CustomTypedList stringList = new CustomTypedList(iPObject, stringListPrototype, false, List.of("testValue1", "testValue2"));

        return Stream.of(
                Arguments.of("testText", Text.html("test html <br/>"), "test html <br/>"),
                Arguments.of("testCurrency", new Currency(BigDecimal.TEN), "10.00"),
                Arguments.of("testEnum", new StatusOpt(new EnumOption("status", "open", "Open", 1, false)), "Open"),
                Arguments.of("testDateOnly", DateOnly.parse("2023-10-20"), "October 20, 2023"),
                Arguments.of("testTimeOnly", TimeOnly.parse("12:01:02.000 +0000"), "1:01:02 PM CET"),
                Arguments.of("testDate", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS Z").parse("2018-09-09 12:13:14.000 +0000"), "September 9, 2018 at 2:13:14 AM CEST"),
                Arguments.of("testStringList", stringList, "testValue1, testValue2"),
                Arguments.of("nonCustomFieldStatus", new StatusOpt(new EnumOption("status", "active", "Active", 2, false)), "Active"),
                Arguments.of("nonCustomFieldAuthor", user, "System Administrator")
        );
    }

    private static IUser mockUser() {
        try (MockedStatic<PlatformContext> platformContextMockedStatic = mockStatic(PlatformContext.class)) {
            IPlatform platform = mock(IPlatform.class);
            ILoginPolicy loginPolicy = mock(ILoginPolicy.class);
            IRepositoryService repositoryService = mock(IRepositoryService.class);
            platformContextMockedStatic.when(PlatformContext::getPlatform).thenReturn(platform);
            when(platform.lookupService(ILoginPolicy.class)).thenReturn(loginPolicy);
            when(platform.lookupService(IRepositoryService.class)).thenReturn(repositoryService);

            IUser user = mock(IUser.class);
            when(user.getLabel()).thenReturn("System Administrator");
            return user;
        }
    }
}
