/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.0.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.mars_sim.msp.config.model.building;

/**
 * Class ResourceInitial.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings("serial")
public class ResourceInitial implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _resource.
     */
    private java.lang.String _resource;

    /**
     * Field _amount.
     */
    private float _amount;

    /**
     * keeps track of state for field: _amount
     */
    private boolean _has_amount;


      //----------------/
     //- Constructors -/
    //----------------/

    public ResourceInitial() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     */
    public void deleteAmount(
    ) {
        this._has_amount= false;
    }

    /**
     * Returns the value of field 'amount'.
     * 
     * @return the value of field 'Amount'.
     */
    public float getAmount(
    ) {
        return this._amount;
    }

    /**
     * Returns the value of field 'resource'.
     * 
     * @return the value of field 'Resource'.
     */
    public java.lang.String getResource(
    ) {
        return this._resource;
    }

    /**
     * Method hasAmount.
     * 
     * @return true if at least one Amount has been added
     */
    public boolean hasAmount(
    ) {
        return this._has_amount;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Marshaller.marshal(this, handler);
    }

    /**
     * Sets the value of field 'amount'.
     * 
     * @param amount the value of field 'amount'.
     */
    public void setAmount(
            final float amount) {
        this._amount = amount;
        this._has_amount = true;
    }

    /**
     * Sets the value of field 'resource'.
     * 
     * @param resource the value of field 'resource'.
     */
    public void setResource(
            final java.lang.String resource) {
        this._resource = resource;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled
     * org.mars_sim.msp.config.model.building.ResourceInitial
     */
    public static org.mars_sim.msp.config.model.building.ResourceInitial unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (org.mars_sim.msp.config.model.building.ResourceInitial) org.exolab.castor.xml.Unmarshaller.unmarshal(org.mars_sim.msp.config.model.building.ResourceInitial.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}
