//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.11.22 at 06:00:17 PM CET 
//


package eu.gloria.rt.catalogue.sesame.resp.jaxb;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for veqr complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="veqr">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="v" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="e" type="{}err" minOccurs="0"/>
 *         &lt;element name="q" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/>
 *         &lt;element name="r" type="{}bibcode" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="band" type="{http://www.w3.org/2001/XMLSchema}token" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "veqr", propOrder = {
    "v",
    "e",
    "q",
    "r"
})
public class Veqr {

    @XmlElement(required = true)
    protected BigDecimal v;
    protected BigDecimal e;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String q;
    protected String r;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String band;

    /**
     * Gets the value of the v property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getV() {
        return v;
    }

    /**
     * Sets the value of the v property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setV(BigDecimal value) {
        this.v = value;
    }

    /**
     * Gets the value of the e property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getE() {
        return e;
    }

    /**
     * Sets the value of the e property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setE(BigDecimal value) {
        this.e = value;
    }

    /**
     * Gets the value of the q property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQ() {
        return q;
    }

    /**
     * Sets the value of the q property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQ(String value) {
        this.q = value;
    }

    /**
     * Gets the value of the r property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getR() {
        return r;
    }

    /**
     * Sets the value of the r property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setR(String value) {
        this.r = value;
    }

    /**
     * Gets the value of the band property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBand() {
        return band;
    }

    /**
     * Sets the value of the band property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBand(String value) {
        this.band = value;
    }

}
