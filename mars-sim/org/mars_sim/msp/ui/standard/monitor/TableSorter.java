package org.mars_sim.msp.ui.standard.monitor;

import java.util.Arrays;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;


/**
 * This TableModel acts as a proxy to provide sorting on a remote Table Model.
 * It is based on the TableSorter provided as part of the Swing UI Tutorial but
 * this version has been simplified as it assumes that only column types that
 * are Comparable will be sorted.
 * Also only one column can be used as a sorting key
 */
public class TableSorter extends AbstractTableModel
                    implements TableModelListener, MonitorModel {

    int             indexes[];
    boolean         sortAscending = true;
    int             sortedColumn;
    MonitorModel    sourceModel;

    /**
     * Create a sorter model that provides sorting in front of the specified
     * model.
     * @param model Real source of data.
     */
    public TableSorter(MonitorModel model) {
        sourceModel = model;
        sourceModel.addTableModelListener(this);

        reallocateIndexes();
    }

    /**
     * Compare two rows according to their cell values
     */
    private int compare(int row1, int row2) {
        Comparable obj1 = (Comparable)sourceModel.getValueAt(row1, sortedColumn);
        Object obj2 = sourceModel.getValueAt(row2, sortedColumn);
        int result = 0;
        if (obj1 == null) {
            result = (obj2 == null ? 0 : 1);
        }
        else if (obj2 == null) {
            result = -1;
        }
        else {
            result = obj1.compareTo(obj2);
        }
        if (result != 0) {
            return sortAscending ? result : -result;
        }
        return 0;
    }

    /**
     * Reset index row mappings
     */
    private void reallocateIndexes() {
        int rowCount = sourceModel.getRowCount();

        // Set up a new array of indexes with the right number of elements
        // for the new data model.
        indexes = new int[rowCount];

        // Initialise with the identity mapping.
        for (int row = 0; row < rowCount; row++) {
            indexes[row] = row;
        }
    }

    /**
     * Fired when there are changed to the source table. If any rows have been
     * deleted/inserted or a change to the sorted column, then re-sort
     */
    public void tableChanged(TableModelEvent e) {
        boolean resort = false;
        TableModelEvent newEvent = null;
        int type = e.getType();

        // Decide whether to resort
        if ((type == TableModelEvent.DELETE) ||
            (type == TableModelEvent.INSERT)) {
            sortModel();
            newEvent = new TableModelEvent(this);
        }
        else if ((e.getColumn() == sortedColumn) ||
                (e.getColumn() == TableModelEvent.ALL_COLUMNS))
        {
            // If the model has been resorted, flag all changes
            if (sortModel()) {
                newEvent = new TableModelEvent(this, 0,
                                               sourceModel.getRowCount(),
                                               e.getColumn());
            }
        }

        // Fallback position, reconstruct new event by applying changes.
        if (newEvent == null) {
            int firstRow = e.getFirstRow();
            int lastRow = e.getLastRow();
            if (e.getFirstRow() == e.getLastRow()) {
                // Just one row changed so adjust the index
                firstRow = indexes[e.getFirstRow()];
                lastRow = indexes[e.getFirstRow()];

            }
            else {
                firstRow = 0;
                lastRow = sourceModel.getRowCount();
            }
            newEvent = new TableModelEvent(this, firstRow, lastRow,
                                                    e.getColumn());
        }

        fireTableChanged(newEvent);
    }

    /**
     * Simple N2 sort
     */
    private void n2sort() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = i+1; j < getRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) < 0) {
                    swap(i, j);
                }
            }
        }
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
    private void shuttlesort(int from[], int to[], int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high)/2;
        shuttlesort(to, from, low, middle);
        shuttlesort(to, from, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
        check to see if the elements in this subset are already
        ordered.  If so, no further comparisons are needed; the
        sub-array can just be copied.  The array must be copied rather
        than assigned otherwise sister calls in the recursion might
        get out of sinc.  When the number of elements is three they
        are partitioned so that the first set, [low, mid), has one
        element and and the second, [mid, high), has two. We skip the
        optimisation when the number of elements is three or less as
        the first compare in the normal merge will produce the same
        sequence of steps. This optimisation seems to be worthwhile
        for partially ordered lists but some analysis is needed to
        find out how the performance drops to Nlog(N) as the initial
        order diminishes - it may drop very quickly.  */

        if (high - low >= 4 && compare(from[middle-1], from[middle]) <= 0) {
            for (int i = low; i < high; i++) {
                to[i] = from[i];
            }
            return;
        }

        // A normal merge.

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            }
            else {
                to[i] = from[q++];
            }
        }
    }

    public int getColumnCount() {
        return sourceModel.getColumnCount();
    }

    public Class getColumnClass(int columnIndex) {
        return sourceModel.getColumnClass(columnIndex);
    }

    public String getColumnName(int columnIndex) {
        return sourceModel.getColumnName(columnIndex);
    }

    public String getName() {
        return sourceModel.getName();
    }

    public int getRowCount() {
        return sourceModel.getRowCount();
    }

    /**
     * Return the cell value according to the ordered rows. This method will
     * remap the specified row according to the sorted mapping.
     * @param aRow Row offset.
     * @param aColumn Column offset.
     * @return Value of cell.
     */
    public Object getValueAt(int aRow, int aColumn) {
        return sourceModel.getValueAt(indexes[aRow], aColumn);
    }

    /**
     * Get a list of objects from the source model. The row indexes are remapped
     * according to the sorted mappings.
     *
     * @param rows Indexes of rows in the sorted model.
     * @return List of objects.
     */
    public Object getObject(int row) {
        return sourceModel.getObject(indexes[row]);
    }

    /**
     * The mapping only affects the contents of the data rows.
     * Pass all requests to these rows through the mapping array: "indexes".
     * @param aValue New value for cell.
     * @param aRow Row offset.
     * @param aColumn Column offset.
     */
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        sourceModel.setValueAt(aValue, indexes[aRow], aColumn);
    }

    public void update() {
        sourceModel.update();
    }

    /**
     * Sort the table model by the column specified in an particular order.
     * If the specified column does not use a Comparable object then it
     * can not be sorted.
     *
     * @param column Column index of sorted column.
     * @param ascending Sort in the ascending order.
     */
    public void sortByColumn(int column, boolean ascending) {
        sortAscending = ascending;
        sortedColumn = column;

        boolean sorted = sortModel();
        if (sorted) {
            // Resorted without adding rows, i.e. updated
            fireTableRowsUpdated(0, sourceModel.getRowCount());
        }
    }

    /**
     * This method sorts the table according to the defined settings.
     * It recreates the row mappings.
     *
     * @return Return whether a reordering has been performed
     */
    private boolean sortModel() {
        int original[] = (int [])indexes.clone();
        reallocateIndexes();

        // Only do sorting if cells contians Comparable
        Class cellType = sourceModel.getColumnClass(sortedColumn);
        if (Comparable.class.isAssignableFrom(cellType)) {

            // Different sorting methods
            n2sort();
            //shuttlesort(original, indexes, 0, indexes.length);
        }

        // Compare the results to see if anything has changed, aims to
        // minimise full refresh events
        boolean sorted = !Arrays.equals(original, indexes);
        if (sorted) {
            System.out.println("Resorted " + sourceModel.getName() +
                               " on " + sortedColumn);
        }

        return sorted;
    }

    private void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }
}
