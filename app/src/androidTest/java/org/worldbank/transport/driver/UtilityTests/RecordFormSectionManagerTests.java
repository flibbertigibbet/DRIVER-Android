package org.worldbank.transport.driver.UtilityTests;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.worldbank.transport.driver.TestModels.TestPerson;
import org.worldbank.transport.driver.activities.RecordFormSectionActivity;
import org.worldbank.transport.driver.activities.RecordItemListActivity;
import org.worldbank.transport.driver.models.AccidentDetails;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.models.Person;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Unit tests form RecordFormSectionManager static methods
 *
 * Created by kathrynkillebrew on 1/7/16.
 */
public class RecordFormSectionManagerTests extends AndroidTestCase {

    private static final String LOG_LABEL = "SectionMgrTests";

    DriverSchema driverSchema;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        driverSchema = new DriverSchema();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SmallTest
    public void testGetActivityClassForSection() {
        // first section is Details
        Class activityClass = RecordFormSectionManager.getActivityClassForSection(0);
        assertEquals("Expected single section form", RecordFormSectionActivity.class, activityClass);

        // second class is Vehicle
        activityClass = RecordFormSectionManager.getActivityClassForSection(1);
        assertEquals("Expected list activity for section with multiple items", RecordItemListActivity.class, activityClass);
    }

    @SmallTest
    public void testGetSectionName() {
        String sectionName = RecordFormSectionManager.getSectionName(0);
        assertEquals("AccidentDetails", sectionName);

        sectionName = RecordFormSectionManager.getSectionName(2);
        assertEquals("Person", sectionName);

        // no fourth section
        sectionName = RecordFormSectionManager.getSectionName(3);
        assertNull("Should not have a name for a non-extant section", sectionName);
    }

    @SmallTest
    public void testGetSectionClass() {
        Class foundClass = RecordFormSectionManager.getSectionClass("Person");
        assertEquals("Did not find expected class for Person", Person.class, foundClass);
    }

    @SmallTest
    public void testSectionHasNext() {
        boolean hasNext = RecordFormSectionManager.sectionHasNext(0);
        assertTrue(hasNext);

        hasNext = RecordFormSectionManager.sectionHasNext(2);
        assertFalse(hasNext);
    }

    @SmallTest
    public void testSectionHasMultiple() {
        boolean multiple = RecordFormSectionManager.sectionHasMultiple(0);
        assertFalse(multiple);

        multiple = RecordFormSectionManager.sectionHasMultiple(1);
        assertTrue(multiple);
    }

    @SmallTest
    public void testFieldForSectionName() {
        Field foundField = RecordFormSectionManager.getFieldForSectionName("Person");
        assertNotNull(foundField);
        assertEquals("Did not find expected Person field", "Person", foundField.getName());
    }

    @SmallTest
    public void testGetPluralTitle() {
        Field foundField = RecordFormSectionManager.getFieldForSectionName("Person");
        String foundPlural = RecordFormSectionManager.getPluralTitle(foundField, "foo");
        assertEquals("Unexpected plural title", "Persons", foundPlural);

        try {
            Field invalidField = TestPerson.class.getField("Injury");
            foundPlural  = RecordFormSectionManager.getPluralTitle(invalidField, "foo");
            assertEquals("Did not get default plural title", foundPlural, "foo");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("Could not test default plural title");
        }
    }

    @SmallTest
    public void testGetSingleTitle() {
        Field foundField = RecordFormSectionManager.getFieldForSectionName("AccidentDetails");
        String foundTitle = RecordFormSectionManager.getSingleTitle(foundField, "foo");
        assertEquals("Unexpected title", "Accident Details", foundTitle);

        try {
            // this field is not a section, and so does not have a title
            Field invalidField = TestPerson.class.getField("Injury");
            foundTitle  = RecordFormSectionManager.getSingleTitle(invalidField, "foo");
            assertEquals("Did not get default plural title", foundTitle, "foo");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("Could not test default plural title");
        }
    }

    @SmallTest
    public void testGetOrCreateSectionObject() {
        // check new section creation
        Field detailsField = RecordFormSectionManager.getFieldForSectionName("AccidentDetails");
        Object obj = RecordFormSectionManager.getOrCreateSectionObject(detailsField, AccidentDetails.class, driverSchema);
        assertNotNull(obj);

        // check fetch of existing section
        driverSchema.AccidentDetails.Description = "blah blah blah";
        obj = RecordFormSectionManager.getOrCreateSectionObject(detailsField, AccidentDetails.class, driverSchema);
        assertNotNull(obj);
        assertEquals(obj.getClass(), AccidentDetails.class);
        AccidentDetails details = (AccidentDetails) obj;
        assertEquals(details.Description, "blah blah blah");
    }

    @SmallTest
    public void testGetCreateDeleteListItems() {
        // create new list and add an object to it
        Field foundField = RecordFormSectionManager.getFieldForSectionName("Person");
        Object obj = RecordFormSectionManager.getOrCreateListItem(foundField, Person.class, driverSchema, 0);
        assertNotNull(obj);
        assertEquals(obj.getClass(), Person.class);
        assertEquals(driverSchema.Person.size(), 1);

        // test getting an object that already exists
        Person newPerson = new Person();
        newPerson.Name = "Bob";
        driverSchema.Person.add(newPerson);
        obj = RecordFormSectionManager.getOrCreateListItem(foundField, Person.class, driverSchema, 1);
        assertNotNull(obj);
        assertEquals(obj.getClass(), Person.class);
        Person gotPerson = (Person) obj;
        assertEquals("Unexpected Person returned from list", gotPerson, newPerson);
        assertEquals("Unexpected name for Person returned from list", gotPerson.Name, "Bob");

        // delete first person
        boolean didItWork = RecordFormSectionManager.deleteListItem(foundField, Person.class, driverSchema, 0);
        assertTrue(didItWork);
        assertEquals(driverSchema.Person.size(), 1);
        obj = RecordFormSectionManager.getOrCreateListItem(foundField, Person.class, driverSchema, 0);

        // first person should now be Bob
        assertNotNull(obj);
        assertEquals(obj.getClass(), Person.class);
        gotPerson = (Person) obj;
        assertEquals("Unexpected name for Person returned from list after deletion", gotPerson.Name, "Bob");
    }

    @SmallTest
    public void testGetSectionList() {
        Person personOne = new Person();
        personOne.Name = "ThingOne";
        driverSchema.Person.add(personOne);

        Person personTwo = new Person();
        personTwo.Name = "Thing Two";
        driverSchema.Person.add(personTwo);

        ArrayList gotPeople = RecordFormSectionManager.getSectionList(driverSchema.Person);
        assertEquals(gotPeople, driverSchema.Person);

        ArrayList gotVehicles = RecordFormSectionManager.getSectionList(driverSchema.Vehicle);
        assertEquals(gotVehicles.size(), 0);
    }
}