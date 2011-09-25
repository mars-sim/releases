/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.0.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.mars_sim.msp.config.model.settlement;

/**
 * Class SettlementNameList.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings("serial")
public class SettlementNameList implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _settlementNameList.
     */
    private java.util.List<org.mars_sim.msp.config.model.settlement.SettlementName> _settlementNameList;


      //----------------/
     //- Constructors -/
    //----------------/

    public SettlementNameList() {
        super();
        this._settlementNameList = new java.util.ArrayList<org.mars_sim.msp.config.model.settlement.SettlementName>();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vSettlementName
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addSettlementName(
            final org.mars_sim.msp.config.model.settlement.SettlementName vSettlementName)
    throws java.lang.IndexOutOfBoundsException {
        this._settlementNameList.add(vSettlementName);
    }

    /**
     * 
     * 
     * @param index
     * @param vSettlementName
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addSettlementName(
            final int index,
            final org.mars_sim.msp.config.model.settlement.SettlementName vSettlementName)
    throws java.lang.IndexOutOfBoundsException {
        this._settlementNameList.add(index, vSettlementName);
    }

    /**
     * Method enumerateSettlementName.
     * 
     * @return an Enumeration over all possible elements of this
     * collection
     */
    public java.util.Enumeration<? extends org.mars_sim.msp.config.model.settlement.SettlementName> enumerateSettlementName(
    ) {
        return java.util.Collections.enumeration(this._settlementNameList);
    }

    /**
     * Method getSettlementName.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the
     * org.mars_sim.msp.config.model.settlement.SettlementName at
     * the given index
     */
    public org.mars_sim.msp.config.model.settlement.SettlementName getSettlementName(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._settlementNameList.size()) {
            throw new IndexOutOfBoundsException("getSettlementName: Index value '" + index + "' not in range [0.." + (this._settlementNameList.size() - 1) + "]");
        }

        return (org.mars_sim.msp.config.model.settlement.SettlementName) _settlementNameList.get(index);
    }

    /**
     * Method getSettlementName.Returns the contents of the
     * collection in an Array.  <p>Note:  Just in case the
     * collection contents are changing in another thread, we pass
     * a 0-length Array of the correct type into the API call. 
     * This way we <i>know</i> that the Array returned is of
     * exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public org.mars_sim.msp.config.model.settlement.SettlementName[] getSettlementName(
    ) {
        org.mars_sim.msp.config.model.settlement.SettlementName[] array = new org.mars_sim.msp.config.model.settlement.SettlementName[0];
        return (org.mars_sim.msp.config.model.settlement.SettlementName[]) this._settlementNameList.toArray(array);
    }

    /**
     * Method getSettlementNameCount.
     * 
     * @return the size of this collection
     */
    public int getSettlementNameCount(
    ) {
        return this._settlementNameList.size();
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
     * Method iterateSettlementName.
     * 
     * @return an Iterator over all possible elements in this
     * collection
     */
    public java.util.Iterator<? extends org.mars_sim.msp.config.model.settlement.SettlementName> iterateSettlementName(
    ) {
        return this._settlementNameList.iterator();
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
     */
    public void removeAllSettlementName(
    ) {
        this._settlementNameList.clear();
    }

    /**
     * Method removeSettlementName.
     * 
     * @param vSettlementName
     * @return true if the object was removed from the collection.
     */
    public boolean removeSettlementName(
            final org.mars_sim.msp.config.model.settlement.SettlementName vSettlementName) {
        boolean removed = _settlementNameList.remove(vSettlementName);
        return removed;
    }

    /**
     * Method removeSettlementNameAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public org.mars_sim.msp.config.model.settlement.SettlementName removeSettlementNameAt(
            final int index) {
        java.lang.Object obj = this._settlementNameList.remove(index);
        return (org.mars_sim.msp.config.model.settlement.SettlementName) obj;
    }

    /**
     * 
     * 
     * @param index
     * @param vSettlementName
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setSettlementName(
            final int index,
            final org.mars_sim.msp.config.model.settlement.SettlementName vSettlementName)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._settlementNameList.size()) {
            throw new IndexOutOfBoundsException("setSettlementName: Index value '" + index + "' not in range [0.." + (this._settlementNameList.size() - 1) + "]");
        }

        this._settlementNameList.set(index, vSettlementName);
    }

    /**
     * 
     * 
     * @param vSettlementNameArray
     */
    public void setSettlementName(
            final org.mars_sim.msp.config.model.settlement.SettlementName[] vSettlementNameArray) {
        //-- copy array
        _settlementNameList.clear();

        for (int i = 0; i < vSettlementNameArray.length; i++) {
                this._settlementNameList.add(vSettlementNameArray[i]);
        }
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
     * org.mars_sim.msp.config.model.settlement.SettlementNameList
     */
    public static org.mars_sim.msp.config.model.settlement.SettlementNameList unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (org.mars_sim.msp.config.model.settlement.SettlementNameList) org.exolab.castor.xml.Unmarshaller.unmarshal(org.mars_sim.msp.config.model.settlement.SettlementNameList.class, reader);
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
