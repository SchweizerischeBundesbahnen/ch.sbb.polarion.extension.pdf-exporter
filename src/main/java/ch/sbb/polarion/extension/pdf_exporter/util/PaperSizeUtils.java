package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class PaperSizeUtils {

    public static final int A5_PORTRAIT_WIDTH = 420;
    public static final int A5_PORTRAIT_HEIGHT = 620;
    public static final int A5_LANDSCAPE_WIDTH = 660;
    public static final int A5_LANDSCAPE_HEIGHT = 380;
    public static final int B5_PORTRAIT_WIDTH = 500;
    public static final int B5_PORTRAIT_HEIGHT = 760;
    public static final int B5_LANDSCAPE_WIDTH = 810;
    public static final int B5_LANDSCAPE_HEIGHT = 460;
    public static final int JIS_B5_PORTRAIT_WIDTH = 520;
    public static final int JIS_B5_PORTRAIT_HEIGHT = 770;
    public static final int JIS_B5_LANDSCAPE_WIDTH = 830;
    public static final int JIS_B5_LANDSCAPE_HEIGHT = 480;
    public static final float NEXT_SIZE_ASPECT_RATIO = 1.41f;
    public static final float NEXT_SIZE_ASPECT_RATIO_TWICE = NEXT_SIZE_ASPECT_RATIO * NEXT_SIZE_ASPECT_RATIO;

    public static final Map<PaperSize, Integer> MAX_PORTRAIT_WIDTHS = Map.of(
            PaperSize.A5, A5_PORTRAIT_WIDTH,
            PaperSize.A4, (int) (A5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_PORTRAIT_WIDTH,
            PaperSize.B4, (int) (B5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_PORTRAIT_WIDTH,
            PaperSize.JIS_B4, (int) (JIS_B5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 610,
            PaperSize.LEGAL, 610,
            PaperSize.LEDGER, 790
    );
    public static final Map<PaperSize, Integer> MAX_LANDSCAPE_WIDTHS = Map.of(
            PaperSize.A5, A5_LANDSCAPE_WIDTH,
            PaperSize.A4, (int) (A5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_LANDSCAPE_WIDTH,
            PaperSize.B4, (int) (B5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_LANDSCAPE_WIDTH,
            PaperSize.JIS_B4, (int) (JIS_B5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 900,
            PaperSize.LEGAL, 1150,
            PaperSize.LEDGER, 1400
    );
    public static final Map<PaperSize, Integer> MAX_PORTRAIT_HEIGHTS = Map.of(
            PaperSize.A5, A5_PORTRAIT_HEIGHT,
            PaperSize.A4, (int) (A5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_PORTRAIT_HEIGHT,
            PaperSize.B4, (int) (B5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_PORTRAIT_HEIGHT,
            PaperSize.JIS_B4, (int) (JIS_B5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 820,
            PaperSize.LEGAL, 1050,
            PaperSize.LEDGER, 1270
    );
    public static final Map<PaperSize, Integer> MAX_LANDSCAPE_HEIGHTS = Map.of(
            PaperSize.A5, A5_LANDSCAPE_HEIGHT,
            PaperSize.A4, (int) (A5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_LANDSCAPE_HEIGHT,
            PaperSize.B4, (int) (B5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_LANDSCAPE_HEIGHT,
            PaperSize.JIS_B4, (int) (JIS_B5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 550,
            PaperSize.LEGAL, 550,
            PaperSize.LEDGER, 730
    );

    public static final Map<PaperSize, Integer> MAX_PORTRAIT_WIDTHS_IN_TABLES = Map.of(
            PaperSize.A5, MAX_PORTRAIT_WIDTHS.get(PaperSize.A5) / 3,
            PaperSize.A4, MAX_PORTRAIT_WIDTHS.get(PaperSize.A4) / 3,
            PaperSize.A3, MAX_PORTRAIT_WIDTHS.get(PaperSize.A3) / 3,
            PaperSize.B5, MAX_PORTRAIT_WIDTHS.get(PaperSize.B5) / 3,
            PaperSize.B4, MAX_PORTRAIT_WIDTHS.get(PaperSize.B4) / 3,
            PaperSize.JIS_B5, MAX_PORTRAIT_WIDTHS.get(PaperSize.JIS_B5) / 3,
            PaperSize.JIS_B4, MAX_PORTRAIT_WIDTHS.get(PaperSize.JIS_B4) / 3,
            PaperSize.LETTER, MAX_PORTRAIT_WIDTHS.get(PaperSize.LETTER) / 3,
            PaperSize.LEGAL, MAX_PORTRAIT_WIDTHS.get(PaperSize.LEGAL) / 3,
            PaperSize.LEDGER, MAX_PORTRAIT_WIDTHS.get(PaperSize.LEDGER) / 3
    );

    public static final Map<PaperSize, Integer> MAX_LANDSCAPE_WIDTHS_IN_TABLES = Map.of(
            PaperSize.A5, MAX_LANDSCAPE_WIDTHS.get(PaperSize.A5) / 3,
            PaperSize.A4, MAX_LANDSCAPE_WIDTHS.get(PaperSize.A4) / 3,
            PaperSize.A3, MAX_LANDSCAPE_WIDTHS.get(PaperSize.A3) / 3,
            PaperSize.B5, MAX_LANDSCAPE_WIDTHS.get(PaperSize.B5) / 3,
            PaperSize.B4, MAX_LANDSCAPE_WIDTHS.get(PaperSize.B4) / 3,
            PaperSize.JIS_B5, MAX_LANDSCAPE_WIDTHS.get(PaperSize.JIS_B5) / 3,
            PaperSize.JIS_B4, MAX_LANDSCAPE_WIDTHS.get(PaperSize.JIS_B4) / 3,
            PaperSize.LETTER, MAX_LANDSCAPE_WIDTHS.get(PaperSize.LETTER) / 3,
            PaperSize.LEGAL, MAX_LANDSCAPE_WIDTHS.get(PaperSize.LEGAL) / 3,
            PaperSize.LEDGER, MAX_LANDSCAPE_WIDTHS.get(PaperSize.LEDGER) / 3
    );

    public static int getMaxWidth(ConversionParams conversionParams) {
        return conversionParams.getOrientation() == Orientation.PORTRAIT
                ? PaperSizeUtils.MAX_PORTRAIT_WIDTHS.get(conversionParams.getPaperSize())
                : PaperSizeUtils.MAX_LANDSCAPE_WIDTHS.get(conversionParams.getPaperSize());
    }

    public static int getMaxHeight(ConversionParams conversionParams) {
        return conversionParams.getOrientation() == Orientation.PORTRAIT
                ? PaperSizeUtils.MAX_PORTRAIT_HEIGHTS.get(conversionParams.getPaperSize())
                : PaperSizeUtils.MAX_LANDSCAPE_HEIGHTS.get(conversionParams.getPaperSize());
    }

    public static int getMaxWidthInTables(ConversionParams conversionParams) {
        return conversionParams.getOrientation() == Orientation.PORTRAIT
                ? PaperSizeUtils.MAX_PORTRAIT_WIDTHS_IN_TABLES.get(conversionParams.getPaperSize())
                : PaperSizeUtils.MAX_LANDSCAPE_WIDTHS_IN_TABLES.get(conversionParams.getPaperSize());
    }

}
