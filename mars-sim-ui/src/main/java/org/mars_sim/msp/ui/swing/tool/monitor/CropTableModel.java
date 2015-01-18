/**
 * Mars Simulation Project
 * CropTableModel.java
 * @version 3.07 2014-12-17
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEvent;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.UnitManagerEvent;
import org.mars_sim.msp.core.UnitManagerEventType;
import org.mars_sim.msp.core.UnitManagerListener;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Crop;
import org.mars_sim.msp.core.structure.building.function.CropType;
import org.mars_sim.msp.core.structure.building.function.Farming;


/**
 * The CropTableModel that maintains a list of crop related objects.
 * It maps food related info into Columns.
 */
// 2014-10-14 Relocated all food related objects from SettlementTableModel Class to here
// & Incorporated five major food groups into MSP
// 2014-11-06 Added SOYBEANS and SOYMILK
// 2014-11-26 Major Overhaul: commented out all individual food items and moved them to another table
// & Kept only crop items and changed name to CropTableModel.java
// 2014-12-01 Added BULB, FLOWER, TUBER, ROOT
// 2014-12-02 Added GRASSES
public class CropTableModel
extends UnitTableModel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
    /** default logger. */
	private static Logger logger = Logger.getLogger(CropTableModel.class.getName());

	// Column indexes
	private final static int NAME = 0;
	private final static int GREENHOUSES = 1;
	private final static int CROPS = 2;
	
	//private final static int FOOD = 3;
	private final static int BULBS = 3;
	private final static int FLOWERS = 4;
	private final static int FRUITS = 5;
	
	private final static int GRAINS = 6;
	private final static int GRASSES = 7;
	private final static int LEGUMES = 8;
	private final static int ROOTS = 9;	
	
	private final static int SPICES = 10;
	private final static int TUBERS = 11;
	private final static int VEGETABLES = 12;
	
	// 2014-11-25 Added NUMCROPTYPE
	private final static int NUMCROPTYPE = 10;
	/** The number of Columns. */
	private final static int COLUMNCOUNT = 13;

	String name = null;
	int numHouse = 0;
	
	enum CropsEnum {
		BULBS,FLOWERS,FRUITS,GRAINS,GRASSES,LEGUMES,
		ROOTS,SPICES,TUBERS,VEGETABLES;
	}


	/** Names of Columns. */
	private static String columnNames[];
	/** Types of columns. */
	private static Class<?> columnTypes[];

	static {
		columnNames = new String[COLUMNCOUNT];
		columnTypes = new Class[COLUMNCOUNT];
		columnNames[NAME] = "Settlement";
		columnTypes[NAME] = String.class;
		columnNames[GREENHOUSES] = "Greenhouses";
		columnTypes[GREENHOUSES] = Integer.class;
		columnNames[CROPS] = "# Crops";
		columnTypes[CROPS] = Integer.class;	
		columnNames[BULBS] = "Bulbs";
		columnTypes[BULBS] = Integer.class;
		columnNames[FLOWERS] = "Flowers";
		columnTypes[FLOWERS] = Integer.class;
		columnNames[FRUITS] = "Fruits";
		columnTypes[FRUITS] = Integer.class;
		columnNames[GRAINS] = "Grains";
		columnTypes[GRAINS] = Integer.class;
		columnNames[GRASSES] = "Grasses";
		columnTypes[GRASSES] = Integer.class;
		columnNames[LEGUMES] = "Legumes";
		columnTypes[LEGUMES] = Integer.class;
		columnNames[ROOTS] = "Roots";
		columnTypes[ROOTS] = Integer.class;
		columnNames[SPICES] = "Spices";
		columnTypes[SPICES] = Integer.class;
		columnNames[TUBERS] = "Tubers";
		columnTypes[TUBERS] = Integer.class;
		columnNames[VEGETABLES] = "Vegetables";
		columnTypes[VEGETABLES] = Integer.class;
		

	};
	// Data members
	private UnitManagerListener unitManagerListener;
	private Map<Unit, List<Integer>> unitCache ;
	/**
	 * Constructs a FoodTableModel model that displays all Settlements
	 * in the simulation.
	 *
	 * @param unitManager Unit manager that holds settlements.
	 */
	public CropTableModel(UnitManager unitManager) {
		super(
			Msg.getString("CropTableModel.tabName"), //$NON-NLS-1$
			"SettlementTableModel.countingSettlements", //$NON-NLS-1$
			columnNames,
			columnTypes
		);

		setSource(unitManager.getSettlements());
		unitManagerListener = new LocalUnitManagerListener();
		unitManager.addUnitManagerListener(unitManagerListener);
	}
	
	/**
	 * Give the position number for a particular crop group 
	 * 
	 * @param String cropCat
	 * @return a position number
	 */
	// Called by getTotalNumforCropGroup() which in terms was called by getValueAt()
	public int getGroupNum(String testCat) {
		 //logger.info(" getGroupNum() : entering");
		 testCat = testCat.toUpperCase();
		 //logger.info(" getGroupNum() : testCat : " + testCat);
		 int num = CropsEnum.valueOf(testCat).ordinal();
		 //logger.info(" num is " + num);
		 return num;
	}
	
	
	/**
	 * Gets the total number of crop in a crop group from cropMap or cropCache
	 * @param return a number
	 */
	// Called by getValueAt()
	public Integer getValueAtColumn(int rowIndex, String cropCat) {
		//logger.info("getValueAtColumn() : entering");
		Settlement settle = (Settlement)getUnit(rowIndex);
		int groupNumber = getGroupNum(cropCat);
		//logger.info("getValueAtColumn() : groupNumber : "+groupNumber);
		List<Integer> cropCache = unitCache.get(settle);
		Integer numCrop = cropCache.get(groupNumber);
		//logger.info("numCrop is " + numCrop);
		return numCrop;
	}
	
	/**
	 * Return the value of a Cell
	 * @param rowIndex Row index of the cell.
	 * @param columnIndex Column index of the cell.
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		//logger.info("getValueAt() : Just Entered ");
		//logger.info("rowIndex : " + rowIndex );
		//logger.info("columnIndex : " + columnIndex);

		Object result = null;

		if (rowIndex < getUnitNumber()) {
			Settlement settle = (Settlement)getUnit(rowIndex);			
			BuildingManager bMgr = settle.getBuildingManager();

			try {
				switch (columnIndex) {
				
				case NAME : {
					String i = settle.getName();
					//if (name != i) {
						result = (Object) i;
					//	name = i;
					//}
				} break;

				case GREENHOUSES : {
					int numGreenhouses = bMgr.getBuildings(BuildingFunction.FARMING).size();
					//if (numHouse != numGreenhouses ) {
					//	numHouse = numGreenhouses;
						result = (Object) numGreenhouses;
					//}
				} break;
							
				case CROPS : {
					result = (Object) getTotalNumOfAllCrops(settle);
					
				} break;

				case BULBS : {
					result = getValueAtColumn(rowIndex, "Bulbs");
					} break;
					
				case FLOWERS : {
					result = getValueAtColumn(rowIndex, "Flowers");
					} break;
					
				case FRUITS : {
					result = getValueAtColumn(rowIndex, "Fruits");
					} break;

				case GRAINS : {
					result = getValueAtColumn(rowIndex, "Grains");
				} break;

				case GRASSES : {
					result = getValueAtColumn(rowIndex, "Grasses");
				} break;
				
				case LEGUMES : {
					result = getValueAtColumn(rowIndex, "Legumes");
				} break;
				
				case ROOTS : {
					result = getValueAtColumn(rowIndex, "Roots");
					} break;
				
				case SPICES : {
					result = getValueAtColumn(rowIndex, "Spices");
				} break;
				
				case TUBERS : {
					result = getValueAtColumn(rowIndex, "Tubers");
					} break;
					
				case VEGETABLES : {
					result = getValueAtColumn(rowIndex, "Vegetables");
				} break;
			}			
			}
			catch (Exception e) {}
		}

		return result;
	}

	/**
	 * Gets the total numbers of all crops for the whole settlement 
	 * @param Unit newUnit 
	 * @return an int
	 */
	// called by getValueAt()
	public int getTotalNumOfAllCrops(Unit newUnit) {
		
		Settlement settle = (Settlement) newUnit;
		BuildingManager bMgr = settle.getBuildingManager();
		List<Building> greenhouses = bMgr.getBuildings(BuildingFunction.FARMING);
		Iterator<Building> i = greenhouses.iterator();

		int num = 0;
		
		while (i.hasNext()) {
			try {
				Building greenhouse = i.next();
				Farming farm = (Farming) greenhouse.getFunction(BuildingFunction.FARMING);
				num += farm.getCrops().size();
			}
			catch (Exception e) {}
		}		 
		return num;
	}

	/**
	 * Sets up a brand new local cropCache (a list of Integers) for a given settlement
	 * 
	 * @param Unit newUnit
	 * @return an Integer List
	 */
	// Called by addUnit()
	public List<Integer> setUpNewCropCache(Unit newUnit) {
		
		List<Integer> intList = new ArrayList<Integer>(NUMCROPTYPE);
		// initialize the intList
		for (int i = 0; i<NUMCROPTYPE; i++) intList.add(0);
	
		Settlement settle = (Settlement) newUnit;
		BuildingManager bMgr = settle.getBuildingManager();
		List<Building> greenhouses = bMgr.getBuildings(BuildingFunction.FARMING);
		Iterator<Building> i = greenhouses.iterator();
		
		while (i.hasNext()) {
			try {
				Building greenhouse = i.next();
				Farming farm = (Farming) greenhouse.getFunction(BuildingFunction.FARMING);
				List<Crop> cropsList = farm.getCrops(); 
					int kk = 0;
					//logger.info("setUpNewCropCache() : cropsList.size is " + cropsList.size() ) ;
					Iterator<Crop> k = cropsList.iterator();
			        while (k.hasNext()) {
			        	kk++;
						//logger.info("setUpNewCropCache() : kk is " + kk  ) ;
			            Crop crop = k.next();
			            CropType cropType = crop.getCropType();
			            String testCat = cropType.getCropCategory();
						//logger.info("setUpNewCropCache() : testCat is " + testCat ) ;
			            int num = getGroupNum(testCat);
						//logger.info("setUpNewCropCache() : num is " + num ) ;         
		            	int val = intList.get(num) + 1 ;
	            	    //logger.info("setUpNewCropCache() : val is " + val ) ;
			            intList.set(num, val);
			    		//logger.info("setUpNewCropCache() : intList.get(" + num + ") : " + intList.get(num));
			        } 				
			} catch (Exception e) {}
		}

		//logger.info("setUpNewCropCache() : intList.toString() : " + intList.toString());
	
		return intList;
	}

	/**
	 * Catch unit update event.
	 * @param event the unit event.
	 */
	public void unitUpdate(UnitEvent event) {
		//logger.info("unitUpdate() : entering");
		// NOTE: unitUpdate() is called a dozen times every second
		Unit unit = (Unit) event.getSource();
		int unitIndex = getUnitIndex(unit);
		UnitEventType eventType = event.getType();
		Object target = event.getTarget();
		//logger.info("unitUpdate() : eventType : " +  eventType.toString());
		
		int columnNum = -1;
		if (eventType == UnitEventType.NAME_EVENT) 
			columnNum = NAME; // = 0
		else if (eventType == UnitEventType.ADD_BUILDING_EVENT) {
			if (target instanceof Farming) 
				columnNum = GREENHOUSES; // = 1
		}
		else if (eventType == UnitEventType.CROP_EVENT) {
			//logger.info("unitUpdate() : CROP_EVENT");
			//logger.info("unitUpdate() : eventType : " +  eventType.toString());
			// TODO: check with total Crops get updated
			//columnNum = CROPS; // = 2
			Crop crop = (Crop) target;
			CropType cropType = crop.getCropType();
			String cropCat = cropType.getCropCategory();
			//logger.info("unitUpdate() : cropCat is " + cropCat);
			
			try {
				int tempColumnNum = -1;
				
				tempColumnNum = getGroupNum(cropCat);
				//logger.info(" tempColumnNum : " + tempColumnNum);
	
				if (tempColumnNum > -1) {
					// Only update cell if value as int has changed.
					int currentValue = (Integer) getValueAt(unitIndex, tempColumnNum);
					//logger.info("unitUpdate() : currentValue : " + currentValue); 
				
					int newValue = getNewValue(unit, cropCat);
					int groupNum = getGroupNum(cropCat);
					//logger.info("unitUpdate() : newValue : " + newValue);
					
					if (currentValue != newValue) {
						columnNum = tempColumnNum;
			
						List<Integer> cropCache = unitCache.get(unit);
						cropCache.set(groupNum, newValue);
						//logger.info("unitUpdate() : cropCache.toString() : " + cropCache.toString());
					}
				}
			}
			catch (Exception e) {}
		} // end of else if (eventType == UnitEventType.CROP_EVENT) {
		if (columnNum > -1) {
			SwingUtilities.invokeLater(new FoodTableCellUpdater(unitIndex, columnNum));
		}		
	}

	/**
	 * Recompute the total number of cropType having a particular cropCategory
	 */
	public int getNewValue(Unit unit, String cropCat) {
		
		int result = 0;
		// recompute only the total number of cropType having cropCategory = cropCat
		// examine match the CropType within List<CropType> having having cropCategory	
		Settlement settle = (Settlement) unit;
		BuildingManager bMgr = settle.getBuildingManager();
		List<Building> greenhouses = bMgr.getBuildings(BuildingFunction.FARMING);
		Iterator<Building> i = greenhouses.iterator();

		int total = 0;
		while (i.hasNext()) {
			try {
				Building greenhouse = i.next();
				Farming farm = (Farming) greenhouse.getFunction(BuildingFunction.FARMING);
				List<Crop> cropsList = farm.getCrops(); 

				Iterator<Crop> j = cropsList.iterator();
		        while (j.hasNext()) {
		            Crop crop = j.next();
		            String type = crop.getCropType().getCropCategory();
		            if (type == cropCat) 
		            	total++;
		        }
			}
			catch (Exception e) {}
		}
		result = total;
		//logger.info("getNewNumCropAtSameCat() : cropCat : " + cropCat + ", total : " + total);
		return result;
	}
	
	/**
	 * Defines the source data from this table
	 */
	private void setSource(Collection<Settlement> source) {
		Iterator<Settlement> iter = source.iterator();
		while(iter.hasNext()) addUnit(iter.next());
	}

	/**
	 * Add a unit (a settlement) to the model.
	 * @param newUnit Unit to add to the model.
	 */
	protected void addUnit(Unit newUnit) {
		//logger.info("addUnit() : just entered in" );
		if (unitCache == null) 
			unitCache = new HashMap<Unit, List<Integer>>();
		// if cropCache does not a record of the settlement
		if (!unitCache.containsKey(newUnit)) {
			try {// Setup a cropCache and cropMap in CropTableModel
				// All crops are to be newly added to the settlement
				List<Integer> cropCache = setUpNewCropCache(newUnit);
				
				unitCache.put(newUnit, cropCache);			
			}
			catch (Exception e) {}
		}
		super.addUnit(newUnit);
		//logger.info("addUnit() : leaving " );
	}

	/**
	 * Remove a unit from the model.
	 * @param oldUnit Unit to remove from the model.
	 */
	protected void removeUnit(Unit oldUnit) {
		if (unitCache == null) unitCache = new HashMap<Unit, List<Integer>>();
		if (unitCache.containsKey(oldUnit)) {			
			List<Integer> cropCache =unitCache.get(oldUnit);
			// TODO: need to check if this way can remove the unit and cropCache
			cropCache.clear();
			cropCache.remove(oldUnit);
		}
		super.removeUnit(oldUnit);
	}

	/**
	 * Prepares the model for deletion.
	 */
	public void destroy() {
		super.destroy();

		UnitManager unitManager = Simulation.instance().getUnitManager();
		unitManager.removeUnitManagerListener(unitManagerListener);
		unitManagerListener = null;

		if (unitCache != null) {
			unitCache.clear();
		}
		unitCache = null;
	}

	private class FoodTableCellUpdater implements Runnable {
		private int row;
		private int column;

		private FoodTableCellUpdater(int row, int column) {
			this.row = row;
			this.column = column;
		}

		public void run() {
			fireTableCellUpdated(row, column);
		}
	}

	/**
	 * UnitManagerListener inner class.
	 */
	 private class LocalUnitManagerListener implements UnitManagerListener {

		 /**
		  * Catch unit manager update event.
		  * @param event the unit event.
		  */
		 public void unitManagerUpdate(UnitManagerEvent event) {
			 Unit unit = event.getUnit();
			 UnitManagerEventType eventType = event.getEventType();
			 if (unit instanceof Settlement) {
				 if (eventType == UnitManagerEventType.ADD_UNIT) {
						logger.info("unitManagerUpdate(): Just entered");
					 if (!containsUnit(unit)) addUnit(unit);
				 }
				 else if (eventType == UnitManagerEventType.REMOVE_UNIT) {
					 if (containsUnit(unit)) removeUnit(unit);
				 }
			 }
		 }
	 }
}