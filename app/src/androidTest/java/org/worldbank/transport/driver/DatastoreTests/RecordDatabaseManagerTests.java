package org.worldbank.transport.driver.DatastoreTests;

import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import org.worldbank.transport.driver.datastore.DriverRecordContract;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;

import java.util.Date;

/**
 * Test the database manager in isolation.
 *
 * Created by kathrynkillebrew on 1/7/16.
 */
public class RecordDatabaseManagerTests extends AndroidTestCase {

    RecordDatabaseManager manager;
    DriverConstantFields someConstants;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new RecordDatabaseManager((new MockContext()), true);
        someConstants = new DriverConstantFields();
        someConstants.occurredFrom = new Date();
    }

    @SmallTest
    public void testGetNonExtantRecord() {
        String result = manager.getSerializedRecordWithId(1);
        assertNull("Found unexpected record", result);
    }

    @SmallTest
    public void testAddRecord() {
        long id = manager.addRecord("someschema", "somedata", someConstants);
        assertEquals("Unexpected ID for added record", 1, id);
    }

    @SmallTest
    public void testGetRecords() {
        long idThingOne = manager.addRecord("someschema", "someone", someConstants);
        long idThingTwo = manager.addRecord("someschema", "sometwo", someConstants);

        String result = manager.getSerializedRecordWithId(idThingOne);
        assertEquals("Did not get expected data for record", "someone", result);

        result = manager.getSerializedRecordWithId(idThingTwo);
        assertEquals("Did not get expected data for record", "sometwo", result);
    }

    @SmallTest
    public void testUpdateRecord() {
        long id = manager.addRecord("someschema", "somedata", someConstants);
        manager.updateRecord("something completely different", someConstants, id);

        String result = manager.getSerializedRecordWithId(id);
        assertEquals("Unexpected result for updated record", "something completely different", result);
    }

    @MediumTest
    public void testGetAllRecords() {
        manager.addRecord("schema", "I have data", someConstants);

        // wait before adding second record, so their timestamps will differ
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        manager.addRecord("schema", "me too", someConstants);

        Cursor cursor = manager.readAllRecords();

        assertEquals("Unexpected number of records found", 2, cursor.getCount());

        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(DriverRecordContract.RecordEntry.COLUMN_DATA);
        String foundData = cursor.getString(idx);

        // should have second record first, if results are sorted by date descending
        assertEquals("Unexpected record data found as first record in set", "me too", foundData);

        cursor.close();
    }
}
