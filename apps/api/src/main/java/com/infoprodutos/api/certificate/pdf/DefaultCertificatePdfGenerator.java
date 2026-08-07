package com.infoprodutos.api.certificate.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Certificado A4 landscape — PKS Consultoria (navy + gold).
 * Layout compacto: logo pequena no canto, texto centrado, assinaturas e cargos
 * acima do rodapé (nada cortado).
 */
@Component
public class DefaultCertificatePdfGenerator implements CertificatePdfGenerator {

    private static final Rectangle PAGE = new Rectangle(842f, 595f);

    private static final Color NAVY = new Color(0x04, 0x0A, 0x16);
    private static final Color SURFACE = new Color(0x0C, 0x13, 0x1F);
    private static final Color GOLD = new Color(0xBA, 0x93, 0x64);
    private static final Color GOLD_LIGHT = new Color(0xCF, 0xAE, 0x83);
    private static final Color WHITE = new Color(0xF9, 0xFA, 0xFC);
    private static final Color SLATE = new Color(0xA7, 0xAD, 0xBA);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));

    private static final String PEDRO_SIGN = "/certificate/signatures/pedro-honorio-navy.png";
    private static final String RAFAEL_SIGN = "/certificate/signatures/rafael-kienen-navy.png";
    private static final String BRAND_LOGO = "/certificate/brand/pks-monogram-navy.png";

    /** Geometria do rodapé — assinaturas ficam acima de FOOTER_TOP. */
    private static final float FOOTER_Y = 30f;
    private static final float FOOTER_H = 70f;
    private static final float FOOTER_TOP = FOOTER_Y + FOOTER_H; // 100

    @Override
    public void generate(CertificatePdfModel model, OutputStream out) {
        Document document = new Document(PAGE, 0, 0, 0, 0);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            PdfContentByte under = writer.getDirectContentUnder();
            PdfContentByte canvas = writer.getDirectContent();
            drawBackground(under);
            drawGoldFrame(canvas);

            float cx = PAGE.getWidth() / 2f;

            // Logo pequena no canto superior esquerdo (não compete com o título)
            drawBrandLogoCorner(canvas);

            // Bloco textual centrado na área útil (abaixo da logo, acima das assinaturas)
            float blockTop = 470f;
            showCentered(canvas, "CERTIFICADO DE CONCLUSÃO", font(22, true, WHITE), cx, blockTop);
            showCentered(canvas, "Certificamos que", font(11, false, SLATE), cx, blockTop - 36f);

            String student = safe(model.studentName());
            showCentered(canvas, student, fitFont(student, 24, 14, WHITE, true), cx, blockTop - 68f);

            showCentered(canvas, "concluiu o curso", font(11, false, SLATE), cx, blockTop - 96f);

            String course = safe(model.courseTitle());
            showCentered(canvas, course, fitFont(course, 16, 12, GOLD_LIGHT, true), cx, blockTop - 124f);

            String narrative = String.format(
                    "com carga horária estimada em %s, finalizado em %s,",
                    formatWorkload(model.workloadHours()),
                    model.completionDate().format(DATE_FMT));
            showCentered(canvas, narrative, font(10, false, SLATE), cx, blockTop - 150f);
            showCentered(
                    canvas,
                    "cumprindo os requisitos estabelecidos pela plataforma.",
                    font(10, false, SLATE),
                    cx,
                    blockTop - 166f);

            // Assinaturas com folga acima do rodapé (nome + cargo visíveis)
            float signBaseline = FOOTER_TOP + 62f;
            drawSignatureBlock(
                    canvas, 230f, signBaseline, model.chiefVisionOfficerName(), "Chief Vision Officer", PEDRO_SIGN);
            drawSignatureBlock(
                    canvas, 612f, signBaseline, model.coordinatorName(), "Coordenador", RAFAEL_SIGN);

            drawFooter(canvas, model);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF do certificado.", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /** Logo reduzida no canto superior esquerdo. */
    private static void drawBrandLogoCorner(PdfContentByte canvas) throws Exception {
        Image logo = loadImage(BRAND_LOGO);
        if (logo == null) {
            showText(canvas, "PKS Consultoria", font(10, true, GOLD_LIGHT), 48f, 548f);
            return;
        }
        logo.scaleToFit(100f, 52f);
        logo.setInterpolation(true);
        // Dentro da moldura dourada (inset ~20) — evita cortar o P
        float x = 58f;
        float y = PAGE.getHeight() - 48f - logo.getScaledHeight();
        logo.setAbsolutePosition(x, y);
        canvas.addImage(logo);
    }

    private static void drawSignatureBlock(
            PdfContentByte canvas, float centerX, float baselineY, String name, String role, String resource)
            throws Exception {
        Image signature = loadImage(resource);
        if (signature != null) {
            signature.scaleToFit(200f, 64f);
            signature.setInterpolation(true);
            float imgX = centerX - signature.getScaledWidth() / 2f;
            float imgY = baselineY + 10f;
            signature.setAbsolutePosition(imgX, imgY);
            canvas.addImage(signature);
        }

        canvas.setColorStroke(GOLD);
        canvas.setLineWidth(0.7f);
        canvas.moveTo(centerX - 110f, baselineY + 4f);
        canvas.lineTo(centerX + 110f, baselineY + 4f);
        canvas.stroke();

        showCentered(canvas, safe(name), font(10, true, WHITE), centerX, baselineY - 12f);
        showCentered(canvas, role, font(8, false, SLATE), centerX, baselineY - 26f);
    }

    private static void drawFooter(PdfContentByte canvas, CertificatePdfModel model) throws Exception {
        float footerX = 44f;
        float footerW = PAGE.getWidth() - 88f;

        canvas.setColorFill(SURFACE);
        canvas.rectangle(footerX, FOOTER_Y, footerW, FOOTER_H);
        canvas.fill();

        showText(canvas, "Código de validação", font(8, false, SLATE), footerX + 14f, FOOTER_Y + 48f);
        showText(canvas, safe(model.validationCode()), font(12, true, WHITE), footerX + 14f, FOOTER_Y + 30f);
        showText(
                canvas,
                "Escaneie o QR Code para validar a autenticidade.",
                font(7, false, SLATE),
                footerX + 14f,
                FOOTER_Y + 14f);

        Image qr = Image.getInstance(renderQrPng(model.validationUrl(), 120));
        qr.scaleAbsolute(52f, 52f);
        float qrX = footerX + footerW - 72f;
        float qrY = FOOTER_Y + 10f;
        qr.setAbsolutePosition(qrX, qrY);
        canvas.addImage(qr);
        showCentered(canvas, "Validar", font(6, false, SLATE), qrX + 26f, FOOTER_Y + 4f);
    }

    private static void drawBackground(PdfContentByte canvas) {
        canvas.setColorFill(NAVY);
        canvas.rectangle(0, 0, PAGE.getWidth(), PAGE.getHeight());
        canvas.fill();
    }

    private static void drawGoldFrame(PdfContentByte canvas) {
        float inset = 20f;
        canvas.setColorStroke(GOLD);
        canvas.setLineWidth(1.4f);
        canvas.rectangle(inset, inset, PAGE.getWidth() - 2 * inset, PAGE.getHeight() - 2 * inset);
        canvas.stroke();

        canvas.setColorStroke(GOLD_LIGHT);
        canvas.setLineWidth(0.55f);
        float inner = inset + 5;
        canvas.rectangle(inner, inner, PAGE.getWidth() - 2 * inner, PAGE.getHeight() - 2 * inner);
        canvas.stroke();

        float c = 24f;
        canvas.setLineWidth(1.1f);
        canvas.setColorStroke(GOLD);
        drawCorner(canvas, inset, PAGE.getHeight() - inset, c, true, true);
        drawCorner(canvas, PAGE.getWidth() - inset, PAGE.getHeight() - inset, c, false, true);
        drawCorner(canvas, inset, inset, c, true, false);
        drawCorner(canvas, PAGE.getWidth() - inset, inset, c, false, false);
    }

    private static void drawCorner(PdfContentByte canvas, float x, float y, float len, boolean left, boolean top) {
        float dx = left ? len : -len;
        float dy = top ? -len : len;
        canvas.moveTo(x, y + dy * 0.35f);
        canvas.lineTo(x, y);
        canvas.lineTo(x + dx * 0.35f, y);
        canvas.stroke();
        canvas.moveTo(x + dx * 0.15f, y + dy);
        canvas.curveTo(x + dx * 0.55f, y + dy * 0.55f, x + dx, y + dy * 0.15f, x + dx, y);
        canvas.stroke();
    }

    private static void showCentered(PdfContentByte canvas, String text, Font font, float x, float y) {
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Paragraph(text, font), x, y, 0);
    }

    private static void showText(PdfContentByte canvas, String text, Font font, float x, float y) {
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Paragraph(text, font), x, y, 0);
    }

    private static Font font(float size, boolean bold, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL, color);
    }

    private static Font fitFont(String text, float preferred, float min, Color color, boolean bold) {
        float size = preferred;
        String value = text == null ? "" : text.trim();
        while (size > min && value.length() * size > 520) {
            size -= 1.5f;
        }
        return font(size, bold, color);
    }

    private static String formatWorkload(BigDecimal hours) {
        if (hours == null) {
            return "carga horária não informada";
        }
        BigDecimal stripped = hours.stripTrailingZeros();
        String n = stripped.scale() <= 0 ? stripped.toBigInteger().toString() : stripped.toPlainString();
        return n + (hours.compareTo(BigDecimal.ONE) == 0 ? " hora" : " horas");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static Image loadImage(String classpath) {
        try (InputStream in = DefaultCertificatePdfGenerator.class.getResourceAsStream(classpath)) {
            if (in == null) {
                return null;
            }
            return Image.getInstance(in.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] renderQrPng(String content, int size) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }
}
