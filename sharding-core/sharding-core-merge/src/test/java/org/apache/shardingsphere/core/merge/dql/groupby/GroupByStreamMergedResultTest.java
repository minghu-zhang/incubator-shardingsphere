/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.merge.dql.groupby;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.core.database.DatabaseTypes;
import org.apache.shardingsphere.core.execute.sql.execute.result.QueryResult;
import org.apache.shardingsphere.core.merge.MergedResult;
import org.apache.shardingsphere.core.merge.dql.DQLMergeEngine;
import org.apache.shardingsphere.core.merge.fixture.ResultSetBasedQueryResultFixture;
import org.apache.shardingsphere.sql.parser.core.constant.AggregationType;
import org.apache.shardingsphere.sql.parser.core.constant.OrderDirection;
import org.apache.shardingsphere.sql.parser.relation.segment.select.groupby.GroupByContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByItem;
import org.apache.shardingsphere.sql.parser.relation.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.Projection;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.impl.AggregationProjection;
import org.apache.shardingsphere.sql.parser.relation.statement.impl.SelectSQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class GroupByStreamMergedResultTest {
    
    private DQLMergeEngine mergeEngine;
    
    private List<ResultSet> resultSets;
    
    private List<QueryResult> queryResults;
    
    private SelectSQLStatementContext selectSQLStatementContext;
    
    @Before
    public void setUp() throws SQLException {
        resultSets = Lists.newArrayList(mockResultSet(), mockResultSet(), mockResultSet());
        queryResults = new ArrayList<>(resultSets.size());
        for (ResultSet each : resultSets) {
            queryResults.add(new ResultSetBasedQueryResultFixture(each));
        }
        AggregationProjection aggregationSelectItem1 = new AggregationProjection(AggregationType.COUNT, "(*)", null);
        aggregationSelectItem1.setIndex(1);
        AggregationProjection aggregationSelectItem2 = new AggregationProjection(AggregationType.AVG, "(num)", null);
        aggregationSelectItem2.setIndex(2);
        AggregationProjection derivedAggregationSelectItem1 = new AggregationProjection(AggregationType.COUNT, "(num)", "AVG_DERIVED_COUNT_0");
        aggregationSelectItem2.setIndex(5);
        aggregationSelectItem2.getDerivedAggregationProjections().add(derivedAggregationSelectItem1);
        AggregationProjection derivedAggregationSelectItem2 = new AggregationProjection(AggregationType.SUM, "(num)", "AVG_DERIVED_SUM_0");
        aggregationSelectItem2.setIndex(6);
        aggregationSelectItem2.getDerivedAggregationProjections().add(derivedAggregationSelectItem2);
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Arrays.<Projection>asList(aggregationSelectItem1, aggregationSelectItem2), Collections.<String>emptyList());
        selectSQLStatementContext = new SelectSQLStatementContext(new SelectStatement(), 
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 3, OrderDirection.ASC, OrderDirection.ASC))), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 3, OrderDirection.ASC, OrderDirection.ASC))), false),
                projectionsContext, new PaginationContext(null, null, Collections.emptyList()));
    }
    
    private ResultSet mockResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(result.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(6);
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("COUNT(*)");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("AVG(num)");
        when(resultSetMetaData.getColumnLabel(3)).thenReturn("id");
        when(resultSetMetaData.getColumnLabel(4)).thenReturn("date");
        when(resultSetMetaData.getColumnLabel(5)).thenReturn("AVG_DERIVED_COUNT_0");
        when(resultSetMetaData.getColumnLabel(6)).thenReturn("AVG_DERIVED_SUM_0");
        return result;
    }
    
    @Test
    public void assertNextForResultSetsAllEmpty() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), selectSQLStatementContext, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForSomeResultSetsEmpty() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), selectSQLStatementContext, queryResults);
        when(resultSets.get(0).next()).thenReturn(true, false);
        when(resultSets.get(0).getObject(1)).thenReturn(20);
        when(resultSets.get(0).getObject(2)).thenReturn(0);
        when(resultSets.get(0).getObject(3)).thenReturn(2);
        when(resultSets.get(0).getObject(4)).thenReturn(new Date(0L));
        when(resultSets.get(0).getObject(5)).thenReturn(2);
        when(resultSets.get(0).getObject(6)).thenReturn(20);
        when(resultSets.get(2).next()).thenReturn(true, true, false);
        when(resultSets.get(2).getObject(1)).thenReturn(20, 30);
        when(resultSets.get(2).getObject(2)).thenReturn(0);
        when(resultSets.get(2).getObject(3)).thenReturn(2, 2, 3);
        when(resultSets.get(2).getObject(4)).thenReturn(new Date(0L));
        when(resultSets.get(2).getObject(5)).thenReturn(2, 2, 3);
        when(resultSets.get(2).getObject(6)).thenReturn(20, 20, 30);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertThat((BigDecimal) actual.getValue(1, Object.class), is(new BigDecimal(40)));
        assertThat(((BigDecimal) actual.getValue(2, Object.class)).intValue(), is(10));
        assertThat((Integer) actual.getValue(3, Object.class), is(2));
        assertThat((Date) actual.getCalendarValue(4, Date.class, Calendar.getInstance()), is(new Date(0L)));
        assertThat((BigDecimal) actual.getValue(5, Object.class), is(new BigDecimal(4)));
        assertThat((BigDecimal) actual.getValue(6, Object.class), is(new BigDecimal(40)));
        assertTrue(actual.next());
        assertThat((BigDecimal) actual.getValue(1, Object.class), is(new BigDecimal(30)));
        assertThat(((BigDecimal) actual.getValue(2, Object.class)).intValue(), is(10));
        assertThat((Integer) actual.getValue(3, Object.class), is(3));
        assertThat((Date) actual.getCalendarValue(4, Date.class, Calendar.getInstance()), is(new Date(0L)));
        assertThat((BigDecimal) actual.getValue(5, Object.class), is(new BigDecimal(3)));
        assertThat((BigDecimal) actual.getValue(6, Object.class), is(new BigDecimal(30)));
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForMix() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), selectSQLStatementContext, queryResults);
        when(resultSets.get(0).next()).thenReturn(true, false);
        when(resultSets.get(0).getObject(1)).thenReturn(20);
        when(resultSets.get(0).getObject(2)).thenReturn(0);
        when(resultSets.get(0).getObject(3)).thenReturn(2);
        when(resultSets.get(0).getObject(5)).thenReturn(2);
        when(resultSets.get(0).getObject(6)).thenReturn(20);
        when(resultSets.get(1).next()).thenReturn(true, true, true, false);
        when(resultSets.get(1).getObject(1)).thenReturn(20, 30, 30, 40);
        when(resultSets.get(1).getObject(2)).thenReturn(0);
        when(resultSets.get(1).getObject(3)).thenReturn(2, 2, 3, 3, 3, 4);
        when(resultSets.get(1).getObject(5)).thenReturn(2, 2, 3, 3, 3, 4);
        when(resultSets.get(1).getObject(6)).thenReturn(20, 20, 30, 30, 30, 40);
        when(resultSets.get(2).next()).thenReturn(true, true, false);
        when(resultSets.get(2).getObject(1)).thenReturn(10, 30);
        when(resultSets.get(2).getObject(2)).thenReturn(10);
        when(resultSets.get(2).getObject(3)).thenReturn(1, 1, 1, 1, 3);
        when(resultSets.get(2).getObject(5)).thenReturn(1, 1, 3);
        when(resultSets.get(2).getObject(6)).thenReturn(10, 10, 30);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertThat((BigDecimal) actual.getValue(1, Object.class), is(new BigDecimal(10)));
        assertThat(((BigDecimal) actual.getValue(2, Object.class)).intValue(), is(10));
        assertThat((Integer) actual.getValue(3, Object.class), is(1));
        assertThat((BigDecimal) actual.getValue(5, Object.class), is(new BigDecimal(1)));
        assertThat((BigDecimal) actual.getValue(6, Object.class), is(new BigDecimal(10)));
        assertTrue(actual.next());
        assertThat((BigDecimal) actual.getValue(1, Object.class), is(new BigDecimal(40)));
        assertThat(((BigDecimal) actual.getValue(2, Object.class)).intValue(), is(10));
        assertThat((Integer) actual.getValue(3, Object.class), is(2));
        assertThat((BigDecimal) actual.getValue(5, Object.class), is(new BigDecimal(4)));
        assertThat((BigDecimal) actual.getValue(6, Object.class), is(new BigDecimal(40)));
        assertTrue(actual.next());
        assertThat((BigDecimal) actual.getValue(1, Object.class), is(new BigDecimal(60)));
        assertThat(((BigDecimal) actual.getValue(2, Object.class)).intValue(), is(10));
        assertThat((Integer) actual.getValue(3, Object.class), is(3));
        assertThat((BigDecimal) actual.getValue(5, Object.class), is(new BigDecimal(6)));
        assertThat((BigDecimal) actual.getValue(6, Object.class), is(new BigDecimal(60)));
        assertTrue(actual.next());
        assertThat((BigDecimal) actual.getValue(1, Object.class), is(new BigDecimal(40)));
        assertThat(((BigDecimal) actual.getValue(2, Object.class)).intValue(), is(10));
        assertThat((Integer) actual.getValue(3, Object.class), is(4));
        assertThat((BigDecimal) actual.getValue(5, Object.class), is(new BigDecimal(4)));
        assertThat((BigDecimal) actual.getValue(6, Object.class), is(new BigDecimal(40)));
        assertFalse(actual.next());
    }
}
