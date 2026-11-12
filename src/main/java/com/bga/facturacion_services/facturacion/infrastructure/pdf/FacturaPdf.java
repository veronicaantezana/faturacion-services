package com.bga.facturacion_services.facturacion.infrastructure.pdf;

import com.bga.facturacion_services.facturacion.application.RegistroFactura;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Hoja de factura compra-venta a partir del registro guardado. */
public final class FacturaPdf {
    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.US);
    private static final Font TEXTO = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font TEXTO_NEGRITA = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font TITULO = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 8, Font.ITALIC);
    private static final Font CABECERA = new Font(Font.HELVETICA, 7, Font.BOLD);
    private static final Font LEYENDA = new Font(Font.HELVETICA, 7, Font.NORMAL);
    private static final Font PIE = new Font(Font.HELVETICA, 8, Font.ITALIC);
    private static final String[] UNIDADES = {
            "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve"
    };
    private static final String[] DECENAS = {
            "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };
    private static final String[] CENTENAS = {
            "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
            "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    private FacturaPdf() {}

    public static byte[] generar(RegistroFactura registro) {
        try (var salida = new ByteArrayOutputStream()) {
            var documento = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(documento, salida);
            documento.open();
            escribir(documento, registro.factura(), registro.emision().cuf());
            documento.close();
            return salida.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura", ex);
        }
    }

    private static void escribir(Document documento, Factura factura, String cuf) throws Exception {
        documento.add(cabecera(factura, cuf));
        documento.add(espacio(10));
        documento.add(centrado("FACTURA", TITULO));
        documento.add(centrado("(Con Derecho a Credito Fiscal)", SUBTITULO));
        documento.add(espacio(10));
        documento.add(cliente(factura));
        documento.add(espacio(8));
        documento.add(detalle(factura));
        documento.add(espacio(6));
        documento.add(totales(factura));
        documento.add(espacio(16));
        documento.add(centrado("ESTA FACTURA CONTRIBUYE AL DESARROLLO DEL PAIS, EL USO ILICITO SERA SANCIONADO PENALMENTE DE ACUERDO A LEY", LEYENDA));
        documento.add(espacio(6));
        documento.add(centrado("Ley N° 453: Se debe promover el consumo solidario, justo, en armonia con la Madre Tierra y precautelando el habitat, en el marco del Vivir Bien.", LEYENDA));
        documento.add(espacio(10));
        documento.add(cierre(factura, cuf));
    }

    private static PdfPTable cabecera(Factura factura, String cuf) {
        var tabla = new PdfPTable(new float[]{58, 42});
        tabla.setWidthPercentage(100);
        var emisor = factura.emisor();
        var izquierda = new StringBuilder();
        izquierda.append(emisor.razonSocial() == null ? "" : emisor.razonSocial().toUpperCase(Locale.ROOT)).append('\n');
        izquierda.append(emisor.sucursal() == 0 ? "CASA MATRIZ" : "SUCURSAL " + emisor.sucursal()).append('\n');
        izquierda.append("No. Punto de Venta ").append(emisor.puntoVenta()).append('\n');
        izquierda.append(texto(emisor.direccion())).append('\n');
        izquierda.append('\n');
        izquierda.append("Telefono: ").append(texto(emisor.telefono()).isBlank() ? "0" : emisor.telefono()).append('\n');
        izquierda.append(texto(emisor.municipio()).toUpperCase(Locale.ROOT));
        tabla.addCell(sinBorde(izquierda.toString(), TEXTO_NEGRITA, Element.ALIGN_CENTER));

        var derecha = new StringBuilder();
        derecha.append("NIT").append(espacios(18)).append(emisor.nit()).append('\n');
        derecha.append("FACTURA N°").append(espacios(8)).append(factura.numero()).append('\n');
        derecha.append("Cod.").append('\n');
        derecha.append("Autorizacion").append('\n');
        derecha.append(partir(cuf == null || cuf.isBlank() ? "" : cuf, 22));
        tabla.addCell(sinBorde(derecha.toString(), TEXTO_NEGRITA, Element.ALIGN_RIGHT));
        return tabla;
    }

    private static PdfPTable cliente(Factura factura) {
        var tabla = new PdfPTable(new float[]{22, 38, 18, 22});
        tabla.setWidthPercentage(100);
        var fecha = factura.fecha().atZoneSameInstant(LA_PAZ).format(FECHA);
        tabla.addCell(sinBorde("Fecha:", TEXTO_NEGRITA, Element.ALIGN_LEFT));
        tabla.addCell(sinBorde(fecha, TEXTO, Element.ALIGN_LEFT));
        tabla.addCell(sinBorde("NIT/CI/CEX:", TEXTO_NEGRITA, Element.ALIGN_RIGHT));
        tabla.addCell(sinBorde(factura.cliente().documento(), TEXTO_NEGRITA, Element.ALIGN_RIGHT));
        tabla.addCell(sinBorde("Nombre/Razon Social:", TEXTO_NEGRITA, Element.ALIGN_LEFT));
        var nombre = new PdfPCell(new Phrase(texto(factura.cliente().razonSocial()), TEXTO_NEGRITA));
        nombre.setColspan(3);
        nombre.setBorder(Rectangle.NO_BORDER);
        nombre.setPadding(3);
        tabla.addCell(nombre);
        return tabla;
    }

    private static PdfPTable detalle(Factura factura) {
        var tabla = new PdfPTable(new float[]{14, 10, 28, 12, 10, 8, 8, 12});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (var titulo : new String[]{"CODIGO PRODUCTO", "CANTIDAD", "DESCRIPCION", "PRECIO UNITARIO", "DESCUENTO", "ICE %", "ICE ESP.", "SUBTOTAL"}) {
            tabla.addCell(caja(titulo, CABECERA, Element.ALIGN_CENTER, new Color(245, 245, 245)));
        }
        for (var linea : factura.detalle()) {
            var item = linea.item();
            tabla.addCell(caja(item.codigo(), TEXTO, Element.ALIGN_CENTER, Color.WHITE));
            tabla.addCell(caja(monto(item.cantidad()), TEXTO, Element.ALIGN_RIGHT, Color.WHITE));
            tabla.addCell(caja(item.descripcion(), TEXTO, Element.ALIGN_LEFT, Color.WHITE));
            tabla.addCell(caja(monto(item.precioUnitario()), TEXTO, Element.ALIGN_RIGHT, Color.WHITE));
            tabla.addCell(caja(monto(item.descuento()), TEXTO, Element.ALIGN_RIGHT, Color.WHITE));
            tabla.addCell(caja(monto(BigDecimal.ZERO), TEXTO, Element.ALIGN_RIGHT, Color.WHITE));
            tabla.addCell(caja(monto(BigDecimal.ZERO), TEXTO, Element.ALIGN_RIGHT, Color.WHITE));
            tabla.addCell(caja(monto(linea.subtotal()), TEXTO, Element.ALIGN_RIGHT, Color.WHITE));
        }
        return tabla;
    }

    private static PdfPTable totales(Factura factura) {
        var moneda = sigla(factura);
        var caja = new PdfPTable(new float[]{70, 30});
        caja.setWidthPercentage(100);
        if (factura.descuentoAdicional() != null && factura.descuentoAdicional().signum() > 0) {
            filaTotal(caja, "DESCUENTO ADICIONAL " + moneda, factura.descuentoAdicional());
        }
        filaTotal(caja, "TOTAL ICE ESPECIFICO " + moneda, BigDecimal.ZERO);
        filaTotal(caja, "TOTAL ICE PORCENTUAL " + moneda, BigDecimal.ZERO);
        filaTotal(caja, "TOTAL A PAGAR " + moneda, factura.total());
        filaTotal(caja, "IMPORTE BASE CREDITO FISCAL", factura.totalSujetoIva());

        var tabla = new PdfPTable(new float[]{46, 54});
        tabla.setWidthPercentage(100);
        tabla.addCell(sinBorde(literal(factura.total(), nombreMoneda(factura)), TEXTO_NEGRITA, Element.ALIGN_LEFT));
        var celda = new PdfPCell(caja);
        celda.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celda);
        return tabla;
    }

    private static PdfPTable cierre(Factura factura, String cuf) throws Exception {
        var tabla = new PdfPTable(new float[]{78, 22});
        tabla.setWidthPercentage(100);
        var cita = new Paragraph("\"Este documento es una impresion de un Documento Digital emitido en una Modalidad de Facturacion en Linea\"", PIE);
        cita.setAlignment(Element.ALIGN_CENTER);
        var celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.addElement(cita);
        tabla.addCell(celda);
        var qr = new PdfPCell(codigoQr(factura, cuf), true);
        qr.setBorder(Rectangle.NO_BORDER);
        qr.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(qr);
        return tabla;
    }

    private static Image codigoQr(Factura factura, String cuf) throws Exception {
        var url = "https://pilotosiat.impuestos.gob.bo/consulta/QR?nit=" + URLEncoder.encode(texto(factura.emisor().nit()), StandardCharsets.UTF_8)
                + "&cuf=" + URLEncoder.encode(texto(cuf), StandardCharsets.UTF_8)
                + "&numero=" + factura.numero() + "&t=2";
        var matriz = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 180, 180);
        var imagen = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matriz.getWidth(); x++) {
            for (int y = 0; y < matriz.getHeight(); y++) {
                imagen.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        var pdf = Image.getInstance(imagen, null);
        pdf.scaleToFit(78, 78);
        return pdf;
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, BigDecimal monto) {
        tabla.addCell(caja(etiqueta, TEXTO_NEGRITA, Element.ALIGN_LEFT, Color.WHITE));
        tabla.addCell(caja(monto(monto), TEXTO_NEGRITA, Element.ALIGN_RIGHT, Color.WHITE));
    }

    private static PdfPCell caja(String texto, Font fuente, int alineacion, Color fondo) {
        var celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(4);
        celda.setBackgroundColor(fondo);
        celda.setBorderWidth(0.6f);
        celda.setBorderColor(Color.BLACK);
        return celda;
    }

    private static PdfPCell sinBorde(String texto, Font fuente, int alineacion) {
        var parrafo = new Paragraph(texto == null ? "" : texto, fuente);
        parrafo.setAlignment(alineacion);
        var celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(2);
        celda.addElement(parrafo);
        return celda;
    }

    private static Paragraph centrado(String texto, Font fuente) {
        var parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        parrafo.setSpacingAfter(2);
        return parrafo;
    }

    private static Paragraph espacio(float puntos) {
        var parrafo = new Paragraph(" ");
        parrafo.setSpacingBefore(puntos);
        parrafo.setSpacingAfter(0);
        return parrafo;
    }

    private static String literal(BigDecimal monto, String moneda) {
        var normalizado = (monto == null ? BigDecimal.ZERO : monto).setScale(2, RoundingMode.HALF_UP);
        long enteros = normalizado.longValue();
        int centavos = normalizado.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        var palabras = enteros == 1 ? "Un" : capitalizar(enLetras(enteros));
        return "Son: " + palabras + " " + String.format("%02d", centavos) + "/100 " + moneda;
    }

    static String enLetras(long numero) {
        if (numero == 0) return "cero";
        if (numero < 20) return UNIDADES[(int) numero];
        if (numero < 30) return numero == 20 ? "veinte" : "veinti" + (numero == 21 ? "un" : UNIDADES[(int) numero - 20]);
        if (numero < 100) {
            long resto = numero % 10;
            return DECENAS[(int) (numero / 10)] + (resto == 0 ? "" : " y " + (resto == 1 ? "un" : UNIDADES[(int) resto]));
        }
        if (numero == 100) return "cien";
        if (numero < 1000) {
            long resto = numero % 100;
            return CENTENAS[(int) (numero / 100)] + (resto == 0 ? "" : " " + enLetras(resto));
        }
        if (numero < 2000) return "mil" + (numero % 1000 == 0 ? "" : " " + enLetras(numero % 1000));
        if (numero < 1_000_000) {
            long resto = numero % 1000;
            return enLetras(numero / 1000) + " mil" + (resto == 0 ? "" : " " + enLetras(resto));
        }
        if (numero < 2_000_000) return "un millon" + (numero % 1_000_000 == 0 ? "" : " " + enLetras(numero % 1_000_000));
        long resto = numero % 1_000_000;
        return enLetras(numero / 1_000_000) + " millones" + (resto == 0 ? "" : " " + enLetras(resto));
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String sigla(Factura factura) {
        return factura.venta() != null && factura.venta().codigoMoneda() == 2 ? "USD" : "Bs";
    }

    private static String nombreMoneda(Factura factura) {
        return factura.venta() != null && factura.venta().codigoMoneda() == 2 ? "Dolares" : "Bolivianos";
    }

    private static String texto(String valor) { return valor == null ? "" : valor; }

    private static String espacios(int cantidad) { return " ".repeat(cantidad); }

    private static String partir(String valor, int ancho) {
        if (valor.isEmpty()) return "";
        var lineas = new StringBuilder();
        for (int i = 0; i < valor.length(); i += ancho) {
            if (i > 0) lineas.append('\n');
            lineas.append(valor, i, Math.min(valor.length(), i + ancho));
        }
        return lineas.toString();
    }

    private static String monto(BigDecimal valor) {
        var formato = new DecimalFormat("#,##0.00");
        var simbolos = new DecimalFormatSymbols(Locale.US);
        formato.setDecimalFormatSymbols(simbolos);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor == null ? BigDecimal.ZERO : valor);
    }
}
