package com.dspot.declex.test.model.localdbmodels;

import com.activeandroid.Model;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.dspot.declex.annotation.LocalDBModel;
import com.dspot.declex.test.model.localdbmodels.model.Expense_;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import java.lang.NoSuchMethodException;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = "app/src/main/AndroidManifest.xml",
        sdk = 25
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.powermock.*"})
@PrepareForTest({Expense_.class, LocalDBModel.class, From.class})
public class TestLocalDbModels {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testMockLocalDBModelsAnnotation() {
        LocalDBModel annotation = mock(LocalDBModel.class);

        when(annotation.hasTable()).thenReturn(true);
        when(annotation.custom()).thenReturn(true);
        when(annotation.ignorePut()).thenReturn(false);
        when(annotation.table()).thenReturn("expenses");

        assertFalse(annotation.ignorePut());
        assertTrue(annotation.hasTable());
        assertEquals("expenses", annotation.table());

        verify(annotation).ignorePut();
        verify(annotation).hasTable();
        verify(annotation).table();
    }

    @Test
    public void testMockClassFrom() {
        List<Model> models = new ArrayList<>();

        From exeQuery = mock(From.class);
        when(exeQuery.execute()).thenReturn(models);
        exeQuery.execute();
        verify(exeQuery).execute();

        From exeQueryDelete = mock(new Delete().from(Expense_.class).getClass());
        when(exeQueryDelete.execute()).thenReturn(models);
        exeQueryDelete.execute();
        verify(exeQueryDelete).execute();
    }

    @Test
    public void testCreateQuery() {
        Map<String, Object> args = new HashMap();
        args.put("query", "amount>34.23");
        args.put("orderBy", "user_id ASC");

        String query = Expense_.getLocalDBModelQueryDefault();
        if (args.containsKey("query")) query = (String) args.get("query");

        String orderBy = Expense_.getLocalDBModelQueryDefault();
        if (args.containsKey("orderBy")) orderBy = (String) args.get("orderBy");

        assertEquals("amount>34.23", query);
        assertEquals("user_id ASC", orderBy);
    }

    @Test
    public void testCreateSelectQuery() {
        AtomicBoolean condition = new AtomicBoolean(false);

        Map<String, Object> args = new HashMap();
        args.put("query", "select user_id from Expense");

        String query = Expense_.getLocalDBModelQueryDefault();
        if (args.containsKey("query")) query = (String) args.get("query");
        if (query.toLowerCase().trim().startsWith("select ")) {
            condition.set(true);
        }

        assertTrue(condition.get());
        assertEquals("select user_id from Expense", query);
    }

    @Test
    public void testCreateDeleteQuery() {
        AtomicBoolean condition = new AtomicBoolean(false);

        Map<String, Object> args = new HashMap();
        args.put("query", "delete where user_id=1");

        String query = Expense_.getLocalDBModelQueryDefault();
        if (args.containsKey("query")) query = (String) args.get("query");
        if (query.toLowerCase().trim().startsWith("delete ")) {
            condition.set(true);
            query = query.substring(7);
        }

        assertTrue(condition.get());
        assertEquals("where user_id=1", query);
    }
}