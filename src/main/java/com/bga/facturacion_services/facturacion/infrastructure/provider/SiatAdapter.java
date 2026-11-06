package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.bga.facturacion_services.facturacion.application.*;
import com.bga.facturacion_services.facturacion.domain.Factura;
import java.io.StringWriter;
import javax.xml.stream.*;

/** Preview of the supplied SIAT schema. Fiscal authorization and signing remain external integration work. */
public final class SiatAdapter implements ProveedorFacturacion {
    private final TransporteSimulado transporte;
    public SiatAdapter(TransporteSimulado transporte) { this.transporte = transporte; }
    public String codigo() { return "siat"; }
    public boolean simulado() { return true; }
    public ResultadoEmision emitir(Factura f) {
        if (!"COMPRA_VENTA".equals(f.tipo())) throw new IllegalArgumentException("Tipo no soportado por SIAT");
        try {
            var salida = new StringWriter();
            var xml = XMLOutputFactory.newFactory().createXMLStreamWriter(salida);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("facturaElectronicaCompraVenta");
            xml.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xml.writeStartElement("cabecera");
            campo(xml, "nitEmisor", f.emisor().nit());
            campo(xml, "razonSocialEmisor", f.emisor().razonSocial());
            campo(xml, "municipio", f.emisor().municipio());
            campo(xml, "telefono", null);
            campo(xml, "numeroFactura", f.numero());
            campo(xml, "cuf", "SIM-SIN-VALIDEZ-" + f.id());
            campo(xml, "cufd", "SIM-CUFD");
            campo(xml, "codigoSucursal", f.emisor().sucursal());
            campo(xml, "direccion", f.emisor().direccion());
            campo(xml, "codigoPuntoVenta", f.emisor().puntoVenta());
            campo(xml, "fechaEmision", f.fecha().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
            campo(xml, "nombreRazonSocial", f.cliente().razonSocial());
            campo(xml, "codigoTipoDocumentoIdentidad", f.cliente().tipoDocumento());
            campo(xml, "numeroDocumento", f.cliente().documento());
            campo(xml, "complemento", f.cliente().complemento());
            campo(xml, "codigoCliente", f.cliente().documento());
            campo(xml, "codigoMetodoPago", f.metodoPago());
            campo(xml, "numeroTarjeta", null);
            campo(xml, "montoTotal", f.total());
            campo(xml, "montoTotalSujetoIva", f.totalSujetoIva());
            campo(xml, "codigoMoneda", 1);
            campo(xml, "tipoCambio", 1);
            campo(xml, "montoTotalMoneda", f.total());
            campo(xml, "montoGiftCard", null);
            campo(xml, "descuentoAdicional", f.descuentoAdicional());
            campo(xml, "codigoExcepcion", null);
            campo(xml, "cafc", null);
            campo(xml, "leyenda", "DOCUMENTO DE PRUEBA SIN VALIDEZ FISCAL");
            campo(xml, "usuario", f.usuario());
            campo(xml, "codigoDocumentoSector", 1);
            xml.writeEndElement();
            for (var linea : f.detalle()) {
                var i = linea.item();
                xml.writeStartElement("detalle");
                campo(xml, "actividadEconomica", i.actividad()); campo(xml, "codigoProductoSin", i.codigoSin());
                campo(xml, "codigoProducto", i.codigo()); campo(xml, "descripcion", i.descripcion());
                campo(xml, "cantidad", i.cantidad()); campo(xml, "unidadMedida", i.unidadMedida());
                campo(xml, "precioUnitario", i.precioUnitario()); campo(xml, "montoDescuento", i.descuento());
                campo(xml, "subTotal", linea.subtotal()); campo(xml, "numeroSerie", null); campo(xml, "numeroImei", null);
                xml.writeEndElement();
            }
            xml.writeEndElement(); xml.writeEndDocument(); xml.close();
            return transporte.enviar(codigo(), f.id(), "application/xml", salida.toString());
        } catch (XMLStreamException ex) { throw new IllegalStateException("Error construyendo XML", ex); }
    }
    private void campo(XMLStreamWriter xml, String nombre, Object valor) throws XMLStreamException {
        xml.writeStartElement(nombre);
        if (valor == null) xml.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "nil", "true");
        else xml.writeCharacters(valor.toString());
        xml.writeEndElement();
    }
}
