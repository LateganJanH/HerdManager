package com.herdmanager.app.ui.util;

import android.graphics.pdf.PdfDocument;

/**
 * Helper to create PdfDocument.PageInfo so the 3-argument Builder constructor
 * is resolved correctly when called from Kotlin (avoids overload resolution issues).
 */
public final class PdfPageInfoFactory {

    private PdfPageInfoFactory() {}

    public static PdfDocument.PageInfo create(int pageWidth, int pageHeight, int pageNumber) {
        return new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
    }
}
