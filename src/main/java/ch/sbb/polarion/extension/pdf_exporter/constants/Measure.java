package ch.sbb.polarion.extension.pdf_exporter.constants;

import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class Measure {
    public static final float EX_TO_PX_RATIO = 6.5F;
    public static final String PX = "px";
    public static final String EX = "ex";
    public static final String PERCENT = "%";

    /**
     * What a CSS pixel is worth in the units a length can be stated in, the absolute ones alone. A unit which
     * stands for something else on every element, such as em or a percentage, is not here: it cannot be read
     * without the element it is stated on.
     */
    public static final Map<String, Float> ABSOLUTE_UNITS_IN_PX = Map.of(
            "cm", 96F / 2.54F,
            "mm", 96F / 25.4F,
            "in", 96F,
            "pt", 96F / 72F,
            "pc", 16F,
            EX, EX_TO_PX_RATIO,
            PX, 1F);
}
